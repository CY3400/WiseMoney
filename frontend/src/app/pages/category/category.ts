import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { Validators } from '../../services/validators';
import { Common } from '../../services/common';
import { Api } from '../../services/api';
import { ParentCategoryDto, CategoryDto } from '../../services/api';
import { finalize } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';

type CategoryTypeCode = 'DEPENSE' | 'REVENU' | 'Les_2';
type Id = number | string;

interface Option {
  id: Id;
  label: string;
  type: string;
}

interface Row extends CategoryDto {
  editName: string;
  editParentId?: Id | null;
  saving: boolean;
}

const CATEGORY_TYPE_LABELS: Record<string, string> = {
  DEPENSE: 'Dépense',
  REVENU: 'Revenu',
  Les_2: 'Les 2'
};

@Component({
  selector: 'app-category',
  imports: [CommonModule, FormsModule],
  templateUrl: './category.html',
  styleUrls: ['./category.css']
})
export class Category implements OnInit {
  protected allowedKeys = Validators.allowedKeys;
  protected letterRegex = Validators.letterRegex;

  categoryTypeOptions: CategoryTypeCode[] = [];

  parentOptionsForm: Option[] = [];
  parentOptionsGrid: Option[] = [];

  selectedParentId: Id | null = null;

  loadingParents = false;
  parentsErr = '';
  isSubmitting = false;

  Math = Math;

  pageIndex = 0;
  pageSize = 5;
  pageSizes = [5, 10, 20, 50];

  get total(): number {
    return this.totalGroups;
  }

  get totalGroups(): number {
    return this.groups.length;
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.totalGroups / this.pageSize));
  }

  get pagedGroups(): Array<{ parentId: Id | null; parentLabel: string; items: Row[] }> {
    const start = this.pageIndex * this.pageSize;
    return this.groups.slice(start, start + this.pageSize);
  }

  goToPage(i: number) {
    this.pageIndex = Math.min(Math.max(0, i), this.totalPages - 1);
  }

  next() {
    this.goToPage(this.pageIndex + 1);
  }

  prev() {
    this.goToPage(this.pageIndex - 1);
  }

  setPageSize(n: number) {
    this.pageSize = Number(n);
    this.goToPage(0);
  }

  errors = {
    name: '',
    type: '',
    parent: '',
    global: ''
  };

  category: { name: string; type: CategoryTypeCode | null; parentId: Id | null } = {
    name: '',
    type: null,
    parentId: null
  };

  rows: Row[] = [];
  groups: Array<{ parentId: Id | null; parentLabel: string; items: Row[] }> = [];

  constructor(public common: Common, private api: Api, private snack: MatSnackBar) {}

  hasErrors(): boolean {
    return this.common.hasErrors(this.errors);
  }

  ngOnInit(): void {
    this.api.getCategoryTypes().subscribe({
      next: (types) => (this.categoryTypeOptions = (types ?? []) as CategoryTypeCode[]),
      error: () => (this.categoryTypeOptions = [])
    });

    this.loadParentOptionsForm();
    this.loadParentOptionsGrid();
    this.loadCategories();
  }

  private mapParentOptions(list: ParentCategoryDto[]): Option[] {
    return (list ?? []).map(c => ({
      id: c.id,
      label: c.displayName || c.name,
      type: c.type
    }));
  }

  private loadCategories(): void {
    this.api.getCategories().subscribe({
      next: (list: CategoryDto[]) => {
        this.rows = (list ?? []).map(c => ({
          ...c,
          editName: c.name,
          editParentId: c.parentId,
          saving: false
        }));
        this.buildGroups();
        this.goToPage(this.pageIndex);
      },
      error: () => {
        this.rows = [];
        this.groups = [];
      }
    });
  }

  private buildGroups(): void {
    const map = new Map<Id | null, Row[]>();

    for (const r of this.rows) {
      const k = r.parentId ?? null;
      if (!map.has(k)) {
        map.set(k, []);
      }
      map.get(k)!.push(r);
    }

    for (const [, arr] of map) {
      arr.sort((a, b) => a.name.localeCompare(b.name));
    }

    this.groups = Array.from(map.entries()).map(([parentId, items]) => ({
      parentId,
      parentLabel: this.parentLabelOf(parentId),
      items
    }));

    this.groups.sort((a, b) => a.parentLabel.localeCompare(b.parentLabel));
  }

  private parentLabelOf(parentId: Id | null): string {
    if (parentId == null) return 'Parents';

    const cat = this.rows.find(r => r.id === parentId);
    if (cat) return cat.name;

    const p = this.parentOptionsGrid.find(o => o.id === parentId);
    if (p) return p.label;

    return `Parent ${String(parentId)}`;
  }

  private loadParentOptionsForm(type?: CategoryTypeCode | null): void {
    this.loadingParents = true;
    this.parentsErr = '';

    this.api.getCategoryParents(type ?? undefined).subscribe({
      next: (list: ParentCategoryDto[]) => {
        this.parentOptionsForm = this.mapParentOptions(list);

        if (
          this.category.parentId &&
          !this.parentOptionsForm.some(o => o.id === this.category.parentId)
        ) {
          this.category.parentId = null;
          this.selectedParentId = null;
        }

        this.loadingParents = false;
      },
      error: (e) => {
        this.parentsErr = e?.error?.message || 'Erreur de chargement des catégories parent.';
        this.parentOptionsForm = [];
        this.category.parentId = null;
        this.selectedParentId = null;
        this.loadingParents = false;
      }
    });
  }

  private loadParentOptionsGrid(): void {
    this.api.getCategoryParents().subscribe({
      next: (list: ParentCategoryDto[]) => {
        this.parentOptionsGrid = this.mapParentOptions(list);
      },
      error: () => {
        this.parentOptionsGrid = [];
      }
    });
  }

  onTypeChange(type: CategoryTypeCode | null): void {
    this.category.type = type;
    this.category.parentId = null;
    this.selectedParentId = null;
    this.loadParentOptionsForm(type);
  }

  onParentSelected(id: Id | null): void {
    this.selectedParentId = id;
    this.category.parentId = id;
  }

  labelOf(code: string): string {
    if (!code) return '';

    const normalized = code.trim().toUpperCase();

    if (normalized === 'DEPENSE') return 'Dépense';
    if (normalized === 'REVENU') return 'Revenu';
    if (normalized === 'LES_2') return 'Les 2';

    return code;
  }

  isLesDeux(type: string | null | undefined): boolean {
    if (!type) return false;
    const normalized = type.toString().trim().toUpperCase();
    return normalized === 'LES_2';
  }

  saveName(row: Row): void {
    const next = row.editName?.trim() ?? '';
    const nextParentId = row.editParentId ?? null;

    if (
      !next ||
      next.length < 2 ||
      (next.toLocaleLowerCase() === row.name.toLocaleLowerCase() &&
        nextParentId === row.parentId)
    ) {
      return;
    }

    row.saving = true;
    this.errors.global = '';

    this.api.updateCategory(row.id, { name: next, parentId: nextParentId }).subscribe({
      next: (updated) => {
        row.name = updated.name;
        row.parentId = updated.parentId;
        row.editName = updated.name;
        row.editParentId = updated.parentId;
        row.saving = false;

        this.snack.open('Catégorie mise à jour ✅', '✖', {
          duration: 3000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['custom-toast']
        });

        this.loadCategories();
        this.loadParentOptionsForm(this.category.type);
        this.loadParentOptionsGrid();
      },
      error: (err) => {
        row.saving = false;
        this.errors.global = err?.error?.message || 'Erreur lors de la mise à jour de la catégorie';
      }
    });
  }

  toggleStatus(row: Row): void {
    row.saving = true;

    this.api.toggleCategoryStatus(row.id).subscribe({
      next: (updated) => {
        row.status = updated.status;
        row.saving = false;
        this.snack.open(
          updated.status === 1 ? 'Catégorie activée ✅' : 'Catégorie désactivée ✅',
          '✖',
          {
            duration: 3000,
            horizontalPosition: 'right',
            verticalPosition: 'top',
            panelClass: ['custom-toast']
          }
        );
      },
      error: (err) => {
        row.saving = false;
        this.errors.global = err?.error?.message || 'Erreur lors du changement de statut';
      }
    });
  }

  async onSubmit(form: NgForm): Promise<void> {
    this.errors.name = '';
    this.errors.parent = '';
    this.errors.type = '';
    this.errors.global = '';

    const name = this.category.name.trim();
    const parentId = this.category.parentId;
    const type = this.category.type || '';
    let isValid = true;

    if (!name) {
      this.errors.name = 'Le nom de la catégorie ne peut pas être vide';
      isValid = false;
    } else if (name.length < 2) {
      this.errors.name = 'Le nom de la catégorie doit être au minimum de 2 caractères';
      isValid = false;
    }

    if (!type && !parentId) {
      this.errors.global = 'La catégorie ne peut pas avoir de type ET de parent vides';
      isValid = false;
    }

    if (!isValid) return;

    const payload = {
      name,
      type: type || undefined,
      parentId: parentId ?? undefined
    } satisfies import('../../services/api').CreateCategoryRequest;

    this.isSubmitting = true;

    this.api
      .createCategory(payload)
      .pipe(finalize(() => (this.isSubmitting = false)))
      .subscribe({
        next: (createdCategory) => {
          this.snack.open('Catégorie créée ✅', '✖', {
            duration: 3000,
            horizontalPosition: 'right',
            verticalPosition: 'top',
            panelClass: ['custom-toast']
          });

          if (parentId && type === 'DEPENSE') {
            const budgetPayload = {
              category: createdCategory.id,
              amount: '0',
              type: 'LBP',
              frequency: 1
            } satisfies import('../../services/api').GestionBudgetRequest;

            this.api.createBudgetManagement(budgetPayload).subscribe({
              next: () => console.log('Budget management créé (catégorie parente)'),
              error: (err) => {
                console.error('Erreur createBudgetManagement', err);
                this.snack.open('⚠️ Budget non créé automatiquement', '✖', {
                  duration: 3000,
                  horizontalPosition: 'right',
                  verticalPosition: 'top',
                  panelClass: ['custom-toast']
                });
              }
            });
          }

          form.resetForm({
            name: '',
            type: null,
            parentId: null
          });

          this.category = {
            name: '',
            type: null,
            parentId: null
          };

          this.selectedParentId = null;

          this.loadCategories();
          this.loadParentOptionsForm();
          this.loadParentOptionsGrid();
        },
        error: (err) => {
          this.errors.global = err?.error?.message || 'Erreur lors de la création de la catégorie';
        }
      });
  }
}