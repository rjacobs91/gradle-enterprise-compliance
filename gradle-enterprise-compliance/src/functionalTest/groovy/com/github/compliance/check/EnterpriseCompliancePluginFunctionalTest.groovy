package com.github.compliance.check

import spock.lang.Specification
import spock.lang.TempDir
import org.gradle.testkit.runner.GradleRunner

class EnterpriseCompliancePluginFunctionalTest extends Specification {
    @TempDir
    private File projectDir

    private getBuildFile() {
        new File(projectDir, "build.gradle")
    }

    private getSettingsFile() {
        new File(projectDir, "settings.gradle")
    }

    def "can run task"() {
        given:
        settingsFile << ""
        buildFile << """
        plugins {
            id('gradle.compliance.check')
        }
        """

        when:
        def runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("complianceCheckRun")
        runner.withProjectDir(projectDir)
        def result = runner.build()

        then:
        result.output.contains("Checking for updates and analyzing dependencies for vulnerabilities")
    }
}
