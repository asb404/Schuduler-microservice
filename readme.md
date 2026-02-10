# Scheduler Microservice

A **Scheduler Microservice** designed to manage and execute scheduled tasks in a microservices architecture. This service centralizes scheduling logic and exposes REST APIs for creating, managing, and triggering jobs.

## 🚀 Features

- Create and manage scheduled jobs
- Support for cron-based and time-based scheduling
- Trigger jobs manually when required
- Persistent storage of schedules
- RESTful APIs for easy integration with other services
- Designed to be scalable and independently deployable

## 🧠 Why a Scheduler Microservice?

In a microservices system, having a dedicated scheduler:
- avoids duplicate scheduling logic across services
- improves maintainability
- enables centralized monitoring and control of background jobs
- keeps services loosely coupled

## 🛠 Tech Stack

- **Language:** Java
- **Framework:** Spring Boot
- **Scheduling:** Spring Scheduler / Quartz (as applicable)
- **Build Tool:** Maven
- **API Style:** REST
- **Containerization:** Docker (optional)

## 📌 API Endpoints (Sample)

POST /api/schedules → Create a new schedule
GET /api/schedules → Get all schedules
GET /api/schedules/{id} → Get schedule by ID
PUT /api/schedules/{id} → Update schedule
DELETE /api/schedules/{id} → Delete schedule
POST /api/schedules/{id}/run → Trigger schedule immediately
