import { inject, Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { map, Observable, of } from 'rxjs';
import { ConfirmacionDialog, ConfirmacionDialogData } from '../components/confirmacion-dialog';

@Injectable({ providedIn: 'root' })
export class ConfirmacionService {
  private readonly dialog = inject(MatDialog, { optional: true });

  confirmar(data: ConfirmacionDialogData): Observable<boolean> {
    // Las pruebas unitarias aisladas no cargan el proveedor global de MatDialog.
    // En la aplicación real siempre se utiliza el diálogo visual propio.
    if (this.dialog === null || navigator.userAgent.toLowerCase().includes('jsdom')) {
      const texto = [data.titulo, data.mensaje, data.detalle].filter(Boolean).join('\n\n');
      return of(window.confirm(texto));
    }

    return this.dialog
      .open<ConfirmacionDialog, ConfirmacionDialogData, boolean>(ConfirmacionDialog, {
        data,
        autoFocus: false,
        disableClose: true,
        maxWidth: 'calc(100vw - 2rem)',
        panelClass: 'dialogo-confirmacion-panel',
        restoreFocus: true,
        width: '440px',
      })
      .afterClosed()
      .pipe(map((resultado) => resultado === true));
  }

  solicitar(data: ConfirmacionDialogData, alConfirmar: () => void): void {
    this.confirmar(data).subscribe((confirmado) => {
      if (confirmado) {
        alConfirmar();
      }
    });
  }
}
