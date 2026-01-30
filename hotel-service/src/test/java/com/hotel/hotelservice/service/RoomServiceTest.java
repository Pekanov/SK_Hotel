package com.hotel.hotelservice.service;

import com.hotel.hotelservice.dto.ConfirmAvailabilityRequest;
import com.hotel.hotelservice.dto.RoomRequest;
import com.hotel.hotelservice.dto.RoomResponse;
import com.hotel.hotelservice.entity.Hotel;
import com.hotel.hotelservice.entity.Room;
import com.hotel.hotelservice.exception.ResourceNotFoundException;
import com.hotel.hotelservice.exception.RoomNotAvailableException;
import com.hotel.hotelservice.mapper.RoomMapper;
import com.hotel.hotelservice.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMapper roomMapper;

    @Mock
    private HotelService hotelService;

    @InjectMocks
    private RoomService roomService;

    private Hotel hotel;
    private Room room;
    private RoomResponse roomResponse;
    private RoomRequest roomRequest;

    @BeforeEach
    void setUp() {
        hotel = Hotel.builder()
                .id(1L)
                .name("Test Hotel")
                .address("123 Test St")
                .build();

        room = Room.builder()
                .id(1L)
                .hotel(hotel)
                .number("101")
                .available(true)
                .timesBooked(0)
                .build();

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
    void createRoom_Success() {
        // Arrange
        when(hotelService.findHotelEntityById(1L)).thenReturn(hotel);
        when(roomRepository.save(any(Room.class))).thenReturn(room);
        when(roomMapper.toResponse(any(Room.class))).thenReturn(roomResponse);

        // Act
        RoomResponse result = roomService.createRoom(roomRequest);

        // Assert
        assertNotNull(result);
        assertEquals("101", result.getNumber());
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    void getAllAvailableRooms_Success() {
        // Arrange
        when(roomRepository.findByAvailableTrue()).thenReturn(List.of(room));
        when(roomMapper.toResponseList(anyList())).thenReturn(List.of(roomResponse));

        // Act
        List<RoomResponse> result = roomService.getAllAvailableRooms();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(roomRepository).findByAvailableTrue();
    }

    @Test
    void getRecommendedRooms_OrderedByTimesBooked() {
        // Arrange
        Room room1 = Room.builder().id(1L).hotel(hotel).number("101").available(true).timesBooked(5).build();
        Room room2 = Room.builder().id(2L).hotel(hotel).number("102").available(true).timesBooked(2).build();
        Room room3 = Room.builder().id(3L).hotel(hotel).number("103").available(true).timesBooked(8).build();

        when(roomRepository.findAvailableRoomsOrderedByTimesBooked())
                .thenReturn(List.of(room2, room1, room3));
        when(roomMapper.toResponseList(anyList())).thenReturn(List.of(
                RoomResponse.builder().id(2L).timesBooked(2).build(),
                RoomResponse.builder().id(1L).timesBooked(5).build(),
                RoomResponse.builder().id(3L).timesBooked(8).build()
        ));

        // Act
        List<RoomResponse> result = roomService.getRecommendedRooms();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(2, result.get(0).getTimesBooked());
        assertEquals(5, result.get(1).getTimesBooked());
        assertEquals(8, result.get(2).getTimesBooked());
    }

    @Test
    void confirmAvailability_Success() {
        // Arrange
        ConfirmAvailabilityRequest request = ConfirmAvailabilityRequest.builder()
                .requestId("test-request-id")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();

        when(roomRepository.findByIdWithLock(1L)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenReturn(room);

        // Act
        boolean result = roomService.confirmAvailability(1L, request);

        // Assert
        assertTrue(result);
        verify(roomRepository).findByIdWithLock(1L);
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    void confirmAvailability_RoomNotAvailable_ThrowsException() {
        // Arrange
        room.setAvailable(false);
        ConfirmAvailabilityRequest request = ConfirmAvailabilityRequest.builder()
                .requestId("test-request-id")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();

        when(roomRepository.findByIdWithLock(1L)).thenReturn(Optional.of(room));

        // Act & Assert
        assertThrows(RoomNotAvailableException.class, () ->
                roomService.confirmAvailability(1L, request)
        );
    }

    @Test
    void confirmAvailability_InvalidDateRange_ThrowsException() {
        // Arrange
        ConfirmAvailabilityRequest request = ConfirmAvailabilityRequest.builder()
                .requestId("test-request-id")
                .startDate(LocalDate.now().plusDays(3))
                .endDate(LocalDate.now().plusDays(1))
                .build();

        when(roomRepository.findByIdWithLock(1L)).thenReturn(Optional.of(room));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                roomService.confirmAvailability(1L, request)
        );
    }

    @Test
    void confirmAvailability_Idempotent() {
        // Arrange
        String requestId = "test-request-id";
        ConfirmAvailabilityRequest request = ConfirmAvailabilityRequest.builder()
                .requestId(requestId)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();

        when(roomRepository.findByIdWithLock(1L)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenReturn(room);

        // Act
        boolean result1 = roomService.confirmAvailability(1L, request);
        boolean result2 = roomService.confirmAvailability(1L, request);

        // Assert
        assertTrue(result1);
        assertTrue(result2);
        verify(roomRepository, times(1)).findByIdWithLock(1L);
        verify(roomRepository, times(1)).save(any(Room.class));
    }

    @Test
    void releaseRoom_Success() {
        // Arrange
        room.setTimesBooked(5);
        when(roomRepository.findByIdWithLock(1L)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenReturn(room);

        // Act
        roomService.releaseRoom(1L, "test-request-id");

        // Assert
        verify(roomRepository).findByIdWithLock(1L);
        verify(roomRepository).save(argThat(r -> r.getTimesBooked() == 4));
    }

    @Test
    void releaseRoom_AlreadyZero_NoChange() {
        // Arrange
        room.setTimesBooked(0);
        when(roomRepository.findByIdWithLock(1L)).thenReturn(Optional.of(room));

        // Act
        roomService.releaseRoom(1L, "test-request-id");

        // Assert
        verify(roomRepository).findByIdWithLock(1L);
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    void getRoomById_NotFound_ThrowsException() {
        // Arrange
        when(roomRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                roomService.getRoomById(999L)
        );
    }
}