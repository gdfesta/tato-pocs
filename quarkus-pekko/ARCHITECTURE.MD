# Architecture Documentation

**Project:** Quarkus-Pekko: Reactive Distributed Systems
**Repository:** Local Development Project
**Last Updated:** 2025-10-02

## Table of Contents

1. [Project Structure](#project-structure)
2. [High-Level System Architecture](#high-level-system-architecture)
3. [Core Components](#core-components)
4. [Data Stores](#data-stores)
5. [External Integrations](#external-integrations)
6. [Deployment & Infrastructure](#deployment--infrastructure)
7. [Security Considerations](#security-considerations)
8. [Development & Testing Environment](#development--testing-environment)
9. [Glossary](#glossary)

---

## Project Structure

```
quarkus-pekko/
├── src/main/java/
│   ├── com.gdfesta.example/          # Business logic and domain
│   │   ├── write_side/                # Command handling and event sourcing
│   │   ├── read_side/                 # Query models and projections
│   │   ├── api/                       # REST API endpoints
│   │   └── kafka/                     # Event streaming integration
│   └── com.gdfesta.quarkus.pekko/    # Infrastructure and integration
├── src/main/resources/
│   ├── db/migration/                  # Database schema migrations
│   ├── application.conf               # Pekko configuration
│   └── application.yaml               # Quarkus configuration
├── k8s/                               # Kubernetes manifests
│   ├── scripts/                       # Deployment automation
│   └── *.yaml                         # Resource definitions
└── src/test/                          # Test suites
```

### Directory Explanations

- **write_side**: Contains the command-handling logic, event-sourced aggregates, and business rules. Implements event sourcing patterns where state changes are persisted as immutable events.

- **read_side**: Houses projection handlers that consume events and build optimized read models for queries. Implements the query side of CQRS.

- **api**: REST endpoints that serve as the entry point for synchronous client requests. Translates HTTP requests to internal commands.

- **kafka**: Event streaming components for asynchronous communication, both producing domain events to external systems and consuming commands from message brokers.

- **quarkus.pekko**: Infrastructure layer managing the integration between Quarkus framework and Pekko actor system, including lifecycle management and database session handling.

---

## High-Level System Architecture

This system implements an **Event-Sourced CQRS** architecture using the **Actor Model** for concurrency and distribution.

### System Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        ENTRY POINTS                              │
├────────────────────────────┬────────────────────────────────────┤
│                            │                                     │
│  ┌──────────────────┐      │      ┌──────────────────────┐     │
│  │   REST API       │      │      │  Kafka Consumer      │     │
│  │   (Sync)         │      │      │  (Async Commands)    │     │
│  └────────┬─────────┘      │      └──────────┬───────────┘     │
│           │                │                 │                  │
└───────────┼────────────────┴─────────────────┼──────────────────┘
            │                                   │
            └───────────────┬───────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────────────┐
        │         COMMAND HANDLER                    │
        │  (Distributed Actor System via Sharding)   │
        └───────────────────┬───────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────────────┐
        │        EVENT-SOURCED AGGREGATE             │
        │  - Validates commands                      │
        │  - Generates events                        │
        │  - Rebuilds state from event history       │
        └───────────────────┬───────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────────────┐
        │          EVENT STORE                       │
        │  (Append-only immutable event log)         │
        └───────────────────┬───────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────────────┐
        │         EVENT STREAM                       │
        │  (Ordered stream of all domain events)     │
        └──────────┬───────────────────┬────────────┘
                   │                   │
          ┌────────▼────────┐   ┌─────▼──────────────┐
          │  READ SIDE      │   │  EVENT PUBLISHER   │
          │  PROJECTION     │   │  (Kafka)           │
          └────────┬────────┘   └──────┬─────────────┘
                   │                   │
                   ▼                   ▼
       ┌────────────────────┐  ┌─────────────────────┐
       │   READ MODEL       │  │  External Systems   │
       │   (Optimized       │  │  & Consumers        │
       │    for queries)    │  │                     │
       └────────────────────┘  └─────────────────────┘
```

### Data Flow

1. **Command Entry**: Commands arrive via REST API (synchronous) or Kafka (asynchronous)
2. **Command Routing**: Cluster sharding routes commands to the appropriate aggregate instance
3. **Event Generation**: Aggregates validate commands and produce domain events
4. **Event Persistence**: Events are appended to the immutable event store
5. **Event Streaming**: Persisted events flow to projection handlers and external publishers
6. **Read Model Updates**: Projections consume events to build denormalized views
7. **External Broadcasting**: Domain events are published to Kafka for external consumption

---

## Core Components

### 1. Entry Points Layer

#### REST API
- **Purpose**: Synchronous command handling for external clients
- **Technologies**: Jakarta REST (JAX-RS), Quarkus REST
- **Responsibilities**:
  - Accept HTTP requests
  - Translate to internal commands
  - Route commands to appropriate aggregates via cluster sharding
  - Return acknowledgments or query results
- **Deployment**: Exposed via Kubernetes Service on port 8080

#### Kafka Command Consumer
- **Purpose**: Asynchronous command handling from message queues
- **Technologies**: Quarkus Reactive Messaging, Apache Kafka
- **Responsibilities**:
  - Consume command messages from Kafka topics
  - Deserialize and validate command payloads
  - Route to aggregates via cluster sharding
- **Deployment**: Background consumer within application pods

### 2. Write Side (Command Processing)

#### Event-Sourced Aggregates
- **Purpose**: Business logic enforcement and state management
- **Technologies**: Apache Pekko Persistence, Actor Model
- **Pattern**: Event Sourcing
- **Responsibilities**:
  - Receive commands
  - Validate business rules
  - Generate immutable domain events
  - Maintain in-memory state derived from event history
  - Persist events to event store
- **Distribution**: Cluster-sharded across multiple nodes for scalability

#### Actor System Infrastructure
- **Purpose**: Manage distributed actor lifecycle and messaging
- **Technologies**: Apache Pekko Cluster, Cluster Sharding
- **Responsibilities**:
  - Distribute actors across cluster nodes
  - Route messages to correct actor instances
  - Handle node failures and actor migration
  - Provide location transparency
- **Deployment**: Forms a cluster in Kubernetes using pod discovery

### 3. Read Side (Query Processing)

#### Projection Handlers
- **Purpose**: Build denormalized read models from event streams
- **Technologies**: Apache Pekko Projections, JDBC
- **Pattern**: CQRS Read Side
- **Responsibilities**:
  - Subscribe to event streams
  - Transform events into read-optimized data structures
  - Update read models transactionally
  - Track processing offsets for at-least-once delivery
- **Characteristics**: Eventually consistent, idempotent

#### Read Model Repositories
- **Purpose**: Query optimized data models
- **Technologies**: Hibernate Panache, JPA
- **Responsibilities**:
  - Provide query APIs for read operations
  - Expose denormalized views tailored for specific use cases
  - No write operations (read-only)

### 4. Event Streaming Layer

#### Kafka Event Publisher
- **Purpose**: Broadcast domain events to external systems
- **Technologies**: Quarkus Reactive Messaging, Apache Kafka
- **Responsibilities**:
  - Consume internal event streams
  - Serialize events to Kafka-compatible format
  - Publish to external Kafka topics
  - Enable external system integration and event-driven architectures

### 5. Infrastructure Layer

#### Actor System Producer
- **Purpose**: Bootstrap and integrate Pekko with Quarkus lifecycle
- **Technologies**: Quarkus CDI, Apache Pekko
- **Responsibilities**:
  - Initialize actor system on application startup
  - Configure cluster settings based on environment (local vs Kubernetes)
  - Register entity types for cluster sharding
  - Provide CDI beans for dependency injection
  - Graceful shutdown coordination

#### Database Session Management
- **Purpose**: Integrate Hibernate sessions with Pekko projections
- **Technologies**: Hibernate, JDBC
- **Responsibilities**:
  - Provide transactional database access for projections
  - Manage session lifecycle
  - Handle connection pooling

---

## Data Stores

### Event Store (PostgreSQL)

- **Type**: Relational Database (Event Log)
- **Purpose**: Persist immutable domain events as source of truth
- **Schema**:
  - Event journal tables (append-only)
  - Snapshot tables (performance optimization)
  - Offset tracking tables (projection coordination)
- **Characteristics**:
  - Append-only writes
  - Sequential reads for event replay
  - Optimized for write performance
- **Deployment**: StatefulSet with persistent volume in Kubernetes

### Read Model Store (PostgreSQL)

- **Type**: Relational Database (Denormalized Views)
- **Purpose**: Store query-optimized read models
- **Schema**:
  - Aggregate-specific tables (e.g., greetings count)
  - Indexes optimized for common queries
- **Characteristics**:
  - Eventually consistent with event store
  - Optimized for read performance
  - Can be rebuilt from event store if corrupted
- **Deployment**: Same PostgreSQL instance, separate schema concern

---

## External Integrations

### Apache Kafka

- **Type**: Event Streaming Platform
- **Purpose**:
  - Consume external commands asynchronously
  - Publish domain events to external consumers
- **Integration Method**: Quarkus Reactive Messaging (MicroProfile Messaging)
- **Topics**:
  - **Inbound**: `command.greeting` - receives commands from external systems
  - **Outbound**: `event.greeting` - publishes domain events
- **Deployment**:
  - StatefulSet with ZooKeeper coordination
  - Exposed via internal Kubernetes service

---

## Deployment & Infrastructure

### Cloud Provider / Platform

- **Primary**: Kubernetes (Docker Desktop for local development)
- **Production Target**: Any Kubernetes-compatible platform

### Key Infrastructure Services

#### Application Pods
- **Deployment Type**: Kubernetes Deployment (3 replicas)
- **Scaling**: Horizontal pod autoscaling supported
- **Resource Management**: CPU/memory requests and limits configured
- **Health Monitoring**:
  - Liveness probe: HTTP endpoint check
  - Readiness probe: Pekko cluster health check
  - Startup probe: Initial bootstrap verification

#### Service Discovery
- **Method**: Kubernetes DNS and Service resources
- **Cluster Formation**: Pekko Kubernetes API discovery
- **RBAC**: ServiceAccount with pod read permissions for cluster discovery

#### Persistent Storage
- **PostgreSQL**: Persistent Volume Claim (PVC) for data durability
- **Kafka**: Persistent storage for message logs

### CI/CD Pipeline

- **Build Tool**: Maven
- **Containerization**: Quarkus Jib extension (JVM mode)
- **Deployment Automation**: Shell scripts in `k8s/scripts/`
  - `deploy.sh`: Full deployment orchestration
  - `undeploy.sh`: Clean removal
  - `redeploy.sh`: Combined undeploy and deploy
  - `port-forward.sh`: Local access to cluster services

### Monitoring & Observability

- **Health Endpoints**: Quarkus Health checks (`/q/health`)
- **Pekko Management**: HTTP management endpoints (`/ready`, `/alive`, `/cluster/members`)
- **Kafka Monitoring**: AKHQ UI for topic inspection and message browsing
- **Logging**: Standard output captured by Kubernetes

---

## Security Considerations

### Authentication
- **Current**: None implemented (development/demo project)

---

## Development & Testing Environment

### Local Setup

#### Prerequisites
- Java 21+ (managed via SDKMAN with `.sdkmanrc`)
- Maven 3.9+
- Docker Desktop (for Kubernetes)

#### Quick Start
```bash
# Development mode with live reload and auto-provisioned services
./mvnw quarkus:dev
```

#### Environment Features
- **Dev Services**: Automatic PostgreSQL and Kafka container provisioning
- **Live Coding**: Hot reload on code changes
- **Dev UI**: Web interface at `http://localhost:8080/q/dev/`
- **Single-Node Cluster**: Pekko cluster runs in standalone mode for simplicity

### Testing Frameworks

#### Test Architecture
The project employs a three-tier testing strategy:

1. **Pure Unit Tests** (No framework annotation)
   - **Purpose**: Test individual classes in isolation
   - **Scope**: Business logic, state transitions, validations
   - **Speed**: Milliseconds
   - **Infrastructure**: None required

2. **Integration Tests** (`@QuarkusTest`)
   - **Purpose**: Test components with full application context
   - **Scope**: REST endpoints, database operations, actor interactions
   - **Speed**: Seconds (same JVM)
   - **Infrastructure**: Quarkus CDI, actor system, embedded databases

3. **Packaged Integration Tests** (`@QuarkusIntegrationTest`)
   - **Purpose**: Test packaged application in production-like environment
   - **Scope**: End-to-end flows, packaging verification
   - **Speed**: Slower (separate process)
   - **Infrastructure**: Packaged JAR, external process

#### Test Execution
```bash
# Fast feedback loop (unit + integration tests)
./mvnw test

# Full verification (includes packaged tests)
./mvnw verify
```

### Code Quality Tools
- **Static Analysis**: Maven compiler with strict warnings
- **Database Migrations**: Flyway for schema version control
- **Dependency Management**: Maven dependency plugin

---

## Glossary

### Core Concepts

**Actor Model**: A concurrency model where "actors" are independent units that communicate via asynchronous messages, providing isolation and distribution.

**Aggregate**: A cluster of domain objects treated as a single unit for data changes. Aggregates enforce business invariants.

**CQRS (Command Query Responsibility Segregation)**: Pattern separating read operations (queries) from write operations (commands), allowing independent optimization.

**Event Sourcing**: Persistence pattern where state changes are stored as a sequence of immutable events rather than current state snapshots.

**Cluster Sharding**: Technique for distributing actors across multiple nodes based on entity identifiers, providing scalability and fault tolerance.

**Projection**: Process of deriving read models from event streams, transforming event history into query-optimized views.

**Eventually Consistent**: Data consistency model where read models may temporarily lag behind writes but will eventually reflect all changes.

### Technologies

**Apache Pekko**: Open-source continuation of Akka, providing actor-based toolkit for building concurrent, distributed systems.

**Quarkus**: Kubernetes-native Java framework optimized for GraalVM and HotSpot, designed for cloud-native applications.

**Flyway**: Database migration tool managing schema version control and evolution.

**Panache**: Quarkus extension simplifying Hibernate ORM usage with active record and repository patterns.

### Architectural Patterns

**Write Side**: The command-processing side of CQRS, responsible for validating and persisting state changes.

**Read Side**: The query-processing side of CQRS, providing optimized views for data retrieval.

**Domain Event**: A record of something significant that happened in the business domain, represented as an immutable fact.

**Command**: A request to perform an action that may change system state, subject to validation and business rules.

---

*This architecture documentation is maintained as a living document and should be updated as the system evolves.*
