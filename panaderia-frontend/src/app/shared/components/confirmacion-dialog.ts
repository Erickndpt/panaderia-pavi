import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

export interface ConfirmacionDialogData {
  titulo: string;
  mensaje: string;
  detalle?: string;
  textoConfirmar?: string;
  textoCancelar?: string;
  peligro?: boolean;
}

@Component({
  selector: 'app-confirmacion-dialog',
  imports: [MatButtonModule, MatDialogModule, MatIconModule],
  templateUrl: './confirmacion-dialog.html',
  styleUrl: './confirmacion-dialog.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmacionDialog {
  readonly data = inject<ConfirmacionDialogData>(MAT_DIALOG_DATA);
}
