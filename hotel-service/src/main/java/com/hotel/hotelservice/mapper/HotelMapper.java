package com.hotel.hotelservice.mapper;

import com.hotel.hotelservice.dto.HotelRequest;
import com.hotel.hotelservice.dto.HotelResponse;
import com.hotel.hotelservice.entity.Hotel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HotelMapper {

    Hotel toEntity(HotelRequest request);

    @Mapping(target = "totalRooms", expression = "java(hotel.getRooms() != null ? hotel.getRooms().size() : 0)")
    HotelResponse toResponse(Hotel hotel);

    List<HotelResponse> toResponseList(List<Hotel> hotels);
}