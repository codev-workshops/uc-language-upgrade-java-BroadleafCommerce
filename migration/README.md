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
Phase 0  (toolchain / root POM)
   |
   v
Phase 1  (broadleaf-common)
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

| Phase | Module(s)                                                              | Parallelisable? |
|-------|------------------------------------------------------------------------|-----------------|
| 0     | Root POM / toolchain                                                   | -               |
| 1     | `common/` (broadleaf-common)                                           | No              |
| 2a    | `core/broadleaf-profile/`                                              | Yes (with 2b)   |
| 2b    | `admin/broadleaf-open-admin-platform/`                                 | Yes (with 2a)   |
| 3a    | `core/broadleaf-profile-web/`                                          | Yes (with 3b)   |
| 3b    | `admin/broadleaf-contentmanagement-module/`                            | Yes (with 3a)   |
| 4     | `core/broadleaf-framework/`                                            | No              |
| 5a    | `core/broadleaf-framework-web/`                                        | Yes (with 5b)   |
| 5b    | `admin/broadleaf-admin-module/`                                        | Yes (with 5a)   |
| 6     | `integration/`                                                         | No              |
| 7     | `admin/broadleaf-admin-functional-tests/`                              | No              |

## Playbooks

Each phase has a detailed playbook in `migration/playbooks/`:

- [Phase 0 - Toolchain](playbooks/phase0-toolchain.md)
- [Phase 1 - broadleaf-common](playbooks/phase1-broadleaf-common.md)
- [Phase 2a - broadleaf-profile](playbooks/phase2a-broadleaf-profile.md)
- [Phase 2b - broadleaf-open-admin-platform](playbooks/phase2b-broadleaf-open-admin-platform.md)
- [Phase 3a - broadleaf-profile-web](playbooks/phase3a-broadleaf-profile-web.md)
- [Phase 3b - broadleaf-contentmanagement-module](playbooks/phase3b-broadleaf-contentmanagement-module.md)
- [Phase 4 - broadleaf-framework](playbooks/phase4-broadleaf-framework.md)
- [Phase 5a - broadleaf-framework-web](playbooks/phase5a-broadleaf-framework-web.md)
- [Phase 5b - broadleaf-admin-module](playbooks/phase5b-broadleaf-admin-module.md)
- [Phase 6 - integration](playbooks/phase6-integration.md)
- [Phase 7 - broadleaf-admin-functional-tests](playbooks/phase7-broadleaf-admin-functional-tests.md)

## Execution

Phases can be executed by child Devin sessions. Parallel phases (e.g. 2a/2b) can run concurrently on separate branches and be merged sequentially.

Each playbook contains:
1. Target module path
2. Dependency/version edits scoped to that module
3. Import-rename map (e.g. `org.hibernate.ejb.*` -> `org.hibernate.jpa.*`)
4. Regression command
5. New-test requirements
