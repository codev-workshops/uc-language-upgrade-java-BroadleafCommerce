# Phase 3b - broadleaf-contentmanagement-module

**Status:** Pending

## Target Module

`admin/broadleaf-contentmanagement-module/` (`org.broadleafcommerce:broadleaf-contentmanagement-module`)

## Prerequisites

- Phase 2a (broadleaf-profile) complete
- Phase 2b (broadleaf-open-admin-platform) complete

## Environment Setup

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

## Dependency / Version Edits

Add to `admin/broadleaf-contentmanagement-module/pom.xml`:

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
| `org.thymeleaf.dom.*` | `org.thymeleaf.model.*` | All CMS processors |
| `org.thymeleaf.processor.element.AbstractElementProcessor` | `org.thymeleaf.processor.element.AbstractElementTagProcessor` | ContentProcessor, HrefUrlRewriteProcessor, UrlRewriteProcessor |
| `org.thymeleaf.Arguments` | `org.thymeleaf.context.ITemplateContext` | All CMS processors |
| `org.thymeleaf.processor.ProcessorResult` | *(removed - void return)* | All CMS processors |
| `org.hibernate.criterion.*` | JPA `CriteriaBuilder` / `CriteriaQuery` | StructuredContentServiceImpl |

## Identified Files Requiring Changes

### Thymeleaf 2 -> 3.1 processors (5 files):
1. `cms/web/processor/ContentProcessor.java` - Full TL3 rewrite (extends AbstractModelVariableModifierProcessor from common)
2. `cms/web/processor/HrefUrlRewriteProcessor.java` - TL3 processor rewrite
3. `cms/web/processor/UrlRewriteProcessor.java` - TL3 processor rewrite
4. `cms/web/processor/ContentProcessorExtensionHandler.java` - TL3 API update (uses Arguments)
5. `cms/web/processor/AbstractContentProcessorExtensionHandler.java` - TL3 API update

### Hibernate:
6. `cms/structure/service/StructuredContentServiceImpl.java` - Hibernate criterion usage

## Key Refactoring Tasks

### 1. Thymeleaf 2 -> 3.1 CMS Processors (5 files)

Follow the exact same pattern proven in Phase 1 (see Phase 1 playbook for the complete template).

**Key patterns:**
```java
// OLD (TL2):
public class ContentProcessor extends AbstractModelVariableModifierProcessor {
    protected void modifyModelAttributes(Arguments arguments, Element element) { ... }
}

// NEW (TL3):
public class ContentProcessor extends AbstractModelVariableModifierProcessor {
    // AbstractModelVariableModifierProcessor was already rewritten in Phase 1
    // to extend AbstractElementTagProcessor. Subclasses must match the new
    // doProcess(ITemplateContext, IProcessableElementTag, IElementTagStructureHandler) signature.
    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag,
            IElementTagStructureHandler structureHandler) { ... }
}
```

**Extension handler pattern:** The `ContentProcessorExtensionHandler` and `AbstractContentProcessorExtensionHandler` use `Arguments` from TL2. Update parameter types to `ITemplateContext`:
```java
// OLD:
void processAttributeValues(Arguments arguments, ...);
// NEW:
void processAttributeValues(ITemplateContext context, ...);
```

### 2. Hibernate Criteria (StructuredContentServiceImpl)

If `StructuredContentServiceImpl` uses `Session.createCriteria()`, migrate to JPA criteria API:
```java
// OLD:
Criteria criteria = session.createCriteria(Entity.class);
criteria.add(Restrictions.eq("field", value));

// NEW:
CriteriaBuilder cb = em.getCriteriaBuilder();
CriteriaQuery<Entity> cq = cb.createQuery(Entity.class);
Root<Entity> root = cq.from(Entity.class);
cq.where(cb.equal(root.get("field"), value));
em.createQuery(cq).getResultList();
```

### 3. Additional Patterns (from Phase 1 Lessons)

Always run a full compile first (`mvn -pl admin/broadleaf-contentmanagement-module -am compile`) and systematically fix any additional errors not in this list. Phase 1 revealed ~24 files with errors beyond the initial checklist. Common unexpected issues:
- `SCOPE_GLOBAL_SESSION` -> `SCOPE_SESSION`
- `RequestMatcher` package move
- Removed Spring utility classes

## Regression Command

```bash
mvn -pl admin/broadleaf-contentmanagement-module -am test
```

## New-Test Requirements

- Verify structured content CRUD under Hibernate 5.6
- Verify content Thymeleaf processors under Thymeleaf 3
