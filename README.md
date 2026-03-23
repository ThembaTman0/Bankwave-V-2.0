<div align="center">

# 🏦 Bankwave V2.0

### Production-Ready Microservices Banking Backend

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-2023.x-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-cloud)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)

A cloud-native banking backend demonstrating **microservices architecture** with centralized configuration, service discovery, and container orchestration — built to reflect real-world distributed systems engineering.

</div>

---

## 📌 Table of Contents

- [Architecture](#-architecture)
- [Services](#-services)
- [Tech Stack](#-tech-stack)
- [Engineering Decisions](#-engineering-decisions)
- [Service Discovery & Registration](#-service-discovery--registration)
- [Database Setup](#-database-setup)
- [Getting Started](#-getting-started)
- [API Reference](#-api-reference)
- [Project Structure](#-project-structure)
- [Roadmap](#-roadmap)

---

## 🏗 Architecture

Bankwave follows a **microservices architecture** where each domain (accounts, loans, cards) is an independently deployable service with its own database. A centralized Config Server and Eureka Service Registry coordinate configuration and service-to-service communication.

```
                        ┌─────────────────────┐
                        │     Config Server    │
                        │   (Spring Cloud)     │
                        │      Port 8071       │
                        └──────────┬──────────┘
                                   │  serves config to all services
          ┌────────────────────────┼────────────────────────┐
          │                        │                        │
          ▼                        ▼                        ▼
┌──────────────────┐   ┌──────────────────┐   ┌──────────────────┐
│  Accounts Service│   │  Loans Service   │   │  Cards Service   │
│   Port: 8080     │   │   Port: 8090     │   │   Port: 9000     │
└────────┬─────────┘   └────────┬─────────┘   └────────┬─────────┘
         │                      │                       │
         │   ┌──────────────────┴──────────────────┐   │
         │   │       Eureka Discovery Server        │   │
         └──►│            Port: 8070               │◄──┘
             │   (service registration & lookup)   │
             └──────────────────────────────────────┘
         │                      │                       │
         ▼                      ▼                       ▼
┌──────────────────┐   ┌──────────────────┐   ┌──────────────────┐
│   accountsdb     │   │    loansdb       │   │    cardsdb       │
│   (MySQL 8.0)    │   │   (MySQL 8.0)    │   │   (MySQL 8.0)    │
└──────────────────┘   └──────────────────┘   └──────────────────┘

                  All containers run on a shared Docker network
```

---

## 📦 Services

| Service        | Responsibility                         | Port | Tech                        |
|----------------|----------------------------------------|------|-----------------------------|
| `accounts`     | Customer account lifecycle management  | 8080 | Spring Boot, JPA, MySQL     |
| `loans`        | Loan product data and management       | 8090 | Spring Boot, JPA, MySQL     |
| `cards`        | Card product data and management       | 9000 | Spring Boot, JPA, MySQL     |
| `configserver` | Centralized external configuration     | 8071 | Spring Cloud Config Server  |
| `eurekaserver` | Service registry and discovery         | 8070 | Spring Cloud Netflix Eureka |

---

## 🛠 Tech Stack

| Category              | Technology                              |
|-----------------------|-----------------------------------------|
| Language              | Java 17                                 |
| Framework             | Spring Boot 3.x                         |
| Service Discovery     | Spring Cloud Netflix Eureka             |
| Load Balancing        | Spring Cloud LoadBalancer               |
| Configuration         | Spring Cloud Config Server              |
| Persistence           | Spring Data JPA / Hibernate             |
| Database              | MySQL 8.0                               |
| Containerization      | Docker & Docker Compose                 |
| Build Tool            | Apache Maven                            |
| API Style             | RESTful JSON over HTTP                  |

---

## 🧠 Engineering Decisions

These are the core architectural decisions and the reasoning behind them.

**Database-per-Service**
Each microservice owns its schema and connects to an isolated MySQL container. This enforces bounded contexts — the Loans team can evolve its schema without coordinating with Accounts or Cards.

**Externalized Configuration (12-Factor App)**
All environment-specific configuration (datasource URLs, credentials, ports) lives in the Config Server. Services pull their config on startup, making it trivial to promote the same container image from dev → staging → production without rebuilding.

**Service Discovery over Static Addresses**
Hardcoding service hostnames breaks in containerized environments where IPs are assigned dynamically. Eureka eliminates this: services register themselves by name and discover peers at runtime — no DNS hacks, no hardcoded IPs.

**Client-Side Load Balancing**
Spring Cloud LoadBalancer runs inside the calling service rather than a dedicated proxy. This eliminates a network hop and gives each service independent control over retry and balancing strategy.

**Health-Check-Gated Startup**
Docker Compose health checks ensure that no microservice starts until its MySQL dependency is accepting connections. This prevents a class of race-condition startup failures common in composed environments.

---

## 🔍 Service Discovery & Registration

### The Problem

In Docker and cloud environments, services are assigned dynamic IPs at runtime. Hardcoding `http://loans:8090` into Accounts is fragile — it breaks the moment an instance is restarted, scaled, or redeployed.

### The Solution: Eureka

Bankwave uses **Netflix Eureka** (via Spring Cloud) as its service registry. Every service registers itself by name on startup and discovers peers by name at runtime.

```
  ┌─────────────────────────────────────────────────────────┐
  │                    Startup Flow                         │
  │                                                         │
  │  Loans Service boots → registers as "LOANS-SERVICE"     │
  │                         at its current IP + port        │
  │                                                         │
  │  Cards Service boots → registers as "CARDS-SERVICE"     │
  │                         at its current IP + port        │
  └─────────────────────────────────────────────────────────┘

  ┌─────────────────────────────────────────────────────────┐
  │                  Runtime Call Flow                      │
  │                                                         │
  │  Accounts needs to call Loans:                          │
  │                                                         │
  │  1. Accounts → Eureka:  "Where is LOANS-SERVICE?"       │
  │  2. Eureka   → Accounts: "172.18.0.4:8090"              │
  │  3. LoadBalancer picks one instance (round robin)       │
  │  4. Accounts → Loans:   direct HTTP call                │
  └─────────────────────────────────────────────────────────┘
```

Every service sends a **heartbeat to Eureka every 30 seconds**. If a service stops responding, Eureka evicts it from the registry — no manual intervention required.

---

### Eureka Server Configuration

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
    registerWithEureka: false   # the registry doesn't register itself
    fetchRegistry: false
  server:
    waitTimeInMsWhenSyncEmpty: 0
```

---

### Registering a Microservice (Eureka Client)

Each microservice includes the Eureka Client dependency:

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

And declares its identity in configuration:

```yaml
# accounts/src/main/resources/application.yml
spring:
  application:
    name: ACCOUNTS-SERVICE   # the name Eureka registers under

eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8070/eureka/
  instance:
    preferIpAddress: true
```

---

### Service-to-Service Calls with Load Balancing

The `@LoadBalanced` annotation on `RestTemplate` or `WebClient` enables client-side load balancing automatically. The caller uses the registered service name — Spring resolves the actual IP at runtime.

```java
@Configuration
public class RestTemplateConfig {

    @Bean
    @LoadBalanced  // integrates with Eureka + Spring Cloud LoadBalancer
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
        // Use service name — not a hardcoded IP
        return restTemplate.getForObject(
            "http://LOANS-SERVICE/loans/{customerId}",
            LoanDetails.class,
            customerId
        );
    }
}
```

With Feign Client (declarative, recommended):

```java
@FeignClient(name = "LOANS-SERVICE")
public interface LoansClient {

    @GetMapping("/loans/{customerId}")
    LoanDetails getLoanDetails(@PathVariable String customerId);
}
```

---

### Load Balancing Strategies

Spring Cloud LoadBalancer is the default and integrates with Eureka out of the box.

| Strategy    | Behaviour                                              | How to configure             |
|-------------|--------------------------------------------------------|------------------------------|
| Round Robin | Cycles through instances sequentially (default)        | Default — no config needed   |
| Random      | Picks a random healthy instance                        | `RandomLoadBalancer` bean    |
| Custom      | Implement `ReactorServiceInstanceLoadBalancer`         | Register as a `@Bean`        |

---

## 🗄 Database Setup

Each service owns its database schema. There are no cross-service joins — all cross-domain queries go through service APIs.

| Service  | Database     | Container   |
|----------|--------------|-------------|
| Accounts | `accountsdb` | `accountsdb`|
| Loans    | `loansdb`    | `loansdb`   |
| Cards    | `cardsdb`    | `cardsdb`   |

Example datasource configuration (externalized via Config Server):

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

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker Desktop (for the full stack)

---

### Option 1 — Full Stack with Docker Compose (Recommended)

This starts everything: databases, Config Server, Eureka, and all three microservices.

```bash
# 1. Clone the repository
git clone https://github.com/ThembaTman0/Bankwave-V-2.0.git
cd Bankwave-V-2.0

# 2. Set your database password
echo "MYSQL_ROOT_PASSWORD=yourpassword" > .env

# 3. Start the full system
docker-compose up --build
```

Service health can be verified at:
- Eureka dashboard: http://localhost:8070
- Accounts: http://localhost:8080/actuator/health
- Loans: http://localhost:8090/actuator/health
- Cards: http://localhost:9000/actuator/health

---

### Option 2 — Running Locally (Development)

Start services in dependency order:

```bash
# 1. Config Server (must be first — all others depend on it)
cd configserver && mvn spring-boot:run

# 2. Eureka Server
cd eurekaserver && mvn spring-boot:run

# 3. Microservices (in separate terminals)
cd accounts && mvn spring-boot:run
cd loans    && mvn spring-boot:run
cd cards    && mvn spring-boot:run
```

---

## 📡 API Reference

All APIs follow REST conventions and return JSON.

**Accounts Service** — `http://localhost:8080`

| Method | Endpoint               | Description              |
|--------|------------------------|--------------------------|
| GET    | `/accounts`            | List all accounts        |
| GET    | `/accounts/{id}`       | Get account by ID        |
| POST   | `/accounts`            | Create a new account     |
| PUT    | `/accounts/{id}`       | Update account details   |
| DELETE | `/accounts/{id}`       | Delete an account        |

**Loans Service** — `http://localhost:8090`

| Method | Endpoint             | Description           |
|--------|----------------------|-----------------------|
| GET    | `/loans`             | List all loans        |
| GET    | `/loans/{id}`        | Get loan by ID        |
| POST   | `/loans`             | Create a new loan     |

**Cards Service** — `http://localhost:9000`

| Method | Endpoint             | Description           |
|--------|----------------------|-----------------------|
| GET    | `/cards`             | List all cards        |
| GET    | `/cards/{id}`        | Get card by ID        |
| POST   | `/cards`             | Issue a new card      |

---

## 📁 Project Structure

Each microservice follows a consistent layered architecture:

```
accounts/
├── controller/        # REST endpoints — HTTP in, HTTP out
├── service/           # Business logic — orchestrates domain rules
├── repository/        # Data access via Spring Data JPA
├── entity/            # JPA-managed domain models
├── dto/               # Request/response contracts (decoupled from entity)
└── exception/         # Centralized error handling (@ControllerAdvice)
```

This separation keeps each layer testable in isolation and prevents domain logic from leaking into HTTP or persistence concerns.

---

## 🗺 Roadmap

| Feature                   | Status         | Notes                                         |
|---------------------------|----------------|-----------------------------------------------|
| Centralized config        | ✅ Done        | Spring Cloud Config Server                    |
| Service discovery         | ✅ Done        | Netflix Eureka                                |
| Client-side load balancing| ✅ Done        | Spring Cloud LoadBalancer                     |
| API Gateway               | 🔜 Planned    | Spring Cloud Gateway — single entry point     |
| Circuit breaker           | 🔜 Planned    | Resilience4j — fault tolerance                |
| Distributed tracing       | 🔜 Planned    | Micrometer + Zipkin                           |
| Centralised logging       | 🔜 Planned    | ELK Stack (Elasticsearch, Logstash, Kibana)   |
| Kubernetes deployment     | 🔜 Planned    | Helm charts, HPA, rolling updates             |

---

## 👨‍💻 Author

**Themba Ngobeni**

Built as a demonstration of production-grade microservices engineering with Spring Boot and Spring Cloud.

[![GitHub](https://img.shields.io/badge/GitHub-ThembaTman0-181717?style=flat&logo=github)](https://github.com/ThembaTman0)
