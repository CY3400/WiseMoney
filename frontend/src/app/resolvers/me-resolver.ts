import { ResolveFn, Router } from '@angular/router';
import { Api } from '../services/api';
import { inject } from '@angular/core';
import { catchError, of } from 'rxjs';

export const MeResolver: ResolveFn<any> = () => {
  const api = inject(Api);
  const router = inject(Router);

  return api.me().pipe(
    catchError(() => of(router.parseUrl('/se-connecter')))
  );
};