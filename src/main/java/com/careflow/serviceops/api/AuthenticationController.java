package com.careflow.serviceops.api;

import com.careflow.serviceops.api.dto.LoginRequest;
import com.careflow.serviceops.api.dto.LoginResponse;
import com.careflow.serviceops.security.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authenticationService.login(request);
    }
}