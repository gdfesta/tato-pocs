package com.gdfesta.quarkus.pekko;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManagerFactory;

@ApplicationScoped
public class HibernateSessionFactory {

    private final EntityManagerFactory entityManagerFactory;

    public HibernateSessionFactory(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    public HibernateJdbcSession newInstance() {
        return new HibernateJdbcSession(entityManagerFactory.createEntityManager());
    }
}
