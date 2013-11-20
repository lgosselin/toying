package com.toying.database.mysql_batch_insert_hibernate.service;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

public interface InTransactionCallBack<O> {
    O call(EntityManager em, EntityTransaction trans);
}
