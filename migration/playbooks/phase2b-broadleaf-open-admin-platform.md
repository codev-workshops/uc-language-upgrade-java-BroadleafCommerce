# Phase 2b - broadleaf-open-admin-platform

## Target Module

`admin/broadleaf-open-admin-platform/` (`org.broadleafcommerce:broadleaf-open-admin-platform`)

## Prerequisites

- Phase 1 (broadleaf-common) complete

## Dependency / Version Edits

- Verify all Hibernate / Spring / Thymeleaf versions resolve from parent
- Remove any module-level overrides for removed dependencies

## Import-Rename Map

| Old Import | New Import |
|---|---|
| `org.hibernate.ejb.*` | `org.hibernate.jpa.*` |
| `org.hibernate.criterion.*` | JPA `CriteriaBuilder` / `CriteriaQuery` |
| `org.thymeleaf.spring4.*` | `org.thymeleaf.spring5.*` |
| `org.thymeleaf.dom.*` | `org.thymeleaf.model.*` (Thymeleaf 3 model API) |
| `org.thymeleaf.processor.element.AbstractElementProcessor` | `org.thymeleaf.processor.element.AbstractElementTagProcessor` |

## Key Refactoring Tasks

- **Thymeleaf 3 Migration**: The admin platform has custom Thymeleaf processors/dialects. Thymeleaf 3 completely changed the processor API:
  - `IProcessor` hierarchy replaced; `AbstractElementProcessor` -> `AbstractElementTagProcessor`
  - DOM-based `Node`/`Element` replaced with `IModel`/`ITemplateEvent`
  - Dialect registration API changed
- Hibernate criteria -> JPA criteria in persistence handlers
- `CustomPersistenceHandler` implementations using old Hibernate APIs
- Spring MVC annotation changes (review `@RequestMapping` usage, `WebMvcConfigurerAdapter` -> `WebMvcConfigurer`)

## Regression Command

```bash
mvn -pl admin/broadleaf-open-admin-platform -am test
```

## New-Test Requirements

- Verify admin Thymeleaf dialect registers and processes templates under Thymeleaf 3
- Verify persistence handlers compile and handle CRUD under Hibernate 5.6
