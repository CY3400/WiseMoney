// src/app/pages/reset-password/reset-password.ts
import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Api } from '../../services/api';
import { Validators } from '../../services/validators';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Common } from '../../services/common';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="center-page">
      <div class="main">
        <h2>Réinitialiser le mot de passe</h2>

        <div class="error border-danger" *ngIf="hasErrors()">
          <span *ngIf="errors.global" class="text-danger">{{ errors.global }}</span>
          <span *ngIf="errors.newPassword" class="text-danger">{{ errors.newPassword }}</span>
          <span *ngIf="errors.confirm" class="text-danger">{{ errors.confirm }}</span>
        </div>

        <form #form="ngForm" (ngSubmit)="onSubmit(form)">
          <div class="rd4">
            <label for="New_Password" [class.text-danger]="errors.newPassword">Nouveau mot de passe</label>
            <input
              id="New_Password"
              name="newPassword"
              [type]="showNew ? 'text' : 'password'"
              class="form-control"
              [(ngModel)]="model.newPassword"
              [disabled]="isSubmitting"
              (paste)="common.noPaste($event)"
            />

            <div class="password-show" *ngIf="!showNew" (click)="showNew = true">
              <!-- oeil - afficher -->
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path stroke="currentColor" fill="none" stroke-width="1.5" d="M2.036 12.322a1.012 1.012 0 0 1 0-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178Z"/><path stroke="currentColor" fill="none" stroke-width="1.5" d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z"/></svg>
            </div>
            <div class="password-hide" *ngIf="showNew" (click)="showNew = false">
              <!-- oeil barré - cacher -->
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path stroke="currentColor" fill="none" stroke-width="1.5" d="M3.98 8.223A10.477 10.477 0 0 0 1.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.451 10.451 0 0 1 12 4.5c4.756 0 8.773 3.162 10.065 7.498a10.522 10.522 0 0 1-4.293 5.774M6.228 6.228 3 3m3.228 3.228 3.65 3.65m7.894 7.894L21 21m-3.228-3.228-3.65-3.65m0 0a3 3 0 1 0-4.243-4.243m4.242 4.242L9.88 9.88"/></svg>
            </div>
          </div>

          <div class="rd4">
            <label for="Confirm" [class.text-danger]="errors.confirm">Confirmer le mot de passe</label>
            <input
              id="Confirm"
              name="confirmPassword"
              [type]="showConfirm ? 'text' : 'password'"
              class="form-control"
              [(ngModel)]="confirm"
              [disabled]="isSubmitting"
              (paste)="common.noPaste($event)"
            />

            <div class="password-show" *ngIf="!showConfirm" (click)="showConfirm = true">
              <!-- oeil -->
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path stroke="currentColor" fill="none" stroke-width="1.5" d="M2.036 12.322a1.012 1.012 0 0 1 0-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178Z"/><path stroke="currentColor" fill="none" stroke-width="1.5" d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z"/></svg>
            </div>
            <div class="password-hide" *ngIf="showConfirm" (click)="showConfirm = false">
              <!-- oeil barré -->
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path stroke="currentColor" fill="none" stroke-width="1.5" d="M3.98 8.223A10.477 10.477 0 0 0 1.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.451 10.451 0 0 1 12 4.5c4.756 0 8.773 3.162 10.065 7.498a10.522 10.522 0 0 1-4.293 5.774M6.228 6.228 3 3m3.228 3.228 3.65 3.65m7.894 7.894L21 21m-3.228-3.228-3.65-3.65m0 0a3 3 0 1 0-4.243-4.243m4.242 4.242L9.88 9.88"/></svg>
            </div>
          </div>

          <button class="primary" type="submit" [disabled]="isSubmitting">
            {{ isSubmitting ? 'Veuillez patienter...' : 'Valider' }}
          </button>
        </form>
      </div>
    </div>
  `,
})
export class ResetPassword {
  token: string | null = null;

  // modèle
  model = { newPassword: '' };
  confirm = '';

  // états UI
  isSubmitting = false;
  showNew = false;
  showConfirm = false;

  // erreurs
  errors = { newPassword: '', confirm: '', global: '' };

  constructor(
    private route: ActivatedRoute,
    private api: Api,
    private router: Router,
    private snack: MatSnackBar,
    public common: Common
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token');
    if (!this.token) {
      this.errors.global = 'Lien invalide : token manquant.';
    }
  }

  private valid(): boolean {
    this.errors = { newPassword: '', confirm: '', global: '' };

    const pw = this.model.newPassword?.trim() || '';
    const okPw = Validators.passwordRegex.test(pw) && Validators.hasLetter.test(pw);
    if (!okPw) {
      this.errors.newPassword = pw
        ? "Le mot de passe doit avoir entre 8 et 20 caractères, avec majuscule, minuscule et un caractère spécial"
        : 'Le mot de passe ne peut pas être vide';
    }

    if (pw !== (this.confirm ?? '')) {
      this.errors.confirm = 'Les deux mots de passe ne correspondent pas';
    }

    return !this.errors.newPassword && !this.errors.confirm && !this.errors.global;
  }

  hasErrors(): boolean {
    return !!(this.errors.global || this.errors.newPassword || this.errors.confirm);
  }

  onSubmit(form: NgForm): void {
    if (!this.token) return;
    if (!this.valid()) return;

    this.isSubmitting = true;
    this.api
      .resetPassword({ token: this.token, newPassword: this.model.newPassword.trim() })
      .subscribe({
        next: () => {
          this.isSubmitting = false;
          this.snack.open('Mot de passe réinitialisé ✅', '✖', {
            duration: 3000,
            horizontalPosition: 'right',
            verticalPosition: 'top',
            panelClass: ['custom-toast'],
          });
          this.router.navigate(['/se-connecter']);
        },
        error: (err) => {
          this.isSubmitting = false;
          this.errors.global = err?.error?.message || 'Lien invalide ou expiré.';
        },
      });
  }
}