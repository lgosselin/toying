package com.toying.database.mysql_batch_insert_hibernate.persistence.utils;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.ejb.packaging.PersistenceMetadata;
import org.hibernate.ejb.packaging.PersistenceXmlLoader;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitTransactionType;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

public class HibernateHelper {
    private HibernateHelper() {
    }

    /**
     * Close your eyes, this is not something you'd want to have look at.
     * This method (sort of...) emulates the creation process of a JPA EntityManagerFactory.
     * The base configuration comes from META-INF/persistence.xml and, optionally, some properties are overridden.
     */
    public static EntityManagerFactory buildHibernateEntityManagerFactory(Map<String, String> overrides) throws Exception {
        Enumeration<URL> persistenceXmlUrls = Thread.currentThread()
                .getContextClassLoader()
                .getResources("META-INF/persistence.xml");
        if (!persistenceXmlUrls.hasMoreElements()) {
            throw new IllegalStateException("No persistence.xml detected? This code does not handle this.");
        }
        URL xmlUrl = persistenceXmlUrls.nextElement();
        Ejb3Configuration ejb3Configuration = new Ejb3Configuration();
        List<PersistenceMetadata> persistenceMetadatas = PersistenceXmlLoader.deploy(xmlUrl,
                overrides,
                ejb3Configuration.getHibernateConfiguration().getEntityResolver(),
                PersistenceUnitTransactionType.RESOURCE_LOCAL);
        ejb3Configuration = ejb3Configuration.configure(persistenceMetadatas.get(0), overrides);

        return ejb3Configuration.buildEntityManagerFactory();
    }
}