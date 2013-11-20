package com.toying.database.mysql_batch_insert_hibernate.service;

import com.toying.database.mysql_batch_insert_hibernate.persistence.SomeEntityWithIdentityId;
import com.toying.database.mysql_batch_insert_hibernate.persistence.SomeEntityWithTableId;
import com.toying.database.mysql_batch_insert_hibernate.persistence.SomeEntityWithTableIdAndUniqueField;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

public class BatchCreation {

    private final EntityManagerFactory emf;

    public BatchCreation(EntityManagerFactory emf) {
        this.emf = emf;
    }

    /**
     *
     * @param count Number of entity to create
     * @param manualBatchSize Negative or 0-value disable any manual flushing and clearing of the PersistentContext
     */
    public void createLotsOfSomeEntityWithIdentityId(final int count, final int manualBatchSize) {
        createAndPersistLotsOfEntitiesInNewTx(count, manualBatchSize, new SomeEntityWithIdentityIdFactory());
    }

    /**
     *
     * @param count Number of entity to create
     * @param manualBatchSize Negative or 0-value disable any manual flushing and clearing of the PersistentContext
     */
    public void createLotsOfSomeEntityWithTableId(final int count, final int manualBatchSize) {
        createAndPersistLotsOfEntitiesInNewTx(count, manualBatchSize, new SomeEntityWithTableIdFactory());
    }

    /**
     *
     * @param count Number of entity to create
     * @param manualBatchSize Negative or 0-value disable any manual flushing and clearing of the PersistentContext
     */
    public void createLotsOfSomeEntityWithTableIdWithUniqueField(final int count, final int manualBatchSize, final int duplicatePercentage) {
        createAndPersistLotsOfEntitiesInNewTx(count, manualBatchSize, new SomeEntityWithTableIdWithUniqueFieldFactory(duplicatePercentage));
    }

    private <E> void createAndPersistLotsOfEntitiesInNewTx(final int count, final int manualBatchSize, final Factory<E> entityFactory) {
        InTransaction.doInNewTransaction(emf, new InTransactionCallBack<Void>() {
            @Override
            public Void call(EntityManager em, EntityTransaction trans) {
                for (int i = 1; i <= count; i++) {
                    E entity = entityFactory.create(i);
                    em.persist(entity);
                    if (manualBatchSize > 0 && i % manualBatchSize == 0) {
                        em.flush();
                        em.clear();
                    }
                }
                return null;
            }
        });
    }

    private interface Factory<E> {
        public E create(int externalId);
    }

    private static class SomeEntityWithIdentityIdFactory implements Factory<SomeEntityWithIdentityId> {
        @Override
        public SomeEntityWithIdentityId create(int externalId) {
            SomeEntityWithIdentityId se = new SomeEntityWithIdentityId();
            se.setAnotherLong(externalId);
            se.setTs(System.currentTimeMillis());
            se.setSomeString("Dummy entry " + externalId);
            return se;
        }
    }

    private static class SomeEntityWithTableIdFactory implements Factory<SomeEntityWithTableId> {
        @Override
        public SomeEntityWithTableId create(int externalId) {
            SomeEntityWithTableId se = new SomeEntityWithTableId();
            se.setAnotherLong(externalId);
            se.setTs(System.currentTimeMillis());
            se.setSomeString("Dummy entry " + externalId);
            return se;
        }
    }

    private static class SomeEntityWithTableIdWithUniqueFieldFactory implements Factory<SomeEntityWithTableIdAndUniqueField> {
        private int duplicatePercentage;
        private long generatedCount;
        private long duplicatesCount;
        private int lastExternalId;

        public SomeEntityWithTableIdWithUniqueFieldFactory(int duplicatePercentage) {
            this.duplicatePercentage = duplicatePercentage;
            this.generatedCount = 0;
            this.duplicatesCount = 0;
        }

        @Override
        public SomeEntityWithTableIdAndUniqueField create(int externalId) {
            SomeEntityWithTableIdAndUniqueField se = new SomeEntityWithTableIdAndUniqueField();
            if (duplicatePercentage > 0 && generatedCount > 0) {
                if (100 * (duplicatesCount + 1) / (generatedCount + 1) < duplicatePercentage) {
                    ++duplicatesCount;
                    externalId = lastExternalId; // override desired id to produce a duplicate
                }
            }
            se.setAnotherLong(externalId);
            se.setTs(System.currentTimeMillis());
            se.setSomeString("Dummy entry " + externalId);
            ++generatedCount;
            return se;
        }
    }
}
