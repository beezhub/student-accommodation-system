# Student Accommodation System

[![CI](https://github.com/your-org/student-accommodation-system/actions/workflows/ci.yml/badge.svg)](https://github.com/your-org/student-accommodation-system/actions/workflows/ci.yml)

A web application to help students find, book, and manage accommodation easily. This project consists of a backend (Spring Boot) and a frontend (Angular).

## Features
- Student registration and authentication
- Search and filter accommodations
- Book and manage accommodation
- Admin dashboard for managing listings
- Responsive UI

## Tech Stack
- **Backend:** Java, Spring Boot
- **Frontend:** Angular
- **Database:** PostgreSQL
- **Containerization:** Docker

## Installation

### 1. Clone the repository
```bash
git clone https://github.com/your-username/student-accommodation-system.git
```

### 2. Navigate to the project directory
```bash
cd student-accommodation-system
```

### 3. Configure Environment Variables
**IMPORTANT:** The application requires environment variables for configuration.

1. Copy the example environment file:
   ```bash
   cp .env.example .env
   ```

2. Edit `.env` and replace all placeholder values with your actual credentials:
   - **JWT_SECRET**: Generate a secure 512-bit base64 key (see instructions in `.env.example`)
   - **Database credentials**: Set your PostgreSQL username and password
   - **MinIO credentials**: Set secure access keys for object storage
   - **Google Maps API keys**: Obtain from [Google Cloud Console](https://console.cloud.google.com/apis/credentials)
   - **SMTP credentials**: Configure your email provider settings

3. **Security Notice**:
   - `.env` is excluded from version control via `.gitignore`
   - **NEVER** commit real credentials to the repository
   - If credentials are accidentally exposed, **rotate them immediately**
   - Use different credentials for development, staging, and production

4. For local development with Docker Compose, default credentials are provided in `docker-compose.yml` (ensure these are changed for production deployments)

### 4. Configure the Database (PostgreSQL)
- Ensure you have PostgreSQL installed and running (or use Docker Compose as described below).
- Create a database for the project (e.g., `student_accommodation`).
- Update the database settings in `backend/src/main/resources/application.yml` with your PostgreSQL credentials and database name.
- Flyway will automatically run migrations on backend startup.

### 5. Start with Docker Compose (Recommended)
If you prefer to run the entire stack (backend, frontend, and database) using Docker Compose:
```powershell
docker-compose up --build
```
This will build and start all services as defined in `docker-compose.yml`.

### 6. Manual Setup (Alternative)
If you want to run services manually without Docker Compose, follow these steps:

#### a. Build and Run the Backend (Spring Boot)
Navigate to the backend directory and build the project:
```bash
cd backend
./gradlew build
```
Start the backend server:
```bash
./gradlew bootRun
```

#### b. Install Frontend Dependencies (Angular)
Open a new terminal, navigate to the frontend directory, and install dependencies:
```bash
cd ../frontend
npm install
```

#### c. Start the Frontend Application
From the `frontend` directory:
```bash
ng serve
```

Now, both the backend and frontend servers should be running. Access the application via your browser at the specified frontend URL (typically `http://localhost:4200`).

## Usage
- Register as a student or admin
- Log in to access your dashboard
- Search and book accommodations
- Admins can add, edit, or remove listings

## Application Preferences (Backend B2)
- Fields captured: preferred accommodation type, move-in date, duration (months), room type, special requirements, institution, and course.
- Meal plans and student-paid budgets are not collected; bursaries cover accommodation costs. Related columns are dropped and not exposed via API.
- Submission endpoint: `POST /api/applications/submit` accepts `ApplicationRequest` with the above preference fields only.

## Operations
- [Operator runbook](docs/OPERATOR_RUNBOOK.md) — every account, domain, DNS record, credential and
  GitHub setting the deployment needs from a person, in the order it is needed. Start here before
  any deployment work.
- [Backup and restore runbook](docs/BACKUP_AND_RESTORE.md) — PostgreSQL and MinIO backup, restore
  ordering, and the document retention rule (twelve months after a student's last application
  closes and last tenancy ends). Read it before turning retention off dry-run mode.
- **Logs.** On a deployed host every container's output and the Caddy journal are shipped by
  Alloy to Loki and kept for two weeks (`tools/alloy/config.alloy`, `tools/loki/loki.yml`). Caddy
  serves Grafana at `/grafana`; sign in with `GRAFANA_ADMIN_USER` and `GRAFANA_ADMIN_PASSWORD` and
  use Explore with the Loki datasource:

  ```
  https://nonprod.beezhub.co.za/grafana/explore   # datasource Loki
  ```

  `GRAFANA_ROOT_URL` in the host `.env` must be that public address including `/grafana`, and the
  host's `/etc/caddy/Caddyfile` must be the copy in `tools/caddy/`; the deploy updates neither.

  Useful queries: `{service="student-accommodation-back-end", level="ERROR"}` for backend errors,
  `{service="student-accommodation-back-end"} | requestId="<id>"` to follow one request by the
  `x-request-id` the API returns, `{unit="caddy.service"}` for the TLS proxy's access log, and
  `{service="frontend"}` for nginx. The backend's level is a label; its logger, request id and
  tenant id are structured metadata and filter with `| logger=~"...Service"`. Without Grafana,
  `docker compose logs` on the host also works, but only holds the last 30 MB per container.
- **Metrics and alerts.** Prometheus scrapes the backend every 15 seconds and keeps 30 days on
  its own volume; the dashboards under `tools/grafana/dashboards` read from it in the same
  Grafana. Scraping needs `PROMETHEUS_SCRAPE_PASSWORD` set in `.env`, which feeds both the backend
  and Prometheus. Provisioned alerts email `GRAFANA_ALERT_EMAIL` (default `APP_SUPPORT_EMAIL`)
  through the `MAIL_*` account. Check `up{job="student-accommodation-backend"}` in Explore first:
  `0` means the scrape is failing, and the usual cause is that password.

## Folder Structure
```
student-accommodation-system/
├── backend/      # Spring Boot backend
├── frontend/     # Angular frontend
├── docs/         # Documentation and diagrams
├── docker-compose.yml
└── README.md
```

## Contributing
Contributions are welcome! Please fork the repository and submit a pull request.
Read [CONTRIBUTING.md](CONTRIBUTING.md) first for the branch naming, commit
message, and attribution rules.

## License
This project is licensed under the MIT License.

## Contact
For questions or support, contact [your-email@example.com](mailto:your-email@example.com).

## Authors
- Ayanda Hadebe
- Boitumelo Tshehla

