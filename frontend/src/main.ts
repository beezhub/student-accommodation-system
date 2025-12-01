import { bootstrapApplication } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { AppComponent } from './app/app.component';
import { routes } from './app/app-routing.module';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { authInterceptor } from './app/core/interceptors/auth.interceptor';
import { tokenExpiryInterceptor } from './app/core/interceptors/token-expiry.interceptor';

bootstrapApplication(AppComponent, {
  providers: [
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([authInterceptor, tokenExpiryInterceptor])
    ),
    provideAnimations()
  ],
}).catch((err) => console.error(err));
