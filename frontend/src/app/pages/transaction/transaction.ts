import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { Common } from '../../services/common';
import { Validators } from '../../services/validators';
import { Api, ChildCategoryDto, TransactionDto } from '../../services/api';
import { finalize } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';

type Id = number | string;

interface Option {
  id: Id;
  label: string;
}

interface Row extends TransactionDto {
  editCategory: Id;
  editAmount: string;
  editDate: string;
  editNote: string;
  saving: boolean;

  originalCategory: Id;
  originalAmount: string;
  originalDate: string;
  originalNote: string;
}

@Component({
  selector: 'app-transaction',
  imports: [CommonModule, FormsModule],
  templateUrl: './transaction.html',
  styleUrls: ['./transaction.css']
})
export class Transaction implements OnInit {
  protected allowedKeys = Validators.allowedKeys;
  protected numberRegex = Validators.numberRegex;

  Math = Math;

  pageIndex = 0;
  pageSize = 5;
  pageSizes = [5, 10, 20, 50];

  get total(): number {return this.rows.length;};
  get totalPages(): number { return Math.max(1, Math.ceil(this.total / this.pageSize)); }
  get pagedRows(): Row[] {
    const start = this.pageIndex * this.pageSize;
    return this.rows.slice(start, start + this.pageSize);
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

  errors = {
    global:'',
    category: '',
    amount: '',
    transactionDate:'',
    notes:''
  }

  transaction: {category: Id | null; amount: string, transactionDate: string, notes: string} = {
    category: null,
    amount: '',
    transactionDate:'',
    notes:''
  }

  rows: Row[] = [];

  constructor(public common: Common, private api: Api, private snack: MatSnackBar){}

  childOptions: Option[] = [];
  selectedChildId: Id | null = null;
  trackById = (_: number, o: { id: number | string }) => o.id;
  loadingChildren = false;
  childrenErr = '';
  isSubmitting = false;

  ngOnInit(): void {
    this.loadChildrenOptions();
    this.loadTransactions();
  }

  getCategoryLabel(id: Id): string {
    const o = this.childOptions.find(x => x.id === id);
    return o ? o.label : String(id);
  }

  private toDateOnly(s: string): string {
    return s ? s.slice(0, 10) : '';
  }

  private loadTransactions(): void {
    this.api.getTransactions().subscribe({
      next: (list: any[]) => {
        this.rows = (list ?? []).map((c: any) => {
          const date = c.date || c.transactionDate || '';
          const categoryId = this.extractId(c.category);
          const dOnly = this.toDateOnly(date);
          const amt = String(c.amount ?? '');

          return {
            id: c.id,
            category: categoryId,
            amount: amt,
            date,
            notes: c.notes ?? '',
            editCategory: categoryId,
            editAmount: amt,
            editDate: dOnly,
            editNote: c.notes ?? '',
            saving: false,

            originalCategory: categoryId,
            originalAmount: amt,
            originalDate: dOnly,
            originalNote: c.notes ?? ''
          } as Row;
        });

        this.goToPage(this.pageIndex);
      },
      error:() => {
        this.rows = [];
      }
    });
  }
  
  hasErrors(): boolean {
    return this.common.hasErrors(this.errors);
  }

  hasChanged(r: Row): boolean {
    const equalCat = r.editCategory === r.originalCategory;
    const equalAmt = Number(r.editAmount || 0) === Number(r.originalAmount || 0);
    const equalDate = (r.editDate || '').trim() === (r.originalDate || '').trim();
    const equalNote = (r.editNote || '').trim() === (r.originalNote || '').trim();
    return !(equalCat && equalAmt && equalDate && equalNote);
  }

  private extractId(val: unknown): Id {
    return (val && typeof val === 'object' && 'id' in (val as any))
      ? ((val as any).id as Id)
      : (val as Id);
  }

  private loadChildrenOptions(): void {
    this.loadingChildren = true;
    this.childrenErr = '';

    this.api.getCategoryChildren().subscribe({
      next:(list: ChildCategoryDto[]) => {
        this.childOptions = (list ?? []).map(c => ({
          id: c.id,
          label: c.name
        }));

        if(this.transaction.category && !this.childOptions.some(o => o.id === this.transaction.category)) {
          this.transaction.category = null;
          this.selectedChildId = null;
        }

        this.loadingChildren = false;
      },
      error: (e) => {
        this.childrenErr = e?.error?.message || 'Erreur de chargement des catégories enfants.';
        this.childOptions = [];
        this.transaction.category = null;
        this.selectedChildId = null;
        this.loadingChildren = false;
      }
    })
  }

  async onSubmit(form: NgForm): Promise<void> {
    this.errors.global = '';
    this.errors.category = '';
    this.errors.amount = '';
    this.errors.transactionDate = '';
    this.errors.notes = '';

    const category = this.transaction.category;
    const amount = this.transaction.amount;
    const transactionDate = this.transaction.transactionDate;
    const notes = this.transaction.notes || '';
    let isValid = true;

    if(!category) {
      this.errors.category = 'La catégorie ne peut pas être vide';
      isValid = false;
    }

    if(!amount) {
      this.errors.amount = 'Le montant ne peut pas être vide';
    }
    else {
      const n = Number(amount);
      if (n <= 0) {
        this.errors.amount = 'Le montant doit être plus grand que 0';
        isValid = false;
      }
    }

    if(!transactionDate) {
      this.errors.transactionDate = 'La date ne doit pas être vide';
      isValid = false;
    }

    if(!isValid) return;

    const payload = {
      category: category as Id,
      amount,
      transactionDate,
      notes
    };

    this.isSubmitting = true;

    this.api.createTransaction(payload).pipe(finalize(() => (this.isSubmitting = false))).subscribe({
      next: () => {
        form.reset({category: null, amount: '', transactionDate: null, notes: ''});

        this.loadTransactions();

        this.snack.open('Transaction ajoutée ✅', '✖', {
          duration: 3000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['custom-toast']
        });
      },
      error: (err) => {
        this.errors.global = err?.error?.message || 'Erreur lors de l\'ajout de la transaction';
      }
    })
  }

  modify(row: Row): void {
    const cat = row.editCategory;
    const amount = row.editAmount;
    const date = row.editDate;
    const notes = row.editNote;

    if(!amount || Number(amount) <= 0 || !date) return;

    row.saving = true;
    this.api.updateTransaction(row.id, {category: cat, amount: amount, transactionDate: date, notes: notes}).subscribe({
      next: (updated) => {
        row.category = this.extractId(updated.category);
        row.amount = String(updated.amount ?? amount);
        row.date = updated.date || updated.date || date;
        row.notes = updated.notes ?? notes;

        row.editCategory = row.category;
        row.editAmount = row.amount;
        row.editDate = this.toDateOnly(row.date);
        row.editNote = row.notes ?? '';

        row.originalCategory = row.editCategory;
        row.originalAmount = row.editAmount;
        row.originalDate = row.editDate;
        row.originalNote = row.editNote;

        row.saving = false;
        this.snack.open('Transaction mise à jour ✅', '✖', {
          duration: 3000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['custom-toast']
        });
      },
      error: (err)=> {
        row.saving = false;
        this.errors.global = err?.error?.message || 'Erreur lors de la mise à jour de la transaction';
      }
    })
  }

  delete(id: Id): void {
    this.api.deleteTransaction(id).subscribe({
      next: () => {
        this.loadTransactions();
        
        this.snack.open('Transaction supprimée avec succès ✅', '✖', {
          duration: 3000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['custom-toast']
        });
      }
    })
  }
}
