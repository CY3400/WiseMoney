import { Component, OnInit } from '@angular/core';
import { Header } from '../header/header';
import { CommonModule } from '@angular/common';
import { Api } from '../../services/api';
import { ActivatedRoute, Router } from '@angular/router';
import { Common } from '../../services/common';
import { Validators } from '../../services/validators';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormsModule, NgForm } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';

@Component({
  selector: 'app-myaccount',
  imports: [CommonModule, FormsModule],
  templateUrl: './myaccount.html',
  styleUrls: ['./myaccount.css']
})
export class Myaccount implements OnInit {
  protected allowedKeys = Validators.allowedKeys;
  protected letterRegex = Validators.letterRegex;
  
  user: any = null;

  isEditing = false;
  isSaving = false;

  form = { firstName: '', lastName: '' };

  errors = {
    firstName: '',
    lastName: '',
    global: ''
  };

  constructor(private dialog: MatDialog, private api: Api, private router: Router, public common: Common, private snack: MatSnackBar, private route: ActivatedRoute) {
    
  }

  ngOnInit(): void {
    this.user = this.route.snapshot.data['me'];
  }

  startEdit(): void {
    this.clearErrors();
    this.form.firstName = this.user.firstName;
    this.form.lastName  = this.user.lastName;
    this.isEditing = true;
  }

  cancelEdit(form?: NgForm): void {
    form?.resetForm({
      firstName: this.user.firstName,
      lastName: this.user.lastName
    });
    this.clearErrors();
    this.isEditing = false;
  }

  private clearErrors(){
    this.errors={firstName: '', lastName: '', global: ''};
  }

  hasErrors(): boolean {
    return this.common.hasErrors(this.errors);
  }

  private validate(): boolean {
    this.clearErrors();
    let ok = true;

    const fn = this.form.firstName?.trim() || '';
    const ln = this.form.lastName?.trim() || '';

    if (!this.common.isValid(fn, Validators.fullNameRegex, Validators.hasLetter)) {
      this.errors.firstName = fn ? 'Le prénom doit contenir au moins 2 caractères' : 'Le prénom ne peut pas être vide';
      ok = false;
    }
    if (!this.common.isValid(ln, Validators.fullNameRegex, Validators.hasLetter)) {
      this.errors.lastName = ln ? 'Le nom doit contenir au moins 2 caractères' : 'Le nom ne peut pas être vide';
      ok = false;
    }
    return ok;
  }

  save(): void {
    if (!this.validate()) return;

    this.isSaving = true;
    this.api.updateMe({
      firstName: this.form.firstName.trim(),
      lastName: this.form.lastName.trim()
    }).subscribe({
      next: (updated) => {
        this.user.firstName = updated.firstName;
        this.user.lastName  = updated.lastName;
        this.isSaving = false;
        this.isEditing = false;
        this.snack.open('Profil mis à jour ✅', '✖', { duration: 3000, horizontalPosition: 'right', verticalPosition: 'top', panelClass: ['custom-toast'] });
      },
      error: (err) => {
        this.isSaving = false;
        const msg = err?.error?.message || "Erreur lors de la mise à jour.";
        this.errors.global = msg;
      }
    });
  }

  changePassword(): void {
    this.router.navigate(['/changer-mdp']);
  }

  logout(): void {
    this.api.logout().subscribe({
      next: () => this.router.navigate(['/bienvenue'])
    });
  }
}