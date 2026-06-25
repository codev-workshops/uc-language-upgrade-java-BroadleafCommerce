# Phase 6 - integration

## Target Module

`integration/` (`org.broadleafcommerce:integration`)

## Prerequisites

- Phase 5a (broadleaf-framework-web) complete
- Phase 5b (broadleaf-admin-module) complete

## Dependency / Version Edits

- Update `javax.servlet-api` 3.0.1 -> 3.1.0 (Servlet 3.1+ for Tomcat 9 compatibility)
- Verify all test dependencies resolve from parent (TestNG, Spock, Groovy)
- Groovy dependency groupId: `org.codehaus.groovy` -> `org.apache.groovy` in module-level overrides
- `gmavenplus-plugin` version 1.2 override -> remove (inherit 3.0.2 from parent)

## Import-Rename Map

| Old Import | New Import |
|---|---|
| `org.hibernate.ejb.*` | `org.hibernate.jpa.*` |
| `org.hibernate.criterion.*` | JPA `CriteriaBuilder` / `CriteriaQuery` |

## Key Refactoring Tasks

- Update test Spring context configurations for Spring 5.3
- Update Hibernate configuration in test persistence.xml files
- Fix any test-specific Hibernate criteria usage
- Ensure TestNG test suites work with Surefire 3.2.5
- Spock 2.x uses JUnit Platform; verify TestNG + Spock coexistence

## Regression Command

```bash
mvn -pl integration -am test
```

## New-Test Requirements

- All 159 existing integration tests must pass
- No new tests required beyond fixing existing ones
