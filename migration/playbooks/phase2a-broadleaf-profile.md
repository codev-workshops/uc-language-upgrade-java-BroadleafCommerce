# Phase 2a - broadleaf-profile

## Target Module

`core/broadleaf-profile/` (`org.broadleafcommerce:broadleaf-profile`)

## Prerequisites

- Phase 1 (broadleaf-common) complete

## Dependency / Version Edits

- Verify all Hibernate / Spring / Thymeleaf versions resolve from parent
- Remove any module-level overrides for removed dependencies (asm, cglib, hibernate-jpa-2.0-api)

## Import-Rename Map

| Old Import | New Import |
|---|---|
| `org.hibernate.ejb.*` | `org.hibernate.jpa.*` |
| `org.hibernate.criterion.*` (Criteria) | Migrate to JPA `CriteriaBuilder` / `CriteriaQuery` |
| `org.thymeleaf.spring4.*` | `org.thymeleaf.spring5.*` |
| `org.springframework.security.authentication.encoding.*` | `org.springframework.security.crypto.password.*` |

## Key Refactoring Tasks

- `PasswordEncoder` migration: Spring Security 5.x removed the old `PasswordEncoder` interface from `o.s.s.authentication.encoding`; migrate to `o.s.s.crypto.password.PasswordEncoder`
- Hibernate `Session.createCriteria()` deprecation: replace with JPA criteria API where used in DAOs
- Review `CustomerService` and `RoleService` for Spring Security API changes

## Regression Command

```bash
mvn -pl core/broadleaf-profile -am test
```

## New-Test Requirements

- Verify password encoding/matching works with the new `PasswordEncoder` interface
- Verify customer CRUD operations compile and pass under Hibernate 5.6
