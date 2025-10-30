# Spring Boot + Apache Pekko Integration

A demonstration project showcasing the integration of **Spring Boot 3.4.3** with **Apache Pekko 1.2.0** for building scalable, event-sourced, distributed applications using the Actor Model, CQRS, and Event Sourcing patterns.

This project is a Spring Boot port of the [quarkus-pekko](../quarkus-pekko) project, maintaining the same functionality while using Spring Boot as the application framework instead of Quarkus.

## Features

- **Event Sourcing**: Complete event-sourced system using Pekko Persistence
- **CQRS**: Separate write and read models with optimized query paths
- **Actor Model**: Scalable actor-based domain modeling with Pekko Actors
- **Cluster Sharding**: Distributed actors across cluster nodes
- **Projections**: Read-side projections with exactly-once processing guarantees
- **Kafka Integration**: Event publishing and command consumption via Apache Kafka
- **REST API**: Async REST endpoints using Spring Web MVC
- **PostgreSQL**: Event store and read model persistence
- **Flyway**: Database schema migrations
- **Docker Compose Dev Services**: Automatic PostgreSQL and Kafka startup during development (like Quarkus Dev Services)

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        REST API Layer                        │
│                    (Spring Web MVC)                          │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                      Service Layer                           │
│           (Wraps Actor Communication)                        │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                   Pekko Cluster Sharding                     │
│              (Distributes Actors Across Nodes)               │
└────────┬────────────────────────────────────────────────┬───┘
         │                                                 │
┌────────▼─────────────┐                    ┌─────────────▼────┐
│  Event-Sourced Actor │                    │  Kafka Handler    │
│   (Write Side)       │                    │  (Projection)     │
│                      │                    │                   │
│  Commands → Events   │                    │  Events → Kafka   │
│  Events → State      │                    └──────────┬────────┘
└────────┬─────────────┘                               │
         │                                             │
         │                                    ┌────────▼────────┐
         │                                    │  Apache Kafka    │
         │                                    └─────────────────┘
         │
┌────────▼──────────────────────────────────────────────────┐
│                    PostgreSQL Database                     │
│  ┌─────────────────┐  ┌─────────────────┐                 │
│  │ Event Journal   │  │ Read Model      │                 │
│  │ (Event Store)   │  │ (Projections)   │                 │
│  └─────────────────┘  └─────────────────┘                 │
└───────────────────────────────────────────────────────────┘
```

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.8+
- Docker and Docker Compose (for automatic dev services)

**That's it!** PostgreSQL and Kafka are automatically started via Docker Compose during development.

### Running the Application (Development Mode)

Spring Boot automatically starts PostgreSQL and Kafka for you - no manual setup required:

```bash
# Clone the repository
cd spring-pekko

# Run the application - Docker Compose services start automatically!
./mvnw spring-boot:run

# The application will start on:
# - HTTP: http://localhost:8080
# - Pekko Management: http://localhost:7626
```

On first run, Spring Boot will:
1. **Automatically start** PostgreSQL (localhost:5432) and Kafka (localhost:9092) via Docker Compose
2. Apply Flyway database migrations
3. Initialize the Pekko ActorSystem
4. Start the cluster
5. Initialize cluster sharding
6. Set up projection handlers

Services are **automatically stopped** when you stop the application (Ctrl+C).

### Manual Setup (Alternative)

If you prefer to manage PostgreSQL and Kafka manually:

```bash
# Start services manually
docker compose up -d

# Or use local installations

# Then run the application
./mvnw spring-boot:run
```

### Building for Production

For production/Kubernetes deployment, build without dev dependencies:

```bash
# Build production JAR (excludes Docker Compose support)
./mvnw clean package -P \!dev

# The resulting JAR is safe for production deployment
java -jar target/spring-pekko-1.0.0-SNAPSHOT.jar
```

**Note**: The production JAR will **not** attempt to start Docker Compose services.

## API Usage

### Greet a user (creates or increments greeting)

```bash
curl -X POST http://localhost:8080/greetings/Alice
# Response: {"status":"OpenState","count":1}

curl -X POST http://localhost:8080/greetings/Alice
# Response: {"status":"OpenState","count":2}
```

### Get greeting count

```bash
curl http://localhost:8080/greetings/Alice
# Response: {"status":"OpenState","count":2}
```

### Ungreet a user (decrements greeting)

```bash
curl -X DELETE http://localhost:8080/greetings/Alice
# Response: {"status":"OpenState","count":1}
```

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

### Pekko Cluster Status

```bash
curl http://localhost:7626/cluster/members
```

## Project Structure

```
spring-pekko/
├── src/main/java/
│   ├── com/gdfesta/springboot/pekko/     # Spring-Pekko integration
│   │   ├── SpringBootPekkoActorSystemConfiguration.java
│   │   ├── HibernateSessionFactory.java
│   │   └── HibernateJdbcSession.java
│   └── com/gdfesta/example/
│       ├── SpringPekkoApplication.java    # Main class
│       ├── api/                           # REST controllers
│       ├── write_side/                    # CQRS Write side
│       │   └── greeting/
│       │       ├── aggregate/             # Event-sourced actors
│       │       └── services/              # Domain services
│       ├── read_side/                     # CQRS Read side
│       │   └── greetings_count/           # Read model & projections
│       ├── kafka/                         # Kafka producers/consumers
│       ├── ShardsCreator.java             # Cluster sharding init
│       └── JdbcHandlersCreator.java       # Projection handlers init
├── src/main/resources/
│   ├── application.yml                    # Spring Boot config
│   ├── application.conf                   # Pekko config
│   └── db/migration/                      # Flyway migrations
└── pom.xml
```

## Testing

```bash
# Run all tests
./mvnw test

# Run with coverage
./mvnw verify

# Run specific test
./mvnw test -Dtest=GreetingControllerTest
```

Tests use:
- **Testcontainers** for PostgreSQL integration tests
- **EmbeddedKafka** for Kafka integration tests
- **Awaitility** for async projection testing

## Configuration

### Spring Boot Configuration (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/spring-pekko
    username: spring-pekko
    password: spring-pekko

  kafka:
    bootstrap-servers: localhost:9092
```

### Pekko Configuration (application.conf)

```hocon
pekko {
  actor.provider = "cluster"

  persistence {
    journal.plugin = "jdbc-journal"
    snapshot-store.plugin = "jdbc-snapshot-store"
  }

  cluster {
    seed-nodes = []  # Auto-discovery in K8s
    sharding.number-of-shards = 100
  }
}
```

## Docker Compose Dev Services

Spring Boot automatically manages PostgreSQL and Kafka during development, similar to Quarkus Dev Services. This provides a seamless development experience without manual service setup.

### How It Works

The project includes:
- **compose.yaml**: Defines PostgreSQL and Kafka services
- **Maven dev profile**: Includes `spring-boot-docker-compose` dependency (active by default)
- **Automatic lifecycle**: Services start with `./mvnw spring-boot:run` and stop when you exit

When you run the application in dev mode:
1. Spring Boot detects the `compose.yaml` file
2. Starts PostgreSQL and Kafka containers automatically
3. Configures connection properties dynamically
4. Stops containers when the application exits

### Production Safety

The Docker Compose support is **isolated to development only**:

✅ **Maven Profile**: The `spring-boot-docker-compose` dependency is only in the `dev` profile
✅ **Production Builds**: Use `./mvnw clean package -P \!dev` to exclude dev dependencies
✅ **Kubernetes Safe**: Won't activate in containerized environments (no Docker socket access)
✅ **Explicit Control**: Can be disabled with `SPRING_DOCKER_COMPOSE_ENABLED=false`

### Manual Override

To disable Docker Compose in dev mode:

```bash
# Via environment variable
export SPRING_DOCKER_COMPOSE_ENABLED=false
./mvnw spring-boot:run

# Or via system property
./mvnw spring-boot:run -Dspring.docker.compose.enabled=false
```

## Key Concepts

### Event Sourcing

All state changes are persisted as immutable events in the event journal. The current state is reconstructed by replaying events.

```java
// Event-sourced actor
public class GreetingActorBehavior extends EventSourcedBehavior<Command, Event, State> {
    // Commands change state by persisting events
    // Events are immutable and stored forever
    // State is derived from events
}
```

### CQRS (Command Query Responsibility Segregation)

- **Write Side**: Commands are handled by event-sourced actors
- **Read Side**: Queries are served from optimized read models (projections)

### Projections

Projections consume events from the event journal and update read models:

```java
// Read-side projection
public class GreetingsCountReadSideHandler extends JdbcHandler<...> {
    // Processes events and updates read model
    // Exactly-once processing guarantee
}
```

### Cluster Sharding

Actors are automatically distributed across cluster nodes:

```java
sharding.init(Entity.of(
    GreetingActorBehavior.ENTITY_TYPE_KEY,
    GreetingActorBehavior::create
));
```

## Spring Boot vs Quarkus Differences

This project is a port of the `quarkus-pekko` project to Spring Boot. Key differences:

| Aspect | Quarkus Version | Spring Boot Version |
|--------|----------------|---------------------|
| **DI Framework** | CDI (`@ApplicationScoped`) | Spring DI (`@Component`, `@Service`) |
| **Reactive** | Mutiny (`Uni<T>`) | CompletionStage/CompletableFuture |
| **REST** | JAX-RS (`@Path`, `@GET`) | Spring Web MVC (`@RestController`, `@GetMapping`) |
| **Data Access** | Panache | Spring Data JPA |
| **Messaging** | MicroProfile Reactive Messaging | Spring Kafka |
| **Lifecycle** | `@Observes StartupEvent` | `@EventListener(ApplicationReadyEvent.class)` |
| **Testing** | `@QuarkusTest` | `@SpringBootTest` |
| **Dev Services** | Built-in Dev Services | Docker Compose integration (via `spring-boot-docker-compose`) |

Both versions maintain identical functionality and domain logic, including the seamless development experience with automatic service startup.

## Development

### Adding a New Entity

1. Create the domain model (commands, events, states)
2. Create the event-sourced actor behavior
3. Register with cluster sharding in `ShardsCreator`
4. Create a service to interact with the actor
5. Create REST endpoints
6. Add projection handlers if needed
7. Write tests

See [AGENTS.MD](./AGENTS.MD) for detailed development guidelines.

## Production Considerations

This is a **demonstration project**. For production use, consider:

- **Build Profile**: Always build with `./mvnw clean package -P \!dev` to exclude development dependencies
- **Docker Compose**: Ensure `compose.yaml` is excluded from production images (already in `.dockerignore`)
- **Security**: Add authentication/authorization (Spring Security)
- **Secrets Management**: Use environment variables or secret managers
- **Monitoring**: Add metrics, distributed tracing (Micrometer, OpenTelemetry)
- **High Availability**: Deploy multiple instances behind a load balancer
- **Kubernetes**: Use Pekko Cluster Bootstrap for automatic discovery
- **Backups**: Implement event store backup strategy
- **Performance**: Tune Pekko cluster sharding and database connection pools

## Contributing

This is a demonstration project for learning purposes. Feel free to fork and experiment!

## License

This project is provided as-is for educational purposes.

## Related Projects

- [quarkus-pekko](../quarkus-pekko) - The original Quarkus version
- [Apache Pekko](https://pekko.apache.org/) - Actor toolkit
- [Spring Boot](https://spring.io/projects/spring-boot) - Application framework

## Support

For questions or issues:
- Check [AGENTS.MD](./AGENTS.MD) for detailed technical documentation
- Compare with the [quarkus-pekko](../quarkus-pekko) project
- Refer to [Apache Pekko documentation](https://pekko.apache.org/docs/)
- Refer to [Spring Boot documentation](https://spring.io/projects/spring-boot)
