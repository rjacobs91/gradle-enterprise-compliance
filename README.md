# Gradle Compliance Plugin

This plugin performs standard compliance checks or suppressions against JVM based repos. Currently, this is limited to:

- Running [OWASP vulnerability dependency analysis](https://jeremylong.github.io/DependencyCheck/dependency-check-gradle/index.html)
on demand
- Running [Checkstyle analysis](https://docs.gradle.org/current/userguide/checkstyle_plugin.html) automatically
- Running [SpotBugs analysis](https://plugins.gradle.org/plugin/com.github.spotbugs) automatically
- Running [License report](https://plugins.gradle.org/plugin/com.github.jk1.dependency-license-report) automatically
- Running [Jacoco coverage analysis](https://docs.gradle.org/current/userguide/jacoco_plugin.html) and associated
  reporting automatically
- Running [Jacobo](https://plugins.gradle.org/plugin/com.kageiit.jacobo) plugin which converts from Jacobo coverage 
reporting format to Cobertura reporting format

The benefit of using this plugin in your repo is that we can maintain a central place for standards and suppressions,
rather than diverging project to project.

## How this could help on-call

If there are failures in pipelines due to licensing, vulnerabilities or generic spotbugs rules we can make relevant
changes here and all implementing repos get the benefit.

- For pipelines, the newest version of the plugin is always brought in, so if there are failures you'd only need to
  re-run the project pipeline
- For local runs if you need to pick up the latest version run something along the lines
  of `./gradlew resolveAndLockAll --write-locks --refresh-dependencies`

## How to add to your project

1. Add the following to your project's `settings.gradle` file **on the first line**

```groovy
pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}
```

2. Add `id 'gradle.compliance.check' version '1.X.+'` (or `id 'gradle.compliance.check' version '1.+'` if you're feeling
   confident) to the `plugins` section of `build.gradle` where `X` is the latest minor version
3. Remove `id 'org.owasp.dependencycheck' version 'X.X.X''` plugin and config, if present 
4. Replace usages of gradle task `dependencyCheckAnalyze` with `complianceCheckRun`
5. Remove `id 'java'` plugin and config, if present
6. Remove `id 'checkstyle'` plugin and config, if present
7. Apply the following in `build.gradle` if you need to extend default vulnerability suppressions

```groovy
complianceVulnerabilityCheck {
    additionalSuppressionFiles = ["path_to_my_file"]
}
```

7. Remove `id 'com.github.spotbugs'` plugin and config, if present
8. If you need to override the default
   SpotBugs [config](gradle-enterprise-compliance/src/main/resources/spotBugs/exclusion.xml), add the following
   in `build.gradle` if you need to apply SpotBugs exclusions. Sadly this is **not** additive, unlike our other plugin
   features

```groovy
complianceSpotBugsCheck {
    exclusionFile = "path_to_my_file"
}
```

9. Remove `id 'com.github.jk1.dependency-license-report' version 'X.X'` plugin and config, if present
10. Remove `id 'jacoco'` plugin and config, if present
11. Remove `id 'com.kageiit.jacobo' version 'X.X.X'` plugin and config, if present
13. Apply the following if you need to override the default Jacoco rule of 90% line coverage across your module

```groovy
complianceJacocoCheck {
    applyDefaultRule = false
    rules = [{
                 element = 'BUNDLE'
                 limit {
                     it.counter = 'LINE'
                     it.value = 'COVEREDRATIO'
                     it.minimum = 0.8
                 }
                 // It is acceptable to ignore some classes e.g. config
                 excludes = []
             }]
}
```

## Etiquette

This plugin benefits lots of teams and by keeping it up-to-date we can eliminate future work. Please consider the
following when making any changes:

- Add
  to [license-normalizer-bundle.json](gradle-enterprise-compliance/src/main/resources/licenseCheck/license-normalizer-bundle.json)
  if you find a bundle transformation rule is missing
- Add to [EnterpriseCompliancePlugin](gradle-enterprise-compliance/src/main/groovy/com/github/compliance/check/EnterpriseCompliancePlugin.groovy)
  any licenses which might be null from the license report for your repo