package com.changeflow.web;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.transaction.autoconfigure.TransactionManagerCustomizers;
import org.springframework.context.annotation.*;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.DefaultTransactionStatus;

@Configuration(proxyBeanMethods=false)
public class TransactionConfiguration {
    @Bean JpaTransactionManager transactionManager(EntityManagerFactory factory,ObjectProvider<TransactionManagerCustomizers> customizers) {
        var manager=new JpaTransactionManager(factory) {
            @Override protected void doRollback(DefaultTransactionStatus status) {
                try {super.doRollback(status);}
                catch(TransactionSystemException failure) {throw failure;}
                catch(RuntimeException failure) {
                    // Spring's transaction interceptor attaches the original application exception
                    // to this standard wrapper if rollback itself fails on a lost connection.
                    throw new TransactionSystemException("Transaction rollback failed",failure);
                }
            }
        };
        customizers.ifAvailable(customizer->customizer.customize(manager));
        return manager;
    }
}
