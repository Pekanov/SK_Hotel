package com.hotel.hotelservice.config;

import com.hotel.hotelservice.entity.Hotel;
import com.hotel.hotelservice.entity.Room;
import com.hotel.hotelservice.repository.HotelRepository;
import com.hotel.hotelservice.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;

    @Override
    public void run(String... args) {
        log.info("Initializing hotel data...");

        Hotel hotel1 = Hotel.builder()
                .name("Grand Plaza Hotel")
                .address("123 Main Street, New York, NY 10001")
                .build();
        hotelRepository.save(hotel1);

        List<Room> hotel1Rooms = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Room room = Room.builder()
                    .hotel(hotel1)
                    .number("10" + i)
                    .available(true)
                    .timesBooked(0)
                    .build();
            hotel1Rooms.add(room);
        }
        roomRepository.saveAll(hotel1Rooms);

        Hotel hotel2 = Hotel.builder()
                .name("Seaside Resort")
                .address("456 Beach Boulevard, Miami, FL 33139")
                .build();
        hotelRepository.save(hotel2);

        List<Room> hotel2Rooms = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Room room = Room.builder()
                    .hotel(hotel2)
                    .number("20" + i)
                    .available(true)
                    .timesBooked(0)
                    .build();
            hotel2Rooms.add(room);
        }
        roomRepository.saveAll(hotel2Rooms);

        Hotel hotel3 = Hotel.builder()
                .name("Mountain View Lodge")
                .address("789 Alpine Drive, Denver, CO 80202")
                .build();
        hotelRepository.save(hotel3);

        for (int i = 1; i <= 5; i++) {
            Room room = Room.builder()
                    .hotel(hotel3)
                    .number("30" + i)
                    .available(true)
                    .timesBooked(i - 1)
                    .build();
            roomRepository.save(room);
        }

        log.info("Data initialization completed. Created 3 hotels with 15 rooms total.");
    }
}