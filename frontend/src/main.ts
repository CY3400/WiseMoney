import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { App } from './app/app';
import { AuthInterceptor } from './app/interceptors/auth-interceptor';

bootstrapApplication(App, {
  ...appConfig,
  providers: [
    ...appConfig.providers!,
    provideHttpClient(
      withFetch(),
      withInterceptors([AuthInterceptor])
    )
  ]
}).catch((err) => console.error(err));
