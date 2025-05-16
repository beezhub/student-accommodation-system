-- Create User table
CREATE TABLE app_user (
    id SERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    user_password VARCHAR(255) NOT NULL,
    user_role VARCHAR(20) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create Student table (extends User)
CREATE TABLE student (
    id SERIAL PRIMARY KEY,
    student_number VARCHAR(20) NOT NULL UNIQUE,
    phone_number VARCHAR(20),
    date_of_birth DATE,
    registration_date DATE NOT NULL,
    gender VARCHAR(30),
    user_id INT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT chk_registration_date CHECK (registration_date >= date_of_birth)
);

-- Create Room table
CREATE TABLE room (
    id SERIAL PRIMARY KEY,
    room_number VARCHAR(10) NOT NULL UNIQUE,
    room_type VARCHAR(20) NOT NULL,
    capacity INT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'Available',
    CONSTRAINT chk_capacity_positive CHECK (capacity > 0),
    CONSTRAINT chk_price_positive CHECK (price >= 0)
);

-- Create Booking table
CREATE TABLE booking (
    id SERIAL PRIMARY KEY,
    student_id INT NOT NULL,
    room_id INT NOT NULL,
    booking_date DATE DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30) NOT NULL DEFAULT 'Awaiting Review',
    FOREIGN KEY (student_id) REFERENCES Student(id) ON DELETE CASCADE,
    FOREIGN KEY (room_id) REFERENCES Room(id) ON DELETE CASCADE,
    CONSTRAINT unique_student_room_booking UNIQUE (student_id, room_id,booking_date)
);

-- Create Maintenance_Ticket table
CREATE TABLE maintenance_ticket (
    id SERIAL PRIMARY KEY,
    room_id INT NOT NULL,
    issue_description TEXT NOT NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30) NOT NULL DEFAULT 'Open',
    FOREIGN KEY (room_id) REFERENCES Room(id) ON DELETE CASCADE
);