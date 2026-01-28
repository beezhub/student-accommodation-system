-- Create FF4J feature management tables
CREATE TABLE ff4j_features (
    feat_uid VARCHAR(100) NOT NULL,
    enable INTEGER NOT NULL,
    description VARCHAR(1000),
    strategy VARCHAR(1000),
    expression VARCHAR(255),
    groupname VARCHAR(100),
    PRIMARY KEY(feat_uid)
);

CREATE TABLE ff4j_properties (
    property_id VARCHAR(100) NOT NULL,
    clazz VARCHAR(255) NOT NULL,
    currentvalue VARCHAR(255),
    fixedvalues VARCHAR(1000),
    description VARCHAR(1000),
    feat_uid VARCHAR(100),
    PRIMARY KEY(property_id),
    FOREIGN KEY(feat_uid) REFERENCES ff4j_features(feat_uid)
);

CREATE TABLE ff4j_custom_properties (
    property_id VARCHAR(100) NOT NULL,
    param_name VARCHAR(100) NOT NULL,
    param_value VARCHAR(255),
    PRIMARY KEY(property_id, param_name),
    FOREIGN KEY(property_id) REFERENCES ff4j_properties(property_id)
);

CREATE TABLE ff4j_roles (
    feat_uid VARCHAR(100) NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    PRIMARY KEY(feat_uid, role_name),
    FOREIGN KEY(feat_uid) REFERENCES ff4j_features(feat_uid)
);

-- Insert default feature: auto-allocation (disabled by default)
INSERT INTO ff4j_features (feat_uid, enable, description, groupname)
VALUES ('auto-allocation', 0, 'Enable automatic room allocation logic', 'system');
