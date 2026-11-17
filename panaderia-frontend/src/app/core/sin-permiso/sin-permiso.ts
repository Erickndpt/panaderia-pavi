import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

@Component({
  imports: [RouterLink, MatButtonModule, MatIconModule],
  selector: 'app-sin-permiso',
  styleUrl: './sin-permiso.css',
  templateUrl: './sin-permiso.html',
})
export class SinPermiso {}
