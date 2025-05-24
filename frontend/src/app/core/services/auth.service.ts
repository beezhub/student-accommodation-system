import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {BehaviorSubject, map, Observable} from "rxjs";
import { environment } from '../../../environments/environment';

export interface SignupData {
    firstName: string;
    lastName: string;
    email: string;
    password: string;
}

export interface ProfileData {
    studentNumber: string;
    phoneNumber: string;
    dateOfBirth: string;
    institution: string;
    yearOfStudy: string;
    gender: string;
    specialRequirements?: string;
}

export interface User {
    id: string;
    firstName: string;
    lastName: string;
    email: string;
    profileCompleted: boolean;
    userRole: string;
}

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private apiUrl = `${environment.apiUrl}/auth`;
    private currentUserSubject = new BehaviorSubject<User | null>(null);
    public currentUser = this.currentUserSubject.asObservable();

    constructor(private http: HttpClient) {
        const storedUser = localStorage.getItem('currentUser');
        if (storedUser) {
            this.currentUserSubject.next(JSON.parse(storedUser));
        }
    }

    public get currentUserValue(): User | null {
        return this.currentUserSubject.value;
    }

    signup(data: SignupData): Observable<User> {
        return this.http.post<{ token: string; user: User }>(`${this.apiUrl}/signup`, data)
            .pipe(
                map(response => {
                    localStorage.setItem('token', response.token);
                    localStorage.setItem('currentUser', JSON.stringify(response.user));
                   console.log(localStorage.getItem('token'));
                    console.log(localStorage.getItem('user'));
                    console.log(response);
                    this.currentUserSubject.next(response.user);
                    return response.user;
                })
            );
    }


    uploadDocuments(files: FormData): Observable<any> {
        return this.http.post(`${this.apiUrl}/documents`, files);
    }

    submitApplication(applicationData: any): Observable<any> {
        return this.http.post(`${this.apiUrl}/submit-application`, applicationData);
    }

    logout(): void {
        localStorage.removeItem('currentUser');
        this.currentUserSubject.next(null);
    }
    // Login a user
    login(data: any): Observable<User> {
        return this.http.post<User>(`${this.apiUrl}/login`, data).pipe(
            map(user => {
                localStorage.setItem('currentUser', JSON.stringify(user));
                this.currentUserSubject.next(user);
                return user;
            })
        );
    }
}