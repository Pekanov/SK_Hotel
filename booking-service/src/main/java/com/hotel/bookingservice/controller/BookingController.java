package com.hotel.bookingservice.controller;

import com.hotel.bookingservice.dto.BookingRequest;
import com.hotel.bookingservice.dto.BookingResponse;
import com.hotel.bookingservice.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Booking Management", description = "Booking operations")
@SecurityRequirement(name = "Bearer Authentication")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/booking")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Create a new booking",
            description = "Create a booking with manual room selection or auto-selection")
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody BookingRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        log.info("POST /booking - Creating booking for user: {}", username);
        BookingResponse response = bookingService.createBooking(username, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/bookings")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get user's booking history")
    public ResponseEntity<List<BookingResponse>> getUserBookings(Authentication authentication) {
        String username = authentication.getName();
        log.info("GET /bookings - Fetching bookings for user: {}", username);
        List<BookingResponse> bookings = bookingService.getUserBookings(username);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/booking/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get booking by ID")
    public ResponseEntity<BookingResponse> getBookingById(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication.getName();
        log.info("GET /booking/{} - Fetching booking for user: {}", id, username);
        BookingResponse booking = bookingService.getBookingById(username, id);
        return ResponseEntity.ok(booking);
    }

    @DeleteMapping("/booking/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Cancel booking")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication.getName();
        log.info("DELETE /booking/{} - Cancelling booking for user: {}", id, username);
        bookingService.cancelBooking(username, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bookings/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all bookings", description = "Admin only operation")
    public ResponseEntity<List<BookingResponse>> getAllBookings() {
        log.info("GET /bookings/all - Fetching all bookings (admin)");
        List<BookingResponse> bookings = bookingService.getAllBookings();
        return ResponseEntity.ok(bookings);
    }
}