-- Add academic_year_id foreign key to application table
ALTER TABLE application 
ADD COLUMN academic_year_id INT;

ALTER TABLE application 
ADD CONSTRAINT fk_application_academic_year 
FOREIGN KEY (academic_year_id) REFERENCES academic_year(id) ON DELETE SET NULL;

CREATE INDEX idx_application_academic_year ON application(academic_year_id);

-- Add academic_year_id foreign key to room_allocation table
ALTER TABLE room_allocation 
ADD COLUMN academic_year_id INT;

ALTER TABLE room_allocation 
ADD CONSTRAINT fk_room_allocation_academic_year 
FOREIGN KEY (academic_year_id) REFERENCES academic_year(id) ON DELETE SET NULL;

CREATE INDEX idx_room_allocation_academic_year ON room_allocation(academic_year_id);
