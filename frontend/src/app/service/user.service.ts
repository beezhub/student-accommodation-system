import { Injectable } from '@angular/core';
import { User } from '../model/user.model';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';  

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private registeredUsers: User[] = [];
  private apiUrl = 'http://localhost:8080/api/auth/signup';  

  constructor(private http: HttpClient) { }

  // Register a new user
 
  register(user: any): Observable<any> {
    return this.http.post(this.apiUrl, user); // Send POST request to backend
  }

  // Validate user login
  validateLogin(email: string, password: string): boolean {
    const user = this.registeredUsers.find(u => u.email === email);
    return !!user && user.password === password;  
  }
}
