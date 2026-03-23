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

Each service is an independent Spring Boot application.

---

# Technology Stack

* Java
* Spring Boot
* Spring Cloud Config
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
* **Containerized infrastructure** – managed with Docker Compose

---

# Database Setup

Each microservice connects to its own MySQL container.

| Service  | Database   |
| -------- | ---------- |
| Accounts | accountsdb |
| Loans    | loansdb    |
| Cards    | cardsdb    |

Example datasource configuration:

```
SPRING_DATASOURCE_URL=jdbc:mysql://accountsdb:3306/accountsdb
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=${MYSQL_ROOT_PASSWORD}
```

Health checks ensure that services only start after their database containers are ready.

---

# Project Structure

Each microservice follows a layered structure:

```
controller   → REST endpoints
service      → business logic
repository   → database access
entity       → domain models
dto          → request/response contracts
exception    → centralized error handling
```

---

# Running the Project

## 1. Clone the repository

```
git clone https://github.com/ThembaTman0/Bankwave-V-2.0.git
cd Bankwave-V-2.0
```

---

# Running Locally (Development)

Start the Config Server first:

```
cd configserver
mvn spring-boot:run
```

Then start the microservices in separate terminals.

Accounts:

```
cd accounts
mvn spring-boot:run
```

Loans:

```
cd loans
mvn spring-boot:run
```

Cards:

```
cd cards
mvn spring-boot:run
```

---

# Running the Full System with Docker

Create a `.env` file in the root directory:

```
MYSQL_ROOT_PASSWORD=yourpassword
```

Start everything:

```
docker-compose up --build
```

Docker will start:

* MySQL databases
* All microservices
* Shared network configuration

Database data is stored in Docker volumes.

---

# Testing APIs

APIs can be tested using:

* Postman
* curl
* Browser (GET requests)

Example endpoint:

```
http://localhost:8080/accounts
```

---

# Engineering Practices

While building this project the focus was on keeping the code clear and maintainable. Some practices used include:

* Layered architecture
* DTO-based API contracts
* Centralized exception handling
* Externalized configuration
* Service isolation
* Meaningful logging

---

# Future Improvements

Possible next steps for the system include:

* Service discovery
* API gateway
* Resilience patterns
* Observability and monitoring

---

# Author

Built and maintained by **Themba Ngobeni**.
