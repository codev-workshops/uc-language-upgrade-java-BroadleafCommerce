# BroadleafCommerce — Java 7 → Java 17 Migration Playbook

Repeatable recipes for migrating each Maven module of the reactor from Java 7 to Java 17, derived from
**Phase 1 (`common` / broadleaf-common)**. Apply these per module, bottom-up by the dependency graph.

> ## ⚠️ THERE IS NO `javax.* → jakarta.*` NAMESPACE FLIP
> This is a **Java API migration**, not a namespace migration. We deliberately target **Spring Framework 5.3.x +
> Spring Security 5.8.x + Hibernate 5.6.x**, which are the last lines that run on **JDK 17 while staying on the
> `javax.*` namespace**. Keep `javax.persistence`, `javax.servlet`, `javax.annotation`, `javax.ws.rs`, `javax.mail`,
> etc. **as `javax.*`**. Do NOT bump to Spring 6 / Hibernate 6. If you find yourself renaming `javax.*` to
> `jakarta.*`, STOP — that is wrong for this migration. Ignore any older knowledge note / playbook that says otherwise.

## Target stack (staged in the root pom — Phase 0)

| Concern | Coordinate | Version |
|---|---|---|
| Spring Framework | `org.springframework:spring-*` | `5.3.39` |
| Spring Security | `org.springframework.security:spring-security-*` | `5.8.16` |
| Hibernate | `org.hibernate:hibernate-core` / `-envers` / `-ehcache` | `5.6.15.Final` |
| JPA API | `javax.persistence:javax.persistence-api` | `2.2` |
| Servlet API | `javax.servlet:javax.servlet-api` | `4.0.1` |
| Thymeleaf | `org.thymeleaf:thymeleaf` + `thymeleaf-spring5` | `3.1.2.RELEASE` |
| Bytecode | `org.ow2.asm:asm` / `asm-commons` (9.7), `org.javassist:javassist` (3.29.2-GA) | — |
| Logging | log4j2 `2.23.1` (`log4j-api`, `log4j-core`, `log4j-1.2-api` bridge, `log4j-slf4j-impl`), slf4j `1.7.36` | — |
| Mail | `com.sun.mail:javax.mail` | `1.6.2` |
| Velocity | `org.apache.velocity:velocity-engine-core` (+ `velocity-tools-generic` if needed) | `2.3` |
| Tests | JUnit `4.13.2`, EasyMock `5.2.0`, TestNG `7.10.2`, Spock `2.3-groovy-4.0`, Groovy `4.0.24`, GreenMail `2.0.1` | — |

Versions live in the root `<properties>` / `<dependencyManagement>`. **Module poms must consume them without
`<version>`.** The root pom still has a temporary "PHASE 0 LEGACY COORDINATES" block so unmigrated modules keep
resolving — do not rely on it for a module you are migrating, and **do not edit the root pom in a module phase.**

---

## 1. POM bump template (per module)

1. Drop `easymockclassextension` (folded into `org.easymock:easymock` 5).
2. log4j1 → log4j2 (see GAV table).
3. `javax.mail:mail` → `com.sun.mail:javax.mail`.
4. `org.apache.velocity:velocity` (+ `velocity-tools`) → `velocity-engine-core` (+ `velocity-tools-generic` if used).
5. `javax.servlet:servlet-api` → `javax.servlet:javax.servlet-api`.
6. `org.hibernate.javax.persistence:hibernate-jpa-2.0-api` → `javax.persistence:javax.persistence-api`.
7. `asm:asm` / `asm-commons` → `org.ow2.asm:asm` / `asm-commons`.
8. `thymeleaf-spring4` → `thymeleaf-spring5`.
9. `org.codehaus.woodstox:woodstox-core-asl` → `com.fasterxml.woodstox:woodstox-core`.
10. `org.codehaus.groovy:groovy-all` → `org.apache.groovy:groovy`.
11. Remove cglib (Hibernate 5.6 uses ByteBuddy internally; Broadleaf weaving uses javassist/ASM directly).
12. Leave versions unspecified (inherit from root). Keep `hibernate-core`/`-envers`/`-ehcache`;
    `hibernate-entitymanager` is a relocation in 5.6 (its types are folded into `hibernate-core`).

### Legacy → new GAV swap table

| Legacy GAV | New GAV |
|---|---|
| `org.easymock:easymockclassextension` | *(removed — use `org.easymock:easymock` 5)* |
| `log4j:log4j` | `org.apache.logging.log4j:log4j-api` + `:log4j-core` + `:log4j-1.2-api` (bridge) |
| `org.slf4j:slf4j-log4j12` | `org.apache.logging.log4j:log4j-slf4j-impl` (runtime) |
| `javax.mail:mail` | `com.sun.mail:javax.mail` |
| `org.apache.velocity:velocity` | `org.apache.velocity:velocity-engine-core` |
| `org.apache.velocity:velocity-tools` | `org.apache.velocity.tools:velocity-tools-generic` *(only if used)* |
| `javax.servlet:servlet-api` | `javax.servlet:javax.servlet-api` |
| `org.hibernate.javax.persistence:hibernate-jpa-2.0-api` | `javax.persistence:javax.persistence-api` |
| `asm:asm`, `asm:asm-commons` | `org.ow2.asm:asm`, `org.ow2.asm:asm-commons` |
| `org.thymeleaf:thymeleaf-spring4` | `org.thymeleaf:thymeleaf-spring5` |
| `org.codehaus.woodstox:woodstox-core-asl` | `com.fasterxml.woodstox:woodstox-core` |
| `org.codehaus.groovy:groovy-all` | `org.apache.groovy:groovy` |
| `cglib:cglib` | *(removed)* |

### Compiler plugin (already fixed in root)
The root pom originally pinned `maven-compiler-plugin:3.1`, which predates the `<release>` option (needs 3.6+). This was
fixed in a Phase 0 hotfix — the root now pins `maven-compiler-plugin:3.13.0` with `<release>${java.version}</release>`.
**Module poms no longer need a local compiler-plugin override.** (Phase 1's temporary local override has been removed.)

### Checkpoint
```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn -pl <module> -am clean compile
```
Dependencies must **resolve** first; source errors come next.

---

## 2. Spring 4 → 5.3 / Hibernate 4 → 5.6 API cheatsheet (NO namespace change)

| Symptom (Java 7 / Spring 4 / Hibernate 4) | Java 17 fix (Spring 5.3 / Hibernate 5.6, still javax) |
|---|---|
| `org.hibernate.ejb.*`, `Ejb3Configuration` | Removed. Use standard JPA bootstrap + `em.unwrap(Session.class)` / `emf.unwrap(SessionFactory.class)`. |
| `HibernateEntityManager` / `HibernateEntityManagerFactory` | Removed. Use `javax.persistence.EntityManager(Factory)` and `.unwrap(...)`. |
| `org.hibernate.ejb.QueryHints` | `org.hibernate.jpa.QueryHints` (or `org.hibernate.annotations.QueryHints`). |
| Hibernate `Type` / `TypeResolver` / dialect registry internals | Packages moved under `org.hibernate.type.*`; prefer public `ServiceRegistry` / `Metadata` APIs. |
| `WebRequest.SCOPE_GLOBAL_SESSION` | Removed (portlet scope dropped). Use `SCOPE_SESSION`. |
| `org.springframework.ui.velocity.VelocityEngineFactoryBean` | **Removed in Spring 5.** Provide a small `FactoryBean<VelocityEngine>` (see §4). |
| `org.thymeleaf.spring4.*` | `org.thymeleaf.spring5.*`. |
| Custom `LoadTimeWeaver` / instrumentation reflection | Spring 5.3 internals are close to 4.x; reflection mostly survives + `--add-opens` (see §3). |

Keep ALL `javax.*` imports as `javax.*`. **Compile checkpoint after each subsystem** so error counts only ever go down.

### Thymeleaf 2 → 3 (only where a module has Thymeleaf dialects/processors)
The TL2 DOM/Arguments API was removed in TL3. Rewrite:

| TL2 | TL3 |
|---|---|
| `AbstractDialect` (+ `getProcessors()`) | `AbstractProcessorDialect` (super `(name, prefix, precedence)`, `getProcessors(String prefix)`) |
| `AbstractElementProcessor` | `AbstractElementTagProcessor` (tag-level) or `AbstractElementModelProcessor` (model-level) |
| `AbstractLocalVariableDefinitionElementProcessor` | `AbstractElementTagProcessor` + `structureHandler.setLocalVariable(...)` then `removeTags()` to unwrap |
| `Arguments`, `Element`, `ProcessorResult` | `ITemplateContext`, `IProcessableElementTag` / `IModel`, `IElementTagStructureHandler` / `IElementModelStructureHandler` |
| DOM building (`new Element(...)`) | `context.getModelFactory().createOpenElementTag/StandaloneElementTag/...` + `IModel.insert/replace/reset/add` |
| `getExecutionAttributes()` to expose `${#obj}` | implement `IExpressionObjectFactory` + register via `IExpressionObjectDialect` |
| `AbstractMessageResolver.resolveMessage(Arguments,String,Object[]) → MessageResolution` | implement `IMessageResolver.resolveMessage(ITemplateContext, Class<?>, String, Object[]) → String` (+ `createAbsentMessageRepresentation(...)`) |
| `ServletContextTemplateResolver` | extend `SpringResourceTemplateResolver` (TL3.1 removed the servlet coupling); override `computeResourceName(...)` |
| custom `ITemplateResolver` | TL3 signature: `resolveTemplate(IEngineConfiguration, String ownerTemplate, String template, Map<String,Object> attrs) → TemplateResolution` |
| `AbstractGeneralTemplateWriter`, `ITemplateModeHandler`, template-mode-handler architecture | **Removed with no TL3 equivalent** (TL3 is immutable/streaming). Delete the wrapper classes; downstream XML refs are handled in the phase that owns them. |
| template mode string `"HTML5"` | `"HTML"` (TL3 `TemplateMode` only has HTML, XML, TEXT, JAVASCRIPT, CSS, RAW). |

---

## 3. Bytecode weaving + reflection (Hibernate 5.6, javassist 3.29 / ASM 9)

**Files:** `BroadleafClassTransformer`, `InterceptFieldClassFileTransformer`, `DirectCopyClassTransformer`,
`SingleTableInheritanceClassTransformer`, `MaterializedClobTypeClassTransformer`.

- The Hibernate 4 manual hook (`org.hibernate.ejb.use_class_enhancer` +
  `org.hibernate.ejb.instrument.InterceptFieldClassFileTransformer`) **was removed in Hibernate 5**. As of Hibernate 5
  bytecode enhancement is registered automatically by the JPA bootstrap based on `hibernate.enhancer.*` settings — do
  **not** re-register it manually in `MergePersistenceUnitManager`.
- Keep weaving on **javassist 3.29 / ASM 9**; **remove cglib**. The `DirectCopyClassTransformer` pattern (copy fields,
  methods and interfaces from a template class into a target's bytecode via `javassist.ClassPool`/`CtClass`) works
  unchanged on JDK 17 once on ASM 9.
- Register Hibernate SPI extensions via `META-INF/services/org.hibernate.service.spi.ServiceContributor` (replacing the
  old `Integrator`-based wiring where applicable).

### `MergePersistenceUnitManager` — the documented reflection exception
It reflects (`setAccessible`) into `org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager` private
members. Verified present and compatible in **Spring 5.3.39**:

- Fields: `persistenceXmlLocations` (`String[]`), `persistenceUnitInfoNames` (`Set`), `persistenceUnitInfos` (`Map`),
  `resourcePatternResolver` (`ResourcePatternResolver`).
- Methods: `readPersistenceUnitInfos()`, `determineDefaultPersistenceUnitRootUrl()`.
- Per-PU `init(LoadTimeWeaver)` / `init(ClassLoader)` live on the **package-private** `SpringPersistenceUnitInfo`
  (subclass of `MutablePersistenceUnitInfo`) — invoke via `mPui.getClass().getDeclaredMethod("init", ...)`.

This is the **explicit exception** to the avoid-reflection rule: no public API exists to inject merged persistence
units. Keep the reflection minimal and **pin it with a contract test** (see `MergePersistenceUnitManagerTest`) so a
future Spring bump that renames a member fails loudly.

### LTW / instrumentation flags
Reflecting into Spring's own classes (unnamed module) needs no flags. For load-time weaving and any reflection into the
JDK, append to surefire `argLine` (preserve the existing `${surefire.argLine}` jacoco hook — **append, don't replace**):

```
-javaagent:<path>/spring-instrument-5.3.39.jar
--add-opens java.base/java.lang=ALL-UNNAMED
```
*(Phase 1 `common` did not require these flags; add only if a module's LTW/merged-PU tests need them.)*

---

## 4. Spring XML cheatsheet

- **Velocity 2 engine** — `org.springframework.ui.velocity.VelocityEngineFactoryBean` is gone. Replace the bean class
  with a tiny module-local `FactoryBean<VelocityEngine>` that does `new VelocityEngine(); engine.init(props)`
  (see `org.broadleafcommerce.common.email.service.message.VelocityEngineFactoryBean`). The existing
  `<property name="velocityProperties"><value>…</value></property>` block is reusable unchanged.
- **Mail** — `org.springframework.mail.javamail.JavaMailSenderImpl` is unchanged in Spring 5; it now backs
  `com.sun.mail:javax.mail:1.6.2`. No XML change beyond the dependency swap.
- **Thymeleaf** — `org.thymeleaf.spring4.dialect.SpringStandardDialect` → `org.thymeleaf.spring5.dialect.SpringStandardDialect`.
- **Template mode** — `templateMode="HTML5"` → `templateMode="HTML"`.

---

## 5. Test recipes (JDK 17)

- **Discovery is the #1 gotcha.** Surefire 3 runs on the **JUnit Platform** (pulled in by Spock 2). Legacy
  **JUnit 3 (`extends junit.framework.TestCase`)** and **JUnit 4** tests are invisible to the platform provider unless
  the **JUnit Vintage engine** is on the test classpath. Add (pin the version — not managed in root):
  ```xml
  <dependency>
    <groupId>org.junit.vintage</groupId>
    <artifactId>junit-vintage-engine</artifactId>
    <version>5.9.0</version> <!-- aligns with junit-platform 1.9.x from Spock 2.3 -->
    <scope>test</scope>
  </dependency>
  ```
  Without this you will see only the Spock specs run (Phase 1 went from **1 → 36** discovered tests after adding it).
- **EasyMock 2 → 5**: `org.easymock.classextension.*` → `org.easymock.*` (class mocking folded into core; drop
  `easymockclassextension`).
- **TestNG 7**: no `jdk15` classifier.
- **Spock**: `spock.lang.Specification`; Spock 2 runs on the JUnit Platform — ensure surefire 3 + Groovy 4.
- **JUnit 4.13.2** for new tests.

### Run
```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn -pl <module> -am test
```

### Testing policy (REVISED)
- **All existing tests must pass.**
- Add **new tests only for code that was architecturally changed** in the phase (e.g. the merge/weaving subsystem).
  Do **not** blanket-test untouched classes.
- **There is NO 80% / numeric coverage gate. Do NOT add a JaCoCo `check`/gate rule.**

---

## 6. Per-module PR + rollout / rollback boilerplate

- Branch off the integration branch: `git checkout -b devin/$(date +%s)-java17-phase<N>-<module>` from `java17-upgrade`.
- **Modify only files under the module** (plus this playbook in Phase 1). Do **not** touch the root pom or other modules.
- **GATE** before opening the PR:
  ```bash
  JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn -pl <module> -am clean install
  ```
  must be GREEN on JDK 17.
- Open ONE PR titled `[java17] <module> (Phase N)` against **`java17-upgrade`** (never `main`). The orchestrator merges.
- **PR body:** coordinate swaps, API fixes, any merge/weaving/reflection adaptations, new tests, existing-test pass
  counts, and the rollout/rollback note below.
- **ROLLOUT:** merged into `java17-upgrade` only; `main` stays Java 7 until the whole reactor is green.
- **ROLLBACK:** revert this phase's merge commit on `java17-upgrade`. Each phase is an isolated, revertible unit; `main`
  is never affected.

---

## Phase status

| Phase | Module | Status |
|---|---|---|
| 0 | root pom (toolchain + staged versions) | merged |
| 1 | `common` (broadleaf-common) | this PR |
| 2+ | core / admin / integration modules | follow this playbook |
