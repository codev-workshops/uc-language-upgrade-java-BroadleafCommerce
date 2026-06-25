# Phase 1 - broadleaf-common

## Target Module

`common/` (`org.broadleafcommerce:broadleaf-common`)

## Prerequisites

- Phase 0 complete (root POM upgraded)

## Dependency / Version Edits

Module-level `pom.xml` changes (if any overrides exist in `common/pom.xml`):

- Ensure all Hibernate references resolve to 5.6.15.Final from parent
- Ensure Spring references resolve to 5.3.39 from parent
- Remove any module-level `hibernate-jpa-2.0-api` references
- Remove any direct `asm:asm` or `cglib:cglib-nodep` dependencies if overridden locally

## Import-Rename Map

| Old Import | New Import |
|---|---|
| `org.hibernate.ejb.HibernatePersistence` | `org.hibernate.jpa.HibernatePersistenceProvider` |
| `org.hibernate.ejb.*` | `org.hibernate.jpa.*` |
| `org.hibernate.engine.spi.SessionImplementor` | `org.hibernate.engine.spi.SharedSessionContractImplementor` (where applicable) |
| `org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter` | (same, but verify API changes) |
| `org.thymeleaf.spring4.*` | `org.thymeleaf.spring5.*` |

## Key Refactoring Tasks

### 1. MergePersistenceUnitManager

`org.broadleafcommerce.common.extensibility.jpa.MergePersistenceUnitManager`

- Currently uses reflection to access internals of Spring's `DefaultPersistenceUnitManager`
- **Rework**: Port to extend or delegate to `org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager` using its public API in Spring 5.3
- Spring 5.3's `DefaultPersistenceUnitManager` has broader hooks; use `postProcessPersistenceUnitInfo()` instead of reflective field access
- Verify `MutablePersistenceUnitInfo` compatibility

### 2. Javassist Bytecode Transformers

The following class transformers use javassist to manipulate bytecode at load time. They must be updated for Java 17 bytecode (class file version 61):

#### EntityMarkerClassTransformer
- `org.broadleafcommerce.common.extensibility.jpa.copy.EntityMarkerClassTransformer`
- Update javassist `ClassPool` / `CtClass` usage for bytecode version 61
- Ensure `--add-opens` JVM args are documented if needed for reflective access

#### SingleTableInheritanceClassTransformer
- `org.broadleafcommerce.common.extensibility.jpa.convert.inheritance.SingleTableInheritanceClassTransformer`
- Verify javassist 3.29.x handles the class file format correctly
- Test with entities using `@Inheritance(strategy = InheritanceType.SINGLE_TABLE)`

#### DirectCopyClassTransformer
- `org.broadleafcommerce.common.extensibility.jpa.copy.DirectCopyClassTransformer`
- This is the most complex transformer; it copies fields/methods from template entities at load time
- Validate `CtField.make()`, `CtMethod.make()`, `CtNewMethod.copy()` work with Java 17 class files
- Handle any new `javassist.CannotCompileException` cases from stricter bytecode verification in JDK 17

### 3. Spring 5.3 API Changes

- `org.springframework.web.context.request.RequestAttributes` usage is stable
- Check for deprecated Spring 4.x APIs that are removed in 5.3
- `BeanFactory.getBean()` generics may need adjustment
- `PropertySourcesPlaceholderConfigurer` replaces `PropertyPlaceholderConfigurer` (already deprecated in Spring 3.1, removed behavior in 5.x)

### 4. Hibernate 5.6 API Changes

- `Session.createCriteria()` is deprecated; the Criteria API moved to JPA `CriteriaBuilder`
  - Phase 1 does not need to migrate all criteria queries, but any in broadleaf-common should be addressed
- `Configuration` API changes for bootstrapping
- `TypeResolver` -> `TypeConfiguration` changes

## JVM Arguments

For Java 17, module access may be required. Add to surefire/test configs:

```
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.lang.reflect=ALL-UNNAMED
--add-opens java.base/java.util=ALL-UNNAMED
```

## Regression Command

```bash
mvn -pl common -am test
```

## New-Test Requirements

1. **MergePersistenceUnitManagerTest**: Verify that multiple persistence units from different modules are correctly merged under Spring 5.3's `DefaultPersistenceUnitManager`
2. **DirectCopyClassTransformerTest**: Verify that a sample entity annotated with `@DirectCopyTransform` is correctly transformed when compiled to Java 17 bytecode (class file version 61)
3. **EntityMarkerClassTransformerTest**: Verify marker interface injection on a Java 17-compiled entity
4. **SingleTableInheritanceClassTransformerTest**: Verify single-table inheritance rewrite produces valid JPA entities under Hibernate 5.6
