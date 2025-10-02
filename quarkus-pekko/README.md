# Quarkus-Pekko: Reactive Distributed Systems

A demonstration project showcasing the integration of **Quarkus** (Supersonic Subatomic Java Framework) with **Apache Pekko** (actor-based toolkit) for building reactive, distributed, and resilient cloud-native applications.

## 🏗️ Project Overview

This application demonstrates:

- **Event Sourcing** with Pekko Persistence
- **CQRS** (Command Query Responsibility Segregation) with read-side projections
- **Actor Model** for concurrent and distributed processing
- **Cluster Sharding** for scalable actor distribution
- **Database Integration** with PostgreSQL and Flyway migrations
- **Cloud-Native Deployment** on Kubernetes

## 🎯 Key Technologies

- **[Quarkus](https://quarkus.io/)** - Cloud-native Java framework
- **[Apache Pekko](https://pekko.apache.org/)** - Actor toolkit (successor to Akka)
- **[Apache Kafka](https://kafka.apache.org/)** - Event streaming and messaging platform
- **PostgreSQL** - Database for persistence and projections
- **Hibernate Panache** - Simplified data access
- **Flyway** - Database migration management

## 🛠️ Development

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker (for Kubernetes deployment)
- kubectl (for Kubernetes management)

## 🚀 Running Options

### Option 1: Local Development Mode

Perfect for development with live coding and automatic restarts:

```bash
./mvnw quarkus:dev
```

**Features:**
- Live coding with hot reload
- Dev Services automatically start PostgreSQL containers
- Dev UI available at <http://localhost:8080/q/dev/>
- Single-node Pekko cluster for development

**Access:**
- Application: <http://localhost:8080>
- Health checks: <http://localhost:8080/q/health>
- Pekko Management: <http://localhost:7626/ready>

### Option 2: Local Kubernetes (Docker Desktop)

Deploy a full multi-node cluster locally with persistent storage using scripts in the `k8s/scripts` directory:

```bash
# Quick deployment
k8s/scripts/deploy.sh

# Access services locally
k8s/scripts/port-forward.sh all

# Redeploy (undeploy + deploy + port-forward in one command)
k8s/scripts/redeploy.sh

# Scale the cluster
kubectl scale deployment/quarkus-pekko --replicas=5 -n quarkus-pekko

# Clean up when done (preserves namespace and persistent data)
k8s/scripts/undeploy.sh

# Clean up everything including namespace and persistent data
k8s/scripts/undeploy.sh --delete-namespace
```

**Features:**
- 3-node Pekko cluster
- Persistent PostgreSQL
- Kafka messaging with Zookeeper
- AKHQ Kafka UI for monitoring topics and messages
- Service discovery via Kubernetes API
- Production-like environment locally

**Prerequisites:**
- Docker Desktop with Kubernetes enabled
- kubectl configured for local cluster

## 🧪 Testing the Application

### Manual API Testing

Once running, try these endpoints:

```bash
# Create a greeting (triggers actor and event sourcing)
curl -X POST http://localhost:8080/greetings/Alice

# Get greeting count
curl http://localhost:8080/greetings/Alice

# Health checks
curl http://localhost:8080/q/health

# Pekko cluster status (when running in cluster mode)
curl http://localhost:7626/cluster/members

```

### Automated Testing

This project includes comprehensive automated tests covering unit tests, integration tests, and end-to-end scenarios.

**Prerequisites:**
```bash
# Ensure you're using Java 21 (if using SDKMAN)
sdk env install
java -version  # Should show Java 21
```

**Running Tests:**
```bash
# Run unit tests only
./mvnw test

# Run all tests including integration tests 
# Packages the app and runs *IT tests against the JAR
./mvnw verify

# Run specific test class
./mvnw test -Dtest=GreetingResourceTest
```

**Test Types:**

This project has three types of tests, each serving a different purpose:

**1. Pure Unit Tests** (no annotation - `*Test.java`):
- Examples: `OpenStateTest`, `CloseStateTest`
- Test single classes in isolation
- No Quarkus infrastructure needed
- No CDI injection, no database, no actors
- Just plain Java objects and assertions
- **Very fast** (milliseconds)
- Run with: `./mvnw test`

**2. Integration Tests** (with `@QuarkusTest` - `*Test.java`):
- Examples: `GreetingsCountRepositoryTest`, `GreetingResourceTest`, `GreetingsCountReadSideHandlerTest`, `GreetingsKafkaHandlerTest`
- Test multiple layers together (REST → Actors → Database → Kafka)
- Need Quarkus infrastructure (CDI, database, actor system, Kafka)
- Support CDI injection (`@Inject`)
- Run in same JVM as tests (fast startup)
- **Fast** (seconds) - good for development loop
- Run with: `./mvnw test`

**3. Packaged Integration Tests** (with `@QuarkusIntegrationTest` - `*IT.java`):
- Example: `GreetingResourceIT`
- Test against packaged JAR in separate process
- Production-like environment
- No CDI injection available (separate process)
- Catches packaging issues (missing resources, native compilation problems)
- **Slower** (must package app first)
- Run with: `./mvnw verify`

**Difference between `test` and `verify`:**

| Command | What it does | When to use |
|---------|-------------|-------------|
| `./mvnw test` | Runs unit tests + `@QuarkusTest` integration tests in same JVM (fast) | Development, TDD, quick feedback |
| `./mvnw verify` | Runs `test` phase + packages app + runs `@QuarkusIntegrationTest` tests | CI/CD, pre-release, production validation |

## 📊 Architecture

This application implements **Event Sourcing** and **CQRS** patterns using the **Actor Model**:

- **Write Side**: Event-sourced actors handle commands and persist events
- **Read Side**: Projection handlers build optimized query models
- **Event Streaming**: Kafka integration for external systems

**For detailed architecture documentation, diagrams, and component descriptions:**

**→ See [`ARCHITECTURE.MD`](ARCHITECTURE.MD)**

## 📚 Documentation

### Project Documentation

- **[`ARCHITECTURE.MD`](ARCHITECTURE.MD)** - Detailed system architecture, patterns, and design decisions
- **[`AGENTS.MD`](AGENTS.MD)** - Developer guide for code patterns, conventions, and workflows for humans and AI agents

### External Resources

- **[Quarkus REST](https://quarkus.io/guides/rest)** - Jakarta REST implementation
- **[Apache Pekko](https://pekko.apache.org/)** - Actor toolkit documentation
- **[Flyway](https://quarkus.io/guides/flyway)** - Database schema migrations
- **[PostgreSQL](https://quarkus.io/guides/datasource)** - Database connectivity
- **[Kubernetes](https://quarkus.io/guides/kubernetes)** - Cloud-native deployment
- **[Container Images](https://quarkus.io/guides/container-image)** - Containerization