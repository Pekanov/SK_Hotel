package com.hotel.bookingservice.controller;

import com.hotel.bookingservice.dto.AuthRequest;
import com.hotel.bookingservice.dto.AuthResponse;
import com.hotel.bookingservice.dto.UserRequest;
import com.hotel.bookingservice.dto.UserResponse;
import com.hotel.bookingservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "User registration, authentication and management")
public class UserController {

    private final UserService userService;

    @PostMapping("/user/register")
    @Operation(summary = "Register a new user", description = "Public endpoint for user registration")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody UserRequest request) {
        log.info("POST /user/register - Registering user: {}", request.getUsername());
        AuthResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/user/auth")
    @Operation(summary = "Authenticate user", description = "Public endpoint for user authentication")
    public ResponseEntity<AuthResponse> authenticate(@Valid @RequestBody AuthRequest request) {
        log.info("POST /user/auth - Authenticating user: {}", request.getUsername());
        AuthResponse response = userService.authenticate(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/user")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Create a new user", description = "Admin only operation")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        log.info("POST /user - Creating user: {}", request.getUsername());
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/user")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Update user", description = "Admin only operation")
    public ResponseEntity<UserResponse> updateUser(
            @RequestParam Long id,
            @Valid @RequestBody UserRequest request) {
        log.info("PATCH /user?id={} - Updating user", id);
        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/user")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Delete user", description = "Admin only operation")
    public ResponseEntity<Void> deleteUser(@RequestParam Long id) {
        log.info("DELETE /user?id={} - Deleting user", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get all users", description = "Admin only operation")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("GET /users - Fetching all users");
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
}