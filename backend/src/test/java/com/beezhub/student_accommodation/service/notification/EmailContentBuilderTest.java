package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailContentBuilderTest {

    @Mock
    private SpringTemplateEngine templateEngine;

    @InjectMocks
    private EmailContentBuilder emailContentBuilder;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailContentBuilder, "supportEmail", "test@test.com");
        ReflectionTestUtils.setField(emailContentBuilder, "universityName", "Test Uni");
    }

    @Test
    void extractFloorNumber_validRoomNumber_returnsFloor() {
        assertEquals(1, emailContentBuilder.extractFloorNumber("101"));
        assertEquals(2, emailContentBuilder.extractFloorNumber("205"));
    }

    @Test
    void extractFloorNumber_invalidRoomNumber_returns1() {
        assertEquals(1, emailContentBuilder.extractFloorNumber("Invalid"));
        assertEquals(1, emailContentBuilder.extractFloorNumber(""));
        assertEquals(1, emailContentBuilder.extractFloorNumber(null));
    }

    @Test
    void determineRoomType_capacity1_returnsSingle() {
        Room room = new Room();
        room.setCapacity(1);
        assertEquals("Single", emailContentBuilder.determineRoomType(room));
    }

    @Test
    void determineRoomType_capacity4_returnsQuad() {
        Room room = new Room();
        room.setCapacity(4);
        assertEquals("Quad", emailContentBuilder.determineRoomType(room));
    }

    @Test
    void calculateDuration_10Months_returns10Months() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 1, 0, 0);
        assertEquals("10 months", emailContentBuilder.calculateDuration(start, end));
    }

    @Test
    void calculateDuration_1Year_returns1Year() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2027, 1, 1, 0, 0);
        assertEquals("1 year", emailContentBuilder.calculateDuration(start, end));
    }

    @Test
    void parseWarnings_validJson_returnsList() {
        String json = "[\"Warning 1\", \"Warning 2\"]";
        List<String> result = emailContentBuilder.parseWarnings(json);
        assertEquals(2, result.size());
        assertEquals("Warning 1", result.get(0));
    }

    @Test
    void parseWarnings_invalidJson_returnsEmptyList() {
        String json = "Invalid JSON";
        List<String> result = emailContentBuilder.parseWarnings(json);
        assertTrue(result.isEmpty());
    }

    @Test
    void buildDocumentCommentEmailContent_validData_callsTemplateEngine() {
        when(templateEngine.process(eq("email/document-comment"), any(Context.class))).thenReturn("<html></html>");
        
        emailContentBuilder.buildDocumentCommentEmailContent("Student", "Doc", "Type", "Ref", "Comment", "Url");
        
        verify(templateEngine).process(eq("email/document-comment"), any(Context.class));
    }

    @Test
    void buildPasswordResetEmailContent_validData_callsTemplateEngine() {
        when(templateEngine.process(eq("email/password-reset"), any(Context.class))).thenReturn("<html></html>");

        emailContentBuilder.buildPasswordResetEmailContent("Student", "Url");

        verify(templateEngine).process(eq("email/password-reset"), any(Context.class));
    }
}
