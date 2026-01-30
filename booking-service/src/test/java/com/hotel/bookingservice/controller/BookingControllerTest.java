package com.hotel.bookingservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.bookingservice.dto.BookingRequest;
import com.hotel.bookingservice.dto.BookingResponse;
import com.hotel.bookingservice.service.BookingService;
import com.hotel.bookingservice.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    private BookingRequest bookingRequest;
    private BookingResponse bookingResponse;

    @BeforeEach
    void setUp() {
        bookingRequest = BookingRequest.builder()
                .roomId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .autoSelect(false)
                .build();

        bookingResponse = BookingResponse.builder()
                .id(1L)
                .userId(1L)
                .username("testuser")
                .roomId(1L)
                .startDate(bookingRequest.getStartDate())
                .endDate(bookingRequest.getEndDate())
                .status("CONFIRMED")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void createBooking_Success() throws Exception {
        // Arrange
        when(bookingService.createBooking(eq("testuser"), any(BookingRequest.class)))
                .thenReturn(bookingResponse);

        // Act & Assert
        mockMvc.perform(post("/booking")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void getUserBookings_Success() throws Exception {
        // Arrange
        when(bookingService.getUserBookings("testuser"))
                .thenReturn(List.of(bookingResponse));

        // Act & Assert
        mockMvc.perform(get("/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].username").value("testuser"));
    }

    @Test
    void createBooking_Unauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/booking")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isUnauthorized());
    }
}