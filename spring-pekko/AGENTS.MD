# AI Agent Guide for Spring-Pekko

This guide helps AI agents understand and work effectively with the Spring Boot + Pekko project.

## Project Overview

**Type**: Reactive distributed system (demonstration project)
**Stack**: Spring Boot 3.4.3 + Apache Pekko 1.2.0 + PostgreSQL + Kafka
**Patterns**: Event Sourcing, CQRS, Actor Model, Cluster Sharding
**Language**: Java 21
**Build Tool**: Maven

**Purpose**: Demonstrates integration of Spring Boot with Apache Pekko for building scalable, event-sourced, distributed applications using the actor model. This is a Spring Boot port of the quarkus-pekko project.

## Quick Start

### Prerequisites Check
```bash
# Verify Java version (must be 21+)
java -version

# If using SDKMAN and wrong version
source ~/.sdkman/bin/sdkman-init.sh && sdk env install
```

### Development Mode

**Automatic Dev Services** (Recommended):
```bash
# Spring Boot automatically starts PostgreSQL and Kafka via Docker Compose
# Just run the application - no manual setup needed!
./mvnw spring-boot:run

# Services started automatically:
# - PostgreSQL: localhost:5432 (database: spring-pekko, user: spring-pekko, password: spring-pekko)
# - Kafka: localhost:9092

# Access points:
# - API: http://localhost:8080
# - Actuator: http://localhost:8080/actuator/health
# - Pekko Management: http://localhost:7626/ready
```

**Manual Setup** (if you prefer):
```bash
# Start services manually using Docker Compose
docker compose up -d

# Or use local PostgreSQL/Kafka installations

# Then start the application
./mvnw spring-boot:run
```

**Production Build** (without dev dependencies):
```bash
# Build for production (excludes Docker Compose support)
./mvnw clean package -P \!dev

# The resulting JAR is safe for Kubernetes/production deployment
java -jar target/spring-pekko-1.0.0-SNAPSHOT.jar
```

### Testing
```bash
# Unit + integration tests
./mvnw test

# All tests including integration tests
./mvnw verify

# Specific test class
./mvnw test -Dtest=GreetingControllerTest
```

## Build & Test Commands

### Building
```bash
# Compile only
./mvnw compile

# Package application
./mvnw package

# Build and run
./mvnw spring-boot:run

# Build jar
./mvnw clean package
java -jar target/spring-pekko-1.0.0-SNAPSHOT.jar
```

### Testing Strategy

**Two test types serve different purposes:**

1. **Pure Unit Tests** (no framework, very fast)
   - Files: `*Test.java` without Spring annotations
   - Examples: `OpenStateTest`, `CloseStateTest`
   - Test isolated classes (state transitions)
   - Run with: `./mvnw test`

2. **Integration Tests** (with `@SpringBootTest`, comprehensive)
   - Files: `*Test.java` with `@SpringBootTest`
   - Examples: `GreetingControllerTest`, `GreetingsCountRepositoryTest`
   - Test full stack (REST → Actors → Database → Kafka)
   - Support dependency injection
   - Run with: `./mvnw test`

**Test Execution**:
```bash
# Fast feedback loop (development)
./mvnw test

# Complete validation (pre-commit, CI/CD)
./mvnw verify

# Parallel execution (faster)
./mvnw test -T 1C
```

## Code Style & Conventions

### Package Organization
```
com.gdfesta.springboot.pekko/  - Spring Boot-Pekko integration layer
com.gdfesta.example/
  ├── api/                     - REST endpoints and DTOs
  ├── write_side/              - Command handling (CQRS write side)
  │   ├── greeting.aggregate/  - Event-sourced actors
  │   └── greeting.services/   - Domain services
  ├── read_side/               - Query handling (CQRS read side)
  │   └── greetings_count/     - Read models and projections
  └── kafka/                   - Messaging (consumers/producers)
```

### Naming Conventions

**Actors**:
- Behavior classes: `{Entity}ActorBehavior` (e.g., `GreetingActorBehavior`)
- Entity type key: `{entity}-aggregate` (e.g., `greeting-aggregate`)
- Persistence ID: `{entity}-aggregate|{id}` (e.g., `greeting-aggregate|Alice`)

**Commands/Events/States**:
- Sealed interfaces for type safety
- Command: `{Entity}Command.{Action}` (e.g., `GreetingCommand.Greet`)
- Event: `{Entity}Event.{PastTense}` (e.g., `GreetingEvent.Greeted`)
- State: `{Status}State` (e.g., `OpenState`, `CloseState`)

**Services**:
- Pattern: `{Entity}Service` (e.g., `GreetingService`)
- Use `@Service` annotation
- Encapsulates actor communication logic
- Returns CompletionStage for async operations
- Handles StatusReply unwrapping and error conversion

**Tests**:
- Unit tests: `{ClassName}Test.java`
- Integration tests: `{ClassName}Test.java` (with `@SpringBootTest`)

**Projection Handlers**:
- Pattern: `{Entity}{Purpose}Handler` (e.g., `GreetingsCountReadSideHandler`, `GreetingsKafkaHandler`)

### Code Patterns

**Event-Sourced Actors**:
```java
public class EntityActorBehavior extends EventSourcedBehavior<Command, Event, State> {
    // 1. Define entity type key for cluster sharding
    public static final EntityTypeKey<Command> ENTITY_TYPE_KEY =
        EntityTypeKey.create(Command.class, "entity-aggregate");

    // 2. Factory method for entity creation
    public static Behavior<Command> create(EntityContext<Command> ctx) {
        return new EntityActorBehavior(
            PersistenceId.of(ctx.getEntityTypeKey().name(), ctx.getEntityId())
        );
    }

    // 3. Command handling with state delegation
    @Override
    public CommandHandler<Command, Event, State> commandHandler() {
        return newCommandHandlerBuilder()
            .forAnyState()
            .onCommand(Command.class, (state, cmd) -> {
                var events = state.onCommand(cmd);  // Delegate to state
                return Effect().persist(events).thenRun(...);
            })
            .build();
    }

    // 4. Event handling with state delegation
    @Override
    public EventHandler<State, Event> eventHandler() {
        return newEventHandlerBuilder()
            .forAnyState()
            .onAnyEvent((state, event) -> state.onEvent(event));
    }
}
```

**Replicated Event-Sourced Actors** (for multi-region deployment):
```java
public class EntityActorBehavior extends ReplicatedEventSourcedBehavior<Command, Event, State> {
    // 1. Define entity type key for cluster sharding
    public static final EntityTypeKey<Command> ENTITY_TYPE_KEY =
        EntityTypeKey.create(Command.class, "entity-aggregate");

    // 2. Factory method for entity creation with ReplicationContext
    public static Behavior<Command> create(ReplicationContext replicationContext) {
        return new EntityActorBehavior(replicationContext);
    }

    // 3. Constructor accepts ReplicationContext instead of PersistenceId
    private EntityActorBehavior(ReplicationContext replicationContext) {
        super(replicationContext);
    }

    // 4. Command and event handlers remain the same
    @Override
    public CommandHandler<Command, Event, State> commandHandler() {
        return newCommandHandlerBuilder()
            .forAnyState()
            .onCommand(Command.class, (state, cmd) -> {
                var events = state.onCommand(cmd);
                return Effect().persist(events).thenRun(...);
            })
            .build();
    }

    @Override
    public EventHandler<State, Event> eventHandler() {
        return newEventHandlerBuilder()
            .forAnyState()
            .onAnyEvent((state, event) -> state.onEvent(event));
    }
}
```

**Service Layer** (encapsulates actor communication):
```java
@Service
public class EntityService {
    private final ClusterSharding sharding;

    public EntityService(ClusterSharding sharding) {
        this.sharding = sharding;
    }

    public CompletionStage<State> command(String entityId) {
        var entityRef = sharding.entityRefFor(
            EntityActorBehavior.ENTITY_TYPE_KEY,
            entityId
        );

        return entityRef.<StatusReply<State>>ask(
                replyTo -> new Command(entityId, replyTo),
                Duration.ofSeconds(5))
            .thenCompose(this::toCompletionStage);
    }

    private CompletionStage<State> toCompletionStage(StatusReply<State> statusReply) {
        if (statusReply.isSuccess()) {
            return CompletableFuture.completedFuture(statusReply.getValue());
        } else {
            return CompletableFuture.failedFuture(statusReply.getError());
        }
    }
}
```

**REST to Service Communication**:
```java
@RestController
@RequestMapping("/entities")
public class EntityController {
    private final EntityService service;

    public EntityController(EntityService service) {
        this.service = service;
    }

    @PostMapping("/{id}")
    public CompletionStage<Response> command(@PathVariable String id) {
        return service.command(id)
            .thenApply(state -> ResponseEntity.ok(state));
    }
}
```

**Projection Handlers**:
```java
@Component
public class EntityReadSideHandler extends JdbcHandler<Event, EventEnvelope<Event>> {
    @Override
    public void process(EventEnvelope<Event> envelope) {
        Event event = envelope.event();
        // Update read model based on event type
        // Use Spring Data JPA for database operations
    }
}
```

## Testing Guidelines

### Test Structure

**Pure Unit Tests** (state logic):
```java
class OpenStateTest {
    @Test
    void testGreetCommand() {
        var state = new OpenState(0, 5);
        var events = state.onCommand(new GreetingCommand.Greet(...));
        assertEquals(1, events.size());
        assertTrue(events.getFirst() instanceof GreetingEvent.Greeted);
    }
}
```

**Integration Tests** (full stack):
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:tc:postgresql:15:///testdb",
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
@EmbeddedKafka
class GreetingControllerTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testGreetingEndpoint() {
        String name = UUID.randomUUID().toString();  // Unique ID!

        ResponseEntity<GreetingResponse> response =
            restTemplate.postForEntity("/greetings/" + name, null, GreetingResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().count());
    }
}
```

### Critical Testing Patterns

1. **Always use unique entity IDs in tests**:
   ```java
   String entityId = UUID.randomUUID().toString();
   ```
   Reason: Actors are singletons per ID, state persists across tests

2. **Use Awaitility for async projections**:
   ```java
   await()
       .atMost(5, TimeUnit.SECONDS)
       .until(() -> repository.findById(id).isPresent());
   ```

3. **Use Testcontainers for PostgreSQL**:
   ```java
   @TestPropertySource(properties = {
       "spring.datasource.url=jdbc:tc:postgresql:15:///testdb"
   })
   ```

4. **Use EmbeddedKafka for Kafka tests**:
   ```java
   @EmbeddedKafka(topics = {"event.greeting", "command.greeting"})
   ```

### Functional Programming Guidelines

**Always prefer functional programming constructs in Java code:**
- Use `Optional`, `Stream`, and lambda expressions for data processing and control flow.
- Favor immutability and pure functions—avoid mutating state or reassigning variables.
- Use method references and functional interfaces where possible.
- Refactor imperative logic to declarative style (e.g., replacing null checks with `Optional`, loops with `Stream`).
- Prefer returning new objects over modifying existing ones.
- Example (refactor):
```java
// Imperative
GreetingsCountModel greeting = findById(name).orElse(null);
if (greeting == null) {
    greeting = new GreetingsCountModel(name, 1, Instant.now());
} else {
    greeting = new GreetingsCountModel(name, greeting.count + 1, Instant.now());
}
saveAndFlush(greeting);

// Functional
GreetingsCountModel greeting = findById(name)
    .map(existing -> new GreetingsCountModel(name, existing.count + 1, Instant.now()))
    .orElseGet(() -> new GreetingsCountModel(name, 1, Instant.now()));
saveAndFlush(greeting);
```
- Use functional style in event sourcing, service, and projection handler logic.
- Review code for opportunities to use functional programming during code review.

## Database Management

### Schema Migrations (Flyway)
```
src/main/resources/db/migration/
├── V1__create_pekko_persistence_tables.sql  # Event store
├── V2__create_projection_tables.sql         # Projection offsets
└── V3__greeting_read_side.sql               # Read models
```

**Migration Rules**:
- Never modify existing migrations
- Always increment version number (V4, V5, etc.)
- Test migrations with `./mvnw spring-boot:run` (auto-applies)
- Rollback requires manual SQL scripts

### Database Schema

**Pekko Persistence** (automatically managed):
- `event_journal`: Immutable event log
- `snapshot`: State snapshots (every 100 events)
- `event_tag`: Tag index for projections

**Application Tables** (Flyway managed):
- `greetings_count`: Read model for greeting counts
- `projection_offset_store`: Projection progress tracking

**Access**:
```bash
# Local Development
psql -h localhost -p 5432 -U spring-pekko -d spring-pekko
```

## Development Workflow

### Adding a New Actor

1. **Define Commands/Events/State**:
   ```java
   // Commands (sealed interface)
   public sealed interface EntityCommand permits EntityCommand.Action1, ... {}

   // Events (sealed interface)
   public sealed interface EntityEvent permits EntityEvent.Happened1, ... {}

   // States (implement transition logic)
   public sealed interface EntityState permits State1, State2 {
       List<EntityEvent> onCommand(EntityCommand cmd);
       EntityState onEvent(EntityEvent event);
   }
   ```

2. **Create Actor Behavior**:
   ```java
   public class EntityActorBehavior extends EventSourcedBehavior<...> {
       public static final EntityTypeKey<EntityCommand> ENTITY_TYPE_KEY =
           EntityTypeKey.create(EntityCommand.class, "entity-aggregate");
       // ... implement commandHandler, eventHandler, etc.
   }
   ```

3. **Register with Cluster Sharding**:
   ```java
   // In ShardsCreator.java
   @EventListener(ApplicationReadyEvent.class)
   public void onApplicationReady() {
       sharding.init(Entity.of(
           EntityActorBehavior.ENTITY_TYPE_KEY,
           EntityActorBehavior::create
       ));
   }
   ```

4. **Configure Serialization**:
   ```java
   // In application.conf
   serialization-bindings {
       "com.gdfesta.example.EntityCommand" = jackson-json
       "com.gdfesta.example.EntityEvent" = jackson-json
       "com.gdfesta.example.EntityState" = jackson-json
   }
   ```

5. **Create Service Layer**:
   ```java
   @Service
   public class EntityService {
       private final ClusterSharding sharding;

       public EntityService(ClusterSharding sharding) {
           this.sharding = sharding;
       }

       public CompletionStage<EntityState> performAction(String entityId) {
           var entityRef = sharding.entityRefFor(
               EntityActorBehavior.ENTITY_TYPE_KEY,
               entityId
           );
           return entityRef.<StatusReply<EntityState>>ask(
                   replyTo -> new EntityCommand.Action(replyTo),
                   Duration.ofSeconds(5))
               .thenCompose(this::toCompletionStage);
       }
   }
   ```

6. **Add Projection Handler** (if read model needed):
   ```java
   @Component
   public class EntityReadSideHandler extends JdbcHandler<...> {
       // Process events and update read model
   }
   ```

7. **Create REST Endpoints**:
   ```java
   @RestController
   @RequestMapping("/entities")
   public class EntityController {
       private final EntityService service;

       public EntityController(EntityService service) {
           this.service = service;
       }

       @PostMapping("/{id}")
       public CompletionStage<ResponseEntity<?>> action(@PathVariable String id) {
           return service.performAction(id)
               .thenApply(state -> ResponseEntity.ok(state));
       }
   }
   ```

8. **Write Tests**:
   - Unit tests for state transitions
   - Integration tests for API endpoints
   - Test with unique entity IDs

### Making Configuration Changes

**Spring Boot Config** (`application.yml`):
- Database connections, server ports, logging
- Kafka topics and consumer groups
- Spring-specific configuration

**Pekko Config** (`application.conf`):
- Cluster settings (discovery, bootstrap)
- Persistence configuration
- Serialization bindings
- Management HTTP settings
- Replicated Event Sourcing settings (for multi-region)

### Multi-Region Deployment with Replicated Event Sourcing

The project uses **Replicated Event Sourcing** for multi-region deployment, enabling active-active writes across regions with eventual consistency.

**Key Concepts**:
- Each region runs an independent cluster with its own database
- Events are replicated between regions automatically
- Same entity can be active in multiple regions simultaneously
- Writes are local (low latency), replication is asynchronous
- Automatic conflict resolution

**Configuration**:

1. **Set replica ID per region** (via environment variable):
   ```bash
   # Region 1
   export PEKKO_REPLICA_ID=region-1

   # Region 2
   export PEKKO_REPLICA_ID=region-2

   # Region 3
   export PEKKO_REPLICA_ID=region-3
   ```

2. **Configure replication endpoints**:
   ```bash
   # For Region 1 to connect to Region 2 and 3
   export PEKKO_REGION_2_HOST=region-2.example.com
   export PEKKO_REGION_2_PORT=8558
   export PEKKO_REGION_3_HOST=region-3.example.com
   export PEKKO_REGION_3_PORT=8558
   ```

3. **In Kubernetes**, use service names:
   ```yaml
   env:
     - name: PEKKO_REPLICA_ID
       value: "region-1"
     - name: PEKKO_REGION_1_HOST
       value: "spring-pekko-replication.region-1.svc.cluster.local"
     - name: PEKKO_REGION_2_HOST
       value: "spring-pekko-replication.region-2.svc.cluster.local"
     - name: PEKKO_REGION_3_HOST
       value: "spring-pekko-replication.region-3.svc.cluster.local"
   ```

**Initialization** (in `ShardsCreator.java`):
```java
@Component
public class ShardsCreator {
    private final ClusterSharding sharding;
    private final String replicaId;

    public ShardsCreator(
        ClusterSharding sharding,
        @Value("${pekko.replicated-event-sourcing.replica-id}") String replicaId
    ) {
        this.sharding = sharding;
        this.replicaId = replicaId;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        // Define all replicas
        Set<ReplicaId> allReplicaIds = Set.of(
            new ReplicaId("region-1"),
            new ReplicaId("region-2"),
            new ReplicaId("region-3")
        );

        // Initialize replicated entity
        ReplicatedEntity<Command, ?> replicatedEntity = ReplicatedEntity.create(
            EntityActorBehavior.ENTITY_TYPE_KEY,
            allReplicaIds,
            EntityActorBehavior::create
        ).withDirectReplication(true);

        sharding.init(ReplicatedEntityProvider.create(
            replicatedEntity,
            new ReplicaId(replicaId)
        ));
    }
}
```

**Network Requirements**:
- Each region needs bidirectional connectivity on the replication port (default: 8558)
- Use VPN/VPC peering for secure inter-region communication
- Firewall rules must allow gRPC traffic between regions

**Monitoring**:
- Check replication lag via Pekko Management HTTP endpoints
- Monitor event journal size in each region's database
- Track replication errors in application logs

## Docker Compose Dev Services

### How It Works

Spring Boot automatically detects the `compose.yaml` file and starts PostgreSQL and Kafka when running in development mode via the `dev` Maven profile. This provides a Quarkus Dev Services-like experience.

### Safety for Production/Kubernetes

The Docker Compose dependency is **only included in the dev profile**:

1. **Maven Profile Isolation**: The `spring-boot-docker-compose` dependency is in the `dev` profile (active by default for development)
2. **Production Builds**: Use `./mvnw clean package -P \!dev` to exclude dev dependencies
3. **Automatic Detection**: Even if included, Docker Compose won't activate in containerized environments (no Docker socket access)

### Configuration Files

- `compose.yaml`: Defines PostgreSQL and Kafka services for local development
- `pom.xml`: Contains the `dev` profile with Docker Compose dependency
- `.dockerignore`: Should exclude `compose.yaml` from production images (if using Docker builds)

### Manual Control

To disable Docker Compose support even in dev mode:

```bash
# Via environment variable
export SPRING_DOCKER_COMPOSE_ENABLED=false
./mvnw spring-boot:run

# Or via application property
./mvnw spring-boot:run -Dspring.docker.compose.enabled=false
```

## Troubleshooting

### Common Issues

**Actor not receiving commands**:
- Verify cluster sharding initialization in `ShardsCreator`
- Check entity type key matches in actor and REST controller
- Ensure unique entity IDs in tests

**Projection not updating**:
- Projections are async (use Awaitility in tests)
- Check `projection_offset_store` table for progress
- Verify event tagging in actor behavior

**Database connection errors**:
- Ensure PostgreSQL is running on localhost:5432
- Verify datasource config in `application.yml`
- Check database credentials

**Kafka connection errors**:
- Ensure Kafka is running on localhost:9092
- Check Kafka configuration in `application.yml`
- Verify topic creation

**Cluster not forming in local development**:
- Check ActorSystem initialization logs
- Verify Pekko configuration in `application.conf`
- Check that datasource properties are correctly passed to Pekko

### Useful Commands

```bash
# View application logs
./mvnw spring-boot:run

# Check actuator health
curl http://localhost:8080/actuator/health

# Check cluster status
curl http://localhost:7626/cluster/members

# Database access
psql -h localhost -p 5432 -U spring-pekko -d spring-pekko

# View projection progress
SELECT * FROM projection_offset_store;

# View event journal
SELECT * FROM event_journal WHERE persistence_id LIKE 'greeting-aggregate|%';
```

## Key Differences from Quarkus Version

1. **Dependency Injection**: Uses Spring DI instead of CDI
   - `@Component`, `@Service`, `@Configuration` instead of `@ApplicationScoped`
   - Constructor injection is preferred in Spring

2. **Reactive Types**: Uses CompletionStage/CompletableFuture instead of Mutiny Uni
   - `CompletionStage<T>` instead of `Uni<T>`
   - `thenApply`, `thenCompose` instead of `map`, `flatMap`

3. **REST Framework**: Uses Spring Web MVC instead of JAX-RS
   - `@RestController`, `@RequestMapping` instead of `@Path`
   - `@GetMapping`, `@PostMapping` instead of `@GET`, `@POST`

4. **Data Access**: Uses Spring Data JPA instead of Panache
   - `JpaRepository` interface with default methods
   - `@Repository` annotation

5. **Kafka**: Uses Spring Kafka instead of MicroProfile Reactive Messaging
   - `KafkaTemplate` for producing
   - `@KafkaListener` for consuming
   - Separate configuration class for Kafka setup

6. **Lifecycle**: Uses Spring lifecycle hooks instead of Quarkus events
   - `@EventListener(ApplicationReadyEvent.class)` instead of `@Observes StartupEvent`
   - `@PreDestroy` for shutdown hooks

7. **Testing**: Uses Spring Boot Test instead of Quarkus Test
   - `@SpringBootTest` instead of `@QuarkusTest`
   - Testcontainers for database
   - `@EmbeddedKafka` for Kafka testing

## Commit Message Guidelines

Follow conventional commits format:

```
<type>: <subject>

<optional body>

<optional footer>
```

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `refactor`: Code restructuring
- `test`: Test additions/changes
- `docs`: Documentation
- `chore`: Maintenance tasks

**Examples**:
```
feat: add projection handler for greeting events

Implements GreetingsKafkaHandler to publish domain events
to Kafka topic for external consumers.

test: add integration test for async projection updates

Uses Awaitility to verify projection handler processes
events and updates read model within 5 seconds.

fix: correct Kafka configuration for Spring Boot

Updates KafkaConfiguration to use proper Spring Kafka
serializers and deserializers.
```

## Additional Resources

- **Spring Boot Docs**: https://spring.io/projects/spring-boot
- **Apache Pekko Docs**: https://pekko.apache.org/docs/
- **Event Sourcing**: https://martinfowler.com/eaaDev/EventSourcing.html
- **CQRS**: https://martinfowler.com/bliki/CQRS.html
- **Original Quarkus Project**: See `../quarkus-pekko/AGENTS.MD`
