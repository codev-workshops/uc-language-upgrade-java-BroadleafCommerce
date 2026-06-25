# Phase 3b - broadleaf-contentmanagement-module

## Target Module

`admin/broadleaf-contentmanagement-module/` (`org.broadleafcommerce:broadleaf-contentmanagement-module`)

## Prerequisites

- Phase 2a (broadleaf-profile) complete
- Phase 2b (broadleaf-open-admin-platform) complete

## Dependency / Version Edits

- Verify all versions resolve from parent
- Review any Solr version-specific API usage (Solr 4.x -> 8.x)

## Import-Rename Map

| Old Import | New Import |
|---|---|
| `org.hibernate.ejb.*` | `org.hibernate.jpa.*` |
| `org.hibernate.criterion.*` | JPA `CriteriaBuilder` / `CriteriaQuery` |
| `org.thymeleaf.spring4.*` | `org.thymeleaf.spring5.*` |

## Key Refactoring Tasks

- Hibernate criteria queries in content DAOs -> JPA criteria
- Custom persistence handlers for structured content
- Thymeleaf processor changes for content rendering (Thymeleaf 3 API)
- Solr-based content indexing if present (SolrJ 8.x API changes)

## Regression Command

```bash
mvn -pl admin/broadleaf-contentmanagement-module -am test
```

## New-Test Requirements

- Verify structured content CRUD under Hibernate 5.6
- Verify content Thymeleaf processors under Thymeleaf 3
