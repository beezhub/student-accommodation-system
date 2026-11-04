package com.beezhub.student_accommodation.mapper;

import com.beezhub.student_accommodation.model.dto.StudentRequest;
import com.beezhub.student_accommodation.model.dto.StudentResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class StudentMapperTest {

    private final StudentMapper mapper = Mappers.getMapper(StudentMapper.class);
    private final ObjectMapper json = JsonMapper.builder().findAndAddModules().build();

    /**
     * #464: MapStruct generated {@code new SimpleDateFormat().format(...)}, so the API answered in
     * the server's locale, such as {@code 5/14/00, 12:00 AM}, and the browser had to guess.
     */
    @Test
    void toResponse_dateOfBirthFromTheApi_comesBackAsTheSameCalendarDate() throws Exception {
        StudentRequest request = json.readValue("{\"dateOfBirth\":\"2000-05-14\"}", StudentRequest.class);

        StudentResponse response = mapper.toResponse(mapper.toEntity(request));

        assertThat(response.getDateOfBirth()).isEqualTo("2000-05-14");
    }

    @Test
    void toResponse_noDateOfBirth_staysNull() throws Exception {
        StudentRequest request = json.readValue("{}", StudentRequest.class);

        StudentResponse response = mapper.toResponse(mapper.toEntity(request));

        assertThat(response.getDateOfBirth()).isNull();
    }
}
