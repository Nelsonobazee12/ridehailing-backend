# Ride-hailing Backend API

A real-time ride-hailing and delivery backend built with Java 21 + Spring Boot 3.5.

## Tech Stack
- **Java 21** + **Spring Boot 3.5**
- **PostgreSQL** — primary database
- **Redis** — real-time driver location (GEO indexing)
- **Kafka** — async event processing
- **WebSocket** — real-time trip updates
- **JWT** — stateless authentication with refresh token rotation
- **Docker Compose** — local infrastructure

## Features
- [x] Auth (register, login, refresh token, logout)
- [x] Driver profile management
- [x] Real-time driver location tracking via Redis GEO
- [x] Nearby driver search with distance calculation
- [ ] Trip lifecycle with state machine
- [ ] Pricing engine with surge calculation
- [ ] Paystack payment integration
- [ ] SMS notifications via Termii
- [ ] WebSocket real-time trip updates

## Running Locally
```bash
docker compose up -d
./gradlew bootRun
```

API docs: http://localhost:8080/swagger-ui.html
