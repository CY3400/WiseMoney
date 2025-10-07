import { Component, OnInit } from '@angular/core';
import { Validators } from '../../services/validators';
import { Common } from '../../services/common';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { Api } from '../../services/api';
import { finalize } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';
import type { Budget as BudgetDto } from '../../services/api';

@Component({
  selector: 'app-budget',
  imports: [CommonModule, FormsModule],
  templateUrl: './budget.html',
  styleUrls: ['./budget.css']
})
export class Budget implements OnInit {
  protected allowedKeys = Validators.allowedKeys;
  protected numberRegex = Validators.numberRegex;

  isSubmitting = false;
  total = 0;

  now = new Date();
  month = this.now.getMonth() + 1;
  year = this.now.getFullYear();

  currentBudgetId: number | string | null = null;

  errors = { amount: '' };

  budget = {
    amount: 0
  };

  constructor(public common: Common, private api: Api, private snack: MatSnackBar) {}

  hasErrors(): boolean {
    return this.common.hasErrors(this.errors);
  }

  ngOnInit(): void {
    this.api.getBudget().subscribe({
      next: (list: BudgetDto[]) => {
        this.total = (list ?? []).reduce((sum, b) => {
          const raw = b?.amount as unknown;
          const n =
            typeof raw === 'number'
              ? raw
              : parseFloat(String(raw).replace(',', '.'));
          return sum + (Number.isFinite(n) ? n : 0);
        }, 0);

        const chosen = (list ?? [])[0];
        this.currentBudgetId = chosen?.id ?? null;

        this.budget.amount = this.total > 0 ? this.total : 0;
      },
      error: () => {
        this.total = 0;
        this.currentBudgetId = null;
        this.budget.amount = 0;
      }
    });
  }

  onUpdate(form: NgForm): void {
    this.errors.amount = '';

    const amount = this.budget.amount;
    if (!amount || amount == 0) {
      this.errors.amount = 'Le montant du budget ne peut pas être vide';
      return;
    }

    if (!this.currentBudgetId) {
      this.errors.amount = 'Aucun budget à modifier pour ce mois.';
      return;
    }

    this.isSubmitting = true;
    this.api
      .updateBudget(this.currentBudgetId, { amount })
      .pipe(finalize(() => (this.isSubmitting = false)))
      .subscribe({
        next: () => {
          this.snack.open('Budget mis à jour ✅', '✖', {
            duration: 3000,
            horizontalPosition: 'right',
            verticalPosition: 'top',
            panelClass: ['custom-toast']
          });
          this.total = amount;
          this.budget.amount = amount;
        },
        error: (err) => {
          this.errors.amount =
            err?.error?.message || 'Erreur lors de la mise à jour du budget';
        }
      });
  }

  async onSubmit(form: NgForm): Promise<void> {
    this.errors.amount = '';

    const amount = this.budget.amount;
    let isValid = true;

    if (!amount || amount == 0) {
      this.errors.amount = 'Le montant du budget ne peut pas être vide';
      isValid = false;
    }

    if (!isValid) return;

    const payload = {
      amount,
      month: this.month,
      year: this.year
    };

    this.isSubmitting = true;

    this.api
      .createBudget(payload)
      .pipe(finalize(() => (this.isSubmitting = false)))
      .subscribe({
        next: (created) => {
          this.snack.open('Budget créé ✅', '✖', {
            duration: 3000,
            horizontalPosition: 'right',
            verticalPosition: 'top',
            panelClass: ['custom-toast']
          });

          this.currentBudgetId = created?.id ?? null;
          this.total = amount;
          this.budget.amount = amount;
        },
        error: (err) => {
          this.errors.amount =
            err?.error?.message || 'Erreur lors de la création du budget';
        }
      });
  }
}
