# Phase 3a - broadleaf-profile-web

**Status:** Pending

## Target Module

`core/broadleaf-profile-web/` (`org.broadleafcommerce:broadleaf-profile-web`)

## Prerequisites

- Phase 2a (broadleaf-profile) complete
- Phase 2b (broadleaf-open-admin-platform) complete

## Environment Setup

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

## Dependency / Version Edits

Add to `core/broadleaf-profile-web/pom.xml`:

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

Add `--add-opens` to surefire `<argLine>`.

## Import-Rename Map

| Old Import | New Import | Affected Files |
|---|---|---|
| `org.hibernate.ejb.*` | `org.hibernate.jpa.*` | (scan for any remaining) |
| `org.springframework.web.servlet.handler.HandlerInterceptorAdapter` | `org.springframework.web.servlet.HandlerInterceptor` | (if present) |
| `org.springframework.security.web.util.RequestMatcher` | `org.springframework.security.web.util.matcher.RequestMatcher` | (if present) |
| `org.springframework.security.web.util.AntPathRequestMatcher` | `org.springframework.security.web.util.matcher.AntPathRequestMatcher` | (if present) |
| `WebRequest.SCOPE_GLOBAL_SESSION` | `WebRequest.SCOPE_SESSION` | CustomerStateRequestProcessor, CustomerStateRefresher |

## Identified Files Requiring Changes

From source scan:
1. `profile/web/core/CustomerStateRefresher.java` - SCOPE_GLOBAL_SESSION -> SCOPE_SESSION
2. `profile/web/core/security/CustomerStateRequestProcessor.java` - SCOPE_GLOBAL_SESSION -> SCOPE_SESSION

## Key Refactoring Tasks

### 1. SCOPE_GLOBAL_SESSION -> SCOPE_SESSION

Same fix as Phase 1. Spring 5.3 removed `SCOPE_GLOBAL_SESSION`:
```java
// OLD:
request.setAttribute(key, value, WebRequest.SCOPE_GLOBAL_SESSION);
// NEW:
request.setAttribute(key, value, WebRequest.SCOPE_SESSION);
```

### 2. Spring MVC Changes

Check for and fix if present:
- `WebMvcConfigurerAdapter` -> `WebMvcConfigurer` (the adapter was deprecated; the interface now has default methods)
- `HandlerInterceptorAdapter` -> `HandlerInterceptor`

### 3. Spring Security Filter Chain

Review login/registration controllers for Spring Security 5.x API changes:
- `AuthenticationSuccessHandler` / `AuthenticationFailureHandler` interfaces are stable but check for any deprecated methods
- Verify CSRF token handling is compatible with Spring Security 5.8

### 4. Additional Patterns (from Phase 1 Lessons)

Always run a full compile first (`mvn -pl core/broadleaf-profile-web -am compile`) and systematically fix any additional errors not in this list. Phase 1 revealed ~24 files with errors beyond the initial checklist.

## Regression Command

```bash
mvn -pl core/broadleaf-profile-web -am test
```

## New-Test Requirements

- Verify web-tier security filters compile and initialize under Spring Security 5.8
