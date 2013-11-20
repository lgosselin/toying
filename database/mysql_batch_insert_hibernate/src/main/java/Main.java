import com.toying.database.mysql_batch_insert_hibernate.persistence.utils.HibernateHelper;
import com.toying.database.mysql_batch_insert_hibernate.persistence.SomeEntityWithIdentityId;
import com.toying.database.mysql_batch_insert_hibernate.persistence.SomeEntityWithTableId;
import com.toying.database.mysql_batch_insert_hibernate.persistence.SomeEntityWithTableIdAndUniqueField;
import com.toying.database.mysql_batch_insert_hibernate.service.BatchCreation;
import com.toying.database.mysql_batch_insert_hibernate.service.InTransaction;
import com.toying.database.mysql_batch_insert_hibernate.service.InTransactionCallBack;
import org.hibernate.ejb.AvailableSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static Logger logger = LoggerFactory.getLogger(Main.class);

    public static final String DB_URL_REWRITE_PARAMETER = "rewriteBatchedStatements=true";
    public static final String DB_BASE_URL = "jdbc:mysql://localhost:3306/unit_test"; // Adapt this
    public static final String DB_USER = "unit_test"; // Adapt this
    public static final String DB_PASSWORD = "unit_test_pass"; // Adapt this
    public static final int ENTITY_COUNT_TO_INSERT = 100000; // Adapt this: The number of tuples we are going to insert
    public static final int DUPLICATE_PERCENTAGE = 15; // Adapt this: Used during the test of INSERT IGNORE. Approximate percentage of duplicate entities desired

    public static final String PARAM_JDBC_URL = AvailableSettings.JDBC_URL;
    public static final String PARAM_JDBC_USER = AvailableSettings.JDBC_USER;
    public static final String PARAM_JDBC_PASSWORD = AvailableSettings.JDBC_PASSWORD;
    public static final String PARAM_BATCH_SIZE = org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE;
    public static final String PARAM_EMF_NAME = AvailableSettings.ENTITY_MANAGER_FACTORY_NAME;

    private static final String EOL = System.getProperty("line.separator");

    public static void main(String[] args) {
        logger.info("Starting...");
        try {
            new Main().doTests();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            logger.info("Execution ended.");
        }
    }

    public Main() {}

    public void doTests() throws Exception {
        Map<String, String> config0NoBatchNoRewrite = configOverrides(0, false);
        Map<String, String> config1NoBatchRewrite = configOverrides(0, true);
        Map<String, String> config2Batch10NoRewrite = configOverrides(10, false);
        Map<String, String> config3Batch10Rewrite = configOverrides(10, true);
        Map<String, String> config4Batch50NoRewrite = configOverrides(50, false);
        Map<String, String> config5Batch50Rewrite = configOverrides(50, true);

        TestResults config0Results = runTestWithOverrides(config0NoBatchNoRewrite);
        TestResults config1Results = runTestWithOverrides(config1NoBatchRewrite);
        TestResults config2Results = runTestWithOverrides(config2Batch10NoRewrite);
        TestResults config3Results = runTestWithOverrides(config3Batch10Rewrite);
        TestResults config4Results = runTestWithOverrides(config4Batch50NoRewrite);
        TestResults config5Results = runTestWithOverrides(config5Batch50Rewrite);

        StringBuilder sb = new StringBuilder(1024);
        sb.append("Timing for ").append(ENTITY_COUNT_TO_INSERT).append(" insertions");
        sb.append(EOL);
        sb.append(TestResults.headers());
        sb.append(TestResults.logLine("No batching, no rewrite batch", config0Results));
        sb.append(TestResults.logLine("No batching, rewrite batch on", config1Results));
        sb.append(TestResults.logLine("Batching per 10, no rewrite batch", config2Results));
        sb.append(TestResults.logLine("Batching per 10, rewrite batch on", config3Results));
        sb.append(TestResults.logLine("Batching per 50, no rewrite batch", config4Results));
        sb.append(TestResults.logLine("Batching per 50, rewrite batch on", config5Results));

        logger.info(sb.toString());
    }

    private TestResults runTestWithOverrides(Map<String, String> overrides) throws Exception {
        // Easy way to create an EntityManagerFactory, but you cannot override anything
        //EntityManagerFactory emf = Persistence.createEntityManagerFactory("puName1");
        EntityManagerFactory emf = HibernateHelper.buildHibernateEntityManagerFactory(overrides);
        BatchCreation batchCreation = new BatchCreation(emf);

        logger.info("Warm up");
        int entityCount = 100;
        for (int i = 0; i < 10; i++) {
            truncateTestTables(emf);
            createLotsOfSomeEntityWithIdentityId(batchCreation, entityCount, 0);
            createLotsOfSomeEntityWithTableId(batchCreation, entityCount, 0);
            createLotsOfSomeEntityWithTableIdAndUniqueField(batchCreation, entityCount, 0);
        }

        logger.info("Measurements");
        entityCount = ENTITY_COUNT_TO_INSERT;
        truncateTestTables(emf);
        long identityRunTime = createLotsOfSomeEntityWithIdentityId(batchCreation, entityCount, 50);
        long tableIdRunTime = createLotsOfSomeEntityWithTableId(batchCreation, entityCount, 50);
        long tableIdIgnoreRuntime = createLotsOfSomeEntityWithTableIdAndUniqueField(batchCreation, entityCount, 50);
        logger.info("End of measurements");
        emf.close();

        TestResults results = new TestResults(identityRunTime, tableIdRunTime, tableIdIgnoreRuntime);
        return results;
    }

    private Map<String, String> configOverrides(int batchSize, boolean rewriteBatchStatements) {
        Map<String, String> overrides = new HashMap<>();
        //overrides.put(PARAM_EMF_NAME, "pu" + System.currentTimeMillis());
        overrides.put(PARAM_JDBC_USER, DB_USER);
        overrides.put(PARAM_JDBC_PASSWORD, DB_PASSWORD);
        if (rewriteBatchStatements) {
            overrides.put(PARAM_JDBC_URL, DB_BASE_URL + "?" + DB_URL_REWRITE_PARAMETER);
        }
        else {
            overrides.put(PARAM_JDBC_URL, DB_BASE_URL);
        }
        overrides.put(PARAM_BATCH_SIZE, Integer.toString(batchSize));
        return overrides;
    }

    private static void truncateTestTables(final EntityManagerFactory emf) {
        InTransaction.doInNewTransaction(emf, new InTransactionCallBack<Void>() {
            @Override
            public Void call(EntityManager em, EntityTransaction trans) {
                Query truncateQuery;
                truncateQuery = em.createNativeQuery("TRUNCATE TABLE " + SomeEntityWithIdentityId.TABLE_NAME);
                truncateQuery.executeUpdate();
                truncateQuery = em.createNativeQuery("TRUNCATE TABLE " + SomeEntityWithTableId.TABLE_NAME);
                truncateQuery.executeUpdate();
                truncateQuery = em.createNativeQuery("TRUNCATE TABLE " + SomeEntityWithTableIdAndUniqueField.TABLE_NAME);
                truncateQuery.executeUpdate();
                return null;
            }
        });
    }

    private static long createLotsOfSomeEntityWithIdentityId(final BatchCreation batchCreation, final int count, final int manualBatchSize) {
        logger.debug("Starting test with identity id generation. Count [{}], ManualBatchSize [{}]", count, manualBatchSize);
        long start = System.currentTimeMillis();
        batchCreation.createLotsOfSomeEntityWithIdentityId(count, manualBatchSize);
        long end = System.currentTimeMillis();
        long executionTime = end - start;
        logger.debug("Finished in {} ms", executionTime);
        return executionTime;
    }

    private static long createLotsOfSomeEntityWithTableId(final BatchCreation batchCreation, final int count, final int manualBatchSize) {
        logger.debug("Starting test with table id generation. Count [{}], ManualBatchSize [{}]", count, manualBatchSize);
        long start = System.currentTimeMillis();
        batchCreation.createLotsOfSomeEntityWithTableId(count, manualBatchSize);
        long end = System.currentTimeMillis();
        long executionTime = end - start;
        logger.debug("Finished in {} ms", executionTime);
        return executionTime;
    }

    private static long createLotsOfSomeEntityWithTableIdAndUniqueField(final BatchCreation batchCreation, final int count, final int manualBatchSize) {
        logger.debug("Starting test with table id generation and unique field. Count [{}], ManualBatchSize [{}]", count, manualBatchSize);
        long start = System.currentTimeMillis();
        batchCreation.createLotsOfSomeEntityWithTableIdWithUniqueField(count, manualBatchSize, DUPLICATE_PERCENTAGE);
        long end = System.currentTimeMillis();
        long executionTime = end - start;
        logger.debug("Finished in {} ms", executionTime);
        return executionTime;
    }

    private static class TestResults {
        long identityRunTime, tableIdRunTime, tableIdIgnoreRuntime;

        public TestResults(long identityRunTime, long tableIdRunTime, long tableIdIgnoreRuntime) {
            this.identityRunTime = identityRunTime;
            this.tableIdRunTime = tableIdRunTime;
            this.tableIdIgnoreRuntime = tableIdIgnoreRuntime;
        }

        private long getIdentityRunTime() {
            return identityRunTime;
        }

        private long getTableIdRunTime() {
            return tableIdRunTime;
        }

        private long getTableIdIgnoreRuntime() {
            return tableIdIgnoreRuntime;
        }

        public static String headers() {
            StringBuilder sb = new StringBuilder(128);
            sb.append(String.format("| %1$40s ", "Runtime in ms"))
                    .append(String.format("| %1$20s ", "Identity"))
                    .append(String.format("| %1$20s ", "Table id"))
                    .append(String.format("| %1$20s ", "Table id with dedup"))
                    .append('|')
                    .append(EOL);
            return sb.toString();
        }

        public static String logLine(String title, TestResults result) {
            StringBuilder sb = new StringBuilder(128);
            sb.append(String.format("| %1$40s ", title))
                    .append(String.format("| %1$20s ", result.getIdentityRunTime()))
                    .append(String.format("| %1$20s ", result.getTableIdRunTime()))
                    .append(String.format("| %1$20s ", result.getTableIdIgnoreRuntime()))
                    .append('|')
                    .append(EOL);
            return sb.toString();
        }
    }
}
