package com.github.compliance.check

import com.github.jk1.license.LicenseReportExtension
import com.github.jk1.license.LicenseReportPlugin
import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.task.CheckLicenseTask
import com.github.spotbugs.snom.SpotBugsExtension
import com.github.spotbugs.snom.SpotBugsPlugin
import com.github.spotbugs.snom.SpotBugsReport
import com.github.spotbugs.snom.SpotBugsTask
import com.kageiit.jacobo.JacoboPlugin
import com.kageiit.jacobo.JacoboTask
import org.barfuin.gradle.jacocolog.JacocoLogPlugin
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.plugins.quality.CheckstylePlugin
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.owasp.dependencycheck.gradle.DependencyCheckPlugin
import org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension
import com.github.compliance.check.extension.CheckStyleCheckExtension
import com.github.compliance.check.extension.JacocoCheckExtension
import com.github.compliance.check.extension.LicenseCheckExtension
import com.github.compliance.check.extension.SpotBugsCheckExtension
import com.github.compliance.check.extension.VulnerabilityCheckExtension

class EnterpriseCompliancePlugin implements Plugin<Project> {

    // Group constants
    private static final String COMPLIANCE = 'compliance'

    // Task constants
    private static final String DEPENDENCY_CHECK_ANALYZE_TASK = 'dependencyCheckAnalyze'
    private static final String COMPLIANCE_CHECK_TASK = 'complianceCheckRun'
    private static final String COPY_FILES_TASK = 'copyFiles'
    private static final String CHECK_TASK = 'check'
    private static final String TEST_TASK = 'test'
    private static final String JACOCO_TEST_REPORT_TASK = 'jacocoTestReport'
    private static final String JACOBO_TASK = 'jacobo'

    // Extension constants
    private static final String COMPLIANCE_VULNERABILITY_CHECK_EXTENSION = 'complianceVulnerabilityCheck'
    private static final String COMPLIANCE_SPOT_BUGS_CHECK_EXTENSION = 'complianceSpotBugsCheck'
    private static final String COMPLIANCE_JACOCO_CHECK_EXTENSION = 'complianceJacocoCheck'
    private static final String COMPLIANCE_LICENSE_CHECK_EXTENSION = 'complianceLicenseCheck'
    private static final String COMPLIANCE_CHECK_STYLE_CUSTOM_CONFIG_EXTENSION = 'complianceCheckStyleCheck'

    // Resource constants
    private static final String SUPPRESSION_RESOURCE = 'dependencyCheck/suppression.xml'
    private static final String CHECKSTYLE_RESOURCE = 'checkstyle/checkstyle.xml'
    private static final String CHECKSTYLE_NO_FRAMES_RESOURCE = 'checkstyle/checkstyle-noframes-sorted.xsl'
    private static final String LICENSE_BUNDLE_RESOURCE = 'licenseCheck/license-normalizer-bundle.json'
    private static final String PERMISSIVE_LICENSES_RESOURCE = 'licenseCheck/permissive-licenses.json'
    private static final String EXCLUSION_RESOURCE = 'spotBugs/exclusion.xml'

    // Version constants
    private static final String CHECKSTYLE_VERSION = '8.44'
    private static final String SPOTBUGS_VERSION = '4.7.3'
    private static final String JACOCO_VERSION = '0.8.8'

    void apply(Project project) {
        def complianceVulnerabilityCheck = initialiseVulnerabilityCheckConfiguration(project)
        def complianceSpotBugsCheck = initialiseSpotBugsCheckConfiguration(project)
        def complianceJacocoCheck = initialiseJacocoCheckConfiguration(project)
        def complianceLicenseCheck = initialiseLicenseCheckConfiguration(project)
        def checkStyleCustomConfig = initialiseCheckStyleCheckConfiguration(project)
        initialiseDependentPlugins(project)
        def copyFiles = initialiseCopyFilesTask(project)
        configureJava(project)
        configureDependencyCheck(project, complianceVulnerabilityCheck, copyFiles)
        configureCheckstyle(project, checkStyleCustomConfig, copyFiles)
        configureSpotBugs(project, complianceSpotBugsCheck, copyFiles)
        configureLicenseCheck(project, complianceLicenseCheck, copyFiles)
        configureJacoco(project, complianceJacocoCheck)
        configureJacobo(project)
        registerComplianceCheckTask(project)
    }

    private static VulnerabilityCheckExtension initialiseVulnerabilityCheckConfiguration(Project project) {
        return project.extensions.create(COMPLIANCE_VULNERABILITY_CHECK_EXTENSION, VulnerabilityCheckExtension)
    }

    private static SpotBugsCheckExtension initialiseSpotBugsCheckConfiguration(Project project) {
        return project.extensions.create(COMPLIANCE_SPOT_BUGS_CHECK_EXTENSION, SpotBugsCheckExtension)
    }

    private static JacocoCheckExtension initialiseJacocoCheckConfiguration(Project project) {
        return project.extensions.create(COMPLIANCE_JACOCO_CHECK_EXTENSION, JacocoCheckExtension)
    }

    private static LicenseCheckExtension initialiseLicenseCheckConfiguration(Project project) {
        return project.extensions.create(COMPLIANCE_LICENSE_CHECK_EXTENSION, LicenseCheckExtension)
    }

    private static CheckStyleCheckExtension initialiseCheckStyleCheckConfiguration(Project project) {
        return project.extensions.create(COMPLIANCE_CHECK_STYLE_CUSTOM_CONFIG_EXTENSION, CheckStyleCheckExtension)
    }

    private static void initialiseDependentPlugins(Project project) {
        project.getPluginManager().apply(JavaPlugin.class)
        project.getPluginManager().apply(DependencyCheckPlugin.class)
        project.getPluginManager().apply(LicenseReportPlugin.class)
        project.getPluginManager().apply(CheckstylePlugin.class)
        project.getPluginManager().apply(SpotBugsPlugin.class)
        project.getPluginManager().apply(JacocoPlugin.class)
        project.getPluginManager().apply(JacoboPlugin.class)
        project.getPluginManager().apply(JacocoLogPlugin.class)
    }

    private static Task initialiseCopyFilesTask(Project project) {
        def copyFiles = project.tasks.create(COPY_FILES_TASK)

        copyFiles.configure(initialiseFiles(project))
        copyFiles.doLast(initialiseFiles(project))
    }

    private static Closure initialiseFiles(Project project) {
        return {
            def dependencyCheckFolder = project.mkdir "$project.buildDir/config/dependencyCheck"
            def checkstyleFolder = project.mkdir "$project.buildDir/config/checkstyle"
            def licenseCheckFolder = project.mkdir "$project.buildDir/config/licenseCheck"
            def spotBugsFolder = project.mkdir "$project.buildDir/config/spotBugs"

            project.file("$dependencyCheckFolder/defaultSuppression.xml").text = getClass().getClassLoader().getResource(SUPPRESSION_RESOURCE).text

            project.file("$spotBugsFolder/defaultExclusion.xml").text = getClass().getClassLoader().getResource(EXCLUSION_RESOURCE).text

            project.file("$checkstyleFolder/checkstyle.xml").text = getClass().getClassLoader().getResource(CHECKSTYLE_RESOURCE).text
            project.file("$checkstyleFolder/checkstyle-noframes-sorted.xsl").text = getClass().getClassLoader().getResource(CHECKSTYLE_NO_FRAMES_RESOURCE).text

            project.file("$licenseCheckFolder/license-normalizer-bundle.json").text = getClass().getClassLoader().getResource(LICENSE_BUNDLE_RESOURCE).text
            project.file("$licenseCheckFolder/permissive-licenses.json").text = getClass().getClassLoader().getResource(PERMISSIVE_LICENSES_RESOURCE).text
        }
    }

    private static void configureJava(Project project) {
        project.afterEvaluate {
            def java = project.extensions.findByType(JavaPluginExtension.class)
            java.withSourcesJar()
        }
    }

    private static void configureDependencyCheck(Project project, VulnerabilityCheckExtension complianceVulnerabilityCheck, Task copyFiles) {
        // We have to configure the dependent plugin extensions after project evaluation in order to guarantee access to
        // other plugin tasks and extensions at runtime
        project.afterEvaluate {
            def dependencyCheck = project.extensions.findByType(DependencyCheckExtension.class)
            def defaultSuppressionFile = "$project.buildDir/config/dependencyCheck/defaultSuppression.xml".toString()
            dependencyCheck.failBuildOnCVSS = complianceVulnerabilityCheck.failBuildOnCVSS
            dependencyCheck.suppressionFiles = [defaultSuppressionFile] + complianceVulnerabilityCheck.additionalSuppressionFiles

            if (!complianceVulnerabilityCheck.useDefaultVulnerabilityDatabase) {
                dependencyCheck.cve.urlModified = complianceVulnerabilityCheck.modifiedCveFeed
                dependencyCheck.cve.urlBase = complianceVulnerabilityCheck.baseCveFeed
            }
        }

        project.tasks.findByName(DEPENDENCY_CHECK_ANALYZE_TASK).dependsOn copyFiles
    }

    private static void configureJacoco(Project project, JacocoCheckExtension complianceJacocoCheck) {
        // We have to configure the dependent plugin extensions after project evaluation in order to guarantee access to
        // other plugin tasks and extensions at runtime
        project.afterEvaluate {
            def jacoco = project.extensions.findByType(JacocoPluginExtension.class)
            jacoco.toolVersion = JACOCO_VERSION

            def check = project.tasks.findByName(CHECK_TASK)
            project.tasks.withType(JacocoCoverageVerification.class).forEach(task -> {
                // Apply default rule of 90% line coverage across the entire project using the plugin
                if (complianceJacocoCheck.applyDefaultRule) {
                    complianceJacocoCheck.rules.add({
                        element = 'BUNDLE'
                        limit({
                            // Need the explicit it here to refer to inner nested closure, otherwise we attempt to set
                            // property on outer closure variable
                            it.counter = 'LINE'
                            it.value = 'COVEREDRATIO'
                            it.minimum = 0.9
                        })
                    })
                }

                complianceJacocoCheck.rules.forEach(rule -> {
                    task.violationRules.rule(rule)
                })

                check.dependsOn(task)
            })
        }

        project.tasks.withType(JacocoReport.class).forEach(task -> {
            task.reports {
                xml.enabled true
                csv.enabled false
                html.enabled true
            }
        })

        def test = project.tasks.findByName(TEST_TASK)
        def jacocoTestReport = project.tasks.findByName(JACOCO_TEST_REPORT_TASK)
        test.finalizedBy(jacocoTestReport)
    }

    private static void configureJacobo(Project project) {
        def jacobo = project.tasks.create(JACOBO_TASK, JacoboTask.class)
        jacobo.jacocoReport = project.file("$project.buildDir/reports/jacoco/test/jacocoTestReport.xml")
        jacobo.coberturaReport = project.file("$project.buildDir/reports/cobertura/cobertura.xml")
        def sourceSets = project.getExtensions().findByType(JavaPluginExtension.class).getSourceSets()
        jacobo.srcDirs = sourceSets.getByName('main').java.srcDirs
        def jacocoTestReport = project.tasks.findByName(JACOCO_TEST_REPORT_TASK)
        jacobo.dependsOn(jacocoTestReport)
        def check = project.tasks.findByName(CHECK_TASK)
        check.dependsOn(jacobo)
    }

    private static void configureCheckstyle(Project project, CheckStyleCheckExtension complianceCheckStyleCheck, Task copyFiles) {
        def checkstyleFolder = "$project.buildDir/config/checkstyle"

        // We have to configure the dependent plugin extensions after project evaluation in order to guarantee access to
        // other plugin tasks and extensions at runtime
        project.afterEvaluate {
            def checkStyle = project.extensions.findByType(CheckstyleExtension.class)
            checkStyle.toolVersion = CHECKSTYLE_VERSION

            def checkStyleConfigFile

            if (complianceCheckStyleCheck.configFile != null && !complianceCheckStyleCheck.configFile.isBlank()) {
                checkStyleConfigFile = project.file(complianceCheckStyleCheck.configFile)
            } else {
                checkStyleConfigFile = project.file("$checkstyleFolder/checkstyle.xml")
            }

            // These task properties should ideally be set on the extension but there is a bug within checkstyle
            project.tasks.withType(Checkstyle.class).forEach(task -> {
                task.configProperties = ["basedir": checkstyleFolder.toString()]
                task.configFile = checkStyleConfigFile
                task.reports {
                    xml.required = false
                    html.required = true
                    html.stylesheet = project.resources.text.fromFile(project.file("$checkstyleFolder/checkstyle-noframes-sorted.xsl"))
                }

                task.dependsOn(copyFiles)
            })
        }
    }

    private static void configureSpotBugs(Project project, SpotBugsCheckExtension complianceSpotBugsCheck, Task copyFiles) {
        def defaultExclusion = "$project.buildDir/config/spotBugs/defaultExclusion.xml"

        // We have to configure the dependent plugin extensions after project evaluation in order to guarantee access to
        // other plugin tasks and extensions at runtime
        project.afterEvaluate {
            def spotbugs = project.extensions.findByType(SpotBugsExtension.class)
            spotbugs.toolVersion.set(SPOTBUGS_VERSION)

            if (complianceSpotBugsCheck.exclusionFile != null && !complianceSpotBugsCheck.exclusionFile.isBlank()) {
                spotbugs.excludeFilter.set(project.file(complianceSpotBugsCheck.exclusionFile))
            } else {
                spotbugs.excludeFilter.set(project.file(defaultExclusion))
            }
        }

        project.tasks.withType(SpotBugsTask.class).forEach(task -> {
            task.reports(({
                xml.enabled = false
                html.enabled = true
            } as Closure<NamedDomainObjectContainer<? extends SpotBugsReport>>))
            task.dependsOn(copyFiles)
        })
    }

    private static void configureLicenseCheck(Project project, LicenseCheckExtension complianceLicenseCheck, Task copyFiles) {
        def licenseCheckFolder = "$project.buildDir/config/licenseCheck"
        def licenseBundle = "$licenseCheckFolder/license-normalizer-bundle.json"
        def permissiveLicenses = "$licenseCheckFolder/permissive-licenses.json"
        def defaultExcludedGroups = [
                // Add sensible default groups here which do not match patterns within license-normalizer-bundle.json or our extension
                'software.amazon.codeguruprofiler', // Returns `null` as licence, but is Apache https://mvnrepository.com/artifact/software.amazon.codeguruprofiler/codeguru-profiler-java-agent
                'org.junit', // Returns `null` as licence, but is EPL 2.0 https://mvnrepository.com/artifact/org.junit/junit-bom
                'com.fasterxml.jackson', // Returns `null` as licence, but is Apache https://mvnrepository.com/artifact/com.fasterxml.jackson/jackson-bom
                'com.hubspot.jackson', // Returns `null` as licence, but is Apache https://github.com/HubSpot/jackson-datatype-protobuf/blob/master/LICENSE.txt
                'javax.ws.rs', // Returns `null` as licence, but is EPL 2.0 https://mvnrepository.com/artifact/javax.ws.rs/javax.ws.rs-api
                'net.jcip', // Returns `null` as licence, but is Apache https://github.com/stephenc/jcip-annotations/blob/master/LICENSE.txt
                'dom4j', // Returns `null` as licence, but is BSD https://github.com/dom4j/dom4j/blob/master/LICENSE
                'io.netty', // Returns `null` as licence, but is Apache https://mvnrepository.com/artifact/io.netty/netty-tcnative-classes
                'com.h2database', // Returns `null` as licence, but is EPL 1.0 MPL 2.0 https://mvnrepository.com/artifact/com.h2database/h2
                'org.openapitools', // Returns `null` as licence, but is Apache https://github.com/OpenAPITools/jackson-databind-nullable/blob/master/LICENSE
                'org.jetbrains.kotlinx', // Returns `null` as licence, but is Apache https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
                'org.jetbrains.kotlin', // Returns `null` as licence, but is Apache https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-bom
                'org.javamoney', // Returns `null` as license, but is Apache https://mvnrepository.com/artifact/org.javamoney/moneta
                'com.amazonaws', // Returns `null` as licence, but is Apache 2.0 https://mvnrepository.com/artifact/com.amazonaws/aws-java-sdk
                'axis', // Returns `null` as licence, but is Apache 2.0 https://mvnrepository.com/artifact/axis/axis
                'org.apache.axis', // Returns `null` as licence, but is Apache https://mvnrepository.com/artifact/org.apache.axis/axis
                'com.squareup.okio', // Returns `null` as licence, but is Apache 2.0 https://mvnrepository.com/artifact/com.squareup.okio/okio-multiplatform
                'com.squareup.wire', // Returns `null` as licence, but is Apache 2.0 https://mvnrepository.com/artifact/com.squareup.wire/wire-runtime-multiplatform
                'com.charleskorn.kaml', // Returns `null` as licence, but is Apache 2.0 https://mvnrepository.com/artifact/com.charleskorn.kaml/kaml
                'org.antlr', // Returns 'null' but is the BSD license https://www.antlr.org/license.html
                'com.netflix.graphql.dgs', // Returns 'null' but is Apache 2.0 https://github.com/Netflix/dgs-framework/blob/master/LICENSE
        ]

        // We have to configure the dependent plugin extensions after project evaluation in order to guarantee access to
        // other plugin tasks and extensions at runtime
        project.afterEvaluate {
            def licenseReport = project.extensions.findByType(LicenseReportExtension.class)
            licenseReport.filters = [new LicenseBundleNormalizer(licenseBundle, true)]
            licenseReport.excludeGroups = complianceLicenseCheck.additionalExcludedGroups.addAll(defaultExcludedGroups)
            licenseReport.allowedLicensesFile = new File(permissiveLicenses)
        }

        def check = project.tasks.findByName(CHECK_TASK)
        project.tasks.withType(CheckLicenseTask.class).forEach(task -> {
            check.dependsOn(task)
            task.dependsOn(copyFiles)
        })
    }

    private static void registerComplianceCheckTask(Project project) {
        // Our task triggers the other relevant plugin tasks
        project.tasks.register(COMPLIANCE_CHECK_TASK) {
            finalizedBy project.tasks.getByName(DEPENDENCY_CHECK_ANALYZE_TASK)
            setGroup COMPLIANCE
        }
    }
}
