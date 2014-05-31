package com.toying.database.jooq.util.maven;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.persistence.Embeddable;
import javax.persistence.Entity;

import org.h2.util.JdbcUtils;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.jooq.util.GenerationTool;
import org.jooq.util.jaxb.Generator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

public class Bridge {
	
	private static Logger LOGGER = LoggerFactory.getLogger(Bridge.class);
	
	private BridgeParameters p;
	
	public Bridge(BridgeParameters parameters) {
		this.p = parameters;
	}
	
	public void run() throws Exception {
		List<Class<?>> entities = findEntityClasses(p.scanPackages);
		if (entities.isEmpty()) {
			LOGGER.warn("No entity was found in the class path. Please check the configuration.");
		}
		
		Properties props = loadProperties();

		// Open a connection and keep it open. This will keep in-memory DB alive.
		Connection connection = getConnection(props);
		if (connection == null) {
			throw new IllegalStateException("Aborting. Cannot obtain a connection.");
		}
				
		try {
			Configuration cfg = createHibernateConfiguration(entities, props, p.mappingResources);
			generateSchemaAndScript(cfg, p.ddlScriptOutputFilename);
			runJooqCodeGen(connection, p.jooqGeneratorConfiguration);
		}
		finally {
			if (connection != null) {
				JdbcUtils.closeSilently(connection);
			}
		}
	}
	
	private List<Class<?>> findEntityClasses(Collection<String> packagesToScan) {
		if (packagesToScan.isEmpty()) {
			throw new IllegalArgumentException("Cannot proceed with no packages to scan");
		}
		
		List<Class<?>> entities = new LinkedList<Class<?>>();
		
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
		scanner.addIncludeFilter(new AnnotationTypeFilter(Embeddable.class));
		for (String packageString : packagesToScan)
			for (BeanDefinition bd : scanner.findCandidateComponents(packageString)) {
				Class<?> entityClass;
				try {
					entityClass = Class.forName(bd.getBeanClassName());
					entities.add(entityClass);
				} catch (ClassNotFoundException e) {
					LOGGER.error("Class not found [{}]", bd.getBeanClassName());
				}
			}
		return entities;
	}

	private static Connection getConnection(Properties props) {
		try {
			return DriverManager.getConnection(
					props.getProperty(AvailableSettings.URL), 
					props.getProperty(AvailableSettings.USER),
					props.getProperty(AvailableSettings.PASS)
			);
		}
		catch (SQLException e) {
			throw new IllegalStateException("Aborting. Cannot obtain a connection.", e);
		}
	}
	
	private static Configuration createHibernateConfiguration(List<Class<?>> entities, Properties props, Set<String> additionalMappingResources) {
		Configuration cfg = new Configuration();
		cfg.addProperties(props);
		for (Class<?> annotatedClass : entities) {
			cfg.addAnnotatedClass(annotatedClass);
		}
		for (String resource : additionalMappingResources) {
			InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
			if (inputStream == null) {
				LOGGER.warn("Could not access resource [" + resource + "]");
			}
			else {
				cfg.addInputStream(inputStream);
			}
		}
		return cfg;
	}
	
	private static void generateSchemaAndScript(Configuration cfg, String outputScriptFilename) {
		SchemaUpdate export = new SchemaUpdate(cfg);
		export.setDelimiter(";");
		export.setOutputFile(outputScriptFilename);
		export.execute(true, true);
		if (!export.getExceptions().isEmpty()) {
			LOGGER.error("export generated expection(s): " + Arrays.toString(export.getExceptions().toArray()));
			throw new IllegalStateException("Exceptions encountered during schema generation", (Exception)export.getExceptions().get(0));
		}
	}
	
	private static void runJooqCodeGen(Connection connection, org.jooq.util.jaxb.Generator codeGenConf) throws Exception {
		GenerationTool generationTool = new GenerationTool();
		generationTool.setConnection(connection);
		generationTool.run(new org.jooq.util.jaxb.Configuration().withGenerator(codeGenConf));
	}

	private static Properties loadProperties() {
		InputStream in = null;
		try {
			in = Main.class.getResourceAsStream("/generator.properties");
			if (in != null) {
				Properties props = new Properties();
				props.load(in);
				return props;
			}
			throw new RuntimeException("Error while reading generator.properties at the root of the classpath");
		}
		catch (Exception e) {
			throw new RuntimeException("Error while reading generator.properties at the root of the classpath", e);
		}
		finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ignored) {	}
			}
		}
		
	}
	
	public static class BridgeParameters {
		// TODO: Make a builder interface instead of a bare all public member container
		public Set<String> scanPackages;
		public Set<String> mappingResources;
		public String ddlScriptOutputFilename;
		public Generator jooqGeneratorConfiguration;
		
	}

}
