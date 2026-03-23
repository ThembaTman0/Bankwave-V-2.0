<div align="center">

# Bankwave V2.0

### Production-Ready Microservices Banking Backend

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-2023.x-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-cloud)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)

A cloud-native banking backend built with microservices architecture, centralized configuration, service discovery, and container orchestration.

</div>

---

## Table of Contents

- [Architecture](#architecture)
- [Services](#services)
- [Tech Stack](#tech-stack)
- [Engineering Decisions](#engineering-decisions)
- [Service Discovery and Registration](#service-discovery-and-registration)
- [Database Setup](#database-setup)
- [Getting Started](#getting-started)
- [API Reference](#api-reference)
- [Project Structure](#project-structure)
- [Roadmap](#roadmap)

---

## Architecture

Bankwave follows a microservices architecture where each domain (accounts, loans, cards) is an independently deployable service with its own database. A centralized Config Server and Eureka Service Registry handle configuration and service-to-service communication.

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
         |    +-----------------+-----------------+     |
         |    |     Eureka Discovery Server       |     |
         +--->|           Port: 8070              |<----+
              |  (service registration + lookup)  |
              +-----------------------------------+
         |                      |                       |
         v                      v                       v
+------------------+   +------------------+   +------------------+
|   accountsdb     |   |    loansdb       |   |    cardsdb       |
|   (MySQL 8.0)    |   |   (MySQL 8.0)    |   |   (MySQL 8.0)    |
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
| eurekaserver   | Service registry and discovery         | 8070 |

---

## Tech Stack

| Category              | Technology                        |
|-----------------------|-----------------------------------|
| Language              | Java 17                           |
| Framework             | Spring Boot 3.x                   |
| Service Discovery     | Spring Cloud Netflix Eureka       |
| Load Balancing        | Spring Cloud LoadBalancer         |
| Configuration         | Spring Cloud Config Server        |
| Persistence           | Spring Data JPA / Hibernate       |
| Database              | MySQL 8.0                         |
| Containerization      | Docker and Docker Compose         |
| Build Tool            | Apache Maven                      |
| API Style             | RESTful JSON over HTTP            |

---

## Engineering Decisions

**Database per Service**

Each microservice owns its schema and connects to an isolated MySQL container. This enforces bounded contexts so that each service team can evolve its schema independently without coordinating with other services.

**Externalized Configuration**

All environment-specific configuration (datasource URLs, credentials, ports) lives in the Config Server. Services pull their config on startup, which makes it easy to promote the same container image from dev to staging to production without rebuilding it.

**Service Discovery over Static Addresses**

Hardcoding service hostnames breaks in containerized environments where IPs are assigned dynamically. Eureka solves this by letting services register themselves by name and discover peers at runtime, with no DNS hacks or hardcoded IPs.

**Client-Side Load Balancing**

Spring Cloud LoadBalancer runs inside the calling service rather than a dedicated proxy. This removes a network hop and gives each service independent control over its retry and balancing strategy.

**Health-Check-Gated Startup**

Docker Compose health checks make sure no microservice starts until its MySQL dependency is accepting connections. This prevents race-condition startup failures that are common in composed environments.

---

## Service Discovery and Registration

### The Problem

In Docker and cloud environments, services are assigned dynamic IPs at runtime. Hardcoding addresses like `http://loans:8090` into Accounts is fragile and breaks the moment an instance is restarted, scaled, or redeployed.

### The Solution

Bankwave uses Netflix Eureka via Spring Cloud as its service registry. Every service registers itself by name on startup and discovers peers by name at runtime.

```
  Startup Flow

  Loans Service boots  -> registers as "LOANS-SERVICE" at its current IP and port
  Cards Service boots  -> registers as "CARDS-SERVICE" at its current IP and port


  Runtime Call Flow (Accounts calling Loans)

  Step 1: Accounts asks Eureka   "Where is LOANS-SERVICE?"
  Step 2: Eureka tells Accounts  "172.18.0.4:8090"
  Step 3: LoadBalancer picks one instance (round robin by default)
  Step 4: Accounts makes a direct HTTP call to Loans
```

Every service sends a heartbeat to Eureka every 30 seconds. If a service stops responding, Eureka evicts it from the registry automatically.

---

### Eureka Server Setup

```java
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

```yaml
# eurekaserver/src/main/resources/application.yml
server:
  port: 8070

eureka:
  instance:
    hostname: localhost
  client:
    registerWithEureka: false
    fetchRegistry: false
  server:
    waitTimeInMsWhenSyncEmpty: 0
```

---

### Registering a Microservice

Each microservice includes the Eureka Client dependency:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

And declares its identity in its configuration:

```yaml
# accounts/src/main/resources/application.yml
spring:
  application:
    name: ACCOUNTS-SERVICE

eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8070/eureka/
  instance:
    preferIpAddress: true
```

---

### Service-to-Service Calls with Load Balancing

The `@LoadBalanced` annotation enables client-side load balancing. The caller uses the registered service name and Spring resolves the actual IP at runtime.

```java
@Configuration
public class RestTemplateConfig {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

```java
@Service
public class AccountsService {

    private final RestTemplate restTemplate;

    public LoanDetails fetchLoanDetails(String customerId) {
        return restTemplate.getForObject(
            "http://LOANS-SERVICE/loans/{customerId}",
            LoanDetails.class,
            customerId
        );
    }
}
```

With Feign Client (declarative approach):

```java
@FeignClient(name = "LOANS-SERVICE")
public interface LoansClient {

    @GetMapping("/loans/{customerId}")
    LoanDetails getLoanDetails(@PathVariable String customerId);
}
```

---

### Load Balancing Strategies

| Strategy    | Behaviour                                   | Configuration                        |
|-------------|---------------------------------------------|--------------------------------------|
| Round Robin | Cycles through instances in order (default) | Default, no config needed            |
| Random      | Picks a random healthy instance             | Register a RandomLoadBalancer bean   |
| Custom      | Your own selection logic                    | Implement ReactorServiceInstanceLoadBalancer |

---

## Database Setup

Each service owns its database schema. There are no cross-service joins. All cross-domain queries go through service APIs.

| Service  | Database   | Container  |
|----------|------------|------------|
| Accounts | accountsdb | accountsdb |
| Loans    | loansdb    | loansdb    |
| Cards    | cardsdb    | cardsdb    |

Example datasource configuration:

```yaml
spring:
  datasource:
    url: jdbc:mysql://accountsdb:3306/accountsdb
    username: root
    password: ${MYSQL_ROOT_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
```

---

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.8 or higher
- Docker Desktop

---

### Option 1 - Full Stack with Docker Compose (Recommended)

Starts all databases, Config Server, Eureka, and the three microservices together.

```bash
# Clone the repository
git clone https://github.com/ThembaTman0/Bankwave-V-2.0.git
cd Bankwave-V-2.0

# Set your database password
echo "MYSQL_ROOT_PASSWORD=yourpassword" > .env

# Start the full system
docker-compose up --build
```

Once running, verify the services:

- Eureka dashboard: http://localhost:8070
- Accounts health: http://localhost:8080/actuator/health
- Loans health: http://localhost:8090/actuator/health
- Cards health: http://localhost:9000/actuator/health

---

### Option 2 - Running Locally

Start services in dependency order:

```bash
# 1. Config Server must start first
cd configserver && mvn spring-boot:run

# 2. Eureka Server
cd eurekaserver && mvn spring-boot:run

# 3. Microservices (open a separate terminal for each)
cd accounts && mvn spring-boot:run
cd loans    && mvn spring-boot:run
cd cards    && mvn spring-boot:run
```

---

## API Reference

**Accounts Service** - http://localhost:8080

| Method | Endpoint        | Description          |
|--------|-----------------|----------------------|
| GET    | /accounts       | List all accounts    |
| GET    | /accounts/{id}  | Get account by ID    |
| POST   | /accounts       | Create a new account |
| PUT    | /accounts/{id}  | Update account       |
| DELETE | /accounts/{id}  | Delete an account    |

**Loans Service** - http://localhost:8090

| Method | Endpoint    | Description       |
|--------|-------------|-------------------|
| GET    | /loans      | List all loans    |
| GET    | /loans/{id} | Get loan by ID    |
| POST   | /loans      | Create a new loan |

**Cards Service** - http://localhost:9000

| Method | Endpoint    | Description       |
|--------|-------------|-------------------|
| GET    | /cards      | List all cards    |
| GET    | /cards/{id} | Get card by ID    |
| POST   | /cards      | Issue a new card  |

---

## Project Structure

Each microservice follows a consistent layered architecture:

```
accounts/
+-- controller/     REST endpoints
+-- service/        Business logic
+-- repository/     Data access via Spring Data JPA
+-- entity/         JPA-managed domain models
+-- dto/            Request and response contracts
+-- exception/      Centralized error handling
```

---

## Roadmap

| Feature                    | Status  | Notes                                      |
|----------------------------|---------|--------------------------------------------|
| Centralized config         | Done    | Spring Cloud Config Server                 |
| Service discovery          | Done    | Netflix Eureka                             |
| Client-side load balancing | Done    | Spring Cloud LoadBalancer                  |
| API Gateway                | Planned | Spring Cloud Gateway                       |
| Circuit breaker            | Planned | Resilience4j                               |
| Distributed tracing        | Planned | Micrometer and Zipkin                      |
| Centralised logging        | Planned | ELK Stack                                  |
| Kubernetes deployment      | Planned | Helm charts and horizontal pod autoscaling |

---

## Author

**Themba Ngobeni**

[![GitHub](https://img.shields.io/badge/GitHub-ThembaTman0-181717?style=flat&logo=github)](https://github.com/ThembaTman0)
