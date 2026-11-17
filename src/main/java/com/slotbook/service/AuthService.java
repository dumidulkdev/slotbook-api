package com.slotbook.service;

import com.slotbook.domain.UserEntity;
import com.slotbook.dto.auth.AuthResponse;
import com.slotbook.dto.auth.LoginRequest;
import com.slotbook.dto.auth.RegisterRequest;
import com.slotbook.exception.ConflictException;
import com.slotbook.repository.UserRepository;
import com.slotbook.security.JwtService;
import com.slotbook.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email is already registered");
        }
        UserEntity user = new UserEntity(
                request.name(),
                request.email().toLowerCase(),
                passwordEncoder.encode(request.password()),
                request.role());
        userRepository.save(user);
        UserPrincipal principal = new UserPrincipal(user);
        String token = jwtService.generateToken(principal);
        return toAuthResponse(token, principal);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password()));
        UserEntity user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow();
        UserPrincipal principal = new UserPrincipal(user);
        String token = jwtService.generateToken(principal);
        return toAuthResponse(token, principal);
    }

    private AuthResponse toAuthResponse(String token, UserPrincipal principal) {
        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpirationMinutes(),
                principal.getId(),
                principal.getName(),
                principal.getUsername(),
                principal.getRole());
    }
}