# boutique-cartservice

Maintains shopping carts and validates users, products and stock.

## Overview

- **Type:** Spring Boot service
- **Stack:** Java 21, Spring Boot, Maven, Redis, Actuator, Docker
- **Port:** `6379`

## Flow

```text
Client / service → Controller → Business logic → Database / events / downstream services
```

## Main APIs

```text
Delete /items/productId
Post /items
Put /items/productId
```

## Configuration

```text
CART_MAXIMUM_QUANTITY
CART_TTL
DB_CONNECTION_TIMEOUT_MS
DB_MAX_LIFETIME_MS
DB_POOL_MAX_SIZE
DB_POOL_MIN_IDLE
DB_VALIDATION_TIMEOUT_MS
DEPLOYMENT_ENVIRONMENT
```

## Run

```bash
./mvnw spring-boot:run
./mvnw clean verify
```

## Docker

```bash
docker build -t boutique-cartservice:local .
```

## Health

```bash
curl http://localhost:6379/actuator/health
```

## CI/CD

This repository is built and deployed independently through its own GitHub Actions workflow.
