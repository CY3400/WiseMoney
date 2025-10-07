import { Component, OnInit } from '@angular/core';
import { Api } from '../../services/api';
import { Common } from '../../services/common';

@Component({
  selector: 'app-welcome',
  templateUrl: './welcome.html',
  styleUrls: ['./welcome.css','../../../styles.css'],
  standalone: true
})
export class Welcome implements OnInit {
  constructor(private api: Api, public common: Common) {}

  ngOnInit(): void {
    this.api.logout().subscribe({
      next: () => console.log('Déconnexion réussie'),
      error: (err) => console.error('Erreur lors du logout :', err)
    });
  }
}