package com.github.compliance.check.extension

class LicenseCheckExtension {

    /**
     * List of additional groups to ignore from license checks. Populate this param with internally managed groups or
     * groups which do not have known license types in Maven central. We have `defaultExcludedGroups` as a best effort
     * in {@link com.github.compliance.check.EnterpriseCompliancePlugin}.
     * e.g. ['com.mycompany', 'io.netty', ...]
     */
    List<String> additionalExcludedGroups = []
}
