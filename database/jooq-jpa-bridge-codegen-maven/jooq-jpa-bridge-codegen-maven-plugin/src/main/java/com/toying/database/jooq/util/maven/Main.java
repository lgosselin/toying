package com.toying.database.jooq.util.maven;

import java.util.Arrays;
import java.util.HashSet;

import org.jooq.util.jaxb.Database;
import org.jooq.util.jaxb.Generator;
import org.jooq.util.jaxb.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.toying.database.jooq.util.maven.Bridge.BridgeParameters;

public class Main {

	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class); 
	
	public static void main(String[] args) throws Exception {
		BridgeParameters parameters = new BridgeParameters();
		parameters.scanPackages = new HashSet<String>(Arrays.asList("com.toying"));
		parameters.ddlScriptOutputFilename = "target/ddl.sql";
		parameters.mappingResources = new HashSet<String>(Arrays.asList("defaults-orm.xml"));
		parameters.jooqGeneratorConfiguration = loadJooqCodeGenConfiguration();
		Bridge bridge = new Bridge(parameters);
		bridge.run();
	}

	private static org.jooq.util.jaxb.Generator loadJooqCodeGenConfiguration() {
		return new Generator()
						.withName("org.jooq.util.DefaultGenerator")
						.withDatabase(
								new Database()
										.withName("org.jooq.util.h2.H2Database")
										.withIncludes(".*")
										.withExcludes("")
										.withInputSchema("PUBLIC")
										.withOutputSchema(""))
						.withTarget(
								new Target()
										.withPackageName("com.toying.database.meta.jooq")
										.withDirectory("target/generated-sources/jooq")
						)
				;
	}

	
}
