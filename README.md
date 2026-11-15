# CareConnect

CareConnect is a full-stack MVP for coordinating home-care visits. Patients can request care and find eligible caregivers; caregivers manage availability and visit requests; admins verify caregiver profiles.

> **Note:** This is a demo project, not a medical or emergency service. Use demo data only—do not enter real patient or medical information.

## Features

- Request home-care visits and view rule-based caregiver matches.
- Manage caregiver profiles, availability, and booking responses.
- Track visits from request through completion and leave reviews.
- Admin tools for caregiver verification and user management.
- Server-side booking status and scheduling conflict checks.

## Tech stack

- **Frontend:** React, Vite, React Router
- **Backend:** Java 21, Spring Boot 3, Spring Security, Spring Data JPA
- **Database:** H2 by default; PostgreSQL is supported

## Run locally

**Prerequisites:** JDK 21, Maven, Node.js 18+, and npm.

Start the backend in one terminal:

cd backend
mvn spring-boot:run


Start the frontend in a second terminal:

cd frontend
npm install
npm run dev


Open the app at [http://localhost:5173](http://localhost:5173). The backend API runs at `http://localhost:8080/api`.

The backend uses an in-memory H2 database by default, so no database setup is needed for a quick local run. Its data is reset when the backend stops.

### Demo accounts

All demo accounts use the password `demo`.

| Role | Email |
|---|---|
| Patient | `patient@careconnect.test` |
| Caregiver | `caregiver@careconnect.test` |
| Admin | `admin@careconnect.test` |

## Build checks

Backend:

cd backend
mvn test


Frontend:

cd frontend
npm run build


## Contributor

Ruchika Nanhe
```
