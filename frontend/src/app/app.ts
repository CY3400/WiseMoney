import { Component, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialogModule } from '@angular/material/dialog';
import { Header } from './pages/header/header';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, MatSnackBarModule, MatDialogModule, Header, CommonModule],
  templateUrl: './app.html',
  styleUrls: ['./app.css','../styles.css'],
  standalone: true
})
export class App {
  protected readonly title = signal('frontend');
  protected readonly showHeader = signal(true);

  constructor(private router: Router, private titleService: Title) {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      const currentRoute = this.router.routerState.root;
      let route = currentRoute;
      while (route.firstChild) {
        route = route.firstChild;
      }
      const title = route.snapshot.data['title'];
      if(title){
        this.titleService.setTitle(title);
      }

      const isPublic = !!route.snapshot.data['public'];
      this.showHeader.set(!isPublic);
    });
  }
}