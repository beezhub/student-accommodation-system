import { Component, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { Router, RouterModule, ActivatedRoute } from "@angular/router";
import { AuthService } from "../../../core/services/auth.service";
import { ApplicationService } from "../../../core/services/application.service";
import { Application } from "../../../core/models/application.model";

@Component({
  selector: "app-application-confirmation",
  templateUrl: "./application-confirmation.component.html",
  styleUrls: ["./application-confirmation.component.css"],
  standalone: true,
  imports: [CommonModule, RouterModule],
})
export class ApplicationConfirmationComponent implements OnInit {
  currentUser: any;
  currentDate: Date = new Date();
  application: Application | null = null;
  applicationId: number | null = null;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private authService: AuthService,
    private applicationService: ApplicationService
  ) {}

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    if (!this.currentUser) {
      this.router.navigate(["/login"]);
    }
    const idParam = this.route.snapshot.paramMap.get("applicationId");
    this.applicationId =
      idParam !== null && !isNaN(Number(idParam)) ? Number(idParam) : null;
    console.log("Application ID:", this.applicationId);

    if (this.applicationId && this.applicationId > 0) {
      this.applicationService.getApplicationById(this.applicationId).subscribe({
        next: (application: Application) => {
          this.application = application;
        },
        error: (err) => {
          console.error("Failed to load application", err);
          this.router.navigate(["/dashboard"]);
        },
      });
    } else {
      console.error("No valid application ID found in route");
      this.router.navigate(["/dashboard"]);
    }
  }

  goToDashboard() {
    this.router.navigate(["/dashboard"]);
  }
}
