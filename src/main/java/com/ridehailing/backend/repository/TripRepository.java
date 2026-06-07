package com.ridehailing.backend.repository;

import com.ridehailing.backend.model.entity.Trip;
import com.ridehailing.backend.model.enums.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findByRiderIdOrderByCreatedAtDesc(Long riderId);

    List<Trip> findByDriverIdOrderByCreatedAtDesc(Long driverId);

    Optional<Trip> findByIdAndRiderId(Long tripId, Long riderId);

    Optional<Trip> findByIdAndDriverId(Long tripId, Long driverId);

    @Query("SELECT t FROM Trip t WHERE t.driver.id = :driverId AND t.status = :status")
    Optional<Trip> findActiveDriverTrip(Long driverId, TripStatus status);

    @Query("SELECT t FROM Trip t WHERE t.rider.id = :riderId AND t.status NOT IN " +
            "('COMPLETED', 'CANCELLED') ORDER BY t.createdAt DESC")
    Optional<Trip> findActiveRiderTrip(Long riderId);

    @Query("SELECT COUNT(t) FROM Trip t WHERE t.driver.id = :driverId " +
            "AND t.status NOT IN ('COMPLETED', 'CANCELLED')")
    long countActiveTripsForDriver(Long driverId);
}