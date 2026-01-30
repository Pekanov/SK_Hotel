package com.hotel.bookingservice.service;

import com.hotel.bookingservice.client.HotelServiceClient;
import com.hotel.bookingservice.dto.BookingRequest;
import com.hotel.bookingservice.dto.BookingResponse;
import com.hotel.bookingservice.dto.ConfirmAvailabilityRequest;
import com.hotel.bookingservice.dto.RoomResponse;
import com.hotel.bookingservice.entity.Booking;
import com.hotel.bookingservice.entity.BookingStatus;
import com.hotel.bookingservice.entity.Role;
import com.hotel.bookingservice.entity.User;
import com.hotel.bookingservice.exception.BookingConflictException;
import com.hotel.bookingservice.exception.RoomNotAvailableException;
import com.hotel.bookingservice.mapper.BookingMapper;
import com.hotel.bookingservice.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private HotelServiceClient hotelServiceClient;

    @Mock
    private UserService userService;

    @InjectMocks
    private BookingService bookingService;

    private User testUser;
    private BookingRequest bookingRequest;
    private Booking booking;
    private BookingResponse bookingResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("password")
                .role(Role.ROLE_USER)
                .build();

        bookingRequest = BookingRequest.builder()
                .roomId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .autoSelect(false)
                .build();

        booking = Booking.builder()
                .id(1L)
                .user(testUser)
                .roomId(1L)
                .startDate(bookingRequest.getStartDate())
                .endDate(bookingRequest.getEndDate())
                .status(BookingStatus.CONFIRMED)
                .requestId("test-request-id")
                .build();

        bookingResponse = BookingResponse.builder()
                .id(1L)
                .userId(1L)
                .username("testuser")
                .roomId(1L)
                .startDate(bookingRequest.getStartDate())
                .endDate(bookingRequest.getEndDate())
                .status("CONFIRMED")
                .build();
    }

    @Test
    void createBooking_Success() {
        // Arrange
        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(bookingRepository.findConflictingBookings(anyLong(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(hotelServiceClient.confirmAvailability(anyLong(), any(ConfirmAvailabilityRequest.class)))
                .thenReturn(true);
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(bookingResponse);

        // Act
        BookingResponse result = bookingService.createBooking("testuser", bookingRequest);

        // Assert
        assertNotNull(result);
        assertEquals("CONFIRMED", result.getStatus());
        verify(bookingRepository, times(2)).save(any(Booking.class));
        verify(hotelServiceClient).confirmAvailability(anyLong(), any());
    }

    @Test
    void createBooking_RoomNotAvailable_ThrowsException() {
        // Arrange
        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(bookingRepository.findConflictingBookings(anyLong(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(hotelServiceClient.confirmAvailability(anyLong(), any(ConfirmAvailabilityRequest.class)))
                .thenReturn(false);

        // Act & Assert
        assertThrows(RoomNotAvailableException.class, () ->
                bookingService.createBooking("testuser", bookingRequest)
        );

        verify(bookingRepository, atLeast(1)).save(any(Booking.class));
    }

    @Test
    void createBooking_ConflictingBooking_ThrowsException() {
        // Arrange
        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(bookingRepository.findConflictingBookings(anyLong(), any(), any()))
                .thenReturn(List.of(booking));

        // Act & Assert
        assertThrows(BookingConflictException.class, () ->
                bookingService.createBooking("testuser", bookingRequest)
        );

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void createBooking_AutoSelect_Success() {
        // Arrange
        bookingRequest.setAutoSelect(true);
        bookingRequest.setRoomId(null);

        RoomResponse room = RoomResponse.builder()
                .id(2L)
                .hotelId(1L)
                .number("101")
                .available(true)
                .timesBooked(0)
                .build();

        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(hotelServiceClient.getRecommendedRooms()).thenReturn(List.of(room));
        when(bookingRepository.findConflictingBookings(anyLong(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(hotelServiceClient.confirmAvailability(anyLong(), any(ConfirmAvailabilityRequest.class)))
                .thenReturn(true);
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(bookingResponse);

        // Act
        BookingResponse result = bookingService.createBooking("testuser", bookingRequest);

        // Assert
        assertNotNull(result);
        verify(hotelServiceClient).getRecommendedRooms();
    }

    @Test
    void createBooking_InvalidDates_ThrowsException() {
        // Arrange
        bookingRequest.setStartDate(LocalDate.now().minusDays(1));
        when(userService.findByUsername("testuser")).thenReturn(testUser);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                bookingService.createBooking("testuser", bookingRequest)
        );
    }
}