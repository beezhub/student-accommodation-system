import { TestBed } from '@angular/core/testing';
import { TokenStorageService } from './token-storage.service';

describe('TokenStorageService', () => {
  let service: TokenStorageService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(TokenStorageService);
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getToken_tokenExists_returnsToken', () => {
    const token = 'test-token';
    localStorage.setItem('token', token);
    expect(service.getToken()).toBe(token);
  });

  it('getToken_noToken_returnsNull', () => {
    expect(service.getToken()).toBeNull();
  });

  it('removeToken_tokenExists_tokenRemoved', () => {
    const token = 'test-token';
    localStorage.setItem('token', token);
    service.removeToken();
    expect(localStorage.getItem('token')).toBeNull();
  });

  it('isTokenValid_expiredToken_returnsFalse', () => {
    // Create an expired token (1 hour ago)
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const payload = btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) - 3600 }));
    const token = `${header}.${payload}.signature`;

    localStorage.setItem('token', token);
    expect(service.isTokenValid()).toBeFalse();
  });

  it('isTokenValid_validToken_returnsTrue', () => {
    // Create a valid token (expires in 1 hour)
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const payload = btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 }));
    const token = `${header}.${payload}.signature`;

    localStorage.setItem('token', token);
    expect(service.isTokenValid()).toBeTrue();
  });

  it('isTokenValid_malformedToken_returnsFalse', () => {
    localStorage.setItem('token', 'malformed-token');
    expect(service.isTokenValid()).toBeFalse();
  });

  it('isTokenValid_noToken_returnsFalse', () => {
    expect(service.isTokenValid()).toBeFalse();
  });
});
