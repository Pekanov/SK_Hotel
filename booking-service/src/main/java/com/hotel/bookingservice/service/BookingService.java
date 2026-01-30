package com.hotel.bookingservice.service;

import com.hotel.bookingservice.client.HotelServiceClient;
import com.hotel.bookingservice.dto.BookingRequest;
import com.hotel.bookingservice.dto.BookingResponse;
import com.hotel.bookingservice.dto.ConfirmAvailabilityRequest;
import com.hotel.bookingservice.dto.RoomResponse;
import com.hotel.bookingservice.entity.Booking;
import com.hotel.bookingservice.entity.BookingStatus;
import com.hotel.bookingservice.entity.User;
import com.hotel.bookingservice.exception.BookingConflictException;
import com.hotel.bookingservice.exception.ResourceNotFoundException;
import com.hotel.bookingservice.exception.RoomNotAvailableException;
import com.hotel.bookingservice.mapper.BookingMapper;
import com.hotel.bookingservice.repository.BookingRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final HotelServiceClient hotelServiceClient;
    private final UserService userService;

    private final Map<String, BookingResponse> processedBookings = new ConcurrentHashMap<>();

    @Transactional
    public BookingResponse createBooking(String username, BookingRequest request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Creating booking for user: {} with requestId: {}", username, requestId);

        if (processedBookings.containsKey(requestId)) {
            log.info("Booking already processed for requestId: {}", requestId);
            return processedBookings.get(requestId);
        }

        validateDates(request.getStartDate(), request.getEndDate());

        User user = userService.findByUsername(username);

        Long roomId;
        if (Boolean.TRUE.equals(request.getAutoSelect())) {
            log.info("Auto-selecting room for booking");
            roomId = selectRoomAutomatically(request.getStartDate(), request.getEndDate());
        } else {
            if (request.getRoomId() == null) {
                throw new IllegalArgumentException("Room ID is required when autoSelect is false");
            }
            roomId = request.getRoomId();
        }

        checkBookingConflicts(roomId, request.getStartDate(), request.getEndDate());

        Booking booking = Booking.builder()
                .user(user)
                .roomId(roomId)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(BookingStatus.PENDING)
                .requestId(requestId)
                .build();

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking created with PENDING status, id: {}", savedBooking.getId());

        boolean confirmed = false;
        try {
            confirmed = confirmRoomAvailability(roomId, requestId, request.getStartDate(), request.getEndDate());

            if (confirmed) {
                savedBooking.setStatus(BookingStatus.CONFIRMED);
                savedBooking = bookingRepository.save(savedBooking);
                log.info("Booking confirmed successfully, id: {}", savedBooking.getId());
            } else {
                savedBooking.setStatus(BookingStatus.CANCELLED);
                savedBooking = bookingRepository.save(savedBooking);
                log.warn("Room not available, booking cancelled, id: {}", savedBooking.getId());
                throw new RoomNotAvailableException("Room is not available for the selected dates");
            }
        } catch (Exception e) {
            log.error("Error confirming room availability: {}", e.getMessage(), e);
            compensateBooking(savedBooking, roomId, requestId);
            throw new RoomNotAvailableException("Failed to confirm room availability: " + e.getMessage());
        }

        BookingResponse response = bookingMapper.toResponse(savedBooking);
        processedBookings.put(requestId, response);

        return response;
    }

    @CircuitBreaker(name = "hotelService", fallbackMethod = "confirmRoomAvailabilityFallback")
    @Retry(name = "hotelService")
    private boolean confirmRoomAvailability(Long roomId, String requestId, LocalDate startDate, LocalDate endDate) {
        log.info("Confirming room availability for room: {}, requestId: {}", roomId, requestId);

        ConfirmAvailabilityRequest confirmRequest = ConfirmAvailabilityRequest.builder()
                .requestId(requestId)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        return hotelServiceClient.confirmAvailability(roomId, confirmRequest);
    }

    private boolean confirmRoomAvailabilityFallback(Long roomId, String requestId,
                                                    LocalDate startDate, LocalDate endDate,
                                                    Exception e) {
        log.error("Circuit breaker activated for room availability confirmation: {}", e.getMessage());
        return false;
    }

    private void compensateBooking(Booking booking, Long roomId, String requestId) {
        log.info("Executing compensation for booking id: {}", booking.getId());

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        try {
            hotelServiceClient.releaseRoom(roomId, requestId);
            log.info("Room released successfully during compensation");
        } catch (Exception e) {
            log.error("Failed to release room during compensation: {}", e.getMessage(), e);
        }
    }

    @CircuitBreaker(name = "hotelService", fallbackMethod = "selectRoomAutomaticallyFallback")
    @Retry(name = "hotelService")
    private Long selectRoomAutomatically(LocalDate startDate, LocalDate endDate) {
        log.info("Selecting room automatically for dates: {} to {}", startDate, endDate);

        List<RoomResponse> recommendedRooms = hotelServiceClient.getRecommendedRooms();

        if (recommendedRooms.isEmpty()) {
            throw new RoomNotAvailableException("No available rooms found");
        }

        for (RoomResponse room : recommendedRooms) {
            List<Booking> conflicts = bookingRepository.findConflictingBookings(
                    room.getId(), startDate, endDate);

            if (conflicts.isEmpty()) {
                log.info("Selected room: {} with timesBooked: {}", room.getId(), room.getTimesBooked());
                return room.getId();
            }
        }

        throw new RoomNotAvailableException("No available rooms for the selected dates");
    }

    private Long selectRoomAutomaticallyFallback(LocalDate startDate, LocalDate endDate, Exception e) {
        log.error("Failed to select room automatically: {}", e.getMessage());
        throw new RoomNotAvailableException("Unable to select room automatically");
    }

    private void checkBookingConflicts(Long roomId, LocalDate startDate, LocalDate endDate) {
        List<Booking> conflicts = bookingRepository.findConflictingBookings(roomId, startDate, endDate);

        if (!conflicts.isEmpty()) {
            log.warn("Booking conflict detected for room: {} on dates: {} to {}",
                    roomId, startDate, endDate);
            throw new BookingConflictException("Room is already booked for the selected dates");
        }
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }

        if (endDate.isBefore(startDate) || endDate.isEqual(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getUserBookings(String username) {
        log.info("Fetching bookings for user: {}", username);
        User user = userService.findByUsername(username);
        List<Booking> bookings = bookingRepository.findByUserId(user.getId());
        return bookingMapper.toResponseList(bookings);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(String username, Long bookingId) {
        log.info("Fetching booking {} for user: {}", bookingId, username);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (!booking.getUser().getUsername().equals(username)) {
            throw new ResourceNotFoundException("Booking not found with id: " + bookingId);
        }

        return bookingMapper.toResponse(booking);
    }

    @Transactional
    public void cancelBooking(String username, Long bookingId) {
        log.info("Cancelling booking {} for user: {}", bookingId, username);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (!booking.getUser().getUsername().equals(username)) {
            throw new ResourceNotFoundException("Booking not found with id: " + bookingId);
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            log.warn("Booking {} is already cancelled", bookingId);
            return;
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        try {
            hotelServiceClient.releaseRoom(booking.getRoomId(), booking.getRequestId());
            log.info("Booking {} cancelled and room released successfully", bookingId);
        } catch (Exception e) {
            log.error("Failed to release room for cancelled booking: {}", e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings() {
        log.info("Fetching all bookings (admin)");
        List<Booking> bookings = bookingRepository.findAll();
        return bookingMapper.toResponseList(bookings);
    }
}