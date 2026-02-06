import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Validators } from '../../services/validators';
import { Common } from '../../services/common';
import { Api, Objectif } from '../../services/api';
import { MatSnackBar } from '@angular/material/snack-bar';

interface Row extends Objectif {
  originalObjectif: string;
  editObjectif: string;

  saving: boolean;
}

@Component({
  selector: 'app-objectifs',
  imports: [CommonModule, FormsModule],
  templateUrl: './objectifs.html',
  styleUrls: ['./objectifs.css']
})
export class Objectifs implements OnInit {
  protected numberObjectifRegex = Validators.numberObjectifRegex;
  protected allowedKeys = Validators.allowedKeys;

  Math = Math;

  pageIndex = 0;
  pageSize = 5;
  pageSizes = [5, 10, 20, 50];
  

  get total(): number {return this.rows.length;};
  get totalPages(): number {return Math.max(1, Math.ceil(this.total/this.pageSize));}
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
    global: '',
    objectif: ''
  }

  transaction: {objectif: string} = {
    objectif: ''
  }

  rows: Row[] = [];

  constructor(public common: Common, private api: Api, private snack: MatSnackBar) {}

  ngOnInit(): void {
    this.loadObjectives();
  }

  hasErrors(): boolean {
    return this.common.hasErrors(this.errors);
  }

  hasChanged(r: Row): boolean {
    const equalObj = r.editObjectif === r.originalObjectif;
    return !(equalObj);
  }

  

  private loadObjectives(): void {
    this.api.getObjectives().subscribe({
      next: (list: any[]) => {
        this.rows = (list ?? []).map((c: any) => {
          const obj = String(c.objectif ?? 0);

          return {
            id: c.id,
            objectif: obj,
            month: c.month,
            year: c.year,
            editObjectif: obj,
            saving: false,

            originalObjectif: obj
          } as Row;
        });

        this.goToPage(this.pageIndex);
      },
      error:() => {
        this.rows = [];
      }
    });
  }

  onKeyDownNumber(e: KeyboardEvent) {
    const input = e.target as HTMLInputElement;
    const key = e.key;
    const value = input.value;
    const cursorPos = input.selectionStart ?? 0;

    const controlKeys = [
      'Backspace', 'Delete', 'Tab', 'Escape', 'Enter',
      'ArrowLeft', 'ArrowRight', 'Home', 'End'
    ];
    if (controlKeys.includes(key)) return;

    if (/^\d$/.test(key)) return;

    if (key === '-') {
      if (cursorPos === 0 && !value.includes('-')) return;
      e.preventDefault();
      return;
    }

    if (key === '.' || key === ',') {
      if (!value.includes('.') && !value.includes(',')) return;
      e.preventDefault();
      return;
    }

    e.preventDefault();
  }

  modify(row: Row): void {
    const raw = String(row.editObjectif ?? '').trim();
    
    if(!raw){
      this.errors.objectif="L'objectif ne peut pas être vide";
      return
    };

    const normalized = raw.replace(/\s/g, '').replace(/,/g,'.');
    const amount = Number(normalized);

    row.saving = true;
    this.api.updateObjectif(row.id, { amount }).subscribe({
      next: (updated) => {
        const newObj = String(updated?.objectif ?? raw);

        row.objectif = updated.objectif ?? amount;
        
        row.editObjectif = newObj;

        row.originalObjectif = newObj;

        row.saving = false;
        this.snack.open('Objectif mis à jour ✅', '✖', {
          duration: 3000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['custom-toast']
        });
      },
      error: (err)=> {
        row.saving = false;
        this.errors.global = err?.error?.message || "Erreur lors de la mise à jour de l'objectif";
      }
    })
  }
}
