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

## License
This project is licensed under the MIT License.

## Contact
For questions or support, contact [your-email@example.com](mailto:your-email@example.com).

## Authors
- Ayanda Hadebe
- Boitumelo Tshehla

