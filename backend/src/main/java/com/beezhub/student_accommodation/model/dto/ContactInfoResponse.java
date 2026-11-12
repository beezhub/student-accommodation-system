package com.beezhub.student_accommodation.model.dto;

/**
 * How to reach the people running this deployment. The same values the email footer shows.
 *
 * @param email the support mailbox, {@code APP_SUPPORT_EMAIL}
 * @param phone the office number, {@code APP_CONTACT_PHONE}; null when none is configured
 */
public record ContactInfoResponse(String email, String phone) {
}
