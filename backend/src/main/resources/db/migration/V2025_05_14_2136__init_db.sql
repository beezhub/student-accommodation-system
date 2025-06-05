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

--Create Institution table
CREATE TABLE institution (
                             id SERIAL PRIMARY KEY,
                             name VARCHAR(100) NOT NULL UNIQUE,
                             address VARCHAR(255),
                             phone_number VARCHAR(20),
                             email VARCHAR(100),
                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--Create year of study table
CREATE TABLE year_of_study (
                               id SERIAL PRIMARY KEY,
                               year varchar NOT NULL,
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
    special_requirements VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_id INT NOT NULL,
    institution_id INT,
    year_of_study_id INT,
    FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    FOREIGN KEY (institution_id) REFERENCES institution(id) ON DELETE SET NULL,
    FOREIGN KEY (year_of_study_id) REFERENCES year_of_study(id) ON DELETE SET NULL
);

--Create document type table
CREATE TABLE document_type (
                               id SERIAL PRIMARY KEY,
                               type_name VARCHAR(50) NOT NULL UNIQUE,
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--Create document table
CREATE TABLE document (
    id SERIAL PRIMARY KEY,
    document_name VARCHAR(100) NOT NULL,
    document_type_id INT NOT NULL,
    document_path VARCHAR(255) NOT NULL,
    document_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    student_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (document_type_id) REFERENCES document_type(id) ON DELETE SET NULL,
    FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE
);

-- Create Application table
CREATE TABLE application (
    id SERIAL PRIMARY KEY,
    application_code VARCHAR(50) NOT NULL UNIQUE,
    application_date DATE DEFAULT CURRENT_DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    student_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE
);

-- Create Room table
CREATE TABLE room (
    id SERIAL PRIMARY KEY,
    room_number VARCHAR(20) NOT NULL UNIQUE,
    capacity INT NOT NULL,
    building VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--Create Room Allocation table
CREATE TABLE room_allocation (
    id SERIAL PRIMARY KEY,
    allocation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    student_id INT NOT NULL,
    room_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE,
    FOREIGN KEY (room_id) REFERENCES room(id) ON DELETE CASCADE
);

-- Create initial data for app_user
INSERT INTO app_user (email, user_password, user_role, first_name, last_name) VALUES
('test@test.com', '12345678', 'STUDENT', 'John', 'Doe');



--Create initial data for room
INSERT INTO room (room_number, capacity, building) VALUES
('101', 2, 'Main Building'),
('102', 4, 'Science Block'),
('103', 3, 'Arts Wing');



--insert institution
INSERT INTO institution (name, address, phone_number, email) VALUES
('University of Example', '123 Example St, City, Country', '123-456-7890', 'UniversityOfExample@example.com'),
('Example College', '456 Example Ave, City, Country', '987-654-3210', 'ExampleCollege@example.com');

--insert year of study
INSERT INTO year_of_study (year) VALUES
('First Year'),
('Second Year'),
('Third Year'),
('Fourth Year'),
('Postgraduate');

--insert document type
INSERT INTO document_type (type_name) VALUES
('Proof of Enrollment'),
('ID/Passport'),
('Medical Report'),
('Other');
