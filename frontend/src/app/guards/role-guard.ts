import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot } from '@angular/router';
import { Auth } from '../services/auth';
import { catchError, map, Observable, of, tap } from 'rxjs';

@Injectable({providedIn: 'root'})
export class RoleGuard implements CanActivate {
  constructor(private auth: Auth, private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
    const expectedRole = route.data['role'];
    
    return this.auth.isAuthenticated().pipe(
      map(isAuth => {
        const hasRole = this.auth.getRole() === expectedRole;
        return isAuth && hasRole;
      }),
      tap(allowed => {
        if(!allowed){
          this.router.navigate(['se-connecter']);
        }
      }),
      catchError(() => {
        this.router.navigate(['se-cnnecter']);
        return of(false);
      })
    );
  }
}