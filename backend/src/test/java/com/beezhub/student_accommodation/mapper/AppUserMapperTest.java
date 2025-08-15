package com.beezhub.student_accommodation.mapper;

import com.beezhub.student_accommodation.model.dto.SignupRequest;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.dto.UserData;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.*;

class AppUserMapperTest {

    private AppUserMapper mapper = Mappers.getMapper(AppUserMapper.class);

    @Test
    void toEntityAndToUserData() {
        SignupRequest req = new SignupRequest();
        req.setFirstName("John");
        req.setLastName("Doe");
        req.setEmail("john.doe@example.com");
        req.setUserRole("STUDENT");

        AppUser user = mapper.toEntity(req, "hashedPwd");
        assertNotNull(user);
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals("john.doe@example.com", user.getEmail());
        assertEquals("hashedPwd", user.getUserPassword());
        assertNotNull(user.getUserRole());

        UserData ud = mapper.toUserData(user);
        assertNotNull(ud);
        assertEquals(user.getEmail(), ud.getEmail());
    }
}
