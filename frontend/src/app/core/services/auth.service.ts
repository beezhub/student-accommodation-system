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
        console.log(data);
        return this.http.post(`${(this.apiUrl)}/signup`, data);
    }
}