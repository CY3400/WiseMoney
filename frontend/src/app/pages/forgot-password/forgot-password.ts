import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Api } from '../../services/api';
import { Common } from '../../services/common';

@Component({
  standalone: true,
  imports: [CommonModule, FormsModule],
  styleUrls: ['./forgot-password.css'],
  template: `
    <div class="center-page">
      <div class="main">
        <h2 class="principal_txt">Mot de passe oublié</h2>
        <p class="secondary_txt">Entrez votre adresse email pour recevoir un lien de réinitialisation.</p>
        <form (ngSubmit)="submit()">
          <div>
            <label for="email">Email</label>
            <input autocomplete="off" id="email" [(ngModel)]="email" name="email" type="email" class="form-control" />
          </div>
          <div class="actions">
            <button class="primary" type="submit" [disabled]="loading">{{ loading?'Envoi...':'Envoyer le lien' }}</button>
            <button class="secondary" type="button" (click)="common.redirection('/se-connecter')">Annuler</button>
          </div>
        </form>

        <div *ngIf="msg" class="info">{{ msg }}</div>
        <div *ngIf="err" class="error">{{ err }}</div>
      </div>
    </div>
  `
})
export class ForgotPassword {
  email = '';
  loading = false;
  msg = '';
  err = '';
  resetUrl: string | null = null;

  constructor(private api: Api, public common: Common) {}

  submit() {
    this.msg = '';
    this.err = '';
    this.resetUrl = null;
    this.loading = true;

    this.api.forgotPassword(this.email).subscribe({
      next: (r) => {
        this.msg = r.message;
        if (r.resetUrl) {
          this.resetUrl = r.resetUrl;
          console.log('Lien de réinitialisation:', r.resetUrl);
        }
        this.loading = false;
      },
      error: (e) => {
        this.err = e?.error?.message || 'Erreur';
        this.loading = false;
      }
    });
  }
}