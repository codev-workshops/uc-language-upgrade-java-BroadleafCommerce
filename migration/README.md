# BroadleafCommerce Java 17 / Spring 5.3 / Hibernate 5.6 Migration

## Target Stack

| Component         | From              | To            |
|-------------------|-------------------|---------------|
| JDK               | 7 / 8             | 17            |
| Spring Framework  | 4.1.9.RELEASE     | 5.3.39        |
| Spring Security   | 3.2.9.RELEASE     | 5.8.14        |
| Hibernate ORM     | 4.1.11.Final      | 5.6.15.Final  |
| Thymeleaf         | 2.1.4.RELEASE     | 3.1.2.RELEASE |
| Servlet API       | javax.* (3.1+)    | javax.* (3.1+ / Tomcat 9) |

**NOT migrating** to `jakarta.*`.

## Phase DAG

```
Phase 0  (toolchain / root POM) .............. COMPLETE
   |
   v
Phase 1  (broadleaf-common) .................. COMPLETE
(all downstream phases below: COMPLETE)
   |
   +------+------+
   |             |
   v             v
Phase 2a        Phase 2b
broadleaf-      broadleaf-
profile         open-admin-platform
   |             |
   +------+------+
   |             |
   v             v
Phase 3a        Phase 3b
broadleaf-      broadleaf-
profile-web     contentmanagement-module
   |             |
   +------+------+
          |
          v
Phase 4  (broadleaf-framework)
          |
   +------+------+
   |             |
   v             v
Phase 5a        Phase 5b
broadleaf-      broadleaf-
framework-web   admin-module
   |             |
   +------+------+
          |
          v
Phase 6  (integration)
          |
          v
Phase 7  (broadleaf-admin-functional-tests)
```

## Phase Ordering

| Phase | Module(s)                                                              | Parallelisable? | Status |
|-------|------------------------------------------------------------------------|-----------------|--------|
| 0     | Root POM / toolchain                                                   | -               | Complete |
| 1     | `common/` (broadleaf-common)                                           | No              | Complete |
| 2a    | `core/broadleaf-profile/`                                              | Yes (with 2b)   | Complete |
| 2b    | `admin/broadleaf-open-admin-platform/`                                 | Yes (with 2a)   | Complete |
| 3a    | `core/broadleaf-profile-web/`                                          | Yes (with 3b)   | Complete |
| 3b    | `admin/broadleaf-contentmanagement-module/`                            | Yes (with 3a)   | Complete |
| 4     | `core/broadleaf-framework/`                                            | No              | Complete |
| 5a    | `core/broadleaf-framework-web/`                                        | Yes (with 5b)   | Complete |
| 5b    | `admin/broadleaf-admin-module/`                                        | Yes (with 5a)   | Complete |
| 6     | `integration/`                                                         | No              | Complete |
| 7     | `admin/broadleaf-admin-functional-tests/`                              | No              | Complete |

## Playbooks

Each phase has a detailed playbook in `migration/playbooks/`:

- [Phase 0 - Toolchain](playbooks/phase0-toolchain.md) - **Complete**
- [Phase 1 - broadleaf-common](playbooks/phase1-broadleaf-common.md) - **Complete**
- [Phase 2a - broadleaf-profile](playbooks/phase2a-broadleaf-profile.md) - **Complete**
- [Phase 2b - broadleaf-open-admin-platform](playbooks/phase2b-broadleaf-open-admin-platform.md) - **Complete**
- [Phase 3a - broadleaf-profile-web](playbooks/phase3a-broadleaf-profile-web.md) - **Complete**
- [Phase 3b - broadleaf-contentmanagement-module](playbooks/phase3b-broadleaf-contentmanagement-module.md) - **Complete**
- [Phase 4 - broadleaf-framework](playbooks/phase4-broadleaf-framework.md) - **Complete**
- [Phase 5a - broadleaf-framework-web](playbooks/phase5a-broadleaf-framework-web.md) - **Complete**
- [Phase 5b - broadleaf-admin-module](playbooks/phase5b-broadleaf-admin-module.md) - **Complete**
- [Phase 6 - integration](playbooks/phase6-integration.md) - **Complete**
- [Phase 7 - broadleaf-admin-functional-tests](playbooks/phase7-broadleaf-admin-functional-tests.md) - **Complete**

## Execution

Phases can be executed by child Devin sessions. Parallel phases (e.g. 2a/2b) can run concurrently on separate branches and be merged sequentially.

Each playbook contains:
1. Environment setup (JAVA_HOME must be set to JDK 17)
2. Dependency/version edits scoped to that module (with exact XML snippets)
3. Import-rename map with specific affected files identified by source scan
4. Concrete refactoring patterns with before/after code examples
5. Regression command
6. New-test requirements

## Lessons Learned (from Phase 0 + Phase 1 Execution)

### Environment

- **JAVA_HOME must be set explicitly** before every `mvn` command:
  ```bash
  export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
  export PATH=$JAVA_HOME/bin:$PATH
  ```

### Build Infrastructure (every module needs these)

1. **`javax.annotation-api` dependency required**: JDK 17 removed `javax.annotation` from the JRE. Every module using `@Resource`, `@PostConstruct`, or `@PreDestroy` must add:
   ```xml
   <dependency>
       <groupId>javax.annotation</groupId>
       <artifactId>javax.annotation-api</artifactId>
   </dependency>
   ```

2. **`junit-vintage-engine` required**: Surefire 3.2.5 auto-detects JUnit Platform provider when JUnit 5 is on classpath (from Spock/Groovy transitive deps). JUnit 4 tests won't run without:
   ```xml
   <dependency>
       <groupId>org.junit.vintage</groupId>
       <artifactId>junit-vintage-engine</artifactId>
       <version>5.10.2</version>
       <scope>test</scope>
   </dependency>
   ```

3. **`--add-opens` JVM args** needed in surefire for reflective access:
   ```
   --add-opens java.base/java.lang=ALL-UNNAMED
   --add-opens java.base/java.lang.reflect=ALL-UNNAMED
   --add-opens java.base/java.util=ALL-UNNAMED
   ```

### Common Migration Patterns (apply to all phases)

| Pattern | Old | New |
|---|---|---|
| Hibernate package | `org.hibernate.ejb.*` | `org.hibernate.jpa.*` |
| Session interface | `SessionImplementor` | `SharedSessionContractImplementor` |
| Spring scope | `SCOPE_GLOBAL_SESSION` | `SCOPE_SESSION` |
| Security matcher | `o.s.s.web.util.RequestMatcher` | `o.s.s.web.util.matcher.RequestMatcher` |
| MVC adapter | `HandlerInterceptorAdapter` | `HandlerInterceptor` |
| MVC configurer | `WebMvcConfigurerAdapter` | `WebMvcConfigurer` |
| Thymeleaf processor | `AbstractElementProcessor` | `AbstractElementTagProcessor` |
| Thymeleaf args | `Arguments` | `ITemplateContext` |
| Thymeleaf result | `ProcessorResult` | void return |
| Thymeleaf context | `IWebContext.getRequest()` | `IWebContext.getExchange().getRequest()` |

### Gotchas

- **Compile first, fix errors systematically**: Phase 1 had 18 files in the initial checklist but ~24 additional files with errors not on the list. Always run `mvn -pl <module> -am compile` and fix all errors, not just the ones in the playbook.
- **Thymeleaf 3 `setElementTagName()` doesn't exist**: Processors that need to output custom tags must build `IModel` objects and use `structureHandler.replaceWith(model, true)`.
- **Hibernate `CacheKey` removed silently**: No deprecation warning - just gone in 5.6. Any `instanceof CacheKey` checks will fail at runtime.
- **`VelocityEngineUtils` removed**: Use `VelocityEngine.mergeTemplate()` with `VelocityContext` + `StringWriter` directly.
- **`Log4jConfigurer` removed**: Make the wrapper a no-op that logs a warning.
- **TestNG + JUnit Platform conflict**: The `integration/` module uses TestNG which may not be auto-detected when JUnit Platform provider is on classpath. May need explicit surefire-testng provider dependency.
