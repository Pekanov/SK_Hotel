package com.hotel.hotelservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rooms", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"hotel_id", "number"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(nullable = false, length = 20)
    private String number;

    @Column(nullable = false)
    @Builder.Default
    private Boolean available = true;

    @Column(name = "times_booked", nullable = false)
    @Builder.Default
    private Integer timesBooked = 0;

    @Version
    private Integer version;

    public void incrementBookings() {
        this.timesBooked++;
    }
}