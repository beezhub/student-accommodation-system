package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.dto.ContactInfoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Contact details for this deployment, set by {@code APP_SUPPORT_EMAIL} and {@code APP_CONTACT_PHONE}.
 * Each company runs its own deployment (ADR-010), so these are environment settings, not
 * per-tenant rows. A blank phone means none: it is returned as null so no page shows an empty line.
 */
@Service
public class ContactInfoService {

    private final ContactInfoResponse contactInfo;

    public ContactInfoService(
            @Value("${app.support-email:accommodation@university.edu}") String supportEmail,
            @Value("${app.contact-phone:}") String contactPhone) {
        this.contactInfo = new ContactInfoResponse(supportEmail.trim(), blankToNull(contactPhone));
    }

    public ContactInfoResponse getContactInfo() {
        return contactInfo;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
