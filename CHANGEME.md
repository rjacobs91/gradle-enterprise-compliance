# Gradle Compliance Plugin

Only major and minor version changes are documented.

## 1.0.0

- It's a new library, so basically everything has changed. Or nothing has changed, depending on your perspective.
- This version supports:
    - A shared vulnerability suppression file and runs
      the [OWASP Dependency Check](https://jeremylong.github.io/DependencyCheck/dependency-check-gradle/index.html)
      plugin. Additional vulnerability suppression files can be supplied
    - A shared permissive license bundle and runs
      the [License report](https://plugins.gradle.org/plugin/com.github.jk1.dependency-license-report) plugin. Packages
      with unknown licenses can be overriden as allowed licenses
    - A shared checkstyle bundle and runs
      the [Checkstyle](https://docs.gradle.org/current/userguide/checkstyle_plugin.html) plugin. Checkstyle
      configuration can be overriden
    - A shared SpotBugs exclusion file and runs the [SpotBugs](https://plugins.gradle.org/plugin/com.github.spotbugs)
      plugin. SpotBugs configuration can be overriden
    - A default code coverage rule and runs the [Jacoco](https://docs.gradle.org/current/userguide/jacoco_plugin.html)
      plugin. Additional rules can be supplied and the default rule can be disabled
    - A conversion from Jacoco coverage format to Cobertura coverage format by running
      the [Jacobo](https://plugins.gradle.org/plugin/com.kageiit.jacobo) plugin