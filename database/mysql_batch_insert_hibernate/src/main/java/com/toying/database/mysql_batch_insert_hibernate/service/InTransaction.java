package com.toying.database.mysql_batch_insert_hibernate.service;

import com.toying.database.mysql_batch_insert_hibernate.service.InTransactionCallBack;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

public class InTransaction {
    private InTransaction() {}

    // TODO: Get rid of this and have a standard mechanism take care of this (Spring bean and @Transactional?, EJB in a lightweight container?)
    public static <O> O doInNewTransaction(EntityManagerFactory emf, InTransactionCallBack<O> callback) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction trans = em.getTransaction();
        try {
            trans.begin();
            O res = callback.call(em, trans);
            if(trans.isActive() && !trans.getRollbackOnly()) {
                trans.commit();
            }
            else {
                trans.rollback();
            }
            return res;
        }
        finally {
            em.close();
        }
    }
}
