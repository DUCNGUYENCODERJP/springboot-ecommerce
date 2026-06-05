# Luxury Hotel

Hotel booking platform built with Java Spring Boot, MySQL, JWT authentication, validation, testing, Docker, and a lightweight admin page.

## Project Structure

```text
luxury-hotel/
├─ .env.example
├─ Dockerfile
├─ docker-compose.yml
├─ pom.xml
├─ src/
│  ├─ main/
│  │  ├─ java/com/luxuryhotel/
│  │  │  ├─ common/
│  │  │  ├─ config/
│  │  │  ├─ controller/
│  │  │  ├─ domain/
│  │  │  ├─ dto/
│  │  │  ├─ exception/
│  │  │  ├─ mapper/
│  │  │  ├─ repository/
│  │  │  ├─ security/
│  │  │  └─ service/
│  │  └─ resources/
│  │     ├─ application.yml
│  │     └─ static/
│  │        ├─ admin.html
│  │        ├─ admin.css
│  │        └─ admin.js
│  └─ test/
│     ├─ java/com/luxuryhotel/controller/
│     └─ resources/application-test.yml
└─ mvnw / mvnw.cmd
```

## Technology Stack

- Java 17
- Spring Boot 3.3.4
- Spring Security + JWT
- Spring Data JPA
- MySQL 8
- H2 for tests
- MockMvc integration tests
- Docker + Docker Compose

## Database Design

### `users`
- `id` bigint PK
- `full_name` varchar(100)
- `email` varchar(120) unique
- `phone` varchar(20) unique
- `password` varchar(255)
- `enabled` boolean
- `created_at`
- `updated_at`

### `user_roles`
- `user_id` bigint FK -> `users.id`
- `role` enum(`ADMIN`, `CUSTOMER`)

### `hotels`
- `id` bigint PK
- `name` varchar(150)
- `address` varchar(255)
- `city` varchar(100)
- `country` varchar(50)
- `star_rating` int
- `description` varchar(1000)
- `created_at`
- `updated_at`

### `rooms`
- `id` bigint PK
- `hotel_id` bigint FK -> `hotels.id`
- `room_number` varchar(30) unique
- `type` enum(`STANDARD`, `DELUXE`, `SUITE`, `FAMILY`, `PRESIDENTIAL`)
- `status` enum(`AVAILABLE`, `OCCUPIED`, `MAINTENANCE`, `INACTIVE`)
- `price_per_night` decimal(12,2)
- `capacity` int
- `floor` int
- `description` varchar(1000)
- `has_wifi` boolean
- `has_breakfast` boolean
- `created_at`
- `updated_at`

### `bookings`
- `id` bigint PK
- `booking_code` varchar(32) unique
- `user_id` bigint FK -> `users.id`
- `room_id` bigint FK -> `rooms.id`
- `check_in_date` date
- `check_out_date` date
- `guest_count` int
- `total_price` decimal(12,2)
- `status` enum(`PENDING`, `CONFIRMED`, `CHECKED_IN`, `CHECKED_OUT`, `CANCELLED`)
- `special_request` varchar(500)
- `created_at`
- `updated_at`

## Main Modules

### Authentication
- Register customer account
- Login with email and password
- Generate JWT access token
- Read JWT from `Authorization: Bearer <token>`

### User Management
- Get current profile
- Admin get all users
- Admin get user detail
- Admin update user roles and status
- Admin delete user

### Hotel Management
- Public list hotel and hotel detail
- Admin create, update, delete hotel

### Room Management
- Public list rooms
- Public search available rooms by date, hotel, guest count, and max price
- Admin create, update, delete room

### Booking Management
- Customer create booking
- Customer view own bookings
- Customer cancel future bookings
- Admin list all bookings
- Admin filter bookings by hotel

## API Endpoints

### Auth
- `POST /api/auth/register`
- `POST /api/auth/login`

### Users
- `GET /api/users/me`
- `GET /api/users` `ADMIN`
- `GET /api/users/page` `ADMIN`
- `GET /api/users/{userId}` `ADMIN`
- `PUT /api/users/{userId}` `ADMIN`
- `DELETE /api/users/{userId}` `ADMIN`

### Hotels
- `GET /api/hotels`
- `GET /api/hotels/page`
- `GET /api/hotels/{hotelId}`
- `POST /api/hotels` `ADMIN`
- `PUT /api/hotels/{hotelId}` `ADMIN`
- `DELETE /api/hotels/{hotelId}` `ADMIN`

### Rooms
- `GET /api/rooms`
- `GET /api/rooms/page`
- `GET /api/rooms/{roomId}`
- `GET /api/rooms/available`
- `POST /api/rooms` `ADMIN`
- `PUT /api/rooms/{roomId}` `ADMIN`
- `DELETE /api/rooms/{roomId}` `ADMIN`

### Bookings
- `POST /api/bookings`
- `GET /api/bookings/my`
- `GET /api/bookings/my/page`
- `GET /api/bookings/{bookingId}`
- `GET /api/bookings` `ADMIN`
- `GET /api/bookings/page` `ADMIN`
- `PATCH /api/bookings/{bookingId}/cancel`

## Example Requests

### Register

```json
{
  "fullName": "Nguyen Van A",
  "email": "customer@example.com",
  "phone": "0912345678",
  "password": "Password@123"
}
```

### Login

```json
{
  "email": "admin@luxuryhotel.com",
  "password": "Admin@12345"
}
```

### Create Hotel

```json
{
  "name": "Luxury Hanoi",
  "address": "123 West Lake",
  "city": "Hanoi",
  "country": "Vietnam",
  "starRating": 5,
  "description": "Premium business hotel"
}
```

### Create Room

```json
{
  "hotelId": 1,
  "roomNumber": "A101",
  "type": "DELUXE",
  "status": "AVAILABLE",
  "pricePerNight": 150.00,
  "capacity": 2,
  "floor": 10,
  "description": "Ocean view",
  "hasWifi": true,
  "hasBreakfast": true
}
```

### Create Booking

```json
{
  "roomId": 1,
  "checkInDate": "2026-05-01",
  "checkOutDate": "2026-05-03",
  "guestCount": 2,
  "specialRequest": "High floor please"
}
```

## Validation Coverage

- Email format
- Password length
- Phone pattern
- Hotel/room text length
- Star rating range
- Room capacity and price bounds
- Booking date range
- Guest count bounds
- Duplicate email, phone, room number
- Overlapping booking prevention

## Admin Page

Admin page is available at:

- `http://localhost:8080/admin.html`

Capabilities:
- Login and store JWT
- Create customer account
- Manage hotels
- Manage rooms
- Create booking
- View bookings
- View and update users
- Inspect API responses in the built-in console

## Configuration

Main configuration file:

- [application.yml](src/main/resources/application.yml)

Environment variables:

```env
DB_URL=jdbc:mysql://localhost:3306/luxury_hotel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=root
JWT_SECRET=VGhpc0lzQVNlY3VyZUJhc2U2NEtleUZvckx1eHVyeUhvdGVsQXBwMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=
JWT_EXPIRATION_MINUTES=60
JWT_ISSUER=luxury-hotel
ADMIN_FULL_NAME=System Admin
ADMIN_EMAIL=admin@luxuryhotel.com
ADMIN_PHONE=0900000000
ADMIN_PASSWORD=Admin@12345
```

## How To Run Locally

### 1. Start MySQL

Use your local MySQL instance or Docker.

### 2. Configure environment

Copy `.env.example` values into your environment, or export them directly.

### 3. Run the application

```bash
mvn clean spring-boot:run
```

Application URLs:
- API: `http://localhost:8080`
- Admin UI: `http://localhost:8080/admin.html`
- Health: `http://localhost:8080/actuator/health`

## Run With Docker

```bash
docker compose up --build
```

This starts:
- MySQL on `3306`
- Spring Boot app on `8080`

## Run Tests

```bash
mvn clean test
```

Current automated tests cover:
- Authentication register/login
- Unauthorized access JSON response
- Hotel create/list
- Hotel pagination
- Room create
- Available room search
- Booking create
- Booking overlap prevention
- Booking cancellation
- Room delete guard
- User pagination
- Last-admin protection

## VS Code Support

Project-level VS Code recommendations are included in:

- `.vscode/extensions.json`
- `.vscode/settings.json`

These help the Java language server reload Maven changes automatically and reduce false Lombok errors in the editor.

## Default Admin Account

- Email: `admin@luxuryhotel.com`
- Password: `Admin@12345`

## Notes

- JWT token is access-token only; refresh token is not implemented.
- `ddl-auto=update` is convenient for development, but migrations should be used in production.
- Static admin page is intentionally lightweight and API-driven.
