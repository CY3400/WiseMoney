import { Component, OnInit } from '@angular/core';
import { Api } from '../../services/api';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Header } from '../header/header';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './home.html',
  styleUrls: ['./home.css','../../../styles.css']
})
export class Home {
  user: any = null;

  constructor(private api: Api, private router: Router, private route: ActivatedRoute) {
    this.user = this.route.snapshot.data['me'];
  }

  logout(): void {
    this.api.logout().subscribe({
      next: () => {
        this.router.navigate(['/bienvenue']);
      }
    })
  }
}