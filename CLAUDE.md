# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Common Development Commands

### Build and Run
- **Build**: `./gradlew build`
- **Run application**: `./gradlew run`
- **Run tests**: `./gradlew test`
- **Run single test**: `./gradlew test --tests "ClassName.methodName"`

### Development Tools
- **Clean build**: `./gradlew clean build`
- **Generate shadow JAR**: `./gradlew shadowJar`
- **Check dependencies**: `./gradlew dependencies`

## Architecture Overview

This is a **Micronaut-based finance management REST API** written in **Kotlin** with the following key characteristics:

### Technology Stack
- **Framework**: Micronaut 4.9.0 with Netty runtime
- **Language**: Kotlin 1.9.25 targeting JVM 21
- **Database**: PostgreSQL with Hibernate JPA
- **Testing**: Spock (Groovy-based) with comprehensive unit tests
- **Validation**: Jakarta Validation with custom validators
- **JSON**: Jackson with custom serialization

### Domain-Driven Architecture
The application follows a layered architecture pattern:

```
finance/
├── domain/          # JPA entities with validation annotations
├── repositories/    # Data access layer (Micronaut Data JPA)
├── services/        # Business logic layer with interfaces
├── controllers/     # REST API endpoints
├── utils/           # Custom validators, converters, constants
└── configurations/  # Application configuration classes
```

### Key Domain Entities
- **Transaction**: Core financial transaction with receipt images, categories
- **Account**: Financial accounts with different types (credit, debit, investment)
- **Payment**: Scheduled payments with reoccurring patterns
- **Category/Description**: Classification and metadata for transactions
- **User**: Authentication and authorization
- **ReceiptImage**: Image storage for transaction receipts

### Important Patterns
1. **Custom Converters**: Domain enums use JPA converters (e.g., `AccountTypeConverter`, `TransactionStateConverter`)
2. **Validation**: Extensive use of custom validators (`@ValidDate`, `@ValidImage`, `@ValidTimestamp`)
3. **JSON Serialization**: Custom `@JsonGetter`/`@JsonSetter` for date formatting
4. **Repository Pattern**: Micronaut Data repositories extending from base interfaces
5. **Service Interfaces**: Business logic abstracted behind interfaces (e.g., `IAccountService`)

### Database Configuration
- Uses PostgreSQL with connection pooling (HikariCP)
- Hibernate ORM with schema validation (`hibernate.hbm2ddl.auto: validate`)
- Custom database schema with `t_` prefixed table names
- Foreign key relationships between entities

### Testing Strategy
- **Unit Tests**: Spock framework tests for services and domain objects
- **Test Builders**: Helper classes for creating test data (`TransactionBuilder`, `AccountBuilder`, etc.)
- **Domain Validation Tests**: Comprehensive validation testing for all entities

### CORS and Security
- CORS enabled for development frontends (localhost:3000, 4200, 8080)
- JWT token support configured
- SSL/TLS keystore configuration available (commented out)

### Key Development Notes
- All string fields are converted to lowercase using `LowerCaseConverter`
- Extensive regex validation patterns in `Constants.kt`
- Transaction dates use custom JSON serialization for `yyyy-MM-dd` format
- Receipt images are linked to transactions via optional foreign key relationship
- Application uses introspection for domain package scanning