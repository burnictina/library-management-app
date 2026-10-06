# Library Management System

A desktop application for managing day-to-day library operations, developed in Java using JavaFX.

The system is designed primarily for librarians and provides functionality for managing library resources, members, loans, and reservations.

> **Project status:** The application is actively being developed and extended with additional features and improvements.

## Features

### Library Catalog Management
- Add and manage books
- Add and manage authors
- Add and manage magazines
- Store and retrieve library data using multiple persistence mechanisms

### Member Management
- Add and manage library members
- View member information
- Track member loans

### Loan Management
- Create and manage book loans
- Track active loans
- Handle loan-related business rules
- Calculate fines for overdue items

### Reservations
- Create reservations when an item is currently unavailable
- Manage existing reservations
- Handle reservation expiration

### Additional Features
- Librarian authentication
- Librarian management
- Croatian and English language support
- PDF report generation
- XML and JSON data storage
- Integration with the Open Library API
- Application logging
- Configurable application settings

## Technologies

- Java
- JavaFX
- Maven
- H2 Database
- FXML
- CSS
- Jackson
- JAXB
- Apache PDFBox
- SLF4J / Logback
- Open Library API

## Demo

A short demonstration of the current version of the application will be available here.

https://github.com/user-attachments/assets/4cce7e9c-0777-4a21-b581-37d305da1589

## Running the Application

The application currently requires a locally running H2 database.

Database configuration is intentionally excluded from the repository because it contains environment-specific settings.

To run the application locally:

1. Clone the repository.
2. Configure and start a local H2 database.
3. Create the required local database configuration.
4. Build the project using Maven.
5. Run the JavaFX application.

> More detailed setup instructions will be added as the deployment and database setup are further developed.

## Project Structure

The project is organized as a multi-module Maven application:

- `app` – main JavaFX application, UI, business logic, database access, and application services
- `library-utils` – reusable validation, text, and selection utilities

## Development

The project is under active development. Future improvements may include changes to deployment, database configuration, and additional library management functionality.