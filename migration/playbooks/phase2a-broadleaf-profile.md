# Phase 2a - broadleaf-profile

**Status:** Pending

## Target Module

`core/broadleaf-profile/` (`org.broadleafcommerce:broadleaf-profile`)

## Prerequisites

- Phase 1 (broadleaf-common) complete and merged

## Environment Setup

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

## Dependency / Version Edits

Add to `core/broadleaf-profile/pom.xml`:

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

Add `--add-opens` to surefire `<argLine>` (same pattern as common/pom.xml):
```
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.lang.reflect=ALL-UNNAMED
--add-opens java.base/java.util=ALL-UNNAMED
```

## Import-Rename Map

| Old Import | New Import | Affected Files |
|---|---|---|
| `org.hibernate.ejb.*` | `org.hibernate.jpa.*` | ChallengeQuestionDaoImpl, CustomerDaoImpl, CountryDaoImpl, CountrySubdivisionDaoImpl, StateDaoImpl |
| `org.springframework.security.authentication.encoding.PasswordEncoder` | `org.springframework.security.crypto.password.PasswordEncoder` | CustomerService, CustomerServiceImpl |

## Identified Files Requiring Changes

From source scan (5 files with `org.hibernate.ejb` imports):
1. `profile/core/dao/ChallengeQuestionDaoImpl.java` - Hibernate ejb -> jpa
2. `profile/core/dao/CustomerDaoImpl.java` - Hibernate ejb -> jpa
3. `profile/core/dao/CountryDaoImpl.java` - Hibernate ejb -> jpa
4. `profile/core/dao/CountrySubdivisionDaoImpl.java` - Hibernate ejb -> jpa
5. `profile/core/dao/StateDaoImpl.java` - Hibernate ejb -> jpa

From source scan (2 files with old PasswordEncoder):
6. `profile/core/service/CustomerService.java` - PasswordEncoder interface change
7. `profile/core/service/CustomerServiceImpl.java` - PasswordEncoder implementation change

## Key Refactoring Tasks

### 1. Hibernate ejb -> jpa (Simple Rename)

Same pattern as Phase 1. These are typically just import renames:
```java
// OLD:
import org.hibernate.ejb.QueryHints;
// NEW:
import org.hibernate.jpa.QueryHints;
```

### 2. PasswordEncoder Migration

Spring Security 5.x completely removed the old `PasswordEncoder` interface from `org.springframework.security.authentication.encoding`. The new interface is `org.springframework.security.crypto.password.PasswordEncoder`.

**Critical difference:** The old interface had `encodePassword(rawPass, salt)` and `isPasswordValid(encPass, rawPass, salt)`. The new interface has `encode(rawPassword)` and `matches(rawPassword, encodedPassword)` (note: parameter order is reversed for the validation method). The new interface handles salting internally (e.g., BCrypt generates its own salt).

```java
// OLD:
import org.springframework.security.authentication.encoding.PasswordEncoder;
passwordEncoder.encodePassword(rawPassword, salt);
passwordEncoder.isPasswordValid(storedPassword, rawPassword, salt);

// NEW:
import org.springframework.security.crypto.password.PasswordEncoder;
passwordEncoder.encode(rawPassword);
passwordEncoder.matches(rawPassword, storedPassword);  // Note: reversed param order!
```

### 3. Additional Patterns (from Phase 1 Lessons)

Check for and fix if present:
- `SCOPE_GLOBAL_SESSION` -> `SCOPE_SESSION` (search all files)
- `org.springframework.security.web.util.RequestMatcher` -> `org.springframework.security.web.util.matcher.RequestMatcher`
- Any `HandlerInterceptorAdapter` -> `HandlerInterceptor`

## Regression Command

```bash
mvn -pl core/broadleaf-profile -am test
```

## New-Test Requirements

- Verify password encoding/matching works with the new `PasswordEncoder` interface
- Verify customer CRUD operations compile and pass under Hibernate 5.6
