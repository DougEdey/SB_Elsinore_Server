package com.sb.elsinore.rules;

import com.sb.elsinore.models.PIDModel;
import com.sb.elsinore.models.Temperature;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

public class SessionFactoryRule implements MethodRule {
    private SessionFactory sessionFactory;
    private Transaction transaction;
    private Session session;

    @Override
    public Statement apply(final Statement statement, FrameworkMethod method,
                           Object test) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                SessionFactoryRule.this.sessionFactory = createSessionFactory();
                createSession();
                beginTransaction();
                try {
                    statement.evaluate();
                } finally {
                    shutdown();
                }
            }
        };
    }

    private void shutdown() {
        try {
            try {
                try {
                    this.transaction.rollback();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                this.session.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            this.sessionFactory.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private SessionFactory createSessionFactory() {
        Configuration configuration = new Configuration();
        configuration.addAnnotatedClass(Temperature.class)
                .addAnnotatedClass(PIDModel.class);
        configuration.setProperty("hibernate.dialect",
                "org.hibernate.dialect.H2Dialect");
        configuration.setProperty("hibernate.connection.driver_class",
                "org.h2.Driver");
        configuration.setProperty("hibernate.connection.url", "jdbc:h2:mem");
        configuration.setProperty("hibernate.hbm2ddl.auto", "create");
        return configuration.buildSessionFactory();
    }

    public Session createSession() {
        this.session = this.sessionFactory.openSession();
        return this.session;
    }

    public void commit() {
        this.transaction.commit();
    }

    public void beginTransaction() {
        this.transaction = this.session.beginTransaction();
    }

    public Session getSession() {
        return this.session;
    }
}