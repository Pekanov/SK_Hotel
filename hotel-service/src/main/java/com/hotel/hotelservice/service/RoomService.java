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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomMapper roomMapper;
    private final HotelService hotelService;

    private final Map<String, Boolean> processedConfirmations = new ConcurrentHashMap<>();

    @Transactional
    public RoomResponse createRoom(RoomRequest request) {
        log.info("Creating room {} for hotel id: {}", request.getNumber(), request.getHotelId());

        Hotel hotel = hotelService.findHotelEntityById(request.getHotelId());

        Room room = Room.builder()
                .hotel(hotel)
                .number(request.getNumber())
                .available(request.getAvailable())
                .timesBooked(0)
                .build();

        Room savedRoom = roomRepository.save(room);
        log.info("Room created successfully with id: {}", savedRoom.getId());

        return roomMapper.toResponse(savedRoom);
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getAllAvailableRooms() {
        log.info("Fetching all available rooms");
        List<Room> rooms = roomRepository.findByAvailableTrue();
        log.info("Found {} available rooms", rooms.size());
        return roomMapper.toResponseList(rooms);
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getRecommendedRooms() {
        log.info("Fetching recommended rooms (sorted by times booked)");
        List<Room> rooms = roomRepository.findAvailableRoomsOrderedByTimesBooked();
        log.info("Found {} recommended rooms", rooms.size());
        return roomMapper.toResponseList(rooms);
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoomById(Long id) {
        log.info("Fetching room with id: {}", id);
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));
        return roomMapper.toResponse(room);
    }

    @Transactional
    public boolean confirmAvailability(Long roomId, ConfirmAvailabilityRequest request) {
        String requestId = request.getRequestId();
        log.info("Confirming availability for room {} with requestId: {}", roomId, requestId);

        if (processedConfirmations.containsKey(requestId)) {
            log.info("Request {} already processed, returning cached result", requestId);
            return processedConfirmations.get(requestId);
        }

        Room room = roomRepository.findByIdWithLock(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));

        if (!room.getAvailable()) {
            log.warn("Room {} is not available", roomId);
            processedConfirmations.put(requestId, false);
            throw new RoomNotAvailableException("Room is not available");
        }

        if (request.getEndDate().isBefore(request.getStartDate()) ||
                request.getEndDate().isEqual(request.getStartDate())) {
            log.warn("Invalid date range: {} to {}", request.getStartDate(), request.getEndDate());
            processedConfirmations.put(requestId, false);
            throw new IllegalArgumentException("End date must be after start date");
        }

        room.incrementBookings();
        roomRepository.save(room);

        log.info("Room {} availability confirmed and booking counter incremented to {}",
                roomId, room.getTimesBooked());

        processedConfirmations.put(requestId, true);
        return true;
    }

    @Transactional
    public void releaseRoom(Long roomId, String requestId) {
        log.info("Releasing room {} for requestId: {}", roomId, requestId);

        Room room = roomRepository.findByIdWithLock(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));

        if (room.getTimesBooked() > 0) {
            room.setTimesBooked(room.getTimesBooked() - 1);
            roomRepository.save(room);
            log.info("Room {} released, booking counter decremented to {}", roomId, room.getTimesBooked());
        } else {
            log.warn("Room {} already has 0 bookings, nothing to release", roomId);
        }

        processedConfirmations.remove(requestId);
    }
}