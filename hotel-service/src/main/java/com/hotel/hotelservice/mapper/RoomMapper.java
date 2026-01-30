package com.hotel.hotelservice.mapper;

import com.hotel.hotelservice.dto.RoomResponse;
import com.hotel.hotelservice.entity.Room;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoomMapper {

    @Mapping(target = "hotelId", source = "hotel.id")
    @Mapping(target = "hotelName", source = "hotel.name")
    RoomResponse toResponse(Room room);

    List<RoomResponse> toResponseList(List<Room> rooms);
}