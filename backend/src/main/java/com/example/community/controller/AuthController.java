package com.example.community.controller;

import com.example.community.domain.Member;
import com.example.community.dto.AuthRequest;
import com.example.community.dto.AuthResponse;
import com.example.community.security.JwtUtil;
import com.example.community.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody AuthRequest req) {
        Member member = authService.register(req);
        String token = jwtUtil.createToken(member.getUsername());
        return new AuthResponse(member.getId(), member.getUsername(), member.getEmail(), member.getNickname(), token);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest req) {
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.username(), req.password())
        );
        Member member = (Member) auth.getPrincipal();
        String token = jwtUtil.createToken(member.getUsername());
        return new AuthResponse(member.getId(), member.getUsername(), member.getEmail(), member.getNickname(), token);
    }
}

