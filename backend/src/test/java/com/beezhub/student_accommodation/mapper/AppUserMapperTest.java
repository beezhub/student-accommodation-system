package com.beezhub.student_accommodation.mapper;

import com.beezhub.student_accommodation.model.dto.CreateUserRequest;
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

    /**
     * #413: user management is now the only place an admin account is made, and the person behind
     * it gets a password someone else typed. The company admin screen forced a change at first
     * sign-in; moving creation here must not lose that.
     */
    @Test
    void toEntityFromAdminRequest_forcesAPasswordChangeAtFirstSignIn() {
        CreateUserRequest req = new CreateUserRequest();
        req.setFirstName("Thandi");
        req.setLastName("Mokoena");
        req.setEmail("thandi@company.test");
        req.setUserRole("Tenant_Admin");

        AppUser user = mapper.toEntityFromAdminRequest(req, "hashedPwd");

        assertTrue(user.getMustChangePassword());
    }

    /**
     * #413: user management lists UserData. Without the company it cannot tell an admin waiting for
     * one (platform tenant, locked) from a working account, and shows both the same way.
     */
    @Test
    void toUserData_carriesTheCompany() {
        AppUser user = AppUser.builder().email("thandi@company.test").tenantId(7L).build();

        assertEquals(7L, mapper.toUserData(user).getTenantId());
    }
}
