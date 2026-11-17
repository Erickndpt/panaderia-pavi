import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

@Component({
  imports: [RouterLink, MatButtonModule, MatIconModule],
  selector: 'app-no-encontrado',
  template: `
    <section class="pagina no-encontrado">
      <mat-icon class="no-encontrado__icono" aria-hidden="true">search_off</mat-icon>
      <h2 class="no-encontrado__titulo">Página no encontrada</h2>
      <p class="no-encontrado__texto">
        La dirección solicitada no existe o fue movida. Vuelve al inicio para continuar.
      </p>
      <a mat-flat-button color="primary" routerLink="/">
        <mat-icon>home</mat-icon>
        Ir al inicio
      </a>
    </section>
  `,
  styles: [
    `
      .no-encontrado {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.75rem;
        padding: 3rem 1.5rem;
        text-align: center;
      }

      .no-encontrado__icono {
        width: 4rem;
        height: 4rem;
        font-size: 4rem;
        color: #a67c52;
      }

      .no-encontrado__titulo {
        margin: 0;
        color: #3b2416;
        font-size: 1.75rem;
      }

      .no-encontrado__texto {
        margin: 0 0 0.75rem;
        max-width: 55ch;
        color: #6f4e37;
      }
    `,
  ],
})
export class NoEncontrado {}
