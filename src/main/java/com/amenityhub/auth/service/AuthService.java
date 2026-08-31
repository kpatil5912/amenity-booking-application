package com.amenityhub.auth.service;
import com.amenityhub.auth.entity.AppUserDetails;

import com.amenityhub.auth.dto.AuthResponse;
import com.amenityhub.auth.dto.LoginRequest;
import com.amenityhub.auth.dto.RegisterRequest;
import com.amenityhub.common.exception.ConflictException;
import com.amenityhub.common.exception.NotFoundException;
import com.amenityhub.user.entity.Role;
import com.amenityhub.user.repository.RoleRepository;
import com.amenityhub.user.entity.User;
import com.amenityhub.user.repository.UserRepository;
import java.util.List;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String DEFAULT_ROLE = "ROLE_RESIDENT";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already registered");
        }

        Role residentRole = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new NotFoundException("Default role missing: " + DEFAULT_ROLE));

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setPhoneNumber(request.phoneNumber());
        user.addRole(residentRole);
        userRepository.save(user);

        return issueToken(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
            AppUserDetails principal = (AppUserDetails) authentication.getPrincipal();
            return issueToken(principal.getUser());
        } catch (BadCredentialsException ex) {
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    private AuthResponse issueToken(User user) {
        List<String> roles = user.getRoles().stream().map(Role::getName).toList();
        String accessToken = jwtService.generateAccessToken(user.getEmail(), roles);
        return AuthResponse.bearer(accessToken, user.getEmail(), roles);
    }
}
