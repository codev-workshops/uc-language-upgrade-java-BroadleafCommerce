# Phase 3a - broadleaf-profile-web

## Target Module

`core/broadleaf-profile-web/` (`org.broadleafcommerce:broadleaf-profile-web`)

## Prerequisites

- Phase 2a (broadleaf-profile) complete
- Phase 2b (broadleaf-open-admin-platform) complete

## Dependency / Version Edits

- Verify all versions resolve from parent
- Update any Spring MVC or Spring Security references

## Import-Rename Map

| Old Import | New Import |
|---|---|
| `org.hibernate.ejb.*` | `org.hibernate.jpa.*` |
| `org.thymeleaf.spring4.*` | `org.thymeleaf.spring5.*` |
| `org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping` | (same, verify API) |
| `org.springframework.security.web.util.matcher.AntPathRequestMatcher` | (same, verify constructor) |

## Key Refactoring Tasks

- Spring MVC: `WebMvcConfigurerAdapter` -> `WebMvcConfigurer` (interface with defaults in Spring 5)
- Spring Security filter chain configuration changes
- Review login/registration controllers for Spring Security 5.x API changes
- `HandlerInterceptorAdapter` -> `HandlerInterceptor` (deprecated adapter removed)

## Regression Command

```bash
mvn -pl core/broadleaf-profile-web -am test
```

## New-Test Requirements

- Verify web-tier security filters compile and initialize under Spring Security 5.8
