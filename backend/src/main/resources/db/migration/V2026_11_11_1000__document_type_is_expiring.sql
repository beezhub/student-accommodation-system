-- GH-498 step 9: a company marks a document type as expiring, for example proof of registration.
-- The expiry date is not stored: it is the end date of the company's academic year, for the
-- student's institution, that the document was uploaded in, and is worked out per company because
-- one student's document pack is shared by every company they apply to.
ALTER TABLE document_type ADD COLUMN is_expiring BOOLEAN NOT NULL DEFAULT FALSE;

-- Days before expiry that a student is first reminded to upload the new document, and from which
-- they may replace it while an application is live. Platform default; a company overrides it
-- through the system-settings cascade.
INSERT INTO system_settings (setting_key, setting_value, description, tenant_id)
VALUES ('document_expiry_reminder_days', '30',
        'Days before an expiring document expires that the student is reminded to upload the new one', 0)
ON CONFLICT (tenant_id, setting_key) DO NOTHING;

COMMENT ON COLUMN document_type.is_expiring IS
    'Expires at the end of the company academic year it was uploaded in (GH-498)';
