package com.ridehailing.backend.repository;

import com.ridehailing.backend.model.entity.Trip;
import com.ridehailing.backend.model.entity.User;
import com.ridehailing.backend.model.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TripRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("ridehailing_test")
            .withUsername("postgres")
            .withPassword("postgres");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private UserRepository userRepository;

    private User rider;
    private User driver;

    @BeforeEach
    void setUp() {
        tripRepository.deleteAll();
        userRepository.deleteAll();

        rider = userRepository.save(User.builder()
                .firstName("John").lastName("Doe")
                .email("rider@test.com").phone("+2348011111111")
                .password("encoded").role(Role.RIDER)
                .status(UserStatus.ACTIVE).build());

        driver = userRepository.save(User.builder()
                .firstName("James").lastName("Driver")
                .email("driver@test.com").phone("+2348022222222")
                .password("encoded").role(Role.DRIVER)
                .status(UserStatus.ACTIVE).build());
    }

    @Test
    @DisplayName("Should find trips by rider ordered by createdAt desc")
    void shouldFindTripsByRider() {
        saveTrip(rider, null, TripStatus.COMPLETED);
        saveTrip(rider, null, TripStatus.CANCELLED);

        List<Trip> trips = tripRepository.findByRiderIdOrderByCreatedAtDesc(rider.getId());

        assertThat(trips).hasSize(2);
        assertThat(trips.get(0).getRider().getId()).isEqualTo(rider.getId());
    }

    @Test
    @DisplayName("Should find active rider trip")
    void shouldFindActiveRiderTrip() {
        saveTrip(rider, null, TripStatus.REQUESTED);

        Optional<Trip> activeTrip = tripRepository.findActiveRiderTrip(rider.getId());

        assertThat(activeTrip).isPresent();
        assertThat(activeTrip.get().getStatus()).isEqualTo(TripStatus.REQUESTED);
    }

    @Test
    @DisplayName("Should not find active trip when all trips are completed")
    void shouldNotFindActiveTripWhenCompleted() {
        saveTrip(rider, null, TripStatus.COMPLETED);

        Optional<Trip> activeTrip = tripRepository.findActiveRiderTrip(rider.getId());

        assertThat(activeTrip).isEmpty();
    }

    @Test
    @DisplayName("Should count active trips for driver")
    void shouldCountActiveTripsForDriver() {
        saveTrip(rider, driver, TripStatus.IN_PROGRESS);
        saveTrip(rider, driver, TripStatus.COMPLETED);

        long count = tripRepository.countActiveTripsForDriver(driver.getId());

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Should find trip by id and rider id")
    void shouldFindTripByIdAndRiderId() {
        Trip saved = saveTrip(rider, null, TripStatus.REQUESTED);

        Optional<Trip> found = tripRepository.findByIdAndRiderId(saved.getId(), rider.getId());

        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("Should not find trip with wrong rider id")
    void shouldNotFindTripWithWrongRiderId() {
        Trip saved = saveTrip(rider, null, TripStatus.REQUESTED);

        Optional<Trip> found = tripRepository.findByIdAndRiderId(saved.getId(), 999L);

        assertThat(found).isEmpty();
    }

    private Trip saveTrip(User rider, User driver, TripStatus status) {
        return tripRepository.save(Trip.builder()
                .rider(rider).driver(driver)
                .pickupLatitude(6.5244).pickupLongitude(3.3792)
                .pickupAddress("Victoria Island")
                .destinationLatitude(6.6018).destinationLongitude(3.3515)
                .destinationAddress("Ikeja")
                .vehicleType(VehicleType.SEDAN)
                .estimatedFare(1500.0).distanceKm(10.0)
                .surgeApplied(false).surgeMultiplier(1.0)
                .status(status)
                .paymentStatus(PaymentStatus.PENDING)
                .build());
    }
}