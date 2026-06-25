# Phase 2b - broadleaf-open-admin-platform

**Status:** Pending

## Target Module

`admin/broadleaf-open-admin-platform/` (`org.broadleafcommerce:broadleaf-open-admin-platform`)

## Prerequisites

- Phase 1 (broadleaf-common) complete and merged

## Environment Setup

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

## Dependency / Version Edits

Add to `admin/broadleaf-open-admin-platform/pom.xml`:

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

Add `--add-opens` to surefire `<argLine>` (same pattern as common/pom.xml):
```
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.lang.reflect=ALL-UNNAMED
--add-opens java.base/java.util=ALL-UNNAMED
```

## Import-Rename Map

| Old Import | New Import | Affected Files |
|---|---|---|
| `org.hibernate.ejb.*` | `org.hibernate.jpa.*` | DynamicEntityDaoImpl, FieldManager, FieldPathBuilder, AdminPermissionDaoImpl, AdminUserDaoImpl, AdminNavigationDaoImpl |
| `org.hibernate.criterion.*` | JPA `CriteriaBuilder` / `CriteriaQuery` | FieldPathBuilder, BasicFieldMetadataProvider |
| `org.thymeleaf.dom.*` | `org.thymeleaf.model.*` | All admin processors |
| `org.thymeleaf.processor.element.AbstractElementProcessor` | `org.thymeleaf.processor.element.AbstractElementTagProcessor` | All admin processors |
| `org.thymeleaf.Arguments` | `org.thymeleaf.context.ITemplateContext` | All admin processors |
| `org.thymeleaf.processor.ProcessorResult` | *(removed - void return)* | All admin processors |
| `org.thymeleaf.spring4.*` | `org.thymeleaf.spring5.*` | BroadleafAdminRequestProcessor |
| `org.springframework.web.servlet.handler.HandlerInterceptorAdapter` | `org.springframework.web.servlet.HandlerInterceptor` | JSFieldNameCompatibilityInterceptor |

## Identified Files Requiring Changes

### Hibernate ejb -> jpa (6 files):
1. `openadmin/server/dao/DynamicEntityDaoImpl.java`
2. `openadmin/server/service/persistence/module/FieldManager.java`
3. `openadmin/server/service/persistence/module/criteria/FieldPathBuilder.java`
4. `openadmin/server/security/dao/AdminPermissionDaoImpl.java`
5. `openadmin/server/security/dao/AdminUserDaoImpl.java`
6. `openadmin/server/security/dao/AdminNavigationDaoImpl.java`

### Thymeleaf 2 -> 3.1 processors (6 files):
7. `openadmin/web/processor/AdminComponentIdProcessor.java`
8. `openadmin/web/processor/AdminFieldBuilderProcessor.java`
9. `openadmin/web/processor/AdminModuleProcessor.java`
10. `openadmin/web/processor/AdminSectionHrefProcessor.java`
11. `openadmin/web/processor/AdminUserProcessor.java`
12. `openadmin/web/processor/ErrorsProcessor.java`

### Spring MVC / Other:
13. `openadmin/web/compatibility/JSFieldNameCompatibilityInterceptor.java` - HandlerInterceptorAdapter -> HandlerInterceptor
14. `openadmin/web/filter/BroadleafAdminRequestProcessor.java` - SCOPE_GLOBAL_SESSION -> SCOPE_SESSION
15. `openadmin/server/dao/provider/metadata/BasicFieldMetadataProvider.java` - Hibernate criteria changes

## Key Refactoring Tasks

### 1. Thymeleaf 2 -> 3.1 Admin Processors (6 files)

Follow the exact same pattern proven in Phase 1 (see Phase 1 playbook for the template):

```java
// OLD (TL2):
public class AdminFooProcessor extends AbstractElementProcessor {
    protected ProcessorResult processElement(Arguments arguments, Element element) { ... }
}

// NEW (TL3):
public class AdminFooProcessor extends AbstractElementTagProcessor {
    public AdminFooProcessor() {
        super(TemplateMode.HTML, "blc", null, false, "attrName", true, 1000);
    }
    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag,
            IElementTagStructureHandler structureHandler) { ... }
}
```

**Gotchas from Phase 1:**
- `setElementTagName()` does not exist in TL3. If a processor needs to output a tag, build it with `IModelFactory.createModel()` and `structureHandler.replaceWith(model, true)`.
- `IWebContext.getRequest()` removed in TL 3.1. Use `((IWebContext) context).getExchange().getRequest()` -> `IWebRequest`.
- Processors that extend `AbstractModelVariableModifierProcessor` from broadleaf-common: that class was already rewritten in Phase 1 to extend `AbstractElementTagProcessor`. Check that subclasses in this module match the new signature.

### 2. HandlerInterceptorAdapter -> HandlerInterceptor

`HandlerInterceptorAdapter` was deprecated in Spring 5.0 and removed behavior in 5.3. Replace with direct `HandlerInterceptor` implementation:
```java
// OLD:
public class MyInterceptor extends HandlerInterceptorAdapter {
    @Override
    public boolean preHandle(...) throws Exception { ... }
}

// NEW:
public class MyInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(...) throws Exception { ... }
}
```

### 3. SCOPE_GLOBAL_SESSION

Same fix as Phase 1: `WebRequest.SCOPE_GLOBAL_SESSION` -> `WebRequest.SCOPE_SESSION`.

### 4. Hibernate ejb -> jpa (Simple Renames)

Same pattern as Phase 1. Just import renames: `org.hibernate.ejb.QueryHints` -> `org.hibernate.jpa.QueryHints`, etc.

## Regression Command

```bash
mvn -pl admin/broadleaf-open-admin-platform -am test
```

## New-Test Requirements

- Verify admin Thymeleaf dialect registers and processes templates under Thymeleaf 3
- Verify persistence handlers compile and handle CRUD under Hibernate 5.6
