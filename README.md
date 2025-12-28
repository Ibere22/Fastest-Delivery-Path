# Fastest Delivery Path

A REST API service that computes the fastest delivery path between cities using Dijkstra's algorithm. Built with Spring Boot 3.3.7 and Java 17.

## Overview

This courier service API provides two main operations:
- **Manage road networks**: Create/update directed roads between cities with travel times
- **Find fastest routes**: Calculate optimal delivery paths using Dijkstra's algorithm

**Key features**: Directed graphs, automatic city creation, case-insensitive queries, batch operations, handles cycles and complex networks.

## Architecture

**Layered Design:**
```
controller/    → REST endpoints (POST /roads, POST /routes/fastest)
service/       → Business logic (RoadService, PathfindingService with Dijkstra)
repository/    → Data access (Spring Data JPA)
entity/        → JPA entities (City, Road)
dto/           → Request/Response objects
exception/     → Global exception handling
```

**Tech Stack:** Java 17, Spring Boot 3.3.7, Maven, PostgreSQL 16 (Docker), JPA/Hibernate, JUnit 5, Mockito

## Quick Start Tutorial

### Prerequisites
- Java 17
- Docker Desktop
- Maven 

### 1. Start PostgreSQL Database

```bash
docker-compose up -d
```

Database runs on `localhost:5432` with:
- Database: `delivery_db`
- Username: `delivery_user`
- Password: `delivery_pass`

### 2. Configure Application

Application is pre-configured in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/delivery_db
spring.datasource.username=delivery_user
spring.datasource.password=delivery_pass
spring.jpa.hibernate.ddl-auto=update
```

Tables are auto-created on first run.

### 3. Build the Application

```bash
.\mvnw clean package
```

This creates: `target/fastest-delivery-path-0.0.1-SNAPSHOT.jar`

### 4. Run the Application

**Option 1: Run JAR file**
```bash
java -jar target/fastest-delivery-path-0.0.1-SNAPSHOT.jar
```

**Option 2: Run from IntelliJ (My approach)**
- Open `FastestDeliveryPathApplication.java`
- Right-click → Run 'FastestDeliveryPathApplication'

Application starts on `http://localhost:8080`

## API Usage Examples

**Note:** I tested the API using Postman. cURL examples are provided below for reference.

### Create/Update Roads

**Endpoint:** `POST /roads`

**Request:** (Must be JSON array)
```json
[
  {"fromCity": "Tbilisi", "toCity": "Batumi", "travelTimeMinutes": 360},
  {"fromCity": "Batumi", "toCity": "Gonio", "travelTimeMinutes": 45},
  {"fromCity": "Tbilisi", "toCity": "Kutaisi", "travelTimeMinutes": 240}
]
```

**cURL (for reference):**
```bash
curl -X POST http://localhost:8080/roads \
  -H "Content-Type: application/json" \
  -d '[
    {"fromCity": "Tbilisi", "toCity": "Batumi", "travelTimeMinutes": 360},
    {"fromCity": "Batumi", "toCity": "Gonio", "travelTimeMinutes": 45}
  ]'
```

**Response:** `201 Created`
```json
[
  {"fromCity": "TBILISI", "toCity": "BATUMI", "travelTimeMinutes": 360},
  {"fromCity": "BATUMI", "toCity": "GONIO", "travelTimeMinutes": 45}
]
```

---

### Find Fastest Route

**Endpoint:** `POST /routes/fastest`

**Request:**
```json
{
  "sourceCity": "Tbilisi",
  "destinationCity": "Gonio"
}
```

**cURL (for reference):**
```bash
curl -X POST http://localhost:8080/routes/fastest \
  -H "Content-Type: application/json" \
  -d '{"sourceCity": "Tbilisi", "destinationCity": "Gonio"}'
```

**Response:** `200 OK`
```json
{
  "pathCities": ["TBILISI", "BATUMI", "GONIO"],
  "pathRoads": [
    {"fromCity": "TBILISI", "toCity": "BATUMI", "travelTimeMinutes": 360},
    {"fromCity": "BATUMI", "toCity": "GONIO", "travelTimeMinutes": 45}
  ],
  "totalTravelTimeMinutes": 405
}
```

---

### Error Examples

**No Route Found** - `404 Not Found`
```json
{"error": "No route found between Tbilisi and NonExistentCity."}
```

**Invalid Data** - `400 Bad Request`
```json
{"error": "Validation failed: Travel time must be non-negative"}
```

## Testing

```bash
# Run all tests (unit + integration)
.\mvnw test

# I used IntelliJ: Right-click src/test/java → Run 'All Tests'
```

**Test Coverage:**
- Unit tests: `PathfindingServiceTest`, `RoadServiceTest`
- Integration tests: `RoadControllerIntegrationTest`, `RouteControllerIntegrationTest`

Integration tests use in-memory H2 database (no Docker needed).

## Database Schema

**cities**
- `id` (BIGINT, PK)
- `name` (VARCHAR, UNIQUE)

**roads**
- `id` (BIGINT, PK)
- `from_city_id` (FK → cities)
- `to_city_id` (FK → cities)
- `travel_time_minutes` (INTEGER, >= 0)
- Unique constraint: `(from_city_id, to_city_id)`

**Note:** Roads are **one-way**. For two-way travel, create roads in both directions.

## Additional Notes

- **Case-insensitive:** "Tbilisi", "tbilisi", "TBILISI" all map to same city
- **Algorithm:** Dijkstra with priority queue
- **Handles:** Cycles, multiple paths, medium-sized networks (hundreds of cities)

## Stopping

```bash
# Stop application: Ctrl+C

# Stop Docker database
docker-compose down

# Stop and delete all data
docker-compose down -v
```

---
