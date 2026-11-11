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
import java.util.Set;
import java.util.regex.Pattern;

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
    private static final String PORTAL_URL      = "http://localhost:4200";

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
    void layout_blankContactPhone_hidesThePhoneNumber() {
        Map<String, Object> vars = baseContext();
        vars.put("contactPhone", "");
        vars.put("resetUrl", "http://localhost:4200/reset?token=abc123");

        assertThat(render("password-reset", vars)).doesNotContain(CONTACT_PHONE);
    }

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

    /**
     * GH-141 in one test: the map persisted with the notification row must be enough to rebuild
     * the email days later. Anything the retry job cannot render is an email the user never gets.
     * The keys here are exactly those EmailDispatchService.welcomeTemplateData produces.
     */
    @Test
    void welcome_rendersFromThePersistedTemplateData() {
        Map<String, Object> vars = baseContext();
        vars.put("templateName", "welcome");
        vars.put("studentName", "Alice Smith");
        vars.put("email", "alice@example.com");
        vars.put("studentId", "N/A");
        vars.put("registrationDate", "15 Jan 2026");
        vars.put("loginUrl", PORTAL_URL + "/login");

        String html = render("welcome", vars);

        assertBaseRequirements(html, "welcome");
        assertThat(html).contains("Alice Smith");
        assertThat(html).contains(PORTAL_URL + "/login");
    }

    @Test
    void welcome_rendersCorrectly() {
        Map<String, Object> vars = baseContext();
        vars.put("studentName", "John Doe");
        vars.put("email", "john.doe@student.edu");
        vars.put("studentId", "STU-2025-001");
        vars.put("registrationDate", "2026-01-15");
        vars.put("loginUrl", PORTAL_URL + "/login");

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

    /** This email goes out for an OFFERED allocation, which lapses unless the student accepts it. */
    @Test
    void roomAllocated_unansweredOffer_saysToAcceptByTheDeadline() {
        Map<String, Object> vars = baseContext();
        vars.put("studentName", "Alice Brown");
        vars.put("roomNumber", "205B");
        vars.put("buildingName", "Westview Block");
        vars.put("portalUrl", PORTAL_URL + "/dashboard");
        vars.put("offerExpiryDate", "11 November 2026 at 14:30 UTC");

        String html = render("room-allocated", vars);

        assertThat(html).contains("11 November 2026 at 14:30 UTC");
        assertThat(html).contains("must accept");
        assertThat(html).contains("Accept Offer");
        assertThat(html).doesNotContain("Your Room is Ready");
        assertThat(html).doesNotContain("successfully");
        assertThat(html).doesNotContain("confirmed");
    }

    @Test
    void applicationApproved_rendersCorrectly() {
        Map<String, Object> vars = baseContext();
        vars.put("studentName", "Test Student");
        vars.put("applicationId", 42L);
        vars.put("applicationDate", "2026-01-10");
        vars.put("academicYear", "2026");
        vars.put("intake", "Semester 1");
        vars.put("portalUrl", PORTAL_URL);

        String html = render("application-approved", vars);

        assertBaseRequirements(html, "application-approved");
        assertThat(html).contains("Test Student");
        // GH-495: the intake applied for, not a semester guessed from the month.
        assertThat(html).contains("Intake:").contains("Semester 1").doesNotContain("Semester:</strong>");
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
    void applicationChangesRequested_rendersReasonAndLink() {
        Map<String, Object> vars = baseContext();
        vars.put("studentName", "Test Student");
        vars.put("applicationId", 43L);
        vars.put("changesReason", "Proof of registration is for the wrong year");
        vars.put("portalUrl", "http://portal.example.com/applications/43");

        String html = render("application-changes-requested", vars);

        assertBaseRequirements(html, "application-changes-requested");
        assertThat(html)
                .contains("Proof of registration is for the wrong year")
                .contains("href=\"http://portal.example.com/applications/43\"");
    }

    /** The reason is free text typed by an admin, and it ends up in an HTML email. */
    @Test
    void applicationChangesRequested_escapesTheReason() {
        Map<String, Object> vars = baseContext();
        vars.put("studentName", "Test Student");
        vars.put("applicationId", 43L);
        vars.put("changesReason", "<script>alert(1)</script>");
        vars.put("portalUrl", PORTAL_URL);

        String html = render("application-changes-requested", vars);

        assertThat(html).doesNotContain("<script>alert(1)</script>").contains("&lt;script&gt;");
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
        vars.put("declinesRemaining", 1);
        vars.put("maxDeclines", 3);
        vars.put("declineReason", "Room too far from campus");
        vars.put("portalUrl", PORTAL_URL);

        String html = render("offer-declined-requeue", vars);

        assertBaseRequirements(html, "offer-declined-requeue");
        assertThat(html).contains("Room too far from campus");
    }

    @Test
    void offerDeclinedRequeue_firstDecline_showsTheTrueCountAndLimit() {
        Map<String, Object> vars = baseContext();
        vars.put("totalDeclineCount", 1);
        vars.put("declinesRemaining", 1);
        vars.put("maxDeclines", 3);

        String html = render("offer-declined-requeue", vars);

        assertThat(html).doesNotContain("of 2");
        assertThat(html).contains("<strong>1</strong> offer.");
        assertThat(html).contains("<strong>1</strong> more");
        assertThat(html).contains("<strong>3</strong> offers");
    }

    @Test
    void offerDeclinedRequeue_secondDecline_warnsTheNextOneClosesTheApplication() {
        Map<String, Object> vars = baseContext();
        vars.put("totalDeclineCount", 2);
        vars.put("declinesRemaining", 0);
        vars.put("maxDeclines", 3);

        String html = render("offer-declined-requeue", vars);

        assertThat(html).contains("<strong>2</strong> offers.");
        assertThat(html).contains("decline your next offer, your application will be closed");
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

    /** GH-496: "Still coming?" names the move-in and release dates. */
    @Test
    void reconfirmRequest_rendersCorrectly() {
        Map<String, Object> vars = baseContext();
        vars.put("studentName", "Test Student");
        vars.put("roomNumber", "12");
        vars.put("buildingName", "West Residence");
        vars.put("moveInDate", "01 February 2027");
        vars.put("releaseDate", "18 January 2027");
        vars.put("portalUrl", PORTAL_URL);

        for (String template : List.of("reconfirm-request", "reconfirm-reminder")) {
            String html = render(template, vars);
            assertBaseRequirements(html, template);
            assertThat(html).contains("01 February 2027").contains("18 January 2027").contains("Still Coming?");
        }
    }

    @Test
    void placeReleased_rendersCorrectly() {
        Map<String, Object> vars = baseContext();
        vars.put("studentName", "Test Student");
        vars.put("applicationCode", "APP-2027-001");
        vars.put("roomNumber", "12");
        vars.put("buildingName", "West Residence");
        vars.put("moveInDate", "01 February 2027");
        vars.put("portalUrl", PORTAL_URL);

        String html = render("place-released", vars);

        assertBaseRequirements(html, "place-released");
        assertThat(html).contains("APP-2027-001").contains("01 February 2027");
    }

    /** GH-497: the expiry email offers the next open intake when there is one. */
    @Test
    void applicationExpired_withNextIntake_offersIt() {
        Map<String, Object> vars = expiredVars();
        vars.put("nextIntakeName", "Semester 2, 2027");
        vars.put("nextMoveInDate", "15 July 2027");
        vars.put("nextApplyByDate", "01 July 2027");

        String html = render("application-expired", vars);

        assertBaseRequirements(html, "application-expired");
        assertThat(html).contains("APP-2027-007").contains("30 June 2027").contains("Semester 2, 2027")
                .contains("15 July 2027").contains("http://localhost:4200/applications/new");
    }

    @Test
    void applicationExpired_noIntakeOpen_saysSoAndLinksToApplications() {
        String html = render("application-expired", expiredVars());

        assertBaseRequirements(html, "application-expired");
        assertThat(html).contains("No applications are open").doesNotContain("Apply Now");
    }

    /** GH-497: one email lists every application withdrawn on another acceptance. */
    @Test
    void applicationsWithdrawn_listsEachApplication() {
        Map<String, Object> vars = baseContext();
        vars.put("studentName", "Test Student");
        vars.put("acceptedPlace", "Room 12, West Residence");
        vars.put("withdrawn", List.of("APP-1 at Company B, Semester 1, 2027", "APP-2 at Company C, Annual, 2027"));
        vars.put("portalUrl", PORTAL_URL);

        String html = render("applications-withdrawn", vars);

        assertBaseRequirements(html, "applications-withdrawn");
        assertThat(html).contains("Room 12, West Residence").contains("APP-1 at Company B, Semester 1, 2027")
                .contains("APP-2 at Company C, Annual, 2027");
    }

    /** GH-498: one date and one action, before expiry and after it. */
    @Test
    void documentExpiryReminder_beforeAndAfterExpiry_saysWhenAndWhatToDo() {
        Map<String, Object> vars = baseContext();
        vars.put("studentName", "Test Student");
        vars.put("documentName", "Proof of Registration");
        vars.put("expiryDate", "30 November 2026");
        vars.put("companyName", "Company A");
        vars.put("documentsUrl", "http://localhost:4200/documents");

        vars.put("expired", false);
        String soon = render("document-expiry-reminder", vars);
        vars.put("expired", true);
        String expired = render("document-expiry-reminder", vars);

        assertBaseRequirements(soon, "document-expiry-reminder");
        assertThat(soon).contains("expires on").contains("30 November 2026").contains("Upload the new one")
                .contains("http://localhost:4200/documents").doesNotContain("expired on");
        assertThat(expired).contains("expired on").contains("30 November 2026");
    }

    /** GH-498: the confirmation names the intake, the room and the new check-out date. */
    @Test
    void stayExtended_confirmsTheRoomAndTheNewDate() {
        Map<String, Object> vars = baseContext();
        vars.put("studentName", "Test Student");
        vars.put("roomNumber", "12");
        vars.put("buildingName", "West Residence");
        vars.put("intakeName", "Semester 2");
        vars.put("stayEndDate", "30 November 2027");
        vars.put("portalUrl", PORTAL_URL);

        String html = render("stay-extended", vars);

        assertBaseRequirements(html, "stay-extended");
        assertThat(html).contains("Semester 2").contains("West Residence").contains("30 November 2027");
    }

    private Map<String, Object> expiredVars() {
        Map<String, Object> vars = baseContext();
        vars.put("studentName", "Test Student");
        vars.put("applicationCode", "APP-2027-007");
        vars.put("companyName", "Company A");
        vars.put("intakeName", "Semester 1, 2027");
        vars.put("stayEndDate", "30 June 2027");
        vars.put("applyUrl", "http://localhost:4200/applications/new");
        vars.put("portalUrl", PORTAL_URL);
        return vars;
    }

    // -------------------------------------------------------------------------
    // Cross-cutting assertions
    // -------------------------------------------------------------------------

    /**
     * Every template a student is actually sent. welcome and password-reset are left out: nothing
     * sends them, and GH-533 deletes them.
     */
    private static final List<String> SENT_TEMPLATES = List.of(
            "application-approved", "application-rejected", "application-changes-requested",
            "document-verified", "document-rejected", "document-comment",
            "room-allocated",
            "allocation-proposal", "allocation-confirmed", "allocation-rejected", "allocation-moved",
            "offer-declined-requeue", "offer-declined-max-reached",
            "offer-expired", "offer-expiry-reminder",
            "check-in-reminder", "check-in-expired",
            "reconfirm-request", "reconfirm-reminder", "place-released",
            "application-expired", "applications-withdrawn",
            "document-expiry-reminder", "stay-extended");

    /**
     * The app's colour tokens (frontend/src/styles/_variables.scss: primary #7c3aed, success
     * #059669, warning #d97706, error #dc2626, info #0284c7, the slate greys) plus light and
     * dark shades from the same palette families for box backgrounds and headings (GH-526).
     */
    private static final Set<String> SYSTEM_COLOURS = Set.of(
            "#ffffff",
            "#f8fafc", "#f1f5f9", "#e2e8f0", "#cbd5e1", "#64748b", "#475569", "#334155", "#1e293b",
            "#7c3aed", "#6d28d9", "#f5f3ff",
            "#059669", "#ecfdf5", "#065f46",
            "#d97706", "#fffbeb", "#92400e",
            "#dc2626", "#fef2f2", "#991b1b",
            "#0284c7", "#f0f9ff", "#075985");

    /** A CSS hex colour, but not an HTML character reference such as &#127968;. */
    private static final Pattern HEX_COLOUR = Pattern.compile("(?<!&)#(?:[0-9a-fA-F]{6}|[0-9a-fA-F]{3})\\b");

    @Test
    void sentTemplates_useOnlySystemColours() {
        for (String template : SENT_TEMPLATES) {
            String html = render(template, minimalVars(template));
            List<String> colours = HEX_COLOUR.matcher(html).results()
                    .map(m -> m.group().toLowerCase()).distinct().toList();

            assertThat(colours).as("colours in %s", template).allMatch(SYSTEM_COLOURS::contains);
            // Outlook ignores CSS gradients, so a gradient header renders as no colour at all.
            assertThat(html).as("gradients in %s", template).doesNotContain("gradient");
        }
    }

    /** Gmail draws links in its own blue unless the colour is on the element itself. */
    @Test
    void sentTemplates_buttonTextColourIsInline() {
        Pattern button = Pattern.compile("<a[^>]*class=\"cta-button\"[^>]*>");
        for (String template : SENT_TEMPLATES) {
            String html = render(template, minimalVars(template));
            List<String> buttons = button.matcher(html).results().map(m -> m.group()).toList();

            assertThat(buttons).as("buttons in %s", template)
                    .allMatch(tag -> tag.contains("color: #ffffff"));
        }
    }

    /** No terms to accept, no payment taken, no amenities stored, no policy pages (GH-526). */
    @Test
    void sentTemplates_doNotPromiseThingsThatDoNotExist() {
        for (String template : SENT_TEMPLATES) {
            String html = render(template, minimalVars(template)).toLowerCase();

            assertThat(html).as(template)
                    .doesNotContain("terms and conditions")
                    .doesNotContain("payment")
                    .doesNotContain("fee structure")
                    .doesNotContain("amenities")
                    .doesNotContain("gym access")
                    .doesNotContain("privacy policy")
                    .doesNotContain("terms of service")
                    .doesNotContain("href=\"#\"");
        }
    }

    /** The student knows their application by its code, as the dashboard shows it (GH-526). */
    @Test
    void applicationEmails_showTheApplicationCodeNotTheDatabaseId() {
        for (String template : List.of("application-approved", "application-rejected",
                "application-changes-requested")) {
            Map<String, Object> vars = minimalVars(template);
            vars.put("applicationId", 9876L);
            vars.put("applicationCode", "APP-2026-001");

            String html = render(template, vars);

            assertThat(html).as(template).contains("APP-2026-001").doesNotContain("9876");
        }
    }

    /**
     * GH-528: a proposal is a draft the office still reviews. The scoring engine's codes and
     * score are for admins. Older notification rows still carry these keys, which is why they
     * are passed here: the template itself must not show them.
     */
    @Test
    void allocationProposal_showsNoInternalScoringData() {
        Map<String, Object> vars = minimalVars("allocation-proposal");
        vars.put("roomNumber", "301A");
        vars.put("buildingName", "Eastview Hall");
        vars.put("score", new BigDecimal("87.50"));
        vars.put("warnings", List.of("BUILDING_MISMATCH", "CONSTRAINT_OVERRIDE: gender"));
        vars.put("proposalId", "PROP-2026-010");
        vars.put("floorNumber", 3);
        vars.put("moveInDate", "01 February 2027");

        String html = render("allocation-proposal", vars);

        assertThat(html)
                .doesNotContain("87.5")
                .doesNotContain("Match Score")
                .doesNotContain("BUILDING_MISMATCH")
                .doesNotContain("CONSTRAINT_OVERRIDE")
                .doesNotContain("PROP-")
                .doesNotContain("Floor")
                .doesNotContain("Move-In")
                .doesNotContain("Great news")
                .contains("reviewing");
        assertThat(html.split("301A", -1)).as("room number shown once").hasSize(2);
        assertThat(html.split("Eastview Hall", -1)).as("building shown once").hasSize(2);
    }

    @Test
    void allocationRejected_doesNotShowTheProposalId() {
        Map<String, Object> vars = minimalVars("allocation-rejected");
        vars.put("proposalId", "PROP-2026-005");

        assertThat(render("allocation-rejected", vars)).doesNotContain("PROP-");
    }

    /** Rooms store no floor; the old one was guessed from the room number's first digit (GH-528). */
    @Test
    void roomEmails_doNotShowAGuessedFloor() {
        for (String template : List.of("room-allocated", "allocation-confirmed", "allocation-moved")) {
            Map<String, Object> vars = minimalVars(template);
            vars.put("floorNumber", 1);
            vars.put("oldFloorNumber", 1);
            vars.put("newFloorNumber", 2);

            assertThat(render(template, vars)).as(template).doesNotContain("Floor");
        }
    }

    /**
     * GH-529: the offer used to list a bed number, an internal id, an office and move-in steps
     * that no data supplied. Older notification rows still carry those keys, so they are passed.
     */
    @Test
    void roomAllocated_showsNothingInvented() {
        Map<String, Object> vars = minimalVars("room-allocated");
        vars.put("roomNumber", "205B");
        vars.put("bedNumber", "1");
        vars.put("allocationId", "ALLOC-2026-099");
        vars.put("officeLocation", "Main Administration Building, Ground Floor");
        vars.put("buildingAddress", "1 Campus Drive, City");

        String html = render("room-allocated", vars);

        assertThat(html)
                .doesNotContain("Bed Number")
                .doesNotContain("ALLOC-")
                .doesNotContain("Main Administration Building")
                .doesNotContain("orientation")
                .doesNotContain("2:00 PM")
                .doesNotContain("Important Reminders")
                .doesNotContain("View map in your student portal")
                .contains("1 Campus Drive, City");
        assertThat(html.split("205B", -1)).as("room number shown once").hasSize(2);
        assertThat(html.split("Review and Accept Offer", -1)).as("one button").hasSize(2);
    }

    @Test
    void roomAllocated_withMapsUrl_linksToTheMap() {
        Map<String, Object> vars = minimalVars("room-allocated");
        vars.put("buildingAddress", "1 Campus Drive, City");
        vars.put("mapsUrl", "https://www.google.com/maps/search/?api=1&query=-26.1929,28.0305");

        assertThat(render("room-allocated", vars))
                .contains("href=\"https://www.google.com/maps/search/?api=1&amp;query=-26.1929,28.0305\"");
    }

    /** No stay dates on the allocation: no tenancy block, rather than invented dates. */
    @Test
    void roomAllocated_noStayDates_hidesTheTenancy() {
        Map<String, Object> vars = minimalVars("room-allocated");
        vars.remove("startDate");
        vars.remove("endDate");
        vars.remove("duration");

        assertThat(render("room-allocated", vars)).doesNotContain("Tenancy");
    }

    /** By approval the documents are uploaded, and checking them is the office's job (GH-526). */
    @Test
    void applicationApproved_nextStepsAreWhatHappensNext() {
        String html = render("application-approved", minimalVars("application-approved"));

        assertThat(html)
                .doesNotContain("Upload all required documents")
                .doesNotContain("within 7 days")
                .doesNotContain("2-3 business days")
                .contains("room offer");
    }

    @Test
    void allTemplates_containUniversityName() {
        String[] templates = {
            "welcome", "password-reset",
            "application-approved", "application-rejected", "application-changes-requested",
            "document-verified", "document-rejected", "document-comment",
            "room-allocated",
            "allocation-proposal", "allocation-confirmed", "allocation-rejected", "allocation-moved",
            "offer-declined-requeue", "offer-declined-max-reached",
            "offer-expired", "offer-expiry-reminder",
            "check-in-reminder", "check-in-expired",
            "reconfirm-request", "reconfirm-reminder", "place-released",
            "application-expired", "applications-withdrawn",
            "document-expiry-reminder", "stay-extended"
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
            "application-approved", "application-rejected", "application-changes-requested",
            "document-verified", "document-rejected", "document-comment",
            "room-allocated",
            "allocation-proposal", "allocation-confirmed", "allocation-rejected", "allocation-moved",
            "offer-declined-requeue", "offer-declined-max-reached",
            "offer-expired", "offer-expiry-reminder",
            "check-in-reminder", "check-in-expired",
            "reconfirm-request", "reconfirm-reminder", "place-released",
            "application-expired", "applications-withdrawn",
            "document-expiry-reminder", "stay-extended"
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
            "application-approved", "application-rejected", "application-changes-requested",
            "document-verified", "document-rejected", "document-comment",
            "room-allocated",
            "allocation-proposal", "allocation-confirmed", "allocation-rejected", "allocation-moved",
            "offer-declined-requeue", "offer-declined-max-reached",
            "offer-expired", "offer-expiry-reminder",
            "check-in-reminder", "check-in-expired",
            "reconfirm-request", "reconfirm-reminder", "place-released",
            "application-expired", "applications-withdrawn",
            "document-expiry-reminder", "stay-extended"
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
            }
            case "application-approved" -> {
                vars.put("applicationId", 1L);
                vars.put("applicationDate", "2026-01-01");
                vars.put("academicYear", "2026");
                vars.put("intake", "Semester 1");
            }
            case "application-rejected" -> {
                vars.put("applicationId", 1L);
                vars.put("rejectionReason", "Test reason");
                vars.put("applicationDate", "2026-01-01");
                vars.put("reviewDate", "2026-01-05");
                vars.put("nextApplicationPeriod", "March 2026");
            }
            case "application-changes-requested" -> {
                vars.put("applicationId", 1L);
                vars.put("changesReason", "Test reason");
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
                vars.put("declinesRemaining", 1);
                vars.put("maxDeclines", 3);
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
            case "reconfirm-request", "reconfirm-reminder" -> {
                vars.put("roomNumber", "101");
                vars.put("buildingName", "Block A");
                vars.put("moveInDate", "01 February 2027");
                vars.put("releaseDate", "18 January 2027");
            }
            case "applications-withdrawn" -> {
                vars.put("acceptedPlace", "Room 101, Block A");
                vars.put("withdrawn", List.of("APP-001 at Company B"));
            }
            case "application-expired" -> {
                vars.put("applicationCode", "APP-001");
                vars.put("companyName", "Company A");
                vars.put("intakeName", "Semester 1, 2027");
                vars.put("stayEndDate", "30 June 2027");
                vars.put("applyUrl", PORTAL_URL);
            }
            case "document-expiry-reminder" -> {
                vars.put("documentName", "Proof of Registration");
                vars.put("expiryDate", "30 November 2026");
                vars.put("expired", false);
                vars.put("companyName", "Company A");
                vars.put("documentsUrl", PORTAL_URL);
            }
            case "stay-extended" -> {
                vars.put("roomNumber", "101");
                vars.put("buildingName", "Block A");
                vars.put("intakeName", "Semester 2");
                vars.put("stayEndDate", "30 November 2027");
            }
            case "place-released" -> {
                vars.put("applicationCode", "APP-001");
                vars.put("roomNumber", "101");
                vars.put("buildingName", "Block A");
                vars.put("moveInDate", "01 February 2027");
            }
            default -> { /* no extra vars */ }
        }
        return vars;
    }
}
