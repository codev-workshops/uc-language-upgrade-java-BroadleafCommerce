# Phase 0 - Toolchain & Root POM

**Status:** Complete (this PR)

## Scope

Upgrade the root build configuration so the entire reactor runs under JDK 17 with Spring 5.3, Hibernate 5.6, and Thymeleaf 3 dependency management.

## Changes Made

### pluginManagement
- `maven-compiler-plugin` 3.1 -> 3.11.0; replaced `<source>1.7</source>/<target>1.7</target>` with `<release>17</release>`
- `maven-surefire-plugin` 2.10 -> 3.2.5
- `gmavenplus-plugin` 1.5 -> 3.0.2; removed `<targetBytecode>1.7</targetBytecode>`

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

### Build plugins
- Removed `animal-sniffer-maven-plugin` with `org.codehaus.mojo.signature:java17:1.0`

### integration/pom.xml
- Removed `-XX:MaxPermSize=512m` from all surefire argLine entries

## Verification

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
mvn -DskipTests clean install
```

Compilation failures in module source are expected and will be fixed in Phases 1-7.
