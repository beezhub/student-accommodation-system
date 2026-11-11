package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.service.EmailService;

import java.util.Map;

/** One email: its template, subject and typed variables. */
public record TemplatedEmail(String templateName, String subject, EmailContext context) {

    /** The variables with the template name, as stored on the notification for retries. */
    public Map<String, Object> templateData() {
        Map<String, Object> data = EmailService.toVariableMap(context);
        data.put("templateName", templateName);
        return data;
    }
}
