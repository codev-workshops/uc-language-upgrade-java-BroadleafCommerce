# Phase 0 - Toolchain & Root POM

**Status:** Complete

## Scope

Upgrade the root build configuration so the entire reactor runs under JDK 17 with Spring 5.3, Hibernate 5.6, and Thymeleaf 3 dependency management.

## Changes Made

### pluginManagement
- `maven-compiler-plugin` 3.1 -> 3.11.0; replaced `<source>1.7</source>/<target>1.7</target>` with `<release>17</release>`
- `maven-surefire-plugin` 2.10 -> 3.2.5
- `gmavenplus-plugin` 1.5 -> 3.0.2; removed `<targetBytecode>1.7</targetBytecode>`; renamed goals from `testCompile` to `compileTests`, `addTestSources` to `addTestSources`

### Properties
- `spring.version` 4.1.9.RELEASE -> 5.3.39
- `spring.security.version` 3.2.9.RELEASE -> 5.8.14
- `thymeleaf.version` 2.1.4.RELEASE -> 3.1.2.RELEASE
- `groovy.version` 2.3.10 -> 4.0.21
- `spock.core.version` / `spock.spring.version` 1.0-groovy-2.3 -> 2.3-groovy-4.0
- `selenium.version` 2.43.1 -> 4.18.1
- `geb.version` 0.10.0 -> 5.1

### dependencyManagement
- Hibernate 4.1.11.Final -> 5.6.15.Final (hibernate-core, hibernate-entitymanager, hibernate-envers, hibernate-ehcache)
- Removed `hibernate-jpa-2.0-api` (included transitively by Hibernate 5.6)
- `thymeleaf-spring4` -> `thymeleaf-spring5`
- `thymeleaf-layout-dialect` 1.2.5 -> 3.3.0
- Removed `asm:asm:3.3`, `asm:asm-commons:3.3`, `cglib:cglib-nodep:2.1_3`
- `javassist` 3.17.1-GA -> 3.29.2-GA
- Solr 4.10.3 -> 8.11.3
- `ehcache` 2.7.2 -> 2.10.9.2
- `quartz` 2.2.0 -> 2.3.2
- `log4j` 1.2.12 -> 1.2.17
- `groovy-all` groupId `org.codehaus.groovy` -> `org.apache.groovy`
- `javax.annotation-api:1.3.2` added (JDK 17 removed javax.annotation from JRE)
- JaCoCo 0.7.9 -> 0.8.11 (old version crashes on Java 17 class instrumentation)
- JUnit 4.11 -> 4.13.2 (required for junit-vintage-engine compatibility)
- `junit-platform-engine:1.10.2` and `junit-platform-commons:1.10.2` added (aligns Spock 2.3's transitive JUnit 5 deps)

### Build plugins
- Removed `animal-sniffer-maven-plugin` with `org.codehaus.mojo.signature:java17:1.0`

### integration/pom.xml
- Removed `-XX:MaxPermSize=512m` from all surefire argLine entries

### Module POM coordinate fixups (not source changes)
- Updated groovy groupId to `org.apache.groovy` with `<type>pom</type>` across all modules
- `thymeleaf-spring4` -> `thymeleaf-spring5` in all module POMs
- `hibernate-jpa-2.0-api` -> `javax.persistence-api` in all module POMs
- `asm:asm` -> `org.ow2.asm:asm` across all modules

## Verification

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
mvn -DskipTests clean install
```

Compilation failures in module source are expected and will be fixed in Phases 1-7.

## Lessons Learned

1. **JAVA_HOME must be set explicitly** - the environment defaults to JDK 8, which does not support `--release 17`. Always run:
   ```bash
   export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
   export PATH=$JAVA_HOME/bin:$PATH
   ```
2. **JaCoCo 0.7.9 is completely broken on Java 17** - it tries to inject `$jacocoAccess` into `java.util.UUID` which fails with `NoSuchFieldException` under Java 17's module system. Upgraded to 0.8.11.
3. **JUnit Platform version alignment is critical** - Spock 2.3 transitively pulls `junit-platform-engine:1.9.0` via its own dependency, while Groovy 4.0.21 pulls `junit-platform-launcher:1.10.2`. The `getAncestors()` method on `TestDescriptor` only exists in 1.10.x, causing `NoSuchMethodError` at test discovery time. Fixed by adding `junit-platform-engine:1.10.2` and `junit-platform-commons:1.10.2` to dependencyManagement.
4. **junit-vintage-engine required** - Surefire 3.2.5 auto-detects JUnit Platform provider when JUnit 5 is on classpath. JUnit 4 tests are not discovered without `junit-vintage-engine:5.10.2` on the test classpath. Each module with JUnit 4 tests must add this dependency.
5. **javax.annotation-api must be an explicit dependency** - JDK 17 removed `javax.annotation` from the JRE (it was part of java.se.ee which was removed in JDK 11). Every module using `@Resource`, `@PostConstruct`, or `@PreDestroy` needs `javax.annotation:javax.annotation-api:1.3.2` as a compile dependency.
