# Phase 6 - integration

**Status:** Pending

## Target Module

`integration/` (`org.broadleafcommerce:integration`)

## Prerequisites

- Phase 5a (broadleaf-framework-web) complete
- Phase 5b (broadleaf-admin-module) complete

## Environment Setup

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

## Dependency / Version Edits

Changes needed in `integration/pom.xml`:

```xml
<dependency>
    <groupId>javax.annotation</groupId>
    <artifactId>javax.annotation-api</artifactId>
</dependency>
<dependency>
    <groupId>org.junit.vintage</groupId>
    <artifactId>junit-vintage-engine</artifactId>
    <version>5.10.2</version>
    <scope>test</scope>
</dependency>
```

- `javax.servlet-api` 3.0.1 -> 3.1.0 (if overridden at module level)
- Remove any local `gmavenplus-plugin` version override (inherit 3.0.2 from parent)
- Groovy dependency groupId: verify `org.apache.groovy` is used (already fixed in Phase 0)
- Add `--add-opens` to surefire `<argLine>` (MaxPermSize was already removed in Phase 0)

**Critical: TestNG + JUnit coexistence.** The integration module uses TestNG (not JUnit). Surefire 3.2.5 with JUnit Platform provider auto-detection may conflict with TestNG. You may need to explicitly configure the surefire provider:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <dependencies>
        <dependency>
            <groupId>org.apache.maven.surefire</groupId>
            <artifactId>surefire-testng</artifactId>
            <version>3.2.5</version>
        </dependency>
    </dependencies>
</plugin>
```

## Import-Rename Map

| Old Import | New Import | Affected Files |
|---|---|---|
| `org.hibernate.ejb.*` | `org.hibernate.jpa.*` | (scan all test files) |
| `org.hibernate.criterion.*` | JPA `CriteriaBuilder` / `CriteriaQuery` | (scan all test files) |
| `org.testng.annotations.*` | *(same - TestNG is still used)* | All test files |

## Identified Files Requiring Changes

This module contains ~50+ TestNG test files. The full list needs to be determined by running:
```bash
mvn -pl integration -am compile
mvn -pl integration -am test
```

Key test files that likely need changes (from source scan):
1. `common/config/RuntimeEnvironmentPropertiesManagerTest.java`
2. `common/currency/BroadleafCurrencyProvider.java`
3. `common/email/service/EmailTest.java`
4. `common/workflow/RollbackTest.java` / `WorkflowTest.java`
5. All `core/catalog/dao/*Test.java`
6. All `core/order/dao/*Test.java`
7. All `core/offer/service/*Test.java`
8. All `profile/web/core/service/*Test.java`
9. `profile/extensibility/ExtensibilityTest.java`

## Key Refactoring Tasks

### 1. Spring Context Configuration for Tests

Update test application context XML files to be compatible with Spring 5.3:
- Check for deprecated XML namespace versions
- Verify Spring bean definitions work with Spring 5.3 container
- Check for any `PropertyPlaceholderConfigurer` usage (deprecated -> use `PropertySourcesPlaceholderConfigurer`)

### 2. Hibernate Configuration in Test persistence.xml

- Verify `hibernate.dialect` settings are compatible with Hibernate 5.6
- Check for any removed Hibernate properties
- `hibernate.ejb.*` properties may need to be renamed to `hibernate.jpa.*`

### 3. TestNG + Surefire 3.2.5 Compatibility

Surefire 3.2.5 auto-detects JUnit Platform when JUnit 5 is on the classpath (transitively from Spock/Groovy). This can break TestNG test discovery. If tests aren't being discovered:
- Explicitly configure the TestNG surefire provider (see dependency edit above)
- Or exclude JUnit Platform from the classpath for this module

### 4. Spock 2.x Changes

Spock 2.x runs on JUnit Platform (not its own runner):
- `@Unroll` is now the default behavior
- Extension API may have changed
- Check Spock test suites if any exist in integration/

### 5. Additional Patterns (from Phase 1 Lessons)

Always compile first and fix errors systematically. Key patterns that may appear in test code:
- `SCOPE_GLOBAL_SESSION` -> `SCOPE_SESSION`
- `VelocityEngineUtils` removed
- `Log4jConfigurer` removed
- Hibernate Criteria API deprecated
- `SessionImplementor` -> `SharedSessionContractImplementor`

## Regression Command

```bash
mvn -pl integration -am test
```

## New-Test Requirements

- All existing integration tests must pass
- No new tests required beyond fixing existing ones
