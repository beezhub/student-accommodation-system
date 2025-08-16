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
    FOREIGN KEY (institution_id) REFERENCES institution(id) ON DELETE
    SET
        NULL,
        FOREIGN KEY (year_of_study_id) REFERENCES year_of_study(id) ON DELETE
    SET
        NULL
);

--Create document type table
CREATE TABLE document_type (
    id SERIAL PRIMARY KEY,
    type_name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
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
    FOREIGN KEY (document_type_id) REFERENCES document_type(id) ON DELETE
    SET
        NULL,
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
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    capacity INT NOT NULL,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    building_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create Building table to support multiple buildings and reuse across rooms
CREATE TABLE building (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    address VARCHAR(255),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Add foreign key from room to building
ALTER TABLE
    room
ADD
    CONSTRAINT fk_room_building FOREIGN KEY (building_id) REFERENCES building(id) ON DELETE
SET
    NULL;

-- Track current occupancy to help prevent over-allocation (application logic should enforce as well)
ALTER TABLE
    room
ADD
    COLUMN IF NOT EXISTS current_occupancy INT DEFAULT 0;

-- Ensure occupancy never exceeds capacity at the DB level (helps catch issues early)
ALTER TABLE
    room
ADD
    CONSTRAINT chk_room_occupancy CHECK (current_occupancy <= capacity);

-- Room features / amenities to support FR-031 (amenities and features)
CREATE TABLE IF NOT EXISTS room_feature (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS room_feature_map (
    room_id INT NOT NULL,
    feature_id INT NOT NULL,
    PRIMARY KEY (room_id, feature_id),
    FOREIGN KEY (room_id) REFERENCES room(id) ON DELETE CASCADE,
    FOREIGN KEY (feature_id) REFERENCES room_feature(id) ON DELETE CASCADE
);

--Create Room Allocation table
CREATE TABLE room_allocation (
    id SERIAL PRIMARY KEY,
    allocation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    student_id INT NOT NULL,
    room_id INT NOT NULL,
    allocation_start_date DATE,
    allocation_end_date DATE,
    confirmed BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE,
    FOREIGN KEY (room_id) REFERENCES room(id) ON DELETE CASCADE
);