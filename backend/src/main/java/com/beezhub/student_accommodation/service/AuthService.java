package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.EmailAlreadyRegisteredException;
import com.beezhub.student_accommodation.exceptions.UserNotFoundException;
import com.beezhub.student_accommodation.mapper.AppUserMapper;
import com.beezhub.student_accommodation.model.dto.LoginResponse;
import com.beezhub.student_accommodation.model.dto.SignupRequest;
import com.beezhub.student_accommodation.model.dto.UserData;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.security.jwt.JwtUtil;
import com.beezhub.student_accommodation.security.user.AppUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final AppUserMapper appUserMapper;

    @Transactional
    public LoginResponse signup(SignupRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyRegisteredException("Email already registered");
        }
        var user = appUserMapper.toEntity(request, passwordEncoder.encode(request.getPassword()));
        var save = userRepository.save(user);
        var userData = appUserMapper.toUserData(save);
        String token = jwtUtil.generateToken(new AppUserDetails(
                user.getEmail(),
                user.getUserPassword(),
                user.getUserRole().name()
        ));
        return new LoginResponse(token, userData);
    }

    public LoginResponse validateLogin(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtil.generateToken(userDetails);
        var user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        var userData = appUserMapper.toUserData(user);
        return new LoginResponse(token, userData);
    }
}
