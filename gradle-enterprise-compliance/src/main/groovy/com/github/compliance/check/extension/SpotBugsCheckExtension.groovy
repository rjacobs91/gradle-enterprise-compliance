package com.github.compliance.check.extension

class SpotBugsCheckExtension {

    /**
     * It is not possible to add additional SpotBugs exclusions, we need to override the default file path. We can also
     * supply gradle evaluable variables.
     * e.g. $rootDir/config/spotBugs/exclusion.xml
     */
    String exclusionFile
}
