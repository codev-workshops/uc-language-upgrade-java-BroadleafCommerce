# Phase 4 - broadleaf-framework

**Status:** Pending

## Target Module

`core/broadleaf-framework/` (`org.broadleafcommerce:broadleaf-framework`)

## Prerequisites

- Phase 3a (broadleaf-profile-web) complete
- Phase 3b (broadleaf-contentmanagement-module) complete

## Environment Setup

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

## Dependency / Version Edits

Add to `core/broadleaf-framework/pom.xml`:

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
| `org.hibernate.ejb.*` | `org.hibernate.jpa.*` | All DAO files below |
| `org.hibernate.criterion.*` | JPA `CriteriaBuilder` / `CriteriaQuery` | DAO files that use Session.createCriteria() |
| `org.apache.solr.client.solrj.impl.HttpSolrServer` | `org.apache.solr.client.solrj.impl.HttpSolrClient` | Solr service files |
| `org.apache.solr.client.solrj.SolrServer` | `org.apache.solr.client.solrj.SolrClient` | Solr service files |

## Identified Files Requiring Changes

### Hibernate ejb -> jpa + Criteria (11 files):
1. `core/catalog/dao/CategoryDaoImpl.java` - Hibernate ejb + criterion
2. `core/catalog/dao/ProductDaoImpl.java` - Hibernate ejb + criterion
3. `core/catalog/dao/ProductOptionDaoImpl.java` - Hibernate ejb + criterion
4. `core/catalog/dao/SkuDaoImpl.java` - Hibernate ejb + criterion
5. `core/offer/dao/CustomerOfferDaoImpl.java` - Hibernate ejb + criterion
6. `core/offer/dao/OfferCodeDaoImpl.java` - Hibernate ejb + criterion
7. `core/offer/dao/OfferDaoImpl.java` - Hibernate ejb + criterion
8. `core/order/dao/OrderDaoImpl.java` - Hibernate ejb + criterion
9. `core/rating/dao/RatingSummaryDaoImpl.java` - Hibernate criterion
10. `core/search/dao/FieldDaoImpl.java` - Hibernate criterion
11. `core/search/dao/SearchFacetDaoImpl.java` - Hibernate criterion
12. `core/search/redirect/dao/SearchRedirectDaoImpl.java` - Hibernate criterion
13. `core/store/dao/StoreDaoImpl.java` - Hibernate criterion

### Solr 4.x -> 8.x (6 files):
14. `core/search/service/solr/SolrSearchServiceImpl.java` - HttpSolrServer -> HttpSolrClient
15. `core/search/service/solr/SolrIndexServiceImpl.java` - SolrServer -> SolrClient
16. `core/search/service/solr/SolrHelperServiceImpl.java` - SolrServer -> SolrClient
17. `core/search/service/solr/SolrIndexService.java` - SolrServer -> SolrClient (interface)
18. `core/search/service/solr/SolrHelperService.java` - SolrServer -> SolrClient (interface)
19. `core/search/service/solr/SolrContext.java` - SolrServer -> SolrClient

## Key Refactoring Tasks

### 1. Solr Migration (Biggest Change in This Module)

SolrJ 8.x renames and API changes:

```java
// OLD:
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
SolrServer server = new HttpSolrServer("http://localhost:8983/solr");

// NEW:
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
SolrClient client = new HttpSolrClient.Builder("http://localhost:8983/solr").build();
```

**Key differences:**
- `HttpSolrServer` -> `HttpSolrClient` (builder pattern for construction)
- `SolrServer` -> `SolrClient` (base class renamed)
- `EmbeddedSolrServer` constructor may have changed - check carefully
- `SolrQuery` API is largely stable
- Response parsing should be mostly compatible
- `server.shutdown()` -> `client.close()` (implements `Closeable`)

### 2. Hibernate Criteria Migration

This module has the most `Session.createCriteria()` usage. The Hibernate Criteria API is deprecated in 5.x. Migrate to JPA Criteria API:

```java
// OLD (Hibernate Criteria):
Session session = em.unwrap(Session.class);
Criteria criteria = session.createCriteria(ProductImpl.class);
criteria.add(Restrictions.eq("name", name));
criteria.add(Restrictions.between("startDate", start, end));
criteria.addOrder(Order.asc("name"));
List<Product> results = criteria.list();

// NEW (JPA Criteria):
CriteriaBuilder cb = em.getCriteriaBuilder();
CriteriaQuery<Product> cq = cb.createQuery(Product.class);
Root<ProductImpl> root = cq.from(ProductImpl.class);
cq.where(
    cb.equal(root.get("name"), name),
    cb.between(root.get("startDate"), start, end)
);
cq.orderBy(cb.asc(root.get("name")));
List<Product> results = em.createQuery(cq).getResultList();
```

**Common Restrictions mappings:**
| Hibernate Criteria | JPA CriteriaBuilder |
|---|---|
| `Restrictions.eq(prop, val)` | `cb.equal(root.get(prop), val)` |
| `Restrictions.ne(prop, val)` | `cb.notEqual(root.get(prop), val)` |
| `Restrictions.like(prop, val)` | `cb.like(root.get(prop), val)` |
| `Restrictions.in(prop, vals)` | `root.get(prop).in(vals)` |
| `Restrictions.between(prop, lo, hi)` | `cb.between(root.get(prop), lo, hi)` |
| `Restrictions.isNull(prop)` | `cb.isNull(root.get(prop))` |
| `Restrictions.or(c1, c2)` | `cb.or(c1, c2)` |
| `Restrictions.and(c1, c2)` | `cb.and(c1, c2)` |
| `Order.asc(prop)` | `cb.asc(root.get(prop))` |
| `Order.desc(prop)` | `cb.desc(root.get(prop))` |
| `criteria.setMaxResults(n)` | `query.setMaxResults(n)` |
| `criteria.setFirstResult(n)` | `query.setFirstResult(n)` |

### 3. Additional Patterns (from Phase 1 Lessons)

Always run a full compile first and systematically fix errors. Common unexpected issues:
- `SCOPE_GLOBAL_SESSION` -> `SCOPE_SESSION`
- `RequestMatcher` package move
- Removed Spring utility classes (`VelocityEngineUtils`, `Log4jConfigurer`)
- `CacheKey` removed from Hibernate 5.6 (use raw key)
- `SessionImplementor` -> `SharedSessionContractImplementor`

## Regression Command

```bash
mvn -pl core/broadleaf-framework -am test
```

## New-Test Requirements

- Verify Solr indexing and search with SolrJ 8.x client
- Verify catalog/order CRUD under Hibernate 5.6
- Verify offer/pricing workflows execute correctly under Spring 5.3
