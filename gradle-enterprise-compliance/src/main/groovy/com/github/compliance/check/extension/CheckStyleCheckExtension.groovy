package com.github.compliance.check.extension

class CheckStyleCheckExtension {

    /**
     * It is not possible to add additional CheckStyle exclusions, we need to override the default file path. We can
     * also supply gradle evaluable variables.
     * e.g. $rootDir/config/checkstyle/checkstyle.xml
     */
    String configFile
}
