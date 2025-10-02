# AI Agent Guide for Quarkus-Pekko

This guide helps AI agents understand and work effectively with the Quarkus-Pekko project.

## Project Overview

**Type**: Reactive distributed system (demonstration project)
**Stack**: Quarkus 3.27.0 + Apache Pekko 1.2.0 + PostgreSQL + Kafka
**Patterns**: Event Sourcing, CQRS, Actor Model, Cluster Sharding
**Language**: Java 21
**Build Tool**: Maven

**Purpose**: Demonstrates integration of Quarkus with Apache Pekko for building scalable, event-sourced, distributed applications using the actor model.

## Quick Start

### Prerequisites Check
```bash
# Verify Java version (must be 21+)
java -version

# If using SDKMAN and wrong version
source ~/.sdkman/bin/sdkman-init.sh && sdk env install
```

### Development Mode
```bash
# Start with live reload (automatically starts PostgreSQL and Kafka via Dev Services)
./mvnw quarkus:dev

# Access points:
# - API: http://localhost:8080
# - Dev UI: http://localhost:8080/q/dev/
# - Pekko Management: http://localhost:7626/ready
```

### Testing
```bash
# Unit + integration tests (fast, development workflow)
./mvnw test

# All tests including packaged mode (slower, CI/CD)
./mvnw verify

# Specific test class
./mvnw test -Dtest=GreetingResourceTest
```

### Kubernetes Deployment
```bash
# Full deployment to local Kubernetes
k8s/scripts/deploy.sh

# Access services locally
k8s/scripts/port-forward.sh all

# Quick redeploy (undeploy + deploy + port-forward)
k8s/scripts/redeploy.sh

# Clean up
k8s/scripts/undeploy.sh
```

## Build & Test Commands

### Building
```bash
# Compile only
./mvnw compile

# Package application
./mvnw package

# Build container image (for Kubernetes deployment)
./mvnw package -Dquarkus.container-image.build=true -Dquarkus.profile=k8s

# Build uber-jar
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

### Testing Strategy

**Three test types serve different purposes:**

1. **Pure Unit Tests** (no framework, very fast)
   - Files: `*Test.java` without annotations
   - Examples: `OpenStateTest`, `CloseStateTest`
   - Test isolated classes (state transitions)
   - Run with: `./mvnw test`

2. **Integration Tests** (with `@QuarkusTest`, fast)
   - Files: `*Test.java` with `@QuarkusTest`
   - Examples: `GreetingResourceTest`, `GreetingsCountRepositoryTest`, `GreetingsKafkaHandlerTest`
   - Test full stack (REST → Actors → Database → Kafka)
   - Support CDI injection
   - Run in same JVM as tests
   - Run with: `./mvnw test`

3. **Packaged Integration Tests** (with `@QuarkusIntegrationTest`, slower)
   - Files: `*IT.java` with `@QuarkusIntegrationTest`
   - Example: `GreetingResourceIT`
   - Test against packaged JAR in separate process
   - Catch packaging issues
   - Run with: `./mvnw verify`

**Test Execution**:
```bash
# Fast feedback loop (development)
./mvnw test

# Complete validation (pre-commit, CI/CD)
./mvnw verify

# Parallel execution (faster)
./mvnw test -T 1C
```

### Running the Application
```bash
# Development mode (recommended)
./mvnw quarkus:dev

# As uber-jar
./mvnw package -Dquarkus.package.jar.type=uber-jar
java -jar target/*-runner.jar

# In Kubernetes
k8s/scripts/deploy.sh
```

## Code Style & Conventions

### Package Organization
```
com.gdfesta.quarkus.pekko/    - Quarkus-Pekko integration layer
com.gdfesta.example/
  ├── api/                    - REST endpoints and DTOs
  ├── write_side/             - Command handling (CQRS write side)
  │   ├── greeting.aggregate/ - Event-sourced actors
  │   └── greeting.services/  - Domain services
  ├── read_side/              - Query handling (CQRS read side)
  │   └── greetings_count/    - Read models and projections
  └── kafka/                  - Messaging (consumers/producers)
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
- Encapsulates actor communication logic
- Handles StatusReply unwrapping and error conversion

**Tests**:
- Unit tests: `{ClassName}Test.java`
- Integration tests: `{ClassName}Test.java` (with `@QuarkusTest`)
- Packaged tests: `{ClassName}IT.java` (with `@QuarkusIntegrationTest`)

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

**Service Layer** (encapsulates actor communication):
```java
@ApplicationScoped
public class EntityService {
    @Inject ClusterSharding sharding;

    public Uni<State> command(String entityId) {
        var entityRef = sharding.entityRefFor(
            EntityActorBehavior.ENTITY_TYPE_KEY,
            entityId
        );

        return Uni.createFrom().completionStage(
                entityRef.<StatusReply<State>>ask(
                    replyTo -> new Command(entityId, replyTo),
                    Duration.ofSeconds(5)))
            .flatMap(this::toUni);
    }

    private Uni<State> toUni(StatusReply<State> statusReply) {
        if (statusReply.isSuccess()) {
            return Uni.createFrom().item(statusReply.getValue());
        } else {
            return Uni.createFrom().failure(statusReply.getError());
        }
    }
}
```

**REST to Service Communication**:
```java
@Path("/entities")
public class EntityResource {
    @Inject EntityService service;

    @POST
    @Path("/{id}")
    public Uni<Response> command(@PathParam("id") String id) {
        return service.command(id)
            .map(state -> Response.ok(state).build());
    }
}
```

**Projection Handlers**:
```java
@ApplicationScoped
public class EntityReadSideHandler extends JdbcHandler<Event, EventEnvelope<Event>> {
    @Override
    public void process(EventEnvelope<Event> envelope) {
        Event event = envelope.event();
        // Update read model based on event type
        // Use Hibernate/JPA for database operations
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
@QuarkusTest
class GreetingResourceTest {
    @Test
    void testGreetingEndpoint() {
        String name = UUID.randomUUID().toString();  // Unique ID!

        given()
            .when().post("/greetings/" + name)
            .then()
            .statusCode(200)
            .body("count", equalTo(1));
    }
}
```

**Packaged Tests** (production-like):
```java
@QuarkusIntegrationTest
class GreetingResourceIT extends GreetingResourceTest {
    // Inherits all tests, runs against packaged JAR
}
```

### Critical Testing Patterns

1. **Always use unique entity IDs in tests**:
   ```java
   String entityId = UUID.randomUUID().toString();
   ```
   Reason: Actors are singletons per ID, state persists across tests

2. **Clear JPA cache when testing persistence**:
   ```java
   @Inject EntityManager em;

   @Test
   void testPersistence() {
       // Save entity
       em.clear();  // Force fresh database read
       // Verify entity
   }
   ```
   Reason: JPA first-level cache persists across test method

3. **Use Awaitility for async projections**:
   ```java
   await()
       .atMost(5, TimeUnit.SECONDS)
       .until(() -> repository.findById(id).isPresent());
   ```

4. **In-memory messaging for Kafka tests**:
   ```java
   @Inject @Channel("event-greeting") Emitter<Event> emitter;
   @Inject @Channel("event-greeting") Multi<Event> consumer;
   ```

## Security Considerations

### Current State (Demonstration Project)
- No authentication/authorization
- Plain-text database credentials
- No TLS/encryption
- No secrets management

### Production Recommendations
**If adapting this project for production:**

1. **Authentication**: Implement JWT/OAuth2 via Quarkus OIDC extension
2. **Secrets**: Use Kubernetes Secrets + environment variables
3. **Encryption**: Enable TLS for all network communication
4. **Database**: Use connection pooling, encrypted connections
5. **Kafka**: Enable SASL/SSL authentication
6. **Network**: Implement Kubernetes NetworkPolicies

**Security Gotchas**:
- Pekko cluster requires pod listing permission (RBAC configured)
- Event store contains full history (implement encryption for sensitive data)
- Kafka topics should use ACLs in production

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
- Test migrations with `./mvnw quarkus:dev` (auto-applies)
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
# Development (Dev Services)
psql -h localhost -p 5432 -U quarkus -d quarkus

# Kubernetes (port-forward)
k8s/scripts/port-forward.sh postgres
psql -h localhost -p 5432 -U quarkus -d quarkus
```

## Kubernetes Architecture

### Why Multiple Resource Types?

**Deployment** (`app-deployment.yaml`):
- Manages pod lifecycle (creates, restarts, scales replicas)
- Rolling updates without downtime
- Health monitoring (readiness, liveness, startup probes)

**Service** (`app-service.yaml`):
- Stable network endpoint (IP + DNS)
- Load balancing across pod replicas
- Service discovery for other components

**ConfigMap** (`app-configmap.yaml`):
- Externalized configuration
- No rebuild needed for config changes
- Environment-specific settings

**RBAC** (`rbac.yaml`):
- ServiceAccount: Pod identity
- Role: Permissions (list pods in namespace)
- RoleBinding: Links account to role
- **Why**: Pekko uses K8s API to discover cluster members

### Cluster Formation Flow
```
1. Pod starts → 2. Pekko Management HTTP starts →
3. Cluster Bootstrap queries K8s API → 4. Discovers pods with matching labels →
5. Forms cluster with discovered nodes → 6. Sharding distributes actors
```

### Scaling
```bash
# Scale to 5 replicas
kubectl scale deployment/quarkus-pekko --replicas=5 -n quarkus-pekko

# Verify cluster members
curl http://localhost:7626/cluster/members  # (after port-forward)
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
   // In ShardingsCreator.java
   sharding.init(Entity.of(
       EntityActorBehavior.ENTITY_TYPE_KEY,
       EntityActorBehavior::create
   ));
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
   @ApplicationScoped
   public class EntityService {
       @Inject ClusterSharding sharding;

       public Uni<EntityState> performAction(String entityId) {
           var entityRef = sharding.entityRefFor(
               EntityActorBehavior.ENTITY_TYPE_KEY,
               entityId
           );
           return Uni.createFrom().completionStage(
                   entityRef.<StatusReply<EntityState>>ask(
                       replyTo -> new EntityCommand.Action(replyTo),
                       Duration.ofSeconds(5)))
               .flatMap(this::toUni);
       }

       private Uni<EntityState> toUni(StatusReply<EntityState> statusReply) {
           if (statusReply.isSuccess()) {
               return Uni.createFrom().item(statusReply.getValue());
           } else {
               return Uni.createFrom().failure(statusReply.getError());
           }
       }
   }
   ```

6. **Add Projection Handler** (if read model needed):
   ```java
   @ApplicationScoped
   public class EntityReadSideHandler extends JdbcHandler<...> {
       // Process events and update read model
   }
   ```

7. **Create REST Endpoints**:
   ```java
   @Path("/entities")
   public class EntityResource {
       @Inject EntityService service;

       @POST
       @Path("/{id}")
       public Uni<Response> action(@PathParam("id") String id) {
           return service.performAction(id)
               .map(state -> Response.ok(state).build());
       }
   }
   ```

8. **Write Tests**:
   - Unit tests for state transitions
   - Integration tests for API endpoints
   - Test with unique entity IDs

### Making Configuration Changes

**Quarkus Config** (`application.yaml`):
- Database connections, server ports, logging
- Kafka topics and consumer groups
- Environment-specific profiles (`application-k8s.yaml`)

**Pekko Config** (`application.conf`):
- Cluster settings (discovery, bootstrap)
- Persistence configuration
- Serialization bindings
- Management HTTP settings

**Kubernetes Config** (`k8s/app-configmap.yaml`):
- Override for K8s environment
- Mounted as environment variables
- Redeploy after changes: `k8s/scripts/redeploy.sh`

## Deployment & CI/CD

### Local Development
```bash
# One command for rapid development
./mvnw quarkus:dev
```

### Kubernetes Deployment
```bash
# Full deployment pipeline
./mvnw package -Dquarkus.container-image.build=true -Dquarkus.profile=k8s
k8s/scripts/deploy.sh
k8s/scripts/port-forward.sh all

# Or use redeploy script (rebuilds + redeploys)
k8s/scripts/redeploy.sh
```

### CI/CD Pipeline Recommendations
```bash
# 1. Build
./mvnw clean package -Dquarkus.container-image.build=true

# 2. Test
./mvnw verify  # Includes packaged integration tests

# 3. Deploy (Kubernetes)
kubectl apply -f k8s/

# 4. Health check
curl http://<service>/q/health/ready
curl http://<service>:7626/ready
```

## Troubleshooting

### Common Issues

**Actor not receiving commands**:
- Verify cluster sharding initialization in `ShardingsCreator`
- Check entity type key matches in actor and REST resource
- Ensure unique entity IDs in tests

**Projection not updating**:
- Projections are async (use Awaitility in tests)
- Check `projection_offset_store` table for progress
- Verify event tagging in actor behavior

**Database connection errors**:
- Dev mode: Quarkus Dev Services should auto-start PostgreSQL
- K8s: Check PostgreSQL pod status: `kubectl get pods -n quarkus-pekko`
- Verify datasource config in `application.yaml` or ConfigMap

**Cluster not forming in Kubernetes**:
- Check RBAC permissions: `kubectl get roles,rolebindings -n quarkus-pekko`
- Verify pod labels match discovery configuration
- Check Pekko Management logs: `kubectl logs -f deployment/quarkus-pekko -n quarkus-pekko`

**Tests failing with state conflicts**:
- Use unique entity IDs: `UUID.randomUUID().toString()`
- Actors are singletons per ID, state persists across tests

### Useful Commands

```bash
# View application logs
kubectl logs -f deployment/quarkus-pekko -n quarkus-pekko

# Check cluster members
curl http://localhost:7626/cluster/members

# View Kafka topics (via AKHQ)
k8s/scripts/port-forward.sh akhq
# Open http://localhost:8081

# Database access
k8s/scripts/port-forward.sh postgres
psql -h localhost -p 5432 -U quarkus -d quarkus

# View projection progress
SELECT * FROM projection_offset_store;

# View event journal
SELECT * FROM event_journal WHERE persistence_id LIKE 'greeting-aggregate|%';
```

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

fix: clear JPA cache in repository tests

Ensures tests verify actual database persistence rather
than relying on first-level cache.
```

## Pull Request Guidelines

**When creating PRs**:

1. **Title**: Concise description of changes
2. **Summary**: Bullet points of key changes
3. **Test Plan**: How to verify changes work
4. **Breaking Changes**: Highlight any API changes

**Example PR description**:
```markdown
## Summary
- Add Kafka consumer for async command processing
- Implement GreetingCommandConsumer with MicroProfile Messaging
- Add integration tests with in-memory channels

## Test Plan
- [ ] Unit tests pass: ./mvnw test
- [ ] Integration tests pass: ./mvnw verify
- [ ] Manual test: Publish message to command.greeting topic
- [ ] Verify actor receives and processes command

## Notes
- Requires Kafka to be running (auto-started in dev mode)
- Topic auto-created: command.greeting
```

## Additional Resources

- **Quarkus Guides**: https://quarkus.io/guides/
- **Apache Pekko Docs**: https://pekko.apache.org/docs/
- **Event Sourcing**: https://martinfowler.com/eaaDev/EventSourcing.html
- **CQRS**: https://martinfowler.com/bliki/CQRS.html
- **Project Architecture**: See `ARCHITECTURE.md`
- **User Documentation**: See `README.md`
