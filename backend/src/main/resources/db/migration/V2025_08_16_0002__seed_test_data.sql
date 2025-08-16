INSERT INTO
    app_user (
        email,
        user_password,
        user_role,
        first_name,
        last_name
    )
VALUES
    (
        'student1@example.com',
        '$2a$10$QnA9AjasBY32ASSLzZ9Ta.nFhYVYxOuzBxTD2GdJDQj.n4lkguPWC',
        'STUDENT',
        'Alice',
        'Anderson'
    ),
    (
        'student2@example.com',
        '$2a$10$QnA9AjasBY32ASSLzZ9Ta.nFhYVYxOuzBxTD2GdJDQj.n4lkguPWC',
        'STUDENT',
        'Bob',
        'Brown'
    ),
    (
        'admin@example.com',
        '$2a$10$QnA9AjasBY32ASSLzZ9Ta.nFhYVYxOuzBxTD2GdJDQj.n4lkguPWC',
        'ADMIN',
        'Admin',
        'User'
    );

-- Seed institutions
INSERT INTO
    institution (name, address, phone_number, email)
SELECT
    'University of Example',
    '123 Example St, City, Country',
    '123-456-7890',
    'UniversityOfExample@example.com'
WHERE
    NOT EXISTS (
        SELECT
            1
        FROM
            institution
        WHERE
            name = 'University of Example'
    );

INSERT INTO
    institution (name, address, phone_number, email)
SELECT
    'Example College',
    '456 Example Ave, City, Country',
    '987-654-3210',
    'ExampleCollege@example.com'
WHERE
    NOT EXISTS (
        SELECT
            1
        FROM
            institution
        WHERE
            name = 'Example College'
    );

-- Seed year_of_study
INSERT INTO
    year_of_study (year)
SELECT
    'First Year'
WHERE
    NOT EXISTS (
        SELECT
            1
        FROM
            year_of_study
        WHERE
            year = 'First Year'
    );

INSERT INTO
    year_of_study (year)
SELECT
    'Second Year'
WHERE
    NOT EXISTS (
        SELECT
            1
        FROM
            year_of_study
        WHERE
            year = 'Second Year'
    );

INSERT INTO
    year_of_study (year)
SELECT
    'Third Year'
WHERE
    NOT EXISTS (
        SELECT
            1
        FROM
            year_of_study
        WHERE
            year = 'Third Year'
    );

INSERT INTO
    year_of_study (year)
SELECT
    'Fourth Year'
WHERE
    NOT EXISTS (
        SELECT
            1
        FROM
            year_of_study
        WHERE
            year = 'Fourth Year'
    );

INSERT INTO
    year_of_study (year)
SELECT
    'Postgraduate'
WHERE
    NOT EXISTS (
        SELECT
            1
        FROM
            year_of_study
        WHERE
            year = 'Postgraduate'
    );

-- Seed document types
INSERT INTO
    document_type (type_name, description, is_required)
SELECT
    'ID Document',
    'National ID or Passport',
    TRUE
WHERE
    NOT EXISTS (
        SELECT
            1
        FROM
            document_type
        WHERE
            type_name = 'ID Document'
    );

INSERT INTO
    document_type (type_name, description, is_required)
SELECT
    'Proof of Address',
    'Utility bill or bank statement',
    TRUE
WHERE
    NOT EXISTS (
        SELECT
            1
        FROM
            document_type
        WHERE
            type_name = 'Proof of Address'
    );

INSERT INTO
    document_type (type_name, description, is_required)
SELECT
    'Proof of Enrollment',
    'Current enrollment verification',
    TRUE
WHERE
    NOT EXISTS (
        SELECT
            1
        FROM
            document_type
        WHERE
            type_name = 'Proof of Enrollment'
    );

INSERT INTO
    document_type (type_name, description, is_required)
SELECT
    'Financial Aid Application',
    'Application for financial assistance',
    FALSE
WHERE
    NOT EXISTS (
        SELECT
            1
        FROM
            document_type
        WHERE
            type_name = 'Financial Aid Application'
    );

INSERT INTO
    document_type (type_name, description, is_required)
SELECT
    'Medical Certificate',
    'Health clearance certificate',
    FALSE
WHERE
    NOT EXISTS (
        SELECT
            1
        FROM
            document_type
        WHERE
            type_name = 'Medical Certificate'
    );

-- Seed buildings
INSERT INTO
    building (name, address, description)
SELECT
    'Main Building',
    'Main Building Address',
    'Primary campus building'
WHERE
    NOT EXISTS (
        SELECT
            1
        FROM
            building
        WHERE
            name = 'Main Building'
    );

INSERT INTO
    building (name, address, description)
SELECT
    'Science Block',
    'Science Block Address',
    'Science and labs'
WHERE
    NOT EXISTS (
        SELECT
            1
        FROM
            building
        WHERE
            name = 'Science Block'
    );

INSERT INTO
    building (name, address, description)
SELECT
    'Arts Wing',
    'Arts Wing Address',
    'Arts and studio spaces'
WHERE
    NOT EXISTS (
        SELECT
            1
        FROM
            building
        WHERE
            name = 'Arts Wing'
    );

-- Seed rooms (link to building ids)
INSERT INTO
    room (
        name,
        description,
        capacity,
        available,
        building_id
    )
SELECT
    '101',
    'Double room near west entrance',
    2,
    TRUE,
    b.id
FROM
    building b
WHERE
    b.name = 'Main Building'
    AND NOT EXISTS (
        SELECT
            1
        FROM
            room r
        WHERE
            r.name = '101'
    );

INSERT INTO
    room (
        name,
        description,
        capacity,
        available,
        building_id
    )
SELECT
    '102',
    'Large shared room with natural light',
    4,
    TRUE,
    b.id
FROM
    building b
WHERE
    b.name = 'Science Block'
    AND NOT EXISTS (
        SELECT
            1
        FROM
            room r
        WHERE
            r.name = '102'
    );

INSERT INTO
    room (
        name,
        description,
        capacity,
        available,
        building_id
    )
SELECT
    '103',
    'Quiet single room for arts students',
    3,
    TRUE,
    b.id
FROM
    building b
WHERE
    b.name = 'Arts Wing'
    AND NOT EXISTS (
        SELECT
            1
        FROM
            room r
        WHERE
            r.name = '103'
    );

-- Seed room features and mappings
INSERT INTO
    room_feature (name, description)
SELECT
    'Ensuite Bathroom',
    'Private bathroom attached to the room'
WHERE
    NOT EXISTS (
        SELECT
            1
        FROM
            room_feature
        WHERE
            name = 'Ensuite Bathroom'
    );

INSERT INTO
    room_feature (name, description)
SELECT
    'Shared Kitchen',
    'Access to a shared kitchen area'
WHERE
    NOT EXISTS (
        SELECT
            1
        FROM
            room_feature
        WHERE
            name = 'Shared Kitchen'
    );

INSERT INTO
    room_feature (name, description)
SELECT
    'Balcony',
    'Private balcony or outdoor area'
WHERE
    NOT EXISTS (
        SELECT
            1
        FROM
            room_feature
        WHERE
            name = 'Balcony'
    );

-- Map features to rooms (idempotent)
INSERT INTO
    room_feature_map (room_id, feature_id)
SELECT
    r.id,
    f.id
FROM
    room r,
    room_feature f
WHERE
    r.name = '101'
    AND f.name = 'Ensuite Bathroom'
    AND NOT EXISTS (
        SELECT
            1
        FROM
            room_feature_map m
        WHERE
            m.room_id = r.id
            AND m.feature_id = f.id
    );

INSERT INTO
    room_feature_map (room_id, feature_id)
SELECT
    r.id,
    f.id
FROM
    room r,
    room_feature f
WHERE
    r.name = '102'
    AND f.name = 'Shared Kitchen'
    AND NOT EXISTS (
        SELECT
            1
        FROM
            room_feature_map m
        WHERE
            m.room_id = r.id
            AND m.feature_id = f.id
    );

INSERT INTO
    room_feature_map (room_id, feature_id)
SELECT
    r.id,
    f.id
FROM
    room r,
    room_feature f
WHERE
    r.name = '103'
    AND f.name = 'Balcony'
    AND NOT EXISTS (
        SELECT
            1
        FROM
            room_feature_map m
        WHERE
            m.room_id = r.id
            AND m.feature_id = f.id
    );

-- Seed students linked to app_user and institution and year_of_study
INSERT INTO
    student (
        student_number,
        phone_number,
        date_of_birth,
        registration_date,
        gender,
        special_requirements,
        user_id,
        institution_id,
        year_of_study_id
    )
VALUES
    (
        'S2025001',
        '0712345678',
        '2003-04-15',
        CURRENT_DATE,
        'Female',
        NULL,
        (
            SELECT
                id
            FROM
                app_user
            WHERE
                email = 'student1@example.com'
        ),
        (
            SELECT
                id
            FROM
                institution
            WHERE
                name = 'University of Example'
        ),
        (
            SELECT
                id
            FROM
                year_of_study
            WHERE
                year = 'First Year'
        )
    ),
    (
        'S2025002',
        '0722345678',
        '2002-09-20',
        CURRENT_DATE,
        'Male',
        'Wheelchair access',
        (
            SELECT
                id
            FROM
                app_user
            WHERE
                email = 'student2@example.com'
        ),
        (
            SELECT
                id
            FROM
                institution
            WHERE
                name = 'Example College'
        ),
        (
            SELECT
                id
            FROM
                year_of_study
            WHERE
                year = 'Second Year'
        )
    );

-- Seed documents for students
INSERT INTO
    document (
        document_name,
        document_type_id,
        document_path,
        document_status,
        student_id
    )
VALUES
    (
        'Alice_ID.pdf',
        (
            SELECT
                id
            FROM
                document_type
            WHERE
                type_name = 'ID Document'
        ),
        '/docs/alice/id.pdf',
        'APPROVED',
        (
            SELECT
                id
            FROM
                student
            WHERE
                student_number = 'S2025001'
        )
    ),
    (
        'Bob_Enrollment.pdf',
        (
            SELECT
                id
            FROM
                document_type
            WHERE
                type_name = 'Proof of Enrollment'
        ),
        '/docs/bob/enroll.pdf',
        'PENDING',
        (
            SELECT
                id
            FROM
                student
            WHERE
                student_number = 'S2025002'
        )
    );

-- Seed applications for students
INSERT INTO
    application (
        application_code,
        application_date,
        status,
        student_id
    )
VALUES
    (
        'APP-2025-0001',
        CURRENT_DATE,
        'PENDING',
        (
            SELECT
                id
            FROM
                student
            WHERE
                student_number = 'S2025001'
        )
    ),
    (
        'APP-2025-0002',
        CURRENT_DATE,
        'APPROVED',
        (
            SELECT
                id
            FROM
                student
            WHERE
                student_number = 'S2025002'
        )
    );

-- Seed room allocations (link approved application/student to room)
INSERT INTO
    room_allocation (
        allocation_date,
        student_id,
        room_id,
        allocation_start_date,
        allocation_end_date,
        confirmed
    )
VALUES
    (
        CURRENT_TIMESTAMP,
        (
            SELECT
                id
            FROM
                student
            WHERE
                student_number = 'S2025002'
        ),
        (
            SELECT
                id
            FROM
                room
            WHERE
                name = '102'
        ),
        CURRENT_DATE,
        (CURRENT_DATE + INTERVAL '365 days') :: date,
        TRUE
    );

-- Update room current occupancy to reflect allocations
UPDATE
    room r
SET
    current_occupancy = (
        SELECT
            COUNT(*)
        FROM
            room_allocation ra
        WHERE
            ra.room_id = r.id
            AND ra.confirmed = TRUE
    );