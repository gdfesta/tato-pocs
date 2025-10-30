package com.gdfesta.springboot.pekko;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.stereotype.Component;

@Component
public class HibernateSessionFactory {

    private final EntityManagerFactory entityManagerFactory;

    public HibernateSessionFactory(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    public HibernateJdbcSession newInstance() {
        return new HibernateJdbcSession(entityManagerFactory.createEntityManager());
    }
}
