# Phase 4 - broadleaf-framework

## Target Module

`core/broadleaf-framework/` (`org.broadleafcommerce:broadleaf-framework`)

## Prerequisites

- Phase 3a (broadleaf-profile-web) complete
- Phase 3b (broadleaf-contentmanagement-module) complete

## Dependency / Version Edits

- Verify all versions resolve from parent
- Solr 4.10.3 -> 8.11.3: major API changes in `SolrClient`, `EmbeddedSolrServer`
- Review module-level dependency overrides

## Import-Rename Map

| Old Import | New Import |
|---|---|
| `org.hibernate.ejb.*` | `org.hibernate.jpa.*` |
| `org.hibernate.criterion.*` | JPA `CriteriaBuilder` / `CriteriaQuery` |
| `org.thymeleaf.spring4.*` | `org.thymeleaf.spring5.*` |
| `org.apache.solr.client.solrj.impl.HttpSolrServer` | `org.apache.solr.client.solrj.impl.HttpSolrClient` |
| `org.apache.solr.client.solrj.embedded.EmbeddedSolrServer` | (same, but constructor API changed) |

## Key Refactoring Tasks

- **Solr Migration** (biggest change in this module):
  - `HttpSolrServer` -> `HttpSolrClient` (builder pattern)
  - `EmbeddedSolrServer` constructor changes
  - `SolrQuery` API largely stable, but response parsing may differ
  - `SolrServer` base class -> `SolrClient`
- Hibernate criteria queries in catalog/order/offer DAOs
- Spring transaction manager configuration
- Workflow/activity configuration for Spring 5.3
- MVEL expression evaluation (verify mvel2 compatibility)

## Regression Command

```bash
mvn -pl core/broadleaf-framework -am test
```

## New-Test Requirements

- Verify Solr indexing and search with SolrJ 8.x client
- Verify catalog/order CRUD under Hibernate 5.6
- Verify offer/pricing workflows execute correctly under Spring 5.3
