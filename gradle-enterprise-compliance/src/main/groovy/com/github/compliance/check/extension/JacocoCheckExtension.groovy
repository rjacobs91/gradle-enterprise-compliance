package com.github.compliance.check.extension

import org.gradle.api.Action
import org.gradle.testing.jacoco.tasks.rules.JacocoViolationRule

class JacocoCheckExtension {

    /**
     * Use the default rule in the compliance plugin, which requires 90% code coverage across an entire module.
     */
    boolean applyDefaultRule = true

    /**
     * Additional coverage rules to apply. Recommended to populate if {@link #applyDefaultRule} is set to false.
     */
    List<Action<? extends JacocoViolationRule>> rules = []
}
