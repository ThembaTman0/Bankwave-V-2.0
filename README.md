# Bankwave V2.0

Bankwave V2.0 is a microservices-based backend system built with **Spring Boot**, **Spring Cloud Config**, **MySQL**, and **Docker**.

The system demonstrates how multiple independent services can share centralized configuration while maintaining their own databases and deployment lifecycle.

---

# 10‑Second Architecture Overview

```
                +----------------------+
                |    Config Server     |
                |  (Spring Cloud)      |
                +----------+-----------+
                           |
        ----------------------------------------------
        |                     |                      |
+---------------+     +---------------+     +---------------+
|   Accounts    |     |     Loans     |     |     Cards     |
|  Microservice |     |  Microservice |     |  Microservice |
+-------+-------+     +-------+-------+     +-------+-------+
        |                     |                      |
+---------------+     +---------------+     +---------------+
|   AccountsDB  |     |    LoansDB    |     |    CardsDB    |
|    (MySQL)    |     |    (MySQL)    |     |    (MySQL)    |
+---------------+     +---------------+     +---------------+

         All services run in Docker containers
```

---

# Services

| Service      | Description                                | Port |
| ------------ | ------------------------------------------ | ---- |
| accounts     | Handles customer account data              | 8080 |
| loans        | Handles loan related information           | 8090 |
| cards        | Handles card related information           | 9000 |
| configserver | Centralized configuration for all services | 8071 |
| eurekaserver | Service registry for discovery             | 8070 |

Each service is an independent Spring Boot application.

---

# Technology Stack

* Java
* Spring Boot
* Spring Cloud Config
* Spring Cloud Netflix Eureka
* Spring Cloud LoadBalancer
* Spring Data JPA
* MySQL
* Maven
* Docker & Docker Compose

---

# Architecture Principles

The system follows common microservices practices:

* **Database per service** – each service owns its data
* **Service isolation** – services are independently deployable
* **Centralized configuration** – provided through Spring Cloud Config
* **Service discovery** – services locate each other via Eureka
* **Client-side load balancing** – handled by Spring Cloud LoadBalancer
* **Containerized infrastructure** – managed with Docker Compose

---

# Service Discovery & Registration

In a microservices system, services run on dynamic IPs and ports — especially inside Docker. Hardcoding addresses is not an option. Service Discovery solves this by giving every service a way to find the others at runtime.

Bankwave uses **Netflix Eureka** (via Spring Cloud) for this.

## How It Works

```
  1. On startup, each service registers with Eureka:
     "I am LOANS-SERVICE, running at 172.18.0.4:8090"

  2. When Accounts needs to call Loans, it asks Eureka:
     "Where is LOANS-SERVICE?"

  3. Eureka returns the current address.

  4. Spring Cloud LoadBalancer picks one instance
     (Round Robin by default) and the call is made directly.
```

```
+------------------+        (1) register         +-------------------+
|  Loans Service   | --------------------------> |   Eureka Server   |
+------------------+                             |   (port 8070)     |
                                                 +-------------------+
+------------------+   (2) where is Loans?              ^
|  Accounts Service| ---------------------------------> |
|                  | <--------------------------------- |
|  [LoadBalancer]  |   (3) here is the address          |
+------------------+                                    |
        |                                               |
        | (4) direct call                               |
        v                                               |
+------------------+        (1) register               |
|  Loans :8090     | ---------------------------------> +
+------------------+
```

Each service also sends a **heartbeat** to Eureka every 30 seconds. If a service goes down and stops sending heartbeats, Eureka removes it from the registry automatically.

---

## Eureka Server Setup

The Eureka Server is a dedicated Spring Boot application that acts as the service registry.

**Dependency** (`pom.xml`):

```xml

    org.springframework.cloud
    spring-cloud-starter-netflix-eureka-server

```

**Main class**:

```java
@Spring
