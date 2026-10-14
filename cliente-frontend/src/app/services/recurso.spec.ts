import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { RecursoService } from './recurso';

describe('RecursoService', () => {
  let service: RecursoService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient()]
    });
    service = TestBed.inject(RecursoService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
