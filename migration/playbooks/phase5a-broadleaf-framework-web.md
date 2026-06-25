# Phase 5a - broadleaf-framework-web

**Status:** Pending

## Target Module

`core/broadleaf-framework-web/` (`org.broadleafcommerce:broadleaf-framework-web`)

## Prerequisites

- Phase 4 (broadleaf-framework) complete

## Environment Setup

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

## Dependency / Version Edits

Add to `core/broadleaf-framework-web/pom.xml`:

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
| `org.thymeleaf.dom.*` | `org.thymeleaf.model.*` | All 21 processor files |
| `org.thymeleaf.processor.element.AbstractElementProcessor` | `org.thymeleaf.processor.element.AbstractElementTagProcessor` | All 21 processor files |
| `org.thymeleaf.Arguments` | `org.thymeleaf.context.ITemplateContext` | All 21 processor files |
| `org.thymeleaf.processor.ProcessorResult` | *(removed - void return)* | All 21 processor files |
| `org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping` | *(same, verify API)* | BroadleafRestApiMvcConfiguration |
| `WebRequest.SCOPE_GLOBAL_SESSION` | `WebRequest.SCOPE_SESSION` | CartStateRequestProcessor, MergeCartProcessorImpl |

## Identified Files Requiring Changes

### Thymeleaf 2 -> 3.1 Processors (21 files - LARGEST TL3 MIGRATION):
1. `core/web/processor/AddSortLinkProcessor.java`
2. `core/web/processor/BroadleafCacheProcessor.java`
3. `core/web/processor/CatalogRelativeHrefProcessor.java`
4. `core/web/processor/CategoriesProcessor.java` (extends AbstractModelVariableModifierProcessor)
5. `core/web/processor/GoogleAnalyticsProcessor.java` (extends AbstractModelVariableModifierProcessor)
6. `core/web/processor/GoogleUniversalAnalyticsProcessor.java`
7. `core/web/processor/HeadProcessor.java`
8. `core/web/processor/NamedOrderProcessor.java` (extends AbstractModelVariableModifierProcessor)
9. `core/web/processor/OnePageCheckoutProcessor.java`
10. `core/web/processor/PaginationPageLinkProcessor.java`
11. `core/web/processor/PaginationSizeLinkProcessor.java`
12. `core/web/processor/PaginationSortLinkProcessor.java`
13. `core/web/processor/PriceTextDisplayProcessor.java`
14. `core/web/processor/ProductOptionDisplayProcessor.java`
15. `core/web/processor/ProductOptionValueProcessor.java`
16. `core/web/processor/ProductOptionsProcessor.java` (extends AbstractModelVariableModifierProcessor)
17. `core/web/processor/RatingsProcessor.java` (extends AbstractModelVariableModifierProcessor)
18. `core/web/processor/RelatedProductProcessor.java` (extends AbstractModelVariableModifierProcessor)
19. `core/web/processor/RemoveFacetValuesLinkProcessor.java`
20. `core/web/processor/ToggleFacetLinkProcessor.java`
21. `core/web/processor/UncacheableDataProcessor.java`

### Thymeleaf extension handlers (3 files):
22. `core/web/processor/extension/HeadProcessorExtensionListener.java`
23. `core/web/processor/extension/HeadProcessorExtensionManager.java`
24. `core/web/processor/extension/UncacheableDataProcessorExtensionHandler.java`

### Spring / Cache (5 files):
25. `core/web/cache/BLCICacheManager.java`
26. `core/web/service/SimpleCacheKeyResolver.java`
27. `core/web/service/TemplateCacheKeyResolverService.java`
28. `core/web/order/security/CartStateFilter.java` - Spring Security RequestMatcher
29. `core/web/order/security/CartStateRequestProcessor.java` - SCOPE_GLOBAL_SESSION
30. `core/web/order/security/MergeCartProcessorImpl.java` - SCOPE_GLOBAL_SESSION
31. `core/web/api/BroadleafRestApiMvcConfiguration.java` - WebMvcConfigurerAdapter

## Key Refactoring Tasks

### 1. Thymeleaf 2 -> 3.1 Processors (21 files)

This is the **largest TL3 migration** across all phases. Follow the proven Phase 1 pattern:

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
        // Use structureHandler.setAttribute(), structureHandler.setBody(), etc.
    }
}
```

**Processors extending AbstractModelVariableModifierProcessor (7 files):**
`CategoriesProcessor`, `GoogleAnalyticsProcessor`, `NamedOrderProcessor`, `ProductOptionsProcessor`, `RatingsProcessor`, `RelatedProductProcessor`, `UncacheableDataProcessor`

These extend `AbstractModelVariableModifierProcessor` which was already rewritten in Phase 1 (`common/`). The subclasses must now implement `doProcess(ITemplateContext, IProcessableElementTag, IElementTagStructureHandler)` instead of `modifyModelAttributes(Arguments, Element)`. Use `structureHandler.setLocalVariable(name, value)` to set model variables.

**Gotchas from Phase 1:**
- `setElementTagName()` does not exist in TL3 - use `IModelFactory.createModel()` + `structureHandler.replaceWith(model, true)`
- `IWebContext.getRequest()` removed in TL 3.1 - use `getExchange().getRequest()` -> `IWebRequest`
- `BroadleafCacheProcessor` may reference `CacheAwareGeneralTemplateWriter` which was rewritten in Phase 1 as a standalone cache utility

### 2. WebMvcConfigurerAdapter -> WebMvcConfigurer

```java
// OLD:
public class BroadleafRestApiMvcConfiguration extends WebMvcConfigurerAdapter { ... }
// NEW:
public class BroadleafRestApiMvcConfiguration implements WebMvcConfigurer { ... }
```

### 3. SCOPE_GLOBAL_SESSION and RequestMatcher

Same fixes as Phase 1:
- `WebRequest.SCOPE_GLOBAL_SESSION` -> `WebRequest.SCOPE_SESSION`
- `org.springframework.security.web.util.RequestMatcher` -> `org.springframework.security.web.util.matcher.RequestMatcher`

### 4. Additional Patterns (from Phase 1 Lessons)

Always run `mvn -pl core/broadleaf-framework-web -am compile` first and fix errors systematically. Phase 1 revealed ~24 additional files beyond the initial checklist.

## Regression Command

```bash
mvn -pl core/broadleaf-framework-web -am test
```

## New-Test Requirements

- Verify storefront Thymeleaf processors render correctly under Thymeleaf 3
- Verify REST API endpoints compile under Spring 5.3 MVC
