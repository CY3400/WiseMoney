import { Injectable } from '@angular/core';
import { jwtDecode } from 'jwt-decode';
import { Api } from './api';
import { catchError, map, Observable, of } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class Auth {
  constructor(private api: Api){}
  
  getToken(): string | null {
    return null;
  }

  getRole(): string | null {
    const token = this.getToken();
    if(!token) return null;

    const decoded: any = jwtDecode(token);
    return decoded.role || null;
  }

  isAdmin(): boolean {
    return this.getRole() === 'ADMIN';
  }

  isUser(): boolean {
    return this.getRole() === 'USER';
  }

  isAuthenticated(): Observable<boolean> {
    return this.api.me().pipe(
      map(() => true),
      catchError(() => of(false))
    );
  }
}