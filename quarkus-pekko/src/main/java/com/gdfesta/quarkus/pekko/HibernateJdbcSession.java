package com.gdfesta.quarkus.pekko;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import org.apache.pekko.japi.function.Function;
import org.apache.pekko.projection.jdbc.JdbcSession;
import org.hibernate.Session;

import java.sql.Connection;
import java.sql.SQLException;


public class HibernateJdbcSession implements JdbcSession {

  public final EntityManager entityManager;
  private final EntityTransaction transaction;

  public HibernateJdbcSession(EntityManager entityManager) {
    this.entityManager = entityManager;
    this.transaction = this.entityManager.getTransaction();
    this.transaction.begin();
  }

  @Override
  public <Result> Result withConnection(Function<Connection, Result> func) {
    Session hibernateSession = entityManager.unwrap(Session.class);
    return hibernateSession.doReturningWork(
        connection -> {
          try {
            return func.apply(connection);
          } catch (SQLException e) {
            throw e;
          } catch (Exception e) {
            throw new SQLException(e);
          }
        });
  }

  @Override
  public void commit() {
    transaction.commit();
  }

  @Override
  public void rollback() {
    // propagates rollback call if transaction is active
    if (transaction.isActive()) transaction.rollback();
  }

  @Override
  public void close() {
    this.entityManager.close();
  }
}