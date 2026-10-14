import { TestBed } from '@angular/core/testing';
import { Recurso } from './recurso';

describe('Recurso', () => {
  let service: Recurso;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Recurso);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
