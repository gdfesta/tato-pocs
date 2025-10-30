package com.gdfesta.springboot.pekko;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.cluster.sharding.typed.javadsl.ClusterSharding;
import org.apache.pekko.cluster.typed.Cluster;
import org.apache.pekko.cluster.typed.Join;
import org.apache.pekko.management.cluster.bootstrap.ClusterBootstrap;
import org.apache.pekko.management.javadsl.PekkoManagement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SpringBootPekkoActorSystemConfiguration {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    private ActorSystem<Void> actorSystem;
    private PekkoManagement management;

    @Bean
    public ActorSystem<Void> actorSystem() {
        log.info("Initializing Pekko ActorSystem...");

        setSystemPropertyIfAbsent("SPRING_DATASOURCE_URL", datasourceUrl);
        setSystemPropertyIfAbsent("SPRING_DATASOURCE_USERNAME", datasourceUsername);
        setSystemPropertyIfAbsent("SPRING_DATASOURCE_PASSWORD", datasourcePassword);

        log.info("Configured Pekko with Spring datasource: {} (user: {})", datasourceUrl, datasourceUsername);

        // Load Pekko configuration
        Config config = ConfigFactory.load();

        // Create the ActorSystem
        actorSystem = ActorSystem.create(Behaviors.empty(), "spring-pekko", config);

        // Determine if we're running in Kubernetes or local
        boolean isKubernetes = System.getenv("KUBERNETES_SERVICE_HOST") != null;
        log.info("Running in Kubernetes mode: {}", isKubernetes);

        // Start Pekko Management (required for both local and k8s)
        management = PekkoManagement.get(actorSystem);
        management.start();
        log.info("Pekko Management HTTP endpoint started");

        if (isKubernetes) {
            // In Kubernetes, use cluster bootstrap for automatic discovery
            ClusterBootstrap.get(actorSystem).start();
            log.info("Pekko Cluster Bootstrap started for Kubernetes discovery");
        } else {
            // Initialize cluster
            Cluster cluster = Cluster.get(actorSystem);
            log.info("Cluster node address: {}", cluster.selfMember().address());
            // In local development, join the cluster directly
            cluster.manager().tell(new Join(cluster.selfMember().address()));
            log.info("Joined cluster as single node for local development");
        }

        return actorSystem;
    }

    @Bean
    public ClusterSharding clusterSharding(ActorSystem<Void> actorSystem) {
        return ClusterSharding.get(actorSystem);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down Pekko ActorSystem...");

        if (management != null) {
            management.stop();
        }

        if (actorSystem != null) {
            actorSystem.terminate();
            log.info("Pekko ActorSystem terminated");
        }
    }

    private void setSystemPropertyIfAbsent(String key, String value) {
        // Check if already set as environment variable or system property
        if (System.getenv(key) == null && System.getProperty(key) == null) {
            System.setProperty(key, value);
            log.debug("Set system property: {}", key);
        } else {
            log.debug("System property or env var already set: {}", key);
        }
    }
}
