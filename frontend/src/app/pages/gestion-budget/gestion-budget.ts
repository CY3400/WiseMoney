import { Component, OnInit } from '@angular/core';
import { Api, CategoryDto, GestionBudgetDto, GestionBudgetRequest } from '../../services/api';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Common } from '../../services/common';
import { MatSnackBar } from '@angular/material/snack-bar';
import { forkJoin } from 'rxjs';

type Id = number | string;

interface Row {
  id: Id;
  categoryId: Id;
  amount: string;
  allocationType: 'LBP' | '%';

  editAmount: string;
  editType: 'LBP' | '%';
  saving: boolean;

  categoryLabel: string;
  parentId: Id | null;
  parentLabel: string;
}

@Component({
  selector: 'app-gestion-budget',
  imports: [CommonModule, FormsModule],
  templateUrl: './gestion-budget.html',
  styleUrls: ['./gestion-budget.css']
})
export class GestionBudget implements OnInit {
  Math = Math;
  Number = Number;

  pageIndex = 0;
  pageSize = 5;
  pageSizes = [5, 10, 20, 50];

  get total(): number { return this.totalGroups; }
  get totalGroups(): number {return this.groups.length;};
  get totalPages(): number { return Math.max(1, Math.ceil(this.totalGroups / this.pageSize)); }
  get pagedGroups(): Array<{ parentId: Id | null; parentLabel: string; items: Row[] }> {
    const start = this.pageIndex * this.pageSize;
    return this.groups.slice(start, start + this.pageSize);
  }

  goToPage(i: number) {
    this.pageIndex = Math.min(Math.max(0, i), this.totalPages - 1);
  }

  next() { this.goToPage(this.pageIndex + 1); }
  prev() { this.goToPage(this.pageIndex - 1); }

  setPageSize(n: number) {
    this.pageSize = n;
    this.goToPage(0);
  }

  rows: Row[] = [];
  groups: Array<{parentId: Id | null; parentLabel: string; items: Row[]}> = [];

  trackByGroup = (_: number, g: {parentId: Id | null }) => (g.parentId ?? 'root');
  trackByCat = (_: number, r: Row) => r.id;

  constructor(public common: Common, private api: Api, private snack: MatSnackBar) {}

  private categories: CategoryDto[] = [];

  ngOnInit(): void {
      forkJoin({
        cats: this.api.getCategories(),
        mgmt: this.api.getBudgetManagement()
      }).subscribe({
        next: ({ cats, mgmt}) => {
          this.categories = cats ?? [];

          const byId = new Map<Id, CategoryDto>(this.categories.map(c => [c.id, c]));
          
          const getCatId = (catField: any): Id => (catField && typeof catField === 'object') ? (catField.id ?? '') : catField;

          const getCatName = (catField: any, fallback?: string): string => (catField && typeof catField === 'object' && catField.name) ? String(catField.name) : (fallback ?? '—');

          const pickAllocation = (x: any): 'LBP' | '%' => {
            const raw =
              x?.typeAllocation ??
              x?.type_allocation ??
              x?.allocationType ??
              x?.allocation ??
              x?.type;

              console.log(raw)

            const val = String(raw ?? '').trim().toUpperCase();
            if (val === 'PERCENT' || val === '%') return '%';
            if (val === 'LBP') return 'LBP';
            return 'LBP';
          };

          const pickAmount = (x: any): string => String(x?.amount ?? '');

          const pickCategoryField = (x: any) => x?.category ?? x?.categoryId ?? x?.category_id;

          this.rows = (mgmt ?? []).map((item: any) => {
            const catField = pickCategoryField(item);
            const catId = getCatId(catField);
            const cat = byId.get(catId);
            const parentId = cat?.parentId ?? null;
            const parent = parentId ? byId.get(parentId) : null;

            const allocationType = pickAllocation(item);
            const amount = pickAmount(item);

            const row: Row = {
              id: item?.id ?? item?.Id ?? crypto.randomUUID(),
              categoryId: catId,
              amount,
              allocationType,
              editAmount: amount,
              editType: allocationType,
              saving: false,
              categoryLabel: getCatName(catField, cat?.name ?? String(catId)),
              parentId,
              parentLabel: parent?.name ?? (parentId ? '—' : (cat?.name ?? '—')),
            };
            return row;
          });

          this.buildGroups();
          this.goToPage(0);
        },
        error: () => this.snack.open('Échec de chargement des données', '✖', {
          duration: 3000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['custom-toast']
        })
    })
  }

  private buildGroups() {
    const groupsMap = new Map<string, { parentId: Id | null; parentLabel: string; items: Row[] }>();

    for (const r of this.rows) {
      const isParent = r.parentId == null;
      const groupKey = isParent ? String(r.categoryId) : String(r.parentId);
      const groupLabel = isParent ? r.categoryLabel : r.parentLabel || '-';
      
      if (!groupsMap.has(groupKey)) {
        groupsMap.set(groupKey, { parentId: isParent ? r.categoryId : r.parentId, parentLabel: groupLabel, items: [] });
      }
      groupsMap.get(groupKey)!.items.push(r);
    }

    const arr = Array.from(groupsMap.values()).map(g => {
      g.items.sort((a, b) => a.categoryLabel.localeCompare(b.categoryLabel));
      return g;
    });

    arr.sort((a, b) => a.parentLabel.localeCompare(b.parentLabel));

    this.groups = arr;
  }

  private toNumber(v: any): number {
    const n = Number(String(v).replace(',', '.'));
    return Number.isFinite(n) ? n : NaN;
  }

  isRowDirty(r: Row): boolean {
    return r.editType !== r.allocationType || r.editAmount !== r.amount;
  }

  isRowValid(r: Row): boolean {
    const amt = this.toNumber(r.editAmount);
    if (isNaN(amt) || r.editAmount === '' || amt < 0) return false;
    if (r.editType === '%' && amt > 100) return false;
    return true;
  }

  saveRow(r: Row) {
    if (!this.isRowDirty(r) || !this.isRowValid(r)) return;

    r.saving = true;
    const payload: GestionBudgetRequest = {
      category: r.categoryId,
      amount: r.editAmount,
      type: r.editType === '%' ? 'PERCENT' : 'LBP'
    };

    this.api.updateBudgetManagement(r.id, payload).subscribe({
      next: () => {
        r.saving = false;
        r.amount = r.editAmount;
        r.allocationType = r.editType;
        this.snack.open('Modification enregistrée ✅', '✖', {
          duration: 3000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['custom-toast']
        });
      },
      error: () => {
        r.saving = false;
        this.snack.open('Erreur lors de la modification', '✖', {
          duration: 3000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['custom-toast']
        });
      }
    });
  }
}
