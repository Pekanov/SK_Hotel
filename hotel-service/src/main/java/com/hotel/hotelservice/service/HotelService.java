package com.hotel.hotelservice.service;

import com.hotel.hotelservice.dto.HotelRequest;
import com.hotel.hotelservice.dto.HotelResponse;
import com.hotel.hotelservice.entity.Hotel;
import com.hotel.hotelservice.exception.ResourceNotFoundException;
import com.hotel.hotelservice.mapper.HotelMapper;
import com.hotel.hotelservice.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HotelService {

    private final HotelRepository hotelRepository;
    private final HotelMapper hotelMapper;

    @Transactional
    public HotelResponse createHotel(HotelRequest request) {
        log.info("Creating hotel with name: {}", request.getName());

        Hotel hotel = hotelMapper.toEntity(request);
        Hotel savedHotel = hotelRepository.save(hotel);

        log.info("Hotel created successfully with id: {}", savedHotel.getId());
        return hotelMapper.toResponse(savedHotel);
    }

    @Transactional(readOnly = true)
    public List<HotelResponse> getAllHotels() {
        log.info("Fetching all hotels");
        List<Hotel> hotels = hotelRepository.findAll();
        log.info("Found {} hotels", hotels.size());
        return hotelMapper.toResponseList(hotels);
    }

    @Transactional(readOnly = true)
    public HotelResponse getHotelById(Long id) {
        log.info("Fetching hotel with id: {}", id);
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + id));
        return hotelMapper.toResponse(hotel);
    }

    @Transactional(readOnly = true)
    public Hotel findHotelEntityById(Long id) {
        return hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + id));
    }
}