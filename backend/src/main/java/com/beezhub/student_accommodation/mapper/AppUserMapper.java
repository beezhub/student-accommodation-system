package com.beezhub.student_accommodation.mapper;


import com.beezhub.student_accommodation.model.dto.CreateUserRequest;
import com.beezhub.student_accommodation.model.dto.SignupRequest;
import com.beezhub.student_accommodation.model.dto.UserData;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.enums.UserRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface AppUserMapper {
    // Public signup - always creates STUDENT accounts
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "userPassword", source = "password")
    @Mapping(target = "firstName", source = "signupRequest.firstName")
    @Mapping(target = "lastName", source = "signupRequest.lastName")
    @Mapping(target = "email", source = "signupRequest.email")
    @Mapping(target = "userRole", constant = "STUDENT")
    @Mapping(target = "emailVerified", constant = "false")
    @Mapping(target = "mustChangePassword", constant = "false")
    AppUser toEntity(SignupRequest signupRequest, String password);

    // Admin-created users - ADMIN/LANDLORD only
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "userPassword", source = "password")
    @Mapping(target = "firstName", source = "request.firstName")
    @Mapping(target = "lastName", source = "request.lastName")
    @Mapping(target = "email", source = "request.email")
    @Mapping(target = "userRole", expression = "java(mapAdminRole(request.getUserRole()))")
    @Mapping(target = "emailVerified", constant = "true")
    @Mapping(target = "mustChangePassword", constant = "false")
    AppUser toEntityFromAdminRequest(CreateUserRequest request, String password);

    UserData toUserData(AppUser appUser);

    default UserRole mapRole(String role) {
        return UserRole.enumRoleOf(role);
    }

    // Validate only ADMIN/LANDLORD roles for admin-created users
    default UserRole mapAdminRole(String role) {
        UserRole userRole = UserRole.enumRoleOf(role);
        if (userRole != UserRole.ADMIN && userRole != UserRole.LANDLORD) {
            throw new IllegalArgumentException("Can only create ADMIN or LANDLORD users through this endpoint");
        }
        return userRole;
    }
}
