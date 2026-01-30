package com.hotel.hotelservice.controller;

import com.hotel.hotelservice.dto.ConfirmAvailabilityRequest;
import com.hotel.hotelservice.dto.RoomRequest;
import com.hotel.hotelservice.dto.RoomResponse;
import com.hotel.hotelservice.service.RoomService;
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
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Room Management", description = "Room management operations")
@SecurityRequirement(name = "Bearer Authentication")
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new room", description = "Admin only operation")
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody RoomRequest request) {
        log.info("POST /api/rooms - Creating room: {}", request.getNumber());
        RoomResponse response = roomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get all available rooms")
    public ResponseEntity<List<RoomResponse>> getAllAvailableRooms() {
        log.info("GET /api/rooms - Fetching all available rooms");
        List<RoomResponse> rooms = roomService.getAllAvailableRooms();
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/recommend")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SERVICE')")
    @Operation(summary = "Get recommended rooms",
            description = "Returns available rooms sorted by booking count (least booked first)")
    public ResponseEntity<List<RoomResponse>> getRecommendedRooms() {
        log.info("GET /api/rooms/recommend - Fetching recommended rooms");
        List<RoomResponse> rooms = roomService.getRecommendedRooms();
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get room by ID")
    public ResponseEntity<RoomResponse> getRoomById(@PathVariable Long id) {
        log.info("GET /api/rooms/{} - Fetching room", id);
        RoomResponse room = roomService.getRoomById(id);
        return ResponseEntity.ok(room);
    }

    @PostMapping("/{id}/confirm-availability")
    @Operation(summary = "Confirm room availability",
            description = "Internal endpoint for booking service")
    public ResponseEntity<Boolean> confirmAvailability(
            @PathVariable Long id,
            @Valid @RequestBody ConfirmAvailabilityRequest request) {
        log.info("POST /api/rooms/{}/confirm-availability - requestId: {}", id, request.getRequestId());
        boolean confirmed = roomService.confirmAvailability(id, request);
        return ResponseEntity.ok(confirmed);
    }

    @PostMapping("/{id}/release")
    @Operation(summary = "Release room",
            description = "Internal endpoint for compensation")
    public ResponseEntity<Void> releaseRoom(
            @PathVariable Long id,
            @RequestParam String requestId) {
        log.info("POST /api/rooms/{}/release - requestId: {}", id, requestId);
        roomService.releaseRoom(id, requestId);
        return ResponseEntity.ok().build();
    }
}