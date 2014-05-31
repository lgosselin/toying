TODO: Improve text clarity and improve formatting

JPA -> DDL -> JOOQ Codegen Maven Plugin
===
Using [JPA]? [Hibernate] in particular? Your entities are properly annotated so that your ORM can generate a DB schema readable by your DBA and your developers? You would like to use [JOOQ] but you do not need the DB first approach since your generated schema is ~~perfect~~ good enough? Maybe this maven plugin could get you started.

Introduction
---
There are some queries that need to be written in SQL, maybe even in a dialect of SQL for a given *RDBMS*. An *ORM* cannot be the right tool for everything related to the persistence layer.

Where SQL queries are involved, are you maybe constructing those queries by concatening Strings? It ~~can be~~ is bad but we have all done this at some point.

You may have your in-house framework for building SQL requests but if you don't (or want to get rid of it) you can checkout [JOOQ]. Basically it a DSL for creating and running SQL request. (It is more than that, go to their website as I don't do that project justice here). 

Where SQL query and code mix, you will at some point end up with queries out of sync with your code. If you have tests, maybe you will notice it before a stack in production. Or you won't because the query is built dynamically and not all possibilities are tested.

For me the biggest interest for your code & requests maintenance that JOOQ can offer would be:
- type safety
- detecting at compile time where SQL requests and code are out of sync

For this to work you obviously need some metadata of your DB schema transformed into code so that the compiler can do the heavy lifting. JOOQ provides a code generator for that purpose.

However JOOQ approach is _**DB first**_ : You need a reference DB available to generate the code that will help with type safety, tables name, columns name, and so on. If you are working on several branches, you need one per branch. If you have several developers, they all have their own reference database (per branch) slightly ahead of the committed reference (and maybe out of sync too). Then your build server must be able to build a reference DB to produce the right code for your release. The development environment can become rather difficult to maintain and use, especially given that not all your developers are DBA and you might not want to allow modification to your reference DB to anyone.

If your project mainly use an ORM for CRUD and (not too complex) querying, you already have a bunch of _Entity_ classes. If you are considerate enough, you carefully tune your annotations to hint your ORM schema generator to produce a readable schema with all the needed **named indexes**, **named foreign keys**, **named unique keys**, and all the column properties correctly set (**(not) nullable**, field **length**). JPA annotations (although numerous) are not sufficient and you use proprietary annotation to finish the job. (Hibernate: @Index, @ForeignKey(name=...), etc.)

Now would it not be great if JOOQ code generator could harness the information you put in place so diligently? But it does not! It would be quite a rather large development to process the complete JPA annotations madness. Not to mention JPA annotations alone are not expressive enough so you would have to support proprietary extensions from different ORM vendors...

So? If your ORM schema generator works for your project, let's chain it with JOOQ code generator then!

Bridge Hibernate Schema Generator and JOOQ code generator
---
Initial situation:
- A maven module
- Java code contains entities, possibly with references to other entities in other maven modules
- A piece of code written in JOOQ and dependent on generated metadata code.

Desired situation:
- Any modification to an entity is "directly" propagated into JOOQ generated code
- JOOQ generated code is not committed in the VSC

By "directly", I mean "_mvn generate-sources_" produces an updated version of JOOQ code for table, columns, indexes, ... required for type safety without requiring to alter a DB anywhere.

How to?
--- 
In a nutshell we need:
- Compiled Entities
- Call Hibernate's schema generator and have it populate an in-memory DB
- Call JOOQ code generator on the in-memory DB and have it generate the DB metadata code

Since Hibernate schema generator requires the compiled entities, let's wire a first pass compilation to have them compiled:
```maven 
			<!-- <project><build><plugins> -->
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-compiler-plugin</artifactId>
				<configuration>
			        <!-- omitted for brevity -->
				</configuration>
				<executions>
					<execution>
						<id>first-pass</id>
						<phase>generate-sources</phase>
						<goals>
							<goal>compile</goal>
						</goals>
						<configuration>
							<failOnError>false</failOnError>
							<showWarnings>false</showWarnings>
							<showDeprecation>false</showDeprecation>
							<includes>
								<include>**/persistence/**/*.java</include>
							</includes>
						</configuration>
					</execution>
				</executions>
			</plugin>
```
The goal to is compile @Entity classes. We narrow the included sources to only include packages containing "persistence". The classes located in these packages must compile because ```maven-compiler-plugin``` won't write any class if an error occurs. Thus we cannot have code dependency on JOOQ generated sources in these classes. (This should not be a very harsh requirement).

Once we have them, we can: 
- Call Hibernate's schema generator and have it populate an in-memory DB (H2)
- Call JOOQ code generator on the in-memory DB and have it generate the DB metadata code

```maven
			<!-- <project><build><plugins> -->
			<plugin>
				<groupId>com.toying.database.jooq</groupId>
				<artifactId>jooq-jpa-bridge-codegen-maven-plugin</artifactId>
				<executions>
					<execution>
						<phase>generate-sources</phase>
						<goals>
							<goal>generate</goal>
						</goals>
						<configuration>
							<scanPackages>
								<scanPackage>com.toying</scanPackage>
							</scanPackages>
							<!-- <mappingResources>
								<mappingResource>defaults-orm.xml</mappingResource>
							</mappingResources> -->
							<packageName>com.toying.database.meta.jooq</packageName>
						</configuration>
					</execution>
				</executions>
			</plugin>
```
During the _generate-sources_ phase, this snippet executes Hibernate Schema Generator on classes which have their package starting with "_com.toying_", fill an in-memory DB, run JOOQ codegen which produce its output with a default package name. Default output directory is _target/generated-sources/jooq_. 

Then you tell maven (and your IDE at the same time) there is a new directory of sources to compile.
```maven
			<plugin>
				<groupId>org.codehaus.mojo</groupId>
				<artifactId>build-helper-maven-plugin</artifactId>
				<executions>
					<execution>
						<id>add-source</id>
						<phase>generate-sources</phase>
						<goals>
							<goal>add-source</goal>
						</goals>
						<configuration>
							<sources>
								<source>target/generated-sources/jooq</source>
							</sources>
						</configuration>
					</execution>
				</executions>
			</plugin>
```
That's all you need. 

In this repo, you'll find the code of the maven plugin as well as a sample module preconfigured.

Have fun!

[JOOQ]:http://www.jooq.org
[JPA]:http://www.oracle.com/technetwork/java/javaee/tech/persistence-jsp-140049.html
[Hibernate]:http://hibernate.org/