# Phase 5b - broadleaf-admin-module

**Status:** Pending

## Target Module

`admin/broadleaf-admin-module/` (`org.broadleafcommerce:broadleaf-admin-module`)

## Prerequisites

- Phase 4 (broadleaf-framework) complete

## Environment Setup

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

## Dependency / Version Edits

Add to `admin/broadleaf-admin-module/pom.xml`:

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
| `org.hibernate.ejb.*` | `org.hibernate.jpa.*` | (scan all DAO files) |
| `org.hibernate.criterion.*` | JPA `CriteriaBuilder` / `CriteriaQuery` | SkuCustomPersistenceHandler |
| `org.hibernate.engine.spi.SessionImplementor` | `org.hibernate.engine.spi.SharedSessionContractImplementor` | (if present) |

## Identified Files Requiring Changes

From source scan:
1. `admin/server/service/handler/SkuCustomPersistenceHandler.java` - Hibernate criterion usage

Run `mvn -pl admin/broadleaf-admin-module -am compile` to discover the full list.

## Key Refactoring Tasks

### 1. Hibernate Criteria in Admin Persistence Handlers

Same Hibernate Criteria -> JPA Criteria migration as Phase 4 (see Phase 4 playbook for the complete Restrictions mapping table).

```java
// OLD:
Session session = em.unwrap(Session.class);
Criteria criteria = session.createCriteria(Entity.class);
criteria.add(Restrictions.eq("field", value));

// NEW:
CriteriaBuilder cb = em.getCriteriaBuilder();
CriteriaQuery<Entity> cq = cb.createQuery(Entity.class);
Root<Entity> root = cq.from(Entity.class);
cq.where(cb.equal(root.get("field"), value));
em.createQuery(cq).getResultList();
```

### 2. Spring Security Admin Configuration

Review admin filter chain configuration for Spring Security 5.8 changes:
- `RequestMatcher` / `AntPathRequestMatcher` package move (from `o.s.s.web.util` to `o.s.s.web.util.matcher`)
- CSRF configuration
- Admin authentication providers

### 3. Additional Patterns (from Phase 1 Lessons)

Always run `mvn -pl admin/broadleaf-admin-module -am compile` first and fix errors systematically. Common unexpected issues:
- `SCOPE_GLOBAL_SESSION` -> `SCOPE_SESSION`
- Removed Spring utility classes
- `javax.annotation` imports needing `javax.annotation-api` dependency

## Regression Command

```bash
mvn -pl admin/broadleaf-admin-module -am test
```

## New-Test Requirements

- Verify admin persistence handlers work under Hibernate 5.6
- Verify admin security configuration initializes under Spring Security 5.8
