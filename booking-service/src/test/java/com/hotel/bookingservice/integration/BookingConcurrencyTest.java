package com.hotel.bookingservice.integration;

import com.hotel.bookingservice.client.HotelServiceClient;
import com.hotel.bookingservice.dto.BookingRequest;
import com.hotel.bookingservice.dto.BookingResponse;
import com.hotel.bookingservice.dto.ConfirmAvailabilityRequest;
import com.hotel.bookingservice.dto.RoomResponse;
import com.hotel.bookingservice.entity.BookingStatus;
import com.hotel.bookingservice.entity.Role;
import com.hotel.bookingservice.entity.User;
import com.hotel.bookingservice.repository.BookingRepository;
import com.hotel.bookingservice.repository.UserRepository;
import com.hotel.bookingservice.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest
class BookingConcurrencyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private HotelServiceClient hotelServiceClient;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();

        if (!userRepository.existsByUsername("concurrency-user-1")) {
            User user = User.builder()
                    .username("concurrency-user-1")
                    .password(passwordEncoder.encode("password"))
                    .role(Role.ROLE_USER)
                    .build();
            userRepository.save(user);
        }

        when(hotelServiceClient.confirmAvailability(anyLong(), any(ConfirmAvailabilityRequest.class)))
                .thenReturn(true);

        when(hotelServiceClient.getRoomById(anyLong()))
                .thenReturn(RoomResponse.builder()
                        .id(1L)
                        .hotelId(1L)
                        .number("101")
                        .available(true)
                        .timesBooked(0)
                        .build());
    }

    @Test
    void testConcurrentBookingSameRoom() throws InterruptedException {
        // Arrange
        int numberOfThreads = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        Long roomId = 1L;
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(12);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        AtomicInteger callCount = new AtomicInteger(0);
        when(hotelServiceClient.confirmAvailability(anyLong(), any(ConfirmAvailabilityRequest.class)))
                .thenAnswer(invocation -> {
                    int count = callCount.incrementAndGet();
                    return count == 1;
                });

        // Act
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    BookingRequest request = BookingRequest.builder()
                            .roomId(roomId)
                            .startDate(startDate)
                            .endDate(endDate)
                            .autoSelect(false)
                            .build();

                    BookingResponse response = bookingService.createBooking("concurrency-user-1", request);

                    if (response.getStatus().equals(BookingStatus.CONFIRMED.name())) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }

                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();

        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // Assert
        assertTrue(completed, "All threads should complete within timeout");

        assertTrue(successCount.get() >= 1,
                "At least one concurrent booking should succeed");

        long confirmedBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .filter(b -> b.getRoomId().equals(roomId))
                .filter(b -> b.getStartDate().equals(startDate))
                .count();

        assertTrue(confirmedBookings >= 1,
                "Database should have at least one confirmed booking");
    }

    @Test
    void testConcurrentBookingDifferentRooms() throws InterruptedException {
        // Arrange
        int numberOfThreads = 3;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        LocalDate startDate = LocalDate.now().plusDays(15);
        LocalDate endDate = LocalDate.now().plusDays(17);

        AtomicInteger successCount = new AtomicInteger(0);

        when(hotelServiceClient.confirmAvailability(anyLong(), any(ConfirmAvailabilityRequest.class)))
                .thenReturn(true);

        // Act
        for (int i = 0; i < numberOfThreads; i++) {
            final Long roomId = (long) (i + 1);
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    BookingRequest request = BookingRequest.builder()
                            .roomId(roomId)
                            .startDate(startDate)
                            .endDate(endDate)
                            .autoSelect(false)
                            .build();

                    BookingResponse response = bookingService.createBooking("concurrency-user-1", request);

                    if (response.getStatus().equals(BookingStatus.CONFIRMED.name())) {
                        successCount.incrementAndGet();
                    }

                } catch (Exception e) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // Assert
        assertTrue(completed);
        assertTrue(successCount.get() >= 1,
                "At least one booking should succeed");
    }

    @Test
    void testSequentialBookings() {
        // Arrange
        when(hotelServiceClient.confirmAvailability(anyLong(), any(ConfirmAvailabilityRequest.class)))
                .thenReturn(true);

        BookingRequest request1 = BookingRequest.builder()
                .roomId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .autoSelect(false)
                .build();

        BookingRequest request2 = BookingRequest.builder()
                .roomId(2L)
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(7))
                .autoSelect(false)
                .build();

        // Act
        BookingResponse response1 = bookingService.createBooking("concurrency-user-1", request1);
        BookingResponse response2 = bookingService.createBooking("concurrency-user-1", request2);

        // Assert
        assertNotNull(response1);
        assertNotNull(response2);
        assertEquals(BookingStatus.CONFIRMED.name(), response1.getStatus());
        assertEquals(BookingStatus.CONFIRMED.name(), response2.getStatus());
        assertEquals(2, bookingRepository.count());
    }
}