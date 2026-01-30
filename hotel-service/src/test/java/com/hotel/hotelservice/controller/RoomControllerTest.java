package com.hotel.hotelservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.hotelservice.dto.ConfirmAvailabilityRequest;
import com.hotel.hotelservice.dto.RoomRequest;
import com.hotel.hotelservice.dto.RoomResponse;
import com.hotel.hotelservice.security.JwtAuthenticationFilter;
import com.hotel.hotelservice.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = RoomController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RoomService roomService;

    private RoomResponse roomResponse;
    private RoomRequest roomRequest;

    @BeforeEach
    void setUp() {
        roomResponse = RoomResponse.builder()
                .id(1L)
                .hotelId(1L)
                .hotelName("Test Hotel")
                .number("101")
                .available(true)
                .timesBooked(0)
                .build();

        roomRequest = RoomRequest.builder()
                .hotelId(1L)
                .number("101")
                .available(true)
                .build();
    }

    @Test
    void createRoom_Success() throws Exception {
        // Arrange
        when(roomService.createRoom(any(RoomRequest.class))).thenReturn(roomResponse);

        // Act & Assert
        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roomRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.number").value("101"));
    }

    @Test
    void createRoom_Forbidden() throws Exception {
        when(roomService.createRoom(any(RoomRequest.class))).thenReturn(roomResponse);

        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roomRequest)))
                .andExpect(status().isCreated());
    }

    @Test
    void getAllAvailableRooms_Success() throws Exception {
        // Arrange
        when(roomService.getAllAvailableRooms()).thenReturn(List.of(roomResponse));

        // Act & Assert
        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].available").value(true));
    }

    @Test
    void getRecommendedRooms_Success() throws Exception {
        // Arrange
        RoomResponse room1 = RoomResponse.builder().id(1L).timesBooked(0).build();
        RoomResponse room2 = RoomResponse.builder().id(2L).timesBooked(3).build();

        when(roomService.getRecommendedRooms()).thenReturn(List.of(room1, room2));

        // Act & Assert
        mockMvc.perform(get("/api/rooms/recommend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].timesBooked").value(0))
                .andExpect(jsonPath("$[1].timesBooked").value(3));
    }

    @Test
    void confirmAvailability_Success() throws Exception {
        // Arrange
        ConfirmAvailabilityRequest request = ConfirmAvailabilityRequest.builder()
                .requestId("test-request")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();

        when(roomService.confirmAvailability(anyLong(), any(ConfirmAvailabilityRequest.class)))
                .thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/rooms/1/confirm-availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }
}