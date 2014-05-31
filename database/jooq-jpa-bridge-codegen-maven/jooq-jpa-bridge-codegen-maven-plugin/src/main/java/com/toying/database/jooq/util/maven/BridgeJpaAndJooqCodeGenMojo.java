package com.toying.database.jooq.util.maven;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.jooq.util.jaxb.Database;
import org.jooq.util.jaxb.Generator;
import org.jooq.util.jaxb.Target;

import com.toying.database.jooq.util.maven.Bridge.BridgeParameters;

@Mojo(name="generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = false,
		requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class BridgeJpaAndJooqCodeGenMojo extends AbstractMojo
{
	@Component 
	private PluginDescriptor pluginDescriptor;

	@Parameter(defaultValue = "${project.compileClasspathElements}", readonly = true, required = true)
	private List<String> classpathElements;
	
	@Parameter(defaultValue = "${project.runtimeClasspathElements}", readonly = true, required = true)
	private List<String> runtimeClasspathElements;
	
	@Parameter(required = true)
	private Set<String> scanPackages = new HashSet<String>();
	
	@Parameter(required = false)
	private Set<String> mappingResources = new HashSet<String>();
	
	/*@Parameter(defaultValue = "${project.build.outputDirectory}/", required = true, readonly = true)
	private String projectBuildOutputDirectory;*/
	
	@Parameter(defaultValue = "${project.build.outputDirectory}/ddl.sql", required = true, readonly = true)
	private String outputScript;
	
	@Parameter(defaultValue = "${project.build.directory}/generated-sources/jooq", required = true, readonly = true)
	private File outputDirectory;
	
	@Parameter(defaultValue = "", required = false, readonly = false)
	private String packageName;
	
	@Parameter(required = false, readonly = false)
	private Generator generator; 
	
	public void execute() throws MojoExecutionException, MojoFailureException {
		/* 
		 * For the schema generation to work, we need to have @Entity classes
		 * available in our classpath. We expect them to have been compiled
		 * before this plugin execution in the project output directory 
		 * (usually target/classes).
		 * If you are using inheritance of entity and they are defined in
		 * another artifact, you will need dependencies in the classpath
		 * as well. So far I believe runtime scope is large enough.
		 */
		addRuntimeLibrariesToClassPath();
		
		BridgeParameters parameters = new BridgeParameters();
		parameters.scanPackages = scanPackages;
		parameters.mappingResources = mappingResources;
		parameters.ddlScriptOutputFilename = outputScript;
		parameters.jooqGeneratorConfiguration = prepareGeneratorConfiguration(generator); 
		Bridge bridge = new Bridge(parameters);
		try {
			bridge.run();
		}
		catch (Exception cause) {
			throw new MojoExecutionException("An exception occured during execution", cause);
		}
	}

	private void addRuntimeLibrariesToClassPath() throws MojoFailureException {
		ClassRealm realm = pluginDescriptor.getClassRealm(); // nicer than (ClassRealm)Thread.currentThread().getContextClassLoader();
		try {
			for (String directoryOrJar : runtimeClasspathElements) {
				realm.addURL(new File(directoryOrJar).toURI().toURL());
			}
		} catch (MalformedURLException e) {
			throw new MojoFailureException("Cannot add URL to classloader", e);
		}
	}

	private Generator prepareGeneratorConfiguration(Generator generator) {
		if (generator == null) {
			generator = new Generator();
		}
		
		if (generator.getDatabase() != null) {
			getLog().warn("Generator#database information was provided and will be overridden");
		}
		generator.withDatabase(
				new Database()
				.withName("org.jooq.util.h2.H2Database")
				.withIncludes(".*")
				.withExcludes("")
				.withInputSchema("PUBLIC")
				.withOutputSchema(""));
		
		if (generator.getTarget() == null) {
			generator.withTarget(
					new Target()
							.withPackageName(packageName)
							.withDirectory(outputDirectory.toURI().getPath())
			);
		}
		else {
			Target target = generator.getTarget();
			if (target.getPackageName() != null && packageName != null) {
				getLog().warn("You specified a package name in both the generator configuration and the plugin configuration. The generator configuration takes over.");
			}
			else {
				target.setPackageName(packageName);
			}
			
			if (target.getDirectory() != null) {
				getLog().warn("Generator#target#directory was provided. It will be overridden with " + outputDirectory.toURI().toString());
			}
			target.setDirectory(outputDirectory.toURI().toString());
		}
		
		return generator;
	}
	
}
