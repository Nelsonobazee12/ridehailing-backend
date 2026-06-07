package com.ridehailing.backend.model.redis;

import com.ridehailing.backend.model.enums.DriverStatus;
import com.ridehailing.backend.model.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("driver_location")
public class DriverLocation implements Serializable {

    @Id
    private Long driverId;

    @Indexed
    private DriverStatus status;

    private VehicleType vehicleType;

    private Double latitude;
    private Double longitude;

    private String firstName;
    private String lastName;
    private String phone;
    private Double rating;
    private String plateNumber;

    // Auto-expire from Redis after 5 minutes of no update
    @TimeToLive
    private Long ttl = 300L;
}