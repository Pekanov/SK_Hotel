package com.hotel.bookingservice.client;

import com.hotel.bookingservice.dto.ConfirmAvailabilityRequest;
import com.hotel.bookingservice.dto.RoomResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "hotel-service")
public interface HotelServiceClient {

    @GetMapping("/api/rooms/recommend")
    List<RoomResponse> getRecommendedRooms();

    @GetMapping("/api/rooms/{id}")
    RoomResponse getRoomById(@PathVariable("id") Long id);

    @PostMapping("/api/rooms/{id}/confirm-availability")
    Boolean confirmAvailability(
            @PathVariable("id") Long roomId,
            @RequestBody ConfirmAvailabilityRequest request
    );

    @PostMapping("/api/rooms/{id}/release")
    void releaseRoom(
            @PathVariable("id") Long roomId,
            @RequestParam("requestId") String requestId
    );
}