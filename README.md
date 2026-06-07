# Ride-hailing Backend API

A production-grade real-time ride-hailing and delivery backend built with Java 21 + Spring Boot 3.5.

![CI](https://github.com/Nelsonobazee12/ridehailing-backend/actions/workflows/ci.yml/badge.svg)

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Database | PostgreSQL 16 |
| Cache / Geo | Redis 7 (GEO commands) |
| Messaging | Apache Kafka |
| Real-time | WebSocket + STOMP |
| Auth | JWT + Refresh Token Rotation |
| Payments | Paystack |
| SMS | Termii |
| Docs | Swagger / OpenAPI 3 |
| Testing | JUnit 5 + Mockito + Testcontainers |
| Containerization | Docker + Docker Compose |

## Architecture Overview
Client (Rider/Driver App)
│
▼
REST API / WebSocket
│
▼
Spring Boot Application
├── Auth Module (JWT)
├── Driver Location (Redis GEO)
├── Trip State Machine
├── Pricing Engine (Haversine + Surge)
├── Kafka Event Pipeline
│     ├── Producer → trip.requested / accepted / completed / cancelled
│     └── Consumer → WebSocket push + SMS notifications
└── Payment Module (Paystack)
│
▼
PostgreSQL (primary store)
Redis     (driver locations + TTL)
Kafka     (async events)

## Features

- [x] JWT authentication with refresh token rotation
- [x] Rider and driver registration with role-based access control
- [x] Driver profile and vehicle management
- [x] Real-time driver location tracking via Redis GEO commands
- [x] Nearby driver search with distance calculation and vehicle type filter
- [x] Trip request, matching, and full lifecycle management
- [x] Trip state machine: `REQUESTED → ACCEPTED → DRIVER_EN_ROUTE → ARRIVED → IN_PROGRESS → COMPLETED`
- [x] Fare estimation using Haversine formula
- [x] Surge pricing based on real-time driver supply/demand
- [x] Naira-based pricing per vehicle type (BIKE, TRICYCLE, SEDAN, SUV, VAN)
- [x] Kafka event pipeline for async processing
- [x] WebSocket/STOMP real-time push to rider and driver
- [x] SMS notifications via Termii for every trip event
- [x] Paystack payment integration with webhook signature verification
- [x] Bilateral ratings system with rolling driver average
- [x] Global exception handling with structured error responses
- [x] Unit tests, repository tests, and integration tests (Testcontainers)
- [x] Swagger/OpenAPI documentation

## API Endpoints

### Auth
| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/v1/auth/register` | Public |
| POST | `/api/v1/auth/login` | Public |
| POST | `/api/v1/auth/refresh-token` | Public |
| POST | `/api/v1/auth/logout` | Authenticated |

### Drivers
| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/v1/drivers/profile` | DRIVER |
| GET | `/api/v1/drivers/profile` | DRIVER |
| PATCH | `/api/v1/drivers/status` | DRIVER |
| POST | `/api/v1/drivers/location` | DRIVER |
| GET | `/api/v1/drivers/nearby` | RIDER |

### Trips
| Method | Endpoint | Access |
|---|---|---|
| GET | `/api/v1/trips/estimate` | RIDER |
| POST | `/api/v1/trips/request` | RIDER |
| POST | `/api/v1/trips/{id}/accept` | DRIVER |
| PATCH | `/api/v1/trips/{id}/status` | DRIVER |
| POST | `/api/v1/trips/{id}/cancel` | RIDER / DRIVER |
| POST | `/api/v1/trips/{id}/rate` | RIDER / DRIVER |
| GET | `/api/v1/trips/{id}` | RIDER / DRIVER |
| GET | `/api/v1/trips/my-trips` | RIDER / DRIVER |

### Payments
| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/v1/payments/initialize/{tripId}` | RIDER |
| GET | `/api/v1/payments/verify/{reference}` | Authenticated |
| GET | `/api/v1/payments/trip/{tripId}` | RIDER / DRIVER |
| POST | `/api/v1/webhooks/paystack` | Public (Paystack) |

## WebSocket

Connect to: `ws://localhost:8080/ws`

| Destination | Direction | Description |
|---|---|---|
| `/app/driver.location` | Client → Server | Driver sends live location |
| `/user/queue/driver-location` | Server → Rider | Live driver position during trip |
| `/user/queue/trip-status` | Server → User | Trip status updates |
| `/user/queue/trip-request` | Server → Driver | New trip request notification |
| `/topic/trips/{id}` | Server → All | Broadcast trip updates |

## Running Locally

### Prerequisites
- Java 21
- Docker + Docker Compose

### Environment Variables

Copy `.env.example` to `.env` and fill in your values:

```bash
cp .env.example .env
```

### Start Infrastructure

```bash
docker compose up -d
```

### Run Application

```bash
./gradlew bootRun
```

### API Documentation

Open [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

Click **Authorize**, paste your JWT access token from the login response.

## Running Tests

```bash
./gradlew test
```

Tests use Testcontainers — Docker must be running. First run pulls images.

### Test Coverage

| Layer | What's tested |
|---|---|
| Unit | PricingService, AuthService, TripService |
| Repository | TripRepository custom JPQL queries |
| Integration | AuthController full HTTP flow with real Spring Security |

## Environment Variables Reference

| Variable | Description | Example |
|---|---|---|
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/ridehailing` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `postgres` |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `KAFKA_SERVERS` | Kafka bootstrap servers | `localhost:9092` |
| `JWT_SECRET` | JWT signing secret (min 64 chars) | `your-secret` |
| `JWT_EXPIRATION` | Access token TTL in ms | `86400000` |
| `JWT_REFRESH_EXPIRATION` | Refresh token TTL in ms | `604800000` |
| `PAYSTACK_SECRET_KEY` | Paystack secret key | `sk_test_xxx` |
| `TERMII_API_KEY` | Termii API key | `your-key` |
| `TERMII_SENDER_ID` | SMS sender name | `RideHailing` |
