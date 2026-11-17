import { Component, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from './auth-service';

type EstadoCallback = 'procesando' | 'error';

@Component({
  imports: [RouterLink, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  selector: 'app-auth-callback',
  styleUrl: './auth-callback.css',
  templateUrl: './auth-callback.html',
})
export class AuthCallback implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);

  readonly estado = signal<EstadoCallback>('procesando');
  readonly mensaje = signal('Estamos validando tu sesión con Keycloak.');

  ngOnInit(): void {
    void this.procesar();
  }

  reintentar(): void {
    void this.auth.iniciarLogin('/');
  }

  private async procesar(): Promise<void> {
    const parametros = this.ruta.snapshot.queryParamMap;
    const error = parametros.get('error');

    if (error) {
      this.estado.set('error');
      this.mensaje.set(
        parametros.get('error_description') ??
          'Keycloak rechazó la solicitud de autenticación.',
      );
      return;
    }

    const code = parametros.get('code');
    const state = parametros.get('state');

    if (!code || !state) {
      this.estado.set('error');
      this.mensaje.set('La respuesta de Keycloak no contiene el código o el state esperado.');
      return;
    }

    try {
      await this.auth.completarLogin(code, state);
      const destino = this.auth.destinoPendiente();
      const navego = await this.router.navigateByUrl(destino);

      if (!navego) {
        await this.router.navigateByUrl('/');
      }
    } catch {
      this.estado.set('error');
      this.mensaje.set(
        'No se pudo completar el inicio de sesión. Vuelve a intentarlo para generar una nueva solicitud.',
      );
    }
  }
}
