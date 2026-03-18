package com.beezhub.student_accommodation.security.user;

import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.BuildingOwnerRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;
    private final BuildingOwnerRepository buildingOwnerRepository;

    public AppUserDetailsService(AppUserRepository appUserRepository, BuildingOwnerRepository buildingOwnerRepository) {
        this.appUserRepository = appUserRepository;
        this.buildingOwnerRepository = buildingOwnerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = appUserRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Long ownerProfileId = null;
        if (user.getUserRole() == UserRole.BUILDING_OWNER) {
            ownerProfileId = buildingOwnerRepository.findByUser_Id(user.getId())
                    .map(o -> o.getId())
                    .orElse(null);
        }

        return new AppUserDetails(
                user.getEmail(),
                user.getUserPassword(),
                user.getUserRole().name(),
                user.getTenantId(),
                ownerProfileId);
    }
}
