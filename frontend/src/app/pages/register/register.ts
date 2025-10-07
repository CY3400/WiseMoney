import { Component } from '@angular/core';
import { NgForm, FormsModule } from '@angular/forms';
import { Api } from '../../services/api';
import { Common } from '../../services/common';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Validators } from '../../services/validators';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './register.html',
  styleUrls: ['./register.css','../../../styles.css']
})
export class Register {
  protected allowedKeys = Validators.allowedKeys;
  protected letterRegex = Validators.letterRegex;
  protected hasLetter = Validators.hasLetter;
  protected fullNameRegex = Validators.fullNameRegex;
  protected email_Regex = Validators.email_Regex;
  protected emailRegex = Validators.emailRegex;
  protected passwordRegex = Validators.passwordRegex;

  isSubmitting = false;

  user = {
    firstName: '',
    lastName: '',
    email: '',
    password: ''
  };

  errors = {
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    global: ''
  };

  showPassword = false;
  emailTaken = false;

  constructor(private api: Api, public common: Common, private router: Router) {}

  hasErrors(): boolean {
    return this.common.hasErrors(this.errors);
  }

  togglePassword(): void {
    this.showPassword = this.common.toggleAndGetVisibility('register-password');
  }

  async onSubmit(form: NgForm): Promise<void> {
    const { firstName, lastName, email, password } = this.user;
    let isValid = true;

    if (!this.common.isValid(firstName, this.fullNameRegex, this.hasLetter)) {
      this.errors.firstName = firstName ? "Le prénom doit contenir au moins 2 lettres valides" : "Le prénom ne peut pas être vide";
      isValid = false;
    } else {
      this.errors.firstName = '';
    }

    if (!this.common.isValid(lastName, this.fullNameRegex, this.hasLetter)) {
      this.errors.lastName = lastName ? "Le nom doit contenir au moins 2 lettres valides" : "Le nom ne peut pas être vide";
      isValid = false;
    } else {
      this.errors.lastName = '';
    }

    if (!this.common.isValid(email, this.email_Regex, this.hasLetter)) {
      this.errors.email = email ? "Format d'email invalide" : "L'email ne peut pas être vide";
      isValid = false;
    } else {
      const unique = await this.common.isEmailUnique(email);
      if (!unique) {
        this.errors.email = "Cet email est déjà utilisé";
        isValid = false;
      } else {
        this.errors.email = '';
      }
    }

    if (!this.common.isValid(password, this.passwordRegex, this.hasLetter)) {
      this.errors.password = password ? "Le mot de passe doit avoir entre 8 et 20 caractères, avec majuscule, minuscule et un caractère spécial" : "Le mot de passe ne peut pas être vide";
      isValid = false;
    } else {
      this.errors.password = '';
    }

    if (!isValid) return;

    this.isSubmitting = true;

    this.api.register(this.user).pipe(finalize(() => {this.isSubmitting = false;})).subscribe({
      next: (res) => {
        this.router.navigate(['/acceuil'])
      },
      error: (err) => {
        this.errors.global = "Une erreur s'est produite lors de l'enregistrement.";
      }
    })
  }
}