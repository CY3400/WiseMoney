import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { Api } from '../../services/api';
import { Common } from '../../services/common';
import { Router, RouterLink } from '@angular/router';
import { Validators } from '../../services/validators';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrls: ['./login.css','../../../styles.css']
})
export class Login {
  protected allowedKeys = Validators.allowedKeys;
  protected emailRegex = Validators.emailRegex;

  isSubmitting = false;

  user = {
    email: '',
    password: ''
  };

  errors = {
    email: '',
    password: '',
    global: ''
  }

  hasErrors(): boolean {
    return this.common.hasErrors(this.errors);
  }


  showPassword = false;

  constructor(private api: Api, public common: Common, private router: Router) {}

  togglePassword(): void {
    this.showPassword = this.common.toggleAndGetVisibility('login-password');
  }

  async onSubmit(form: NgForm): Promise<void>{
    const {email, password} = this.user;
    let isValid = true;

    if(!email.trim()){
      this.errors.email = "L'email ne peut pas être vide";
      isValid = false;
    }
    else {
      this.errors.email = '';
    }

    if(!password.trim()){
      this.errors.password = "Le mot de passe ne peut pas être vide";
      isValid = false;
    }
    else {
      this.errors.password = '';
    }

    if(!isValid) return;

    this.isSubmitting = true;

    this.api.login(this.user).pipe(finalize(() => { this.isSubmitting = false; })).subscribe({
      next: () => {
        this.router.navigate(['/acceuil']);
      },
      error: () => {
        this.errors.global = "Mauvaises informations d'authentification";
      }
    });
  }
}