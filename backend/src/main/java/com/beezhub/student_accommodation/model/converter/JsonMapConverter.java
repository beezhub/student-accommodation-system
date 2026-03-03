package com.beezhub.student_accommodation.model.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * JPA {@link AttributeConverter} that serialises a {@code Map<String, Object>} to a
 * JSON string for storage in a {@code jsonb} column, and deserialises it back on read.
 *
 * <p>The converter is NOT auto-applied ({@code autoApply = false}) so it must be
 * explicitly declared on each field via {@code @Convert(converter = JsonMapConverter.class)}.
 */
@Converter
@Slf4j
public class JsonMapConverter implements AttributeConverter<Map<String, Object>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialise templateData map to JSON", e);
            return null;
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(dbData, MAP_TYPE);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialise templateData JSON from DB: {}", dbData, e);
            return null;
        }
    }
}
