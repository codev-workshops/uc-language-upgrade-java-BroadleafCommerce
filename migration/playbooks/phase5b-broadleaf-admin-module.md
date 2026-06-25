# Phase 5b - broadleaf-admin-module

## Target Module

`admin/broadleaf-admin-module/` (`org.broadleafcommerce:broadleaf-admin-module`)

## Prerequisites

- Phase 4 (broadleaf-framework) complete

## Dependency / Version Edits

- Verify all versions resolve from parent

## Import-Rename Map

| Old Import | New Import |
|---|---|
| `org.hibernate.ejb.*` | `org.hibernate.jpa.*` |
| `org.hibernate.criterion.*` | JPA `CriteriaBuilder` / `CriteriaQuery` |
| `org.thymeleaf.spring4.*` | `org.thymeleaf.spring5.*` |

## Key Refactoring Tasks

- Admin custom persistence handlers using old Hibernate APIs
- Hibernate criteria -> JPA criteria in admin DAOs
- Spring Security admin filter chain configuration
- Review admin controller classes for Spring MVC 5.3 changes

## Regression Command

```bash
mvn -pl admin/broadleaf-admin-module -am test
```

## New-Test Requirements

- Verify admin persistence handlers work under Hibernate 5.6
- Verify admin security configuration initializes under Spring Security 5.8
