package com.beezhub.student_accommodation.service.notification;

import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests that render all 18 email templates using a standalone
 * Thymeleaf engine (no Spring context required) and verify the output is
 * well-formed and contains expected content.
 */
class EmailTemplateRenderTest {

    private static final String UNIVERSITY_NAME = "Test University";
    private static final String SUPPORT_EMAIL   = "test@university.edu";
    private static final String CONTACT_PHONE   = "+27 (0) 11 000 0000";
    private static final String PORTAL_URL      = "http://localhost:4200/portal";

    private TemplateEngine templateEngine;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(resolver);
        templateEngine.addDialect(new LayoutDialect());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Map<String, Object> baseContext() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("universityName", UNIVERSITY_NAME);
        vars.put("supportEmail", SUPPORT_EMAIL);
        vars.put("contactPhone", CONTACT_PHONE);
        vars.put("currentYear", Year.now().getValue());
        return vars;
    }

    private String render(String templateName, Map<String, Object> vars) {
        Context ctx = new Context();
        ctx.setVariables(vars);
        return templateEngine.process("email/" + templateName, ctx);
    }

    private void assertBaseRequirements(String html, String templateName) {
        assertThat(html).as("Template %s must not be empty", templateName).isNotEmpty();
        assertThat(html).as("Template %s must contain university name", templateName)
                .contains(UNIVERSITY_NAME);
        assertThat(html).as("Template %s must contain support email", templateName)
                .contains(SUPPORT_EMAIL);
        assertThat(html).as("Template %s must not contain <center> tags", templateName)
                .doesNotContain("<center");
        assertThat(html).as("Template %s must not hard-code year 2025", templateName)
                .doesNotContain(">2025<")
                .doesNotContain("© 2025");
    }

    // -------------------------------------------------------------------------
    // Individual template tests
    // -------------------------------------------------------------------------

    @Test
    void passwordReset_rendersCorrectly() {
        Map<String, Object> vars = baseContext();
        vars.put("studentName", "Jane Smith");
        vars.put("resetUrl", "http://localhost:4200/reset?token=abc123");

        String html = render("password-reset", vars);

        assertBaseRequirements(html, "password-reset");
        assertThat(html).contains("http://localhost:4200/reset?token=abc123");
        assertThat(html).contains("Jane Smith");
    }

    @Test
    void welcome_rendersCorrectly() {
        Map<String, Object> vars = baseContext();
        vars.put("studentName", "John Doe");
        vars.put("email", "john.doe@student.edu");
        vars.put("studentId", "STU-2025-001");
        vars.put("registrationDate", "2026-01-15");
        vars.put("loginUrl", PORTAL_URL + "/login");
        vars.put("guideUrl", PORTAL_URL + "/guide");

        String html = render("welcome", vars);

        assertBaseRequirements(html, "welcome");
        assertThat(html).contains("John Doe");
        assertThat(html).contains("STU-2025-001");
    }

    @Test
    void roomAllocated_rendersCorrectly() {
        Map<String, Object> vars = baseContext();
        vars.put("studentName", "Alice Brown");
        vars.put("roomNumber", "205B");
        vars.put("buildingName", "Westview Block");
        vars.put("floorNumber", 2);
        vars.put("roomType", "Single");
        vars.put("bedNumber", "1");
        vars.put("allocationId", "ALLOC-2026-099");
        vars.put("startDate", "2026-02-01");
        vars.put("endDate", "2026-11-30");
        vars.put("duration", "10 months");
        vars.put("officeLocation", "Admin Block, Ground Floor");
        vars.put("buildingAddress", "1 Campus Drive, City");
        vars.put("portalUrl", PORTAL_URL);

        String html = render("room-allocated", vars);

        assertBaseRequirements(html, "room-allocated");
        assertThat(html).contains("205B");
        assertThat(html).contains("Westview Block");
    }

    @Test
    void applicationApproved_rendersCorrectly() {
        Map<String, Object> vars = baseContext();
        vars.put("studentName", "Test Student");
        vars.put("applicationId", 42L);
        vars.put("applicationDate", "2026-01-10");
        vars.put("academicYear", "2026");
        vars.put("semester", "Semester 1");
        vars.put("portalUrl", PORTAL_URL);

        String html = render("application-approved", vars);

        assertBaseRequirements(html, "application-approved");
        assertThat(html).contains("Test Student");
    }

    @Test
    void applicationRejected_rendersCorrectly() {
        Map<String, Object> vars = baseContext();
        vars.put("studentName", "Test Student");
        vars.put("applicationId", 43L);
        vars.put("rejectionReason", "Incomplete documentation");
        vars.put("applicationDate", "2026-01-10");
        vars.put("reviewDate", "2026-01-15");
        vars.put("nextApplicationPeriod", "March 2026");
        vars.put("portalUrl", PORTAL_URL);

        String html = render("application-rejected", vars);

        assertBaseRequirements(html, "application-rejected");
        assertThat(html).contains("Incomplete documentation");
    }

    @Test
    void documentVerified_rendersCorrectly() {
        Map<String, Object> vars = baseContext();
        vars.put("studentName", "Test Student");
        vars.put("documentType", "National ID");
        vars.put("documentReference", "DOC-2026-001");
        vars.put("verificationDate", "2026-01-12");
        vars.put("verifiedBy", "Admin User");
        vars.put("portalUrl", PORTAL_URL);

        String html = render("document-verified", vars);

        assertBaseRequirements(html, "document-verified");
        assertThat(html).contains("National ID");
    }

    @Test
    void documentRejected_rendersCorrectly() {
        Map<String, Object> vars = baseContext();
        vars.put("studentName", "Test Student");
        vars.put("documentType", "Proof of Enrollment");
        vars.put("documentReference", "DOC-2026-002");
        vars.put("submissionDate", "2026-01-10");
        vars.put("reviewDate", "2026-01-13");
        vars.put("rejectionReason", "Document is blurry");
        vars.put("resubmissionDeadline", "2026-01-20");
        vars.put("uploadUrl", PORTAL_URL + "/upload");
        vars.put("feedbackPoints", List.of("Ensure the document is clear", "Use PDF format"));
        vars.put("portalUrl", PORTAL_URL);

        String html = render("document-rejected", vars);

        assertBaseRequirements(html, "document-rejected");
        assertThat(html).contains("Document is blurry");
    }

    @Test
    void documentComment_rendersCorrectly() {
        Map<String, Object> vars = baseContext();
        vars.put("studentName", "Test Student");
        vars.put("documentType", "Bank Statement");
        vars.put("documentName", "bank-statement-jan.pdf");
        vars.put("documentReference", "DOC-2026-003");
        vars.put("uploadDate", "2026-01-08");
        vars.put("documentStatus", "Under Review");
        vars.put("comment", "Please provide a more recent statement.");
        vars.put("reviewerName", "Admin User");
        vars.put("commentDate", "2026-01-14");
        vars.put("portalUrl", PORTAL_URL);

        String html = render("document-comment", vars);

        assertBaseRequirements(html, "document-comment");
        assertThat(html).contains("Please provide a more recent statement.");
    }

    @Test
    void allocationProposal_rendersCorrectly() {
        Map<String, Object> vars = baseContext();
        vars.put("studentName", "Test Student");
        vars.put("roomNumber", "301A");
        vars.put("buildingName", "Eastview Hall");
        vars.put("floorNumber", 3);
        vars.put("roomCapacity", 1);
        vars.put("proposalId", "PROP-2026-010");
        vars.put("score", new BigDecimal("87.50"));
        vars.put("warnings", List.of());
        vars.put("moveInDate", "2026-02-01");
        vars.put("portalUrl", PORTAL_URL);

        String html = render("allocation-proposal", vars);

        assertBaseRequirements(html, "allocation-proposal");
        assertThat(html).contains("301A");
        assertThat(html).contains("Eastview Hall");
    }

    @Test
    void allocationConfirmed_rendersCorrectly() {
        Map<String, Object> vars = baseContext();
        vars.put("studentName", "Test Student");
        vars.put("roomNumber", "301A");
        vars.put("buildingName", "Eastview Hall");
        vars.put("floorNumber", 3);
        vars.put("roomType", "Single");
        vars.put("allocationId", "ALLOC-2026-010");
        vars.put("moveInDate", "2026-02-01");
        vars.put("startDate", "2026-02-01");
        vars.put("endDate", "2026-11-30");
        vars.put("portalUrl", PORTAL_URL);

        String html = render("allocation-confirmed", vars);

        assertBaseRequirements(html, "allocation-confirmed");
        assertThat(html).contains("301A");
    }

    @Test
    void allocationRejected_rendersCorrectly() {
        Map<String, Object> vars = baseContext();
        vars.put("studentName", "Test Student");
        vars.put("roomNumber", "201");
        vars.put("buildingName", "North Block");
        vars.put("proposalId", "PROP-2026-005");
        vars.put("reason", "Higher priority student assigned");
        vars.put("waitingListPosition", 3);
        vars.put("portalUrl", PORTAL_URL);

        String html = render("allocation-rejected", vars);

        assertBaseRequirements(html, "allocation-rejected");
        assertThat(html).contains("Higher priority student assigned");
    }

    @Test
    void allocationMoved_rendersCorrectly() {
        Map<String, Object> vars = baseContext();
        vars.put("studentName", "Test Student");
        vars.put("oldRoomNumber", "204");
        vars.put("oldBuildingName", "Building A");
        vars.put("oldFloorNumber", 2);
        vars.put("newRoomNumber", "305");
        vars.put("newBuildingName", "Building B");
        vars.put("newFloorNumber", 3);
        vars.put("newRoomType", "Single");
        vars.put("effectiveDate", "2026-03-01");
        vars.put("reason", "Maintenance required");
        vars.put("portalUrl", PORTAL_URL);

        String html = render("allocation-moved", vars);

        assertBaseRequirements(html, "allocation-moved");
        assertThat(html).contains("305");
        assertThat(html).contains("Building B");
    }

    @Test
    void offerDeclinedRequeue_rendersCorrectly() {
        Map<String, Object> vars = baseContext();
        vars.put("studentName", "Test Student");
        vars.put("roomNumber", "102");
        vars.put("buildingName", "South Wing");
        vars.put("totalDeclineCount", 1);
        vars.put("declinesRemaining", 2);
        vars.put("declineReason", "Room too far from campus");
        vars.put("portalUrl", PORTAL_URL);

        String html = render("offer-declined-requeue", vars);

        assertBaseRequirements(html, "offer-declined-requeue");
        assertThat(html).contains("Room too far from campus");
    }

    @Test
    void offerDeclinedMaxReached_rendersCorrectly() {
        Map<String, Object> vars = baseContext();
        vars.put("studentName", "Test Student");
        vars.put("totalDeclineCount", 3);
        vars.put("applicationCode", "APP-2026-050");
        vars.put("portalUrl", PORTAL_URL);

        String html = render("offer-declined-max-reached", vars);

        assertBaseRequirements(html, "offer-declined-max-reached");
        assertThat(html).contains("APP-2026-050");
    }

    @Test
    void offerExpired_rendersCorrectly() {
        Map<String, Object> vars = baseContext();
        vars.put("studentName", "Test Student");
        vars.put("applicationCode", "APP-2026-051");
        vars.put("roomNumber", "107");
        vars.put("buildingName", "Central Block");
        vars.put("portalUrl", PORTAL_URL);

        String html = render("offer-expired", vars);

        assertBaseRequirements(html, "offer-expired");
        assertThat(html).contains("APP-2026-051");
    }

    @Test
    void offerExpiryReminder_rendersCorrectly() {
        Map<String, Object> vars = baseContext();
        vars.put("studentName", "Test Student");
        vars.put("roomNumber", "107");
        vars.put("buildingName", "Central Block");
        vars.put("expiryDate", "2026-02-10");
        vars.put("portalUrl", PORTAL_URL);

        String html = render("offer-expiry-reminder", vars);

        assertBaseRequirements(html, "offer-expiry-reminder");
        assertThat(html).contains("2026-02-10");
    }

    @Test
    void checkInReminder_rendersCorrectly() {
        Map<String, Object> vars = baseContext();
        vars.put("studentName", "Test Student");
        vars.put("roomNumber", "410");
        vars.put("buildingName", "West Residence");
        vars.put("deadlineDate", "2026-02-05");
        vars.put("portalUrl", PORTAL_URL);

        String html = render("check-in-reminder", vars);

        assertBaseRequirements(html, "check-in-reminder");
        assertThat(html).contains("2026-02-05");
    }

    @Test
    void checkInExpired_rendersCorrectly() {
        Map<String, Object> vars = baseContext();
        vars.put("studentName", "Test Student");
        vars.put("applicationCode", "APP-2026-052");
        vars.put("roomNumber", "410");
        vars.put("buildingName", "West Residence");
        vars.put("portalUrl", PORTAL_URL);

        String html = render("check-in-expired", vars);

        assertBaseRequirements(html, "check-in-expired");
        assertThat(html).contains("APP-2026-052");
    }

    // -------------------------------------------------------------------------
    // Cross-cutting assertions
    // -------------------------------------------------------------------------

    @Test
    void allTemplates_containUniversityName() {
        String[] templates = {
            "welcome", "password-reset",
            "application-approved", "application-rejected",
            "document-verified", "document-rejected", "document-comment",
            "room-allocated",
            "allocation-proposal", "allocation-confirmed", "allocation-rejected", "allocation-moved",
            "offer-declined-requeue", "offer-declined-max-reached",
            "offer-expired", "offer-expiry-reminder",
            "check-in-reminder", "check-in-expired"
        };

        for (String template : templates) {
            Map<String, Object> vars = minimalVars(template);
            String html = render(template, vars);
            assertThat(html)
                    .as("Template '%s' must contain university name", template)
                    .isNotEmpty()
                    .contains(UNIVERSITY_NAME);
        }
    }

    @Test
    void allTemplates_doNotContainCenterTag() {
        String[] templates = {
            "welcome", "password-reset",
            "application-approved", "application-rejected",
            "document-verified", "document-rejected", "document-comment",
            "room-allocated",
            "allocation-proposal", "allocation-confirmed", "allocation-rejected", "allocation-moved",
            "offer-declined-requeue", "offer-declined-max-reached",
            "offer-expired", "offer-expiry-reminder",
            "check-in-reminder", "check-in-expired"
        };

        for (String template : templates) {
            Map<String, Object> vars = minimalVars(template);
            String html = render(template, vars);
            assertThat(html)
                    .as("Template '%s' must not contain <center> tags", template)
                    .doesNotContain("<center");
        }
    }

    @Test
    void footer_doesNotContainHardcodedYear2025() {
        String[] templates = {
            "welcome", "password-reset",
            "application-approved", "application-rejected",
            "document-verified", "document-rejected", "document-comment",
            "room-allocated",
            "allocation-proposal", "allocation-confirmed", "allocation-rejected", "allocation-moved",
            "offer-declined-requeue", "offer-declined-max-reached",
            "offer-expired", "offer-expiry-reminder",
            "check-in-reminder", "check-in-expired"
        };

        for (String template : templates) {
            Map<String, Object> vars = minimalVars(template);
            String html = render(template, vars);
            assertThat(html)
                    .as("Template '%s' must not hard-code copyright year 2025", template)
                    .doesNotContain("© 2025")
                    .doesNotContain("&copy; 2025");
        }
    }

    // -------------------------------------------------------------------------
    // Minimal variable sets (used by cross-cutting tests)
    // -------------------------------------------------------------------------

    /**
     * Returns the minimum variables required to render a template without
     * Thymeleaf throwing a null-pointer during expression evaluation.
     * Only non-optional (no th:if guard) variables are included here.
     */
    private Map<String, Object> minimalVars(String template) {
        Map<String, Object> vars = baseContext();
        vars.put("studentName", "Test Student");
        vars.put("portalUrl", PORTAL_URL);

        switch (template) {
            case "password-reset" -> vars.put("resetUrl", "http://reset.example.com");
            case "welcome" -> {
                vars.put("email", "test@student.edu");
                vars.put("studentId", "STU-001");
                vars.put("registrationDate", "2026-01-01");
                vars.put("loginUrl", PORTAL_URL);
                vars.put("guideUrl", PORTAL_URL);
            }
            case "application-approved" -> {
                vars.put("applicationId", 1L);
                vars.put("applicationDate", "2026-01-01");
                vars.put("academicYear", "2026");
                vars.put("semester", "Semester 1");
            }
            case "application-rejected" -> {
                vars.put("applicationId", 1L);
                vars.put("rejectionReason", "Test reason");
                vars.put("applicationDate", "2026-01-01");
                vars.put("reviewDate", "2026-01-05");
                vars.put("nextApplicationPeriod", "March 2026");
            }
            case "document-verified" -> {
                vars.put("documentType", "ID");
                vars.put("documentReference", "DOC-001");
                vars.put("verificationDate", "2026-01-05");
                vars.put("verifiedBy", "Admin");
            }
            case "document-rejected" -> {
                vars.put("documentType", "ID");
                vars.put("documentReference", "DOC-001");
                vars.put("submissionDate", "2026-01-01");
                vars.put("reviewDate", "2026-01-05");
                vars.put("rejectionReason", "Blurry");
                vars.put("resubmissionDeadline", "2026-01-20");
                vars.put("uploadUrl", PORTAL_URL);
                vars.put("feedbackPoints", List.of());
            }
            case "document-comment" -> {
                vars.put("documentType", "ID");
                vars.put("documentName", "id.pdf");
                vars.put("documentReference", "DOC-001");
                vars.put("uploadDate", "2026-01-01");
                vars.put("documentStatus", "Under Review");
                vars.put("comment", "Test comment");
                vars.put("reviewerName", "Admin");
                vars.put("commentDate", "2026-01-05");
            }
            case "room-allocated" -> {
                vars.put("roomNumber", "101");
                vars.put("buildingName", "Block A");
                vars.put("floorNumber", 1);
                vars.put("roomType", "Single");
                vars.put("bedNumber", "1");
                vars.put("allocationId", "ALLOC-001");
                vars.put("startDate", "2026-02-01");
                vars.put("endDate", "2026-11-30");
                vars.put("duration", "10 months");
                vars.put("officeLocation", "Admin Building");
            }
            case "allocation-proposal" -> {
                vars.put("roomNumber", "101");
                vars.put("buildingName", "Block A");
                vars.put("floorNumber", 1);
                vars.put("roomCapacity", 1);
                vars.put("proposalId", "PROP-001");
                vars.put("score", new BigDecimal("80.0"));
                vars.put("warnings", List.of());
                vars.put("moveInDate", "2026-02-01");
            }
            case "allocation-confirmed" -> {
                vars.put("roomNumber", "101");
                vars.put("buildingName", "Block A");
                vars.put("floorNumber", 1);
                vars.put("roomType", "Single");
                vars.put("allocationId", "ALLOC-001");
                vars.put("moveInDate", "2026-02-01");
                vars.put("startDate", "2026-02-01");
                vars.put("endDate", "2026-11-30");
            }
            case "allocation-rejected" -> {
                vars.put("roomNumber", "101");
                vars.put("buildingName", "Block A");
                vars.put("proposalId", "PROP-001");
                vars.put("reason", "Test reason");
            }
            case "allocation-moved" -> {
                vars.put("oldRoomNumber", "101");
                vars.put("oldBuildingName", "Block A");
                vars.put("newRoomNumber", "202");
                vars.put("newBuildingName", "Block B");
                vars.put("effectiveDate", "2026-03-01");
            }
            case "offer-declined-requeue" -> {
                vars.put("roomNumber", "101");
                vars.put("buildingName", "Block A");
                vars.put("totalDeclineCount", 1);
                vars.put("declinesRemaining", 2);
            }
            case "offer-declined-max-reached" -> {
                vars.put("totalDeclineCount", 3);
                vars.put("applicationCode", "APP-001");
            }
            case "offer-expired" -> {
                vars.put("applicationCode", "APP-001");
                vars.put("roomNumber", "101");
                vars.put("buildingName", "Block A");
            }
            case "offer-expiry-reminder" -> {
                vars.put("roomNumber", "101");
                vars.put("buildingName", "Block A");
                vars.put("expiryDate", "2026-02-10");
            }
            case "check-in-reminder" -> {
                vars.put("roomNumber", "101");
                vars.put("buildingName", "Block A");
                vars.put("deadlineDate", "2026-02-05");
            }
            case "check-in-expired" -> {
                vars.put("applicationCode", "APP-001");
                vars.put("roomNumber", "101");
                vars.put("buildingName", "Block A");
            }
            default -> { /* no extra vars */ }
        }
        return vars;
    }
}
