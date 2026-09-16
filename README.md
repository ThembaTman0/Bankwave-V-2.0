<div align="center">

# Bankwave V2.0

### Microservices Banking Backend

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-2024.0-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-cloud)
[![MySQL](https://img.shields.io/badge/MySQL-Database-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)

A banking backend built as Spring Boot microservices, with a Git-backed Config Server, a MySQL database per service and Docker Compose environments for the default, QA and prod profiles.

</div>

---

## Table of Contents

- [Architecture](#architecture)
- [Services](#services)
- [Tech Stack](#tech-stack)
- [Engineering Decisions](#engineering-decisions)
- [Database Setup](#database-setup)
- [Getting Started](#getting-started)
- [API Reference](#api-reference)
- [Project Structure](#project-structure)
- [Roadmap](#roadmap)

---

## Architecture

Bankwave follows a microservices architecture where each domain (accounts, loans, cards) is an independently deployable service with its own database. A Config Server supplies each service's configuration from a Git repository when the service starts.

```
                        +---------------------+
                        |    Config Server    |
                        |   (Spring Cloud)    |
                        |      Port 8071      |
                        +----------+----------+
                                   |
          +------------------------+------------------------+
          |                        |                        |
          v                        v                        v
+------------------+   +------------------+   +------------------+
| Accounts Service |   |  Loans Service   |   |  Cards Service   |
|   Port: 8080     |   |   Port: 8090     |   |   Port: 9000     |
+--------+---------+   +--------+---------+   +--------+---------+
         |                      |                       |
         v                      v                       v
+------------------+   +------------------+   +------------------+
|   accountsdb     |   |    loansdb       |   |    cardsdb       |
|     (MySQL)      |   |     (MySQL)      |   |     (MySQL)      |
+------------------+   +------------------+   +------------------+

          All containers run on a shared Docker network
```

---

## Services

| Service        | Responsibility                         | Port |
|----------------|----------------------------------------|------|
| accounts       | Customer account lifecycle management  | 8080 |
| loans          | Loan product data and management       | 8090 |
| cards          | Card product data and management       | 9000 |
| configserver   | Centralized external configuration     | 8071 |

---

## Tech Stack

| Category              | Technology                                        |
|-----------------------|---------------------------------------------------|
| Language              | Java 21 (services), Java 17 (Config Server)       |
| Framework             | Spring Boot 3.4 (services), 4.0 (Config Server)   |
| Configuration         | Spring Cloud Config Server with a Git backend     |
| Persistence           | Spring Data JPA / Hibernate                       |
| Database              | MySQL                                             |
| Containerization      | Docker and Docker Compose                         |
| Build Tool            | Apache Maven                                      |
| API Style             | RESTful JSON over HTTP                            |

---

## Engineering Decisions

**Database per Service**

Each microservice owns its schema and connects to an isolated MySQL container. This enforces bounded contexts so that each service team can evolve its schema independently without coordinating with other services.

**Externalized Configuration**

The Config Server serves each service's profile-specific properties (such as the build version, welcome message and contact details for the default, qa and prod profiles) from a separate Git repository, [Bankwave-config-repo](https://github.com/ThembaTman0/Bankwave-config-repo). Services import that configuration on startup, so the same container image runs under every profile without being rebuilt, while Docker Compose supplies each environment's datasource settings.

**Health-Check-Gated Startup**

Docker Compose health checks make sure no microservice starts until its MySQL database is accepting connections and the Config Server reports ready. This prevents race-condition startup failures that are common in composed environments.

---

## Database Setup

Each service owns its database schema, created from its `schema.sql` on startup. Services don't share tables or call each other; each one exposes its own REST API.

| Service  | Database   | Container  |
|----------|------------|------------|
| Accounts | accountsdb | accountsdb |
| Loans    | loansdb    | loansdb    |
| Cards    | cardsdb    | cardsdb    |

Datasource configuration for a local run (Accounts shown; Loans uses port 3307 and Cards 3308):

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/accountsdb
    username: root
  jpa:
    show-sql: true
  sql:
    init:
      mode: always   # runs schema.sql on startup
```

Under Docker Compose, `SPRING_DATASOURCE_URL` points each service at its own database container instead.

---

## Getting Started

### Prerequisites

- Java 21
- Maven 3.8 or higher
- Docker Desktop

---

### Option 1 - Full Stack with Docker Compose (Recommended)

Starts all three databases, the Config Server and the three microservices together, using the images published on Docker Hub.

```bash
# Clone the repository
git clone https://github.com/ThembaTman0/Bankwave-V-2.0.git
cd Bankwave-V-2.0/docker-compose/default

# Start the full system (use ../qa or ../prod for the other profiles)
docker compose up -d
```

Once running, verify the services:

- Config Server health: http://localhost:8071/actuator/health
- Accounts health: http://localhost:8080/actuator/health
- Loans health: http://localhost:8090/actuator/health
- Cards health: http://localhost:9000/actuator/health

---

### Option 2 - Running Locally

Start services in dependency order, each in its own terminal:

```bash
# 1. Databases (from docker-compose/default)
docker compose up -d accountsdb loansdb cardsdb

# 2. Config Server
cd configserver && mvn spring-boot:run

# 3. Microservices
cd accounts && mvn spring-boot:run
cd loans    && mvn spring-boot:run
cd cards    && mvn spring-boot:run
```

---

## API Reference

Every endpoint sits under `/api`. Customers are identified by a 10-digit `mobileNumber`.

**Accounts Service** - http://localhost:8080

| Method | Endpoint                  | Description                                  |
|--------|---------------------------|----------------------------------------------|
| POST   | /api/create               | Create a customer and account (JSON body)    |
| GET    | /api/fetch?mobileNumber=  | Fetch a customer with their account details  |
| PUT    | /api/update               | Update customer and account details          |
| DELETE | /api/delete?mobileNumber= | Delete a customer and their account          |

**Loans Service** - http://localhost:8090

| Method | Endpoint                  | Description                        |
|--------|---------------------------|------------------------------------|
| POST   | /api/create?mobileNumber= | Create a loan for a mobile number  |
| GET    | /api/fetch?mobileNumber=  | Fetch loan details                 |
| PUT    | /api/update               | Update loan details (JSON body)    |
| DELETE | /api/delete?mobileNumber= | Delete loan details                |

**Cards Service** - http://localhost:9000

| Method | Endpoint                  | Description                        |
|--------|---------------------------|------------------------------------|
| POST   | /api/create?mobileNumber= | Issue a card for a mobile number   |
| GET    | /api/fetch?mobileNumber=  | Fetch card details                 |
| PUT    | /api/update               | Update card details (JSON body)    |
| DELETE | /api/delete?mobileNumber= | Delete card details                |

**Every service** also exposes:

| Method | Endpoint           | Description                                    |
|--------|--------------------|------------------------------------------------|
| GET    | /api/build-info    | Build version served by the Config Server      |
| GET    | /api/java-version  | The `JAVA_HOME` the service is running with    |
| GET    | /api/contact-info  | Contact details served by the Config Server    |

---

## Project Structure

Each microservice follows a consistent layered architecture:

```
accounts/
+-- audit/          JPA auditing (who created or updated a record)
+-- constants/      Shared constants
+-- controller/     REST endpoints
+-- dto/            Request and response contracts
+-- entity/         JPA-managed domain models
+-- exception/      Centralized error handling
+-- mapper/         Entity and DTO mapping
+-- repository/     Data access via Spring Data JPA
+-- service/        Business logic (interfaces, with implementations in impl/)
```

---

## Roadmap

| Feature                    | Status  | Notes                                      |
|----------------------------|---------|--------------------------------------------|
| Centralized config         | Done    | Spring Cloud Config Server                 |
| Service discovery          | Planned | Netflix Eureka                             |
| Client-side load balancing | Planned | Spring Cloud LoadBalancer                  |
| API Gateway                | Planned | Spring Cloud Gateway                       |
| Circuit breaker            | Planned | Resilience4j                               |
| Distributed tracing        | Planned | Micrometer and Zipkin                      |
| Centralised logging        | Planned | ELK Stack                                  |
| Kubernetes deployment      | Planned | Helm charts and horizontal pod autoscaling |

---

## Author

**Themba Ngobeni**

[![GitHub](https://img.shields.io/badge/GitHub-ThembaTman0-181717?style=flat&logo=github)](https://github.com/ThembaTman0)
