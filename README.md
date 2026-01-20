# Audio Service

A reactive microservice built with **Java 21** and **Vert.x** for handling audio metadata and transcoding tasks. It integrates with **Apache Kafka** for event-driven messaging, **PostgreSQL** for metadata storage, and **AWS S3** (or LocalStack) for audio file storage.

## Stack
- **Language:** Java 21
- **Framework:** Vert.x 4.5.13
- **Package Manager:** Maven
- **Broker:** Apache Kafka
- **Database:** PostgreSQL
- **Storage:** AWS S3 / LocalStack
- **Infrastucture:** Docker, Docker Compose

## Requirements
- Java 21+
- Maven
- Docker and Docker Compose (for local development)

## Project Structure
```text
.
├── src/main/java
│   └── com/JoAbyssinia/audioService
│       ├── MainVerticle.java        # Main entry point; deploys other verticles
│       ├── aws/                     # AWS S3 client integration
│       ├── broker/                  # Kafka producer and consumer services
│       ├── config/                  # Configuration for Kafka, Postgres, and S3
│       ├── entity/                  # Data models (Audio, AudioStatus)
│       ├── eventBus/                # Internal Vert.x EventBus handlers
│       ├── repository/              # Database access logic (PostgreSQL)
│       ├── router/                  # HTTP route definitions
│       ├── service/                 # Business logic implementation
│       ├── verticle/                # Vert.x verticles (Metadata, Transcode Worker)
│       └── worker/                  # Background processing logic
├── src/test/java                    # Unit and integration tests
├── Dockerfile                       # Docker image definition
├── docker-compose.yml               # Local infrastructure (Postgres, Kafka, LocalStack)
└── pom.xml                          # Maven dependencies and build config
```

## Setup & Run

### 1. Start Infrastructure
Launch the required services (PostgreSQL, Kafka, LocalStack) using Docker Compose:
```bash
docker-compose up -d
```

### 2. Build the Application
Compile and package the application into a fat JAR:
```bash
./mvnw clean package
```

### 3. Run the Application
You can run it directly using Maven:
```bash
./mvnw clean compile exec:java
```
Or run the fat JAR:
```bash
java -jar target/audioService-1.0.0-SNAPSHOT-fat.jar
```
The server will start on port `8888`.

## Environment Variables
The application can be configured using the following environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `KAFKA_SERVER` | Kafka bootstrap server address | `localhost:9092` |
| `POSTGRES_HOST` | PostgreSQL host | `localhost` |
| `POSTGRES_PORT` | PostgreSQL port | `5432` |
| `POSTGRES_USERNAME`| Database username | `username` |
| `POSTGRES_PASSWORD`| Database password | `password` |
| `POSTGRES_DATABASE`| Database name | `postgres` |
| `AWS_S3_ENDPOINT` | S3 endpoint (LocalStack) | `http://localhost:4566` |
| `AWS_ACCESS_KEY_ID`| AWS access key | `112233445566` |
| `AWS_SECRET_ACCESS_KEY`| AWS secret key | `112233445566` |
| `AWS_REGION` | AWS region | `us-east-1` |

## Scripts
- `./mvnw clean test`: Run unit tests.
- `./mvnw clean package`: Build the project and create a fat JAR.
- `./mvnw clean compile exec:java`: Run the application in development mode.
- `./mvnw spotless:apply`: Apply Google Java Format (configured via Spotless).

## API Endpoints
- `POST /audio/save`: Save audio metadata.
  - Query params: `title`, `trackId`, `originalPath`, `artist`.
- `GET /playlists/tracks`: Retrieve all audio metadata.
- `GET /health`: Health check endpoint.

## Tests
To execute tests, run:
```bash
./mvnw clean test
```

## TODOs
- [ ] Add more comprehensive integration tests for Kafka and Postgres.
- [ ] Implement full audio transcoding logic (currently handled by `AudioTranscodeWorkerVerticle`).
- [ ] Add CI/CD pipeline details (Jenkinsfile exists but configuration is pending).
- [ ] Define API documentation (e.g., OpenAPI/Swagger).

## License
TODO: Add license information (e.g., MIT, Apache 2.0).
