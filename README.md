# Student Accommodation System

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

### 3. Configure the Database (PostgreSQL)
- Ensure you have PostgreSQL installed and running (or use Docker Compose as described below).
- Create a database for the project (e.g., `student_accommodation`).
- Update the database settings in `backend/src/main/resources/application.yml` with your PostgreSQL credentials and database name.
- Flyway will automatically run migrations on backend startup.

### 4. Start with Docker Compose (Recommended)
If you prefer to run the entire stack (backend, frontend, and database) using Docker Compose:
```powershell
docker-compose up --build
```
This will build and start all services as defined in `docker-compose.yml`.

### 5. Manual Setup (Alternative)
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
