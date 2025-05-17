import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import { environment } from '../../../environments/environment';

export interface SignupData {
    firstName: string;
    lastName: string;
    email: string;
    password: string;
    userRole: string;
}

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private apiUrl = `${environment.apiUrl}/auth`;

    constructor(private http: HttpClient) { }

    // Register a new user

    signup(data: any): Observable<any> {
        return this.http.post(`${(this.apiUrl)}/signup`, data);
    }
    // Login a user
    login(data: any): Observable<any> {
        console.log('Login data:', data);
        const response = this.http.post(`${(this.apiUrl)}/login`, data);
        console.log('Login response:', response);
        return response;
    }
}