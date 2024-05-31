package com.github.compliance.check

import org.gradle.api.Action
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testing.jacoco.tasks.rules.JacocoViolationRule
import spock.lang.Specification
import com.github.compliance.check.extension.CheckStyleCheckExtension
import com.github.compliance.check.extension.JacocoCheckExtension
import com.github.compliance.check.extension.SpotBugsCheckExtension
import com.github.compliance.check.extension.VulnerabilityCheckExtension

import java.math.RoundingMode

class EnterpriseCompliancePluginTest extends Specification {
    def "plugin registers all relevant tasks and extensions"() {
        given:
        def project = ProjectBuilder.builder().build()

        when:
        project.plugins.apply("gradle.compliance.check")
        project.getTasksByName('complianceCheckRun', false) // forces a project evaluation

        then:
        project.tasks.findByName("complianceCheckRun") != null
        project.tasks.findByName("dependencyCheckAnalyze") != null
        project.tasks.findByName("copyFiles") != null
        project.tasks.findByName("checkstyleMain") != null
        project.tasks.findByName("checkstyleTest") != null
        project.tasks.findByName("spotbugsMain") != null
        project.tasks.findByName("spotbugsTest") != null
        project.tasks.findByName("checkLicense") != null
        project.tasks.findByName("generateLicenseReport") != null
        project.tasks.findByName("jacocoTestReport") != null
        project.tasks.findByName("jacocoTestCoverageVerification") != null
        project.tasks.findByName("jacocoLogTestCoverage") != null
        project.tasks.findByName("jacobo") != null
        project.tasks.findByName("sourcesJar") != null
        project.extensions.findByName("complianceVulnerabilityCheck") != null
        project.extensions.findByName("complianceSpotBugsCheck") != null
        project.extensions.findByName("complianceLicenseCheck") != null
        project.extensions.findByName("complianceJacocoCheck") != null
        project.extensions.findByName("complianceCheckStyleCheck") != null
        project.extensions.findByName("java") != null
    }

    def "plugin sets default coverage rule"() {
        given:
        def project = ProjectBuilder.builder().build()

        when:
        project.plugins.apply("gradle.compliance.check")
        project.getTasksByName('complianceCheckRun', false) // forces a project evaluation
        def expectedRule = (project.tasks.findByName("jacocoTestCoverageVerification")["violationRules"]["rules"] as ArrayList).get(0)

        then:
        expectedRule != null
        expectedRule["excludes"] == []
        (expectedRule["limits"] as ArrayList).get(0)["counter"] == "LINE"
        (expectedRule["limits"] as ArrayList).get(0)["value"] == "COVEREDRATIO"
        // Big decimal default constructor does not create an exact decimal
        (expectedRule["limits"] as ArrayList).get(0)["minimum"] == new BigDecimal(0.9).setScale(1, RoundingMode.HALF_EVEN)
    }

    def "plugin accepts rule override"() {
        given:
        def project = ProjectBuilder.builder().build()
        def newRules = [{
                            element = 'CLASS'
                            limit {
                                it.counter = 'LINE'
                                it.value = 'COVEREDRATIO'
                                it.minimum = 0.0
                            }
                            // It is acceptable to ignore some classes
                            excludes = []
                        }] as ArrayList<Action<? extends JacocoViolationRule>>

        when:
        project.plugins.apply("gradle.compliance.check")
        project.extensions.getByType(JacocoCheckExtension.class).applyDefaultRule = false
        project.extensions.getByType(JacocoCheckExtension.class).rules = newRules
        project.getTasksByName('complianceCheckRun', false) // forces a project evaluation
        def expectedRule = (project.tasks.findByName("jacocoTestCoverageVerification")["violationRules"]["rules"] as ArrayList).get(0)

        then:
        expectedRule != null
        expectedRule["excludes"] == []
        (expectedRule["limits"] as ArrayList).get(0)["counter"] == "LINE"
        (expectedRule["limits"] as ArrayList).get(0)["value"] == "COVEREDRATIO"
        // Big decimal default constructor does not create an exact decimal
        (expectedRule["limits"] as ArrayList).get(0)["minimum"] == new BigDecimal(0.0).setScale(1, RoundingMode.HALF_EVEN)
    }

    def "plugin sets appropriate checkstyle config directory"() {
        given:
        def project = ProjectBuilder.builder().build()

        when:
        project.plugins.apply("gradle.compliance.check")
        project.getTasksByName('complianceCheckRun', false) // forces a project evaluation

        then:
        (project.tasks.findByName("checkstyleMain")["configProperties"] as Map).get("basedir") != null
        (project.tasks.findByName("checkstyleTest")["configProperties"] as Map).get("basedir") != null
    }

    def "plugin sets custom checkstyle config file"() {
        given:
        def project = ProjectBuilder.builder().build()
        def customFile = getClass().getClassLoader().getResource("checkstyle/test-checkstyle.xml").path

        when:
        project.plugins.apply("gradle.compliance.check")
        project.extensions.getByType(CheckStyleCheckExtension.class).configFile = customFile
        project.getTasksByName('complianceCheckRun', false) // forces a project evaluation

        then:
        project.tasks.findByName("checkstyleMain")["configFile"]["path"] == customFile
    }

    def "plugin applies custom spotbugs exclusions"() {
        given:
        def project = ProjectBuilder.builder().build()
        def exclusionFile = getClass().getClassLoader().getResource("spotBugs/test-exclusion.xml").path

        when:
        project.plugins.apply("gradle.compliance.check")
        project.extensions.getByType(SpotBugsCheckExtension.class).exclusionFile = exclusionFile
        project.getTasksByName('complianceCheckRun', false) // forces a project evaluation

        then:
        project.extensions.findByName("spotbugs")["excludeFilter"] != null
    }

    def "plugin applies default spotbugs exclusions"() {
        given:
        def project = ProjectBuilder.builder().build()

        when:
        project.plugins.apply("gradle.compliance.check")
        project.getTasksByName('complianceCheckRun', false) // forces a project evaluation

        then:
        project.extensions.findByName("spotbugs")["excludeFilter"] != null
    }

    def "plugin applies custom dependency check suppressions"() {
        given:
        def project = ProjectBuilder.builder().build()
        def customSuppressionFile = getClass().getClassLoader().getResource("dependencyCheck/test-suppression.xml").path

        when:
        project.plugins.apply("gradle.compliance.check")
        project.extensions.getByType(VulnerabilityCheckExtension.class).additionalSuppressionFiles = [customSuppressionFile]
        project.getTasksByName('complianceCheckRun', false) // forces a project evaluation

        then:
        (project.extensions.findByName("dependencyCheck")["suppressionFiles"] as ArrayList).size() == 2
        project.extensions.findByName("dependencyCheck")["nvd"]["datafeedUrl"] == null
    }

    def "plugin uses local dependency check files"() {
        given:
        def project = ProjectBuilder.builder().build()

        when:
        project.plugins.apply("gradle.compliance.check")
        project.extensions.getByType(VulnerabilityCheckExtension.class).useDefaultVulnerabilityDatabase = false
        project.extensions.getByType(VulnerabilityCheckExtension.class).modifiedNvdFeed = 'https://nvd-cve.somedomain.com'

        project.getTasksByName('complianceCheckRun', false) // forces a project evaluation

        then:
        project.extensions.findByName("dependencyCheck")["nvd"]["datafeedUrl"] == 'https://nvd-cve.somedomain.com'
    }

    def "plugin applies license check exclusions"() {
        given:
        def project = ProjectBuilder.builder().build()

        when:
        project.plugins.apply("gradle.compliance.check")
        project.getTasksByName('complianceCheckRun', false) // forces a project evaluation

        then:
        def licenseCheckExclusions = [
                'com.github.compliance',
                'software.amazon.codeguruprofiler',
                'org.junit',
                'com.fasterxml.jackson',
                'com.hubspot.jackson',
                'javax.ws.rs',
                'net.jcip',
                'dom4j',
                'io.netty',
                'com.h2database',
                'org.openapitools',
                'org.jetbrains.kotlinx',
                'org.jetbrains.kotlin',
                'org.javamoney',
                'com.amazonaws',
                'axis',
                'org.apache.axis',
                'com.squareup.okio',
                'com.squareup.wire',
                'com.charleskorn.kaml',
                'org.antlr',
                'com.netflix.graphql.dgs',
        ]
        project.extensions.findByName("licenseReport")["excludeGroups"] == licenseCheckExclusions
    }
}
