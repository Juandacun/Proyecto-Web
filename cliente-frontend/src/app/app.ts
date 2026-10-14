import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CatalogoComponent } from './components/catalogo/catalogo';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet,CatalogoComponent],
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  protected readonly title = signal('cliente-frontend');
}
