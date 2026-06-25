# Phase 7 - broadleaf-admin-functional-tests

**Status:** Pending

## Target Module

`admin/broadleaf-admin-functional-tests/` (`org.broadleafcommerce:broadleaf-admin-functional-tests`)

## Prerequisites

- Phase 6 (integration) complete

## Environment Setup

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

## Dependency / Version Edits

Changes needed in `admin/broadleaf-admin-functional-tests/pom.xml`:

```xml
<dependency>
    <groupId>javax.annotation</groupId>
    <artifactId>javax.annotation-api</artifactId>
</dependency>
```

- Selenium 2.43.1 -> 4.18.1 (resolved from parent)
- Geb 0.10.0 -> 5.1 (resolved from parent)
- Groovy 2.3.10 -> 4.0.21 / groupId change (resolved from parent)
- Spock 1.0 -> 2.3 (resolved from parent)
- Verify any module-level overrides are updated or removed

Add `--add-opens` to surefire `<argLine>`.

## Import-Rename Map

| Old Import | New Import | Notes |
|---|---|---|
| `geb.Browser` | `geb.Browser` | Mostly compatible; check for removed methods |
| `geb.spock.GebReportingSpec` | `geb.spock.GebReportingSpec` | Verify Geb 5.x compatibility |
| `org.openqa.selenium.DesiredCapabilities` | `org.openqa.selenium.chrome.ChromeOptions` (etc.) | Capabilities classes deprecated |
| `org.openqa.selenium.internal.FindsBy*` | *(removed in Selenium 4)* | Use `findElement(By.*)` instead |

## Identified Files Requiring Changes

Key Groovy files:
1. `GebConfig.groovy` - Driver configuration (Selenium 4 + Geb 5)
2. `BroadleafAdminSpec.groovy` / `OpenAdminSpec.groovy` - Base test specs
3. All page objects in `page/` directory
4. All spec files

## Key Refactoring Tasks

### 1. GebConfig.groovy (Critical)

Geb 5.x has significant configuration changes:

```groovy
// OLD (Geb 0.10):
driver = {
    def capabilities = DesiredCapabilities.chrome()
    new ChromeDriver(capabilities)
}

// NEW (Geb 5.x + Selenium 4):
driver = {
    def options = new ChromeOptions()
    new ChromeDriver(options)
}
```

**Selenium Manager:** Selenium 4.18 includes Selenium Manager which auto-manages browser drivers. Manual driver downloads (chromedriver, geckodriver) are no longer needed.

### 2. Selenium 4 Migration

**`DesiredCapabilities` -> `Options`:**
```groovy
// OLD:
import org.openqa.selenium.remote.DesiredCapabilities
def caps = DesiredCapabilities.chrome()

// NEW:
import org.openqa.selenium.chrome.ChromeOptions
def options = new ChromeOptions()
```

**`FindsBy*` interfaces removed:** Selenium 4 removed the `FindsByClassName`, `FindsByCssSelector`, etc. interfaces. Use `findElement(By.*)` instead:
```groovy
// OLD:
element.findElementByClassName("my-class")

// NEW:
element.findElement(By.className("my-class"))
```

### 3. Spock 2.x + Geb 5.x Integration

- Spock 2.x runs on JUnit Platform - ensure `geb-spock` 5.1 is compatible
- `@Unroll` is default behavior in Spock 2.x (can remove explicit annotations)
- `GebReportingSpec` API is mostly stable in Geb 5.x

### 4. Groovy 4.x Compatibility

Groovy 4.x is mostly backward compatible. Check for:
- Deprecated method removals
- `groovy.transform.*` annotation behavior changes
- `@CompileStatic` / `@TypeChecked` changes

### 5. Additional Patterns (from Phase 1 Lessons)

Always compile first with `mvn -pl admin/broadleaf-admin-functional-tests -am compile` and fix errors systematically. Remember:
- `javax.annotation-api` dependency needed for `@Resource` / `@PostConstruct`
- `--add-opens` JVM args may be needed for reflective access
- TestNG + JUnit Platform coexistence may need explicit surefire provider config

## Regression Command

```bash
mvn -pl admin/broadleaf-admin-functional-tests -am test
```

## New-Test Requirements

- All existing functional tests must pass with updated Geb/Selenium/Spock stack
- No new tests required beyond fixing existing ones for API compatibility
