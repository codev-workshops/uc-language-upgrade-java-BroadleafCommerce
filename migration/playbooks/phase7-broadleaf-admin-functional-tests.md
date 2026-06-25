# Phase 7 - broadleaf-admin-functional-tests

## Target Module

`admin/broadleaf-admin-functional-tests/` (`org.broadleafcommerce:broadleaf-admin-functional-tests`)

## Prerequisites

- Phase 6 (integration) complete

## Dependency / Version Edits

- Selenium 2.43.1 -> 4.18.1 (resolved from parent)
- Geb 0.10.0 -> 5.1 (resolved from parent)
- Groovy 2.3.10 -> 4.0.21 / groupId change (resolved from parent)
- Spock 1.0 -> 2.3 (resolved from parent)
- Verify any module-level overrides are updated

## Import-Rename Map

| Old Import | New Import |
|---|---|
| `geb.Browser` | (same, but Geb 5.x API changes) |
| `geb.spock.GebReportingSpec` | `geb.spock.GebReportingSpec` (same, verify Geb 5.x) |
| `org.openqa.selenium.*` | (same, but Selenium 4.x deprecates some APIs) |

## Key Refactoring Tasks

- **Geb 5.x Migration**:
  - `GebConfig.groovy` configuration changes
  - Driver management changes (Selenium Manager replaces manual driver setup)
  - Page object API changes
- **Selenium 4 Migration**:
  - `DesiredCapabilities` -> `Options` classes
  - Driver instantiation changes
  - `FindsBy*` interfaces removed
- **Spock 2.x Migration**:
  - JUnit Platform-based execution
  - `@Unroll` behavior changes
  - Extension API changes
- **Groovy 4.x**:
  - Package rename: `groovy.*` -> `groovy.*` (mostly compatible)
  - Some deprecated APIs removed

## Regression Command

```bash
mvn -pl admin/broadleaf-admin-functional-tests -am test
```

## New-Test Requirements

- All existing functional tests must pass with updated Geb/Selenium/Spock stack
- No new tests required beyond fixing existing ones for API compatibility
