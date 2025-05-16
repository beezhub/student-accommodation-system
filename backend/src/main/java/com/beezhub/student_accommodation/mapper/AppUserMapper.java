package com.beezhub.student_accommodation.mapper;


import com.beezhub.student_accommodation.model.dto.SignupRequest;
import com.beezhub.student_accommodation.model.dto.UserData;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.enums.UserRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface AppUserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "userPassword", source = "password")
    @Mapping(target = "firstName", source = "signupRequest.firstName")
    @Mapping(target = "lastName", source = "signupRequest.lastName")
    @Mapping(target = "email", source = "signupRequest.email")
    @Mapping(target = "userRole", expression = "java(mapRole(signupRequest.getUserRole()))")
    AppUser toEntity(SignupRequest signupRequest, String password);

    UserData toUserData(AppUser appUser);

    default UserRole mapRole(String role) {
        return UserRole.enumRoleOf(role);
    }
}
