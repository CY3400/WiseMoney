import { Component, ElementRef, HostListener, ViewChild } from '@angular/core';
import { Api } from '../../services/api';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { Common } from '../../services/common';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './header.html',
  styleUrls: ['./header.css']
})
export class Header {
  isMenuOpen = false;
  @ViewChild('burgerBtn', { static: false }) burgerBtn?: ElementRef<HTMLButtonElement>;

  constructor(private api: Api, private router: Router, public common: Common){
    this.router.events.subscribe(() => this.isMenuOpen = false);
  }

  myAccount(): void {
    this.router.navigate(['/mon-compte']);
  }

  homePage():void {
    this.router.navigate(['/acceuil']);
  }

  toggleMenu(): void{
    this.isMenuOpen = !this.isMenuOpen;
  }

  closeMenu(shouldReturnFocus: boolean): void{
    if(!this.isMenuOpen) return;
    this.isMenuOpen = false;
    if(shouldReturnFocus){
      setTimeout(() => this.burgerBtn?.nativeElement.focus(), 0);
    }
  }

  @HostListener('window:resize')
  onResize(){
    const DESKTOP_BP = 992;
    if(window.innerWidth >= DESKTOP_BP){
      this.closeMenu(false);
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(){
    this.closeMenu(true);
  }
}