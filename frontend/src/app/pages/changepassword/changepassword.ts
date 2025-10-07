import { Component } from '@angular/core';
import { Common } from '../../services/common';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { Validators } from '../../services/validators';
import { Api } from '../../services/api';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs';
import { Header } from '../header/header';

@Component({
  selector: 'app-changepassword',
  imports: [CommonModule, FormsModule],
  templateUrl: './changepassword.html',
  styleUrls: ['./changepassword.css']
})
export class Changepassword {
  protected hasLetter = Validators.hasLetter;
  protected passwordRegex = Validators.passwordRegex;

  isSubmitting = false;

  errors = {
    actualPassword: '',
    newPassword: '',
    global: ''
  };

  user = {
    actualPassword: '',
    newPassword: ''
  };

  showActualPassword = false;
  showNewPassword = false;

  constructor(public common: Common, private api: Api, private router: Router, private snackBar: MatSnackBar){}

  hasErrors(): boolean {
    return this.common.hasErrors(this.errors);
  }

  toggleActualPassword(): void {
    this.showActualPassword = this.common.toggleAndGetVisibility('actual-password');
  }

  toggleNewPassword(): void {
    this.showNewPassword = this.common.toggleAndGetVisibility('new-password');
  }

  async onSubmit(form: NgForm): Promise<void> {
    const { actualPassword, newPassword } = this.user;
    let isValid = true;

    this.errors.actualPassword = '';
    this.errors.newPassword = '';
    this.errors.global = '';

    if (!actualPassword || !actualPassword.trim()) {
      this.errors.actualPassword = "Le mot de passe actuel est requis";
      isValid = false;
    }

    if (!this.common.isValid(newPassword, this.passwordRegex, this.hasLetter)) {
      this.errors.newPassword = newPassword ? "Le nouveau mot de passe doit avoir entre 8 et 20 caractères, avec majuscule, minuscule et un caractère spécial" : "Le nouveau mot de passe ne peut pas être vide";
      isValid = false;
    } else {
      this.errors.newPassword = '';
    }

    if (!isValid) return;

    this.isSubmitting = true;

    this.api.changePassword(this.user).pipe(finalize(() => { this.isSubmitting = false; })).subscribe({
      next: () => {
        this.errors.actualPassword = '';
        this.snackBar.open('Mot de passe changé avec succès.', '✖', { duration: 3000, horizontalPosition: 'right', verticalPosition: 'top', panelClass: ['custom-toast'] });
        this.router.navigate(['/acceuil']);
      },
      error: (err) => {
        if(err.status === 400 || err.status === 401){
          const msg = err?.error?.message;
          if(msg?.toLowerCase().includes('actuel')){
            this.errors.actualPassword = msg;
          }
          else if (msg) {
            this.errors.global = msg;
          }
          else {
            this.errors.global = "Requête invalide.";
          }
        }
        else {
          this.errors.global = "Une erreur est survenue. Réessayez plus tard.";
        }
      }
    })
  }
}