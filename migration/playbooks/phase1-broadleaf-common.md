# Phase 1 - broadleaf-common

**Status:** Complete

## Target Module

`common/` (`org.broadleafcommerce:broadleaf-common`)

## Prerequisites

- Phase 0 complete (root POM upgraded)

## Dependency / Version Edits

Changes made to `common/pom.xml`:

- Added `javax.annotation:javax.annotation-api` (required for `@Resource`/`@PostConstruct` on JDK 17)
- Added `org.junit.vintage:junit-vintage-engine:5.10.2` (test scope, required for JUnit 4 test discovery under Surefire 3.2.5)
- Added `--add-opens` JVM args to surefire `<argLine>`:
  ```
  --add-opens java.base/java.lang=ALL-UNNAMED
  --add-opens java.base/java.lang.reflect=ALL-UNNAMED
  --add-opens java.base/java.util=ALL-UNNAMED
  ```

## Import-Rename Map (Verified)

| Old Import | New Import | Notes |
|---|---|---|
| `org.hibernate.ejb.HibernatePersistence` | `org.hibernate.jpa.HibernatePersistenceProvider` | |
| `org.hibernate.ejb.*` | `org.hibernate.jpa.*` | All DAO files |
| `org.hibernate.engine.spi.SessionImplementor` | `org.hibernate.engine.spi.SharedSessionContractImplementor` | IdOverrideTableGenerator |
| `org.hibernate.service.spi.BasicServiceInitiator` | `org.hibernate.service.spi.StandardServiceInitiator` | LifecycleAwareJDBCServicesInitiator |
| `org.hibernate.service.spi.ServiceRegistryBuilder` | `org.hibernate.boot.registry.StandardServiceRegistryBuilder` | CommonServiceIntegrator |
| `org.hibernate.metamodel.source.MetadataImplementor` | `org.hibernate.boot.spi.MetadataImplementor` | CommonServiceIntegrator |
| `org.hibernate.cache.spi.CacheKey` | *(removed - just use raw key)* | HydratedCacheManagerImpl and subclasses |
| `org.hibernate.type.StringClobType` | *(removed - use string constant)* | MaterializedClobTypeClassTransformer |
| `org.thymeleaf.spring4.*` | `org.thymeleaf.spring5.*` | BroadleafThymeleafViewResolver |
| `org.thymeleaf.dom.*` | `org.thymeleaf.model.*` | All processors |
| `org.thymeleaf.processor.element.AbstractElementProcessor` | `org.thymeleaf.processor.element.AbstractElementTagProcessor` | All processors |
| `org.thymeleaf.Arguments` | `org.thymeleaf.context.ITemplateContext` | All processors |
| `org.thymeleaf.processor.ProcessorResult` | *(removed - void return)* | All processors |
| `org.thymeleaf.dialect.AbstractDialect` | `org.thymeleaf.dialect.AbstractProcessorDialect` | BLCDialect, BLCAdminDialect |
| `org.springframework.security.web.util.RequestMatcher` | `org.springframework.security.web.util.matcher.RequestMatcher` | CsrfFilter, SecurityFilter |
| `org.springframework.security.web.util.AntPathRequestMatcher` | `org.springframework.security.web.util.matcher.AntPathRequestMatcher` | CsrfFilter, SecurityFilter |
| `org.springframework.web.context.request.WebRequest.SCOPE_GLOBAL_SESSION` | `WebRequest.SCOPE_SESSION` | All resolver files |
| `org.springframework.util.Log4jConfigurer` | *(removed - make no-op)* | RuntimeLog4jConfigurer |
| `org.springframework.ui.velocity.VelocityEngineUtils` | *(removed - use Velocity API directly)* | VelocityMessageCreator |
| `org.objectweb.asm.commons.EmptyVisitor` | `org.objectweb.asm.ClassVisitor` with `Opcodes.ASM9` | HydrationScanner |

## Key Refactoring Tasks (All Completed)

### 1. Thymeleaf 2 -> 3.1 Migration (18 files)

**Pattern for processor rewrite:**
```java
// OLD (TL2):
public class MyProcessor extends AbstractElementProcessor {
    protected ProcessorResult processElement(Arguments arguments, Element element) {
        // DOM manipulation
        return ProcessorResult.OK;
    }
}

// NEW (TL3):
public class MyProcessor extends AbstractElementTagProcessor {
    public MyProcessor() {
        super(TemplateMode.HTML, "blc", null, false, "myattr", true, 1000);
    }
    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag,
            IElementTagStructureHandler structureHandler) {
        // Model-based output
    }
}
```

**Gotcha - `setElementTagName()` doesn't exist in TL3:** Processors like `FormProcessor` and `TransparentRedirectCreditCardFormProcessor` that need to output `<form>` tags must build the entire element using `IModelFactory.createModel()` and `structureHandler.replaceWith(model, true)`.

**Gotcha - `IWebContext.getRequest()` removed in TL 3.1:** Use `((IWebContext) context).getExchange().getRequest()` which returns `IWebRequest`. Use `getApplicationPath()` instead of `getContextPath()`.

**Gotcha - `BroadleafVariableExpressionEvaluator`:** TL3's `IExpressionObjectFactory` uses `buildObject()` (not `buildExpressionObject()`).

**Files changed:**
- `web/dialect/AbstractModelVariableModifierProcessor.java`
- `web/dialect/BLCDialect.java` / `BLCAdminDialect.java`
- `web/expression/BroadleafVariableExpressionEvaluator.java`
- `web/BroadleafThymeleafViewResolver.java`
- `web/BroadleafThymeleafMessageResolver.java`
- `web/BroadleafThymeleafServletContextTemplateResolver.java`
- `web/NullBroadleafTemplateResolver.java`
- `web/BroadleafThymeleafStandardTemplateModeHandlers.java`
- `web/BroadleafThymeleafTemplateModeHandler.java`
- `web/processor/ResourceBundleProcessor.java`
- `web/processor/FormProcessor.java`
- `web/processor/DataDrivenEnumerationProcessor.java`
- `web/payment/processor/CreditCardTypesProcessor.java`
- `web/payment/processor/TransparentRedirectCreditCardFormProcessor.java`
- `breadcrumbs/processor/BreadcrumbProcessor.java`
- `web/processor/ConfigVariableProcessor.java`
- `templatewriter/CacheAwareGeneralTemplateWriter.java` (rewritten as standalone cache utility)

### 2. Hibernate 4 -> 5.6 Migration

**`Ejb3Configuration` completely removed:** The entire `org.hibernate.ejb.Ejb3Configuration` class is gone. Replaced with the Hibernate boot API:
```java
// OLD:
Ejb3Configuration cfg = ...;
cfg.getClassMapping(entityName);

// NEW:
StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
    .applySetting("hibernate.dialect", dialect)
    .build();
MetadataSources sources = new MetadataSources(serviceRegistry);
Metadata metadata = sources.buildMetadata();
metadata.getEntityBinding(entityName);
```

**`CacheKey` removed:** Hibernate 5.6 internalized `CacheKey`. All `instanceof CacheKey` checks and `.getEntityOrRoleName()` calls must be removed. Just use the raw key.

**Method signature changes in `IdOverrideTableGenerator`:**
- `generate(SessionImplementor, Object)` -> `generate(SharedSessionContractImplementor, Object)`
- `configure(Type, Properties, Dialect)` -> `configure(Type, Properties, ServiceRegistry)`

**`StringClobType` removed:** Replace with string constant `"org.hibernate.type.StringClobType"`.

**Files changed:** EJB3ConfigurationDaoImpl, DynamicDaoHelper, DynamicDaoHelperImpl, IdOverrideTableGenerator, SequenceGeneratorCorruptionDetection, CommonServiceIntegrator, LifecycleAwareJDBCServicesInitiator, MaterializedClobTypeClassTransformer, HydratedCacheManagerImpl, EhcacheHydratedCacheManagerImpl, BigMemoryHydratedCacheManagerImpl, all DAO files (ejb -> jpa import rename)

### 3. Spring 5.3 / Security 5.8 Changes

- `SCOPE_GLOBAL_SESSION` removed -> use `SCOPE_SESSION` (7 resolver files)
- `Log4jConfigurer` removed -> `RuntimeLog4jConfigurer` now logs warning and returns
- `VelocityEngineUtils.mergeTemplateIntoString()` removed -> use `VelocityEngine.mergeTemplate()` with `VelocityContext` + `StringWriter`
- `getApplicationListeners(event)` changed -> `getApplicationListeners(event, ResolvableType.forInstance(event))`
- `createWebApplicationContext(ServletContext, ApplicationContext)` two-arg method removed -> use single-arg override only
- `RequestMatcher` / `AntPathRequestMatcher` moved from `o.s.s.web.util` to `o.s.s.web.util.matcher`
- `TokenBasedRememberMeServices` no-arg constructor removed -> call `super("_deprecated_key_", new InMemoryUserDetailsManager())`

### 4. ASM 3 -> 9

`EmptyVisitor` removed. Rewrite with delegation pattern:
```java
// OLD:
class HydrationScanner extends EmptyVisitor { ... }

// NEW:
class HydrationScanner extends ClassVisitor {
    HydrationScanner() { super(Opcodes.ASM9); }
    // Inner classes: HydrationFieldVisitor extends FieldVisitor,
    //                HydrationAnnotationVisitor extends AnnotationVisitor
}
```

### 5. MergePersistenceUnitManager

Removed reflective access to `InterceptFieldClassFileTransformer` (which was removed in Hibernate 5.6). The `preparePersistenceUnitInfos()` method still uses reflection to call `DefaultPersistenceUnitManager.readPersistenceUnitInfos()` (which is protected), but this works under Java 17 with `--add-opens`.

## Build Fixes Applied

- JaCoCo 0.7.9 -> 0.8.11 (crashes on Java 17)
- JUnit 4.11 -> 4.13.2 (required by junit-vintage-engine)
- `junit-platform-engine:1.10.2` / `junit-platform-commons:1.10.2` aligned in root POM
- `junit-vintage-engine:5.10.2` added to common/pom.xml for JUnit 4 test discovery
- `--add-opens` args added to surefire for reflective access

## JVM Arguments

Added to `common/pom.xml` surefire config:
```
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.lang.reflect=ALL-UNNAMED
--add-opens java.base/java.util=ALL-UNNAMED
```

## Regression Command

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
mvn -pl common -am test
```

**Result:** 53 tests pass (0 failures)

## New Tests Created

1. **MergePersistenceUnitManagerTest** (4 tests): Verifies class hierarchy, transformer list initialization, getter/setter, Java 17 compatibility
2. **DirectCopyClassTransformerTest** (4 tests): Verifies BroadleafClassTransformer implementation, null className handling, javassist parsing of Java 17 bytecode (class file version 61), xformTemplates configuration
3. **EntityMarkerClassTransformerTest** (4 tests): Verifies @Entity detection, non-entity exclusion, null className handling, Java 17 bytecode version verification
4. **SingleTableInheritanceClassTransformerTest** (5 tests): Verifies BroadleafClassTransformer implementation, JPA property compilation, transform with/without configured info, null className handling
