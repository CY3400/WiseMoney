import { HttpErrorResponse, HttpEvent, HttpHandlerFn, HttpInterceptorFn, HttpRequest } from "@angular/common/http";
import { inject } from "@angular/core";
import { MatSnackBar } from "@angular/material/snack-bar";
import { Router } from "@angular/router";
import { Observable, throwError } from "rxjs";
import { catchError } from "rxjs/operators";

let hasShownMessage = false;

export const AuthInterceptor: HttpInterceptorFn = (req: HttpRequest<any>, next: HttpHandlerFn): Observable<HttpEvent<any>> => {
  const snackBar = inject(MatSnackBar);
  const router = inject(Router);

  const isAuthAction = req.url.endsWith('/login') || req.url.endsWith('/change-password');

  const noToast = req.url.endsWith('/forgot-password') || req.url.endsWith('/reset-password');

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if ((error.status === 401 || error.status === 403) && !noToast) {
        if (isAuthAction) return throwError(() => error);

        if (!hasShownMessage) {
          hasShownMessage = true;
          snackBar.open('Session expirée. Veuillez vous reconnecter.', '✖', { duration: 3000, horizontalPosition: 'right', verticalPosition: 'top', panelClass: ['custom-toast'] });
          setTimeout(() => (hasShownMessage = false), 3000);
        }
      }

      const alreadyOnAuth = router.url.startsWith('/se-connecter') || router.url.startsWith('/s-enregistrer');

        if (!alreadyOnAuth) {
          setTimeout(() => router.navigate(['/se-connecter']), 0);
        }

      return throwError(() => error);
    })
  );
};