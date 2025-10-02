package com.gdfesta.quarkus.pekko;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;


import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.cluster.sharding.typed.javadsl.ClusterSharding;
import org.apache.pekko.cluster.typed.Cluster;
import org.apache.pekko.cluster.typed.Join;
import org.apache.pekko.management.cluster.bootstrap.ClusterBootstrap;
import org.apache.pekko.management.javadsl.PekkoManagement;
import org.jboss.logging.Logger;
import jakarta.annotation.Priority;


@ApplicationScoped
public class QuarkusPekkoActorSystemProducer {

    private static final Logger LOG = Logger.getLogger(QuarkusPekkoActorSystemProducer.class);

    @ConfigProperty(name = "quarkus.datasource.jdbc.url", defaultValue = "jdbc:postgresql://localhost:5432/quarkus")
    String datasourceUrl;

    @ConfigProperty(name = "quarkus.datasource.username", defaultValue = "quarkus")
    String datasourceUsername;

    @ConfigProperty(name = "quarkus.datasource.password", defaultValue = "quarkus")
    String datasourcePassword;

    private ActorSystem<Void> actorSystem;
    private ClusterSharding sharding;
    private PekkoManagement management;

    void onStart(@Observes @Priority(1001) StartupEvent event) {
        LOG.info("Initializing Pekko ActorSystem...");

        setSystemPropertyIfAbsent("QUARKUS_DATASOURCE_JDBC_URL", datasourceUrl);
        setSystemPropertyIfAbsent("QUARKUS_DATASOURCE_USERNAME", datasourceUsername);
        setSystemPropertyIfAbsent("QUARKUS_DATASOURCE_PASSWORD", datasourcePassword);

        LOG.infof("Configured Pekko with Quarkus datasource: %s (user: %s)", datasourceUrl, datasourceUsername);

        // Load Pekko configuration
        Config config = ConfigFactory.load();

        // Create the ActorSystem
        actorSystem = ActorSystem.create(
            Behaviors.empty(),
            "quarkus-pekko",
            config
        );

        // Determine if we're running in Kubernetes or local
        boolean isKubernetes = System.getenv("KUBERNETES_SERVICE_HOST") != null;
        LOG.infof("Running in Kubernetes mode: %s", isKubernetes);

        // Start Pekko Management (required for both local and k8s)
        management = PekkoManagement.get(actorSystem);
        management.start();
        LOG.infof("Pekko Management HTTP endpoint started");

        if (isKubernetes) {
            // In Kubernetes, use cluster bootstrap for automatic discovery
            ClusterBootstrap.get(actorSystem).start();
            LOG.info("Pekko Cluster Bootstrap started for Kubernetes discovery");
        } else {
            // Initialize cluster
            Cluster cluster = Cluster.get(actorSystem);
            LOG.infof("Cluster node address: %s", cluster.selfMember().address());
            // In local development, join the cluster directly
            cluster.manager().tell(new Join(cluster.selfMember().address()));
            LOG.info("Joined cluster as single node for local development");
        }

        sharding = ClusterSharding.get(actorSystem);
    }

    void onStop(@Observes ShutdownEvent event) {
        LOG.info("Shutting down Pekko ActorSystem...");

        if (management != null) {
            management.stop();
        }

        if (actorSystem != null) {
            actorSystem.terminate();
            LOG.info("Pekko ActorSystem terminated");
        }
    }

    @Produces
    @ApplicationScoped
    public ActorSystem<Void> actorSystem() {
        return actorSystem;
    }

    @Produces
    @ApplicationScoped
    public ClusterSharding clusterSharding() {
        return sharding;
    }

    private void setSystemPropertyIfAbsent(String key, String value) {
        // Check if already set as environment variable or system property
        if (System.getenv(key) == null && System.getProperty(key) == null) {
            System.setProperty(key, value);
            LOG.debugf("Set system property: %s", key);
        } else {
            LOG.debugf("System property or env var already set: %s", key);
        }
    }
}