# Bankwave V2.0

A microservices-based banking backend built with **Spring Boot** and **Spring Cloud Config**.

This project demonstrates how independent services can share centralized configuration while remaining loosely coupled and independently deployable.

---

## Services

| Service      | Description                      | Default Port |
| ------------ | -------------------------------- | ------------ |
| accounts     | Manages customer account data    | 8080         |
| loans        | Manages loan information         | 8090         |
| cards        | Manages card details             | 9000         |
| configserver | Centralized configuration server | 8071         |

Each service runs as a standalone Spring Boot application.

---

## Tech Stack

* Java
* Spring Boot
* Spring Cloud Config
* Spring Data JPA
* Maven
* Docker & Docker Compose

---

## Architecture

The system follows a microservices architecture where:

* Each service owns its own data
* Services communicate through REST APIs
* Configuration is centralized using Spring Cloud Config
* Services remain independently deployable

The **Config Server** provides configuration to all services during startup.

---

## Project Structure

Each microservice follows a simple layered structure:

* `controller` – REST endpoints
* `service` – business logic
* `repository` – data access layer
* `entity` – database models
* `dto` – API request and response objects

---

## Running the Project

### 1. Clone the repository

```
git clone https://github.com/ThembaTman0/Bankwave-V-2.0.git
cd Bankwave-V-2.0
```

---

### 2. Start the Config Server

The configuration server must be started first.

```
cd configserver
mvn spring-boot:run
```

Verify the server is running:

```
http://localhost:8071
```

---

### 3. Start the Microservices

Run each service in a separate terminal.

Accounts service:

```
cd accounts
mvn spring-boot:run
```

Loans service:

```
cd loans
mvn spring-boot:run
```

Cards service:

```
cd cards
mvn spring-boot:run
```

---

## Running with Docker (Optional)

From the root directory:

```
docker-compose up --build
```

---

## Testing APIs

You can test endpoints using:

* Postman
* curl
* Browser (for GET requests)

Example:

```
http://localhost:8080/accounts
```

---

## Engineering Practices

Some practices used while building this project:

* Centralized exception handling
* DTO based API design
* Layered architecture
* Externalized configuration
* Service isolation
* Meaningful logging

The goal is to keep services simple, predictable, and easy to maintain.

---

## Future Improvements

Possible next steps for the system:

* Service discovery
* API gateway
* Resilience patterns
* Monitoring and observability

---

## Author

Built and maintained by Themba Ngobeni.
