-- =============================================================================
-- Fix FF4j schema to match what JdbcFeatureStore (ff4j 1.9) actually queries.
--
-- The original schema (V2026_02_19_2100) used column name `uid` and type BOOLEAN
-- for the features table. FF4j 1.9 issues SQL like:
--   SELECT FEAT_UID,ENABLE,... FROM FF4J_FEATURES WHERE FEAT_UID = ?
--   SELECT COUNT(FEAT_UID) FROM FF4J_FEATURES WHERE FEAT_UID = ?
--   UPDATE FF4J_FEATURES SET ENABLE = 1 WHERE FEAT_UID = ?
-- so the PK column must be FEAT_UID and ENABLE must be INTEGER (1/0), not BOOLEAN.
-- FF4J_PROPERTIES table was also missing entirely.
-- =============================================================================

-- Drop dependent tables first (FK references ff4j_features(uid))
DROP TABLE IF EXISTS ff4j_custom_properties;
DROP TABLE IF EXISTS ff4j_roles;
DROP TABLE IF EXISTS ff4j_audit;
DROP TABLE IF EXISTS ff4j_features;

-- Recreate with FF4j 1.9 expected schema
-- (unquoted names; PostgreSQL folds to lower-case, matching FF4j's unquoted queries)

CREATE TABLE FF4J_FEATURES (
    FEAT_UID    VARCHAR(100) PRIMARY KEY,
    ENABLE      INTEGER      NOT NULL,
    DESCRIPTION VARCHAR(1000),
    STRATEGY    VARCHAR(1000),
    EXPRESSION  VARCHAR(255),
    GROUPNAME   VARCHAR(100)
);

CREATE TABLE FF4J_ROLES (
    FEAT_UID  VARCHAR(100) NOT NULL REFERENCES FF4J_FEATURES(FEAT_UID),
    ROLE_NAME VARCHAR(100) NOT NULL,
    PRIMARY KEY (FEAT_UID, ROLE_NAME)
);

CREATE TABLE FF4J_CUSTOM_PROPERTIES (
    PROPERTY_ID  VARCHAR(100) NOT NULL,
    CLAZZ        VARCHAR(255) NOT NULL,
    CURRENTVALUE VARCHAR(255),
    FIXEDVALUES  VARCHAR(1000),
    DESCRIPTION  VARCHAR(1000),
    FEAT_UID     VARCHAR(100) REFERENCES FF4J_FEATURES(FEAT_UID),
    PRIMARY KEY (PROPERTY_ID, FEAT_UID)
);

-- General (non-feature-scoped) property store — required by PropertyStoreSpringJdbc
CREATE TABLE FF4J_PROPERTIES (
    PROPERTY_ID  VARCHAR(100) NOT NULL PRIMARY KEY,
    CLAZZ        VARCHAR(255) NOT NULL,
    CURRENTVALUE VARCHAR(255),
    FIXEDVALUES  VARCHAR(1000),
    DESCRIPTION  VARCHAR(1000)
);

CREATE TABLE FF4J_AUDIT (
    EVT_UUID     VARCHAR(40)  NOT NULL,
    EVT_TIME     TIMESTAMP    NOT NULL,
    EVT_TYPE     VARCHAR(30)  NOT NULL,
    EVT_NAME     VARCHAR(100) NOT NULL,
    EVT_ACTION   VARCHAR(100) NOT NULL,
    EVT_HOSTNAME VARCHAR(100) NOT NULL,
    EVT_SOURCE   VARCHAR(30)  NOT NULL,
    EVT_DURATION INTEGER,
    EVT_USER     VARCHAR(30),
    EVT_VALUE    VARCHAR(100),
    EVT_KEYS     VARCHAR(255),
    PRIMARY KEY (EVT_UUID, EVT_TIME)
);

-- Re-seed the auto-allocation feature flag.
-- ENABLE = 0 (disabled); FF4jConfig overrides this at startup via ff4j.enable/disable().
INSERT INTO FF4J_FEATURES (FEAT_UID, ENABLE, DESCRIPTION)
VALUES ('auto-allocation', 0, 'Enable automatic room allocation for approved applications')
ON CONFLICT (FEAT_UID) DO NOTHING;
