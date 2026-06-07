package com.ridehailing.backend.repository;

import com.ridehailing.backend.model.entity.DriverProfile;
import com.ridehailing.backend.model.enums.DriverStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverProfileRepository extends JpaRepository<DriverProfile, Long> {
    Optional<DriverProfile> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    boolean existsByPlateNumber(String plateNumber);
    List<DriverProfile> findByDriverStatus(DriverStatus status);
}