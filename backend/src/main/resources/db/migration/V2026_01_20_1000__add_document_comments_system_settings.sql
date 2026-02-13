-- Create document_comments table for storing admin comments on rejected documents
CREATE TABLE document_comments (
    id SERIAL PRIMARY KEY,
    document_id INT NOT NULL,
    admin_id INT NOT NULL,
    comment_text TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (document_id) REFERENCES document(id) ON DELETE CASCADE,
    FOREIGN KEY (admin_id) REFERENCES app_user(id) ON DELETE CASCADE
);

-- Create indexes for document_comments table
CREATE INDEX idx_document_comments_document_id ON document_comments(document_id);
CREATE INDEX idx_document_comments_admin_id ON document_comments(admin_id);

-- Create system_settings table for feature flags and configuration
CREATE TABLE system_settings (
    id SERIAL PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value VARCHAR(255),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index for setting_key lookups
CREATE INDEX idx_system_settings_key ON system_settings(setting_key);

-- Insert default system settings
INSERT INTO system_settings (setting_key, setting_value, description) VALUES
('auto_allocation_enabled', 'false', 'Enable or disable automatic room allocation for approved applications');
