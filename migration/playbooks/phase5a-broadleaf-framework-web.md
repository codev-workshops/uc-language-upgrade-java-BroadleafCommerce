# Phase 5a - broadleaf-framework-web

## Target Module

`core/broadleaf-framework-web/` (`org.broadleafcommerce:broadleaf-framework-web`)

## Prerequisites

- Phase 4 (broadleaf-framework) complete

## Dependency / Version Edits

- Verify all versions resolve from parent

## Import-Rename Map

| Old Import | New Import |
|---|---|
| `org.hibernate.ejb.*` | `org.hibernate.jpa.*` |
| `org.thymeleaf.spring4.*` | `org.thymeleaf.spring5.*` |
| `org.thymeleaf.dom.*` | `org.thymeleaf.model.*` |
| `org.thymeleaf.processor.element.AbstractElementProcessor` | `org.thymeleaf.processor.element.AbstractElementTagProcessor` |

## Key Refactoring Tasks

- **Thymeleaf 3 Processors**: This module contains many custom Thymeleaf processors for storefront rendering:
  - All `AbstractElementProcessor` subclasses -> `AbstractElementTagProcessor`
  - `Arguments` parameter -> `ITemplateContext`
  - DOM manipulation -> `IModelFactory` / `IModel`
  - `ProcessorResult` -> void return with model modification
- Spring MVC: `HandlerInterceptorAdapter` -> `HandlerInterceptor`
- `WebMvcConfigurerAdapter` -> `WebMvcConfigurer`
- Review REST endpoint controllers for Spring 5.3 changes

## Regression Command

```bash
mvn -pl core/broadleaf-framework-web -am test
```

## New-Test Requirements

- Verify storefront Thymeleaf processors render correctly under Thymeleaf 3
- Verify REST API endpoints compile under Spring 5.3 MVC
