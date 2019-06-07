/**
 * ============================================================================
 *  Copyright ©  2015-2019,    Cristiano V. Gavião
 *
 *  All rights reserved.
 *  This program and the accompanying materials are made available under
 *  the terms of the Eclipse Public License v1.0 which accompanies this
 *  distribution and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * ============================================================================
 */
package br.com.c8tech.tools.maven.plugin.subsystems;

import java.util.HashMap;
import java.util.Map;

import org.apache.aries.subsystem.core.archive.SubsystemManifestVersionHeader;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * 
 * @author Cristiano Gavião
 *
 */
public class PojoSubsystemManifestConfiguration {

    public enum ProvisionPolicy {
        ACCEPT_DEPENDENCIES("acceptDependencies"), REJECT_DEPENDENCIES(
                "rejectDependencies");

        private static class Holder {
            static final Map<String, ProvisionPolicy> CODE_MAP = new HashMap<>();

            private Holder() {
            }
        }

        private final String name;

        ProvisionPolicy(String name) {
            this.name = name;
            Holder.CODE_MAP.put(name, this);
        }

        public static ProvisionPolicy convertFromString(String code) {
            return Holder.CODE_MAP.get(code);
        }

        public String getName() {
            return name;
        }
    }

    /**
     * The Export-Package header declares the exported packages for a Scoped
     * Subsystem.
     */
    @Parameter(alias = "Export-Package")
    private String exportPackage;

    /**
     * The Import-Package header declares the imported packages for a Scoped
     * Subsystem.
     */
    @Parameter(alias = "Import-Package")
    private String importPackage;

    /**
     * The Preferred-Provider header declares a list artifacts and Subsystems
     * which are the providers of capabilities that are preferred when wiring
     * the requirements of a Scoped Subsystem.
     */
    @Parameter(alias = "Preferred-Provider")
    private String preferredProvider;

    /**
     * The Provide-Capability header declares the capabilities exported for a
     * Scoped Subsystem.
     */
    @Parameter(alias = "Provide-Capability")
    private String provideCapability;

    /**
     * This is a directive used to set the provision-policy of the subsystem.
     * <p>
     * There are two policies defined by the spec:
     * 
     * rejectDependencies or acceptDependencies
     * 
     * @see ProvisionPolicy
     */
    @Parameter()
    private ProvisionPolicy provisionPolicy;

    /**
     * The Require-Bundle header declares the required artifacts for a Scoped
     * Subsystem.
     */
    @Parameter(alias = "Require-Bundle")
    private String requireBundle;

    /**
     * The Require-Capability header declares the required capabilities for a
     * Scoped Subsystem.
     */
    @Parameter(alias = "Require-Capability")
    private String requireCapability;

    /**
     * The Subsystem-Category header identifies the categories of the subsystem
     * as a comma-delimited list.
     * <p>
     * 
     * eg: Subsystem-Category: osgi, test, nursery
     */
    @Parameter(alias = "Subsystem-Category")
    private String subsystemCategory;

    /**
     * The Subsystem-ContactAddress header identifies the contact address where
     * problems with the subsystem may be reported; for example, an email
     * address.
     * <p>
     * 
     * eg: Subsystem-ContactAddress: 2400 Oswego Road, Austin, TX 74563
     */
    @Parameter(alias = "Subsystem-ContactAddress")
    private String subsystemContactAddress;

    /**
     * The Subsystem-Content header lists requirements for testResources that
     * are considered to be the contents of this Subsystem.
     */
    @Parameter(alias = "Subsystem-Content")
    private String subsystemContent;

    /**
     * TThe Subsystem-Copyright header identifies the subsystem's copyright
     * information.
     * <p>
     * eg: Subsystem-Copyright: C4Biz (c) 2012-2015
     */
    @Parameter(alias = "Subsystem-Copyright")
    private String subsystemCopyright;

    /**
     * The Subsystem-Description header defines a human-readable description for
     * this Subsystem, which can potentially be localized.
     */
    @Parameter(alias = "Subsystem-Description")
    private String subsystemDescription;

    /**
     * The Subsystem-DocURL header identifies the subsystem's documentation URL,
     * from which further information about the subsystem may be obtained.
     * <p>
     * 
     * eg: Subsystem-DocURL: http://www.example.com/Firewall/doc
     */
    @Parameter(alias = "Subsystem-DocURL")
    private String subsystemDocURL;

    /**
     * The Subsystem-ExportService header specifies the exported services for a
     * Scoped Subsystem.
     */
    @Parameter(alias = "Subsystem-ExportService")
    private String subsystemExportService;

    /**
     * The optional Subsystem-Icon header provides a list of URLs to icons
     * representing this subsystem in different sizes.
     * <p>
     * The following attribute is permitted:
     * <li>size - (integer) Specifies the size of the icon in pixels horizontal.
     * It is recommended to always include a 64x64 icon.
     * <p>
     * The URLs are interpreted as relative to the subsystem archive. <br>
     * That is, if a URL with a scheme is provided, then this is taken as an
     * absolute URL. Otherwise, the path points to an entry in the subsystem
     * archive file. *
     * <p>
     * 
     * eg: Subsystem-Icon: /icons/acme-logo.png; size=64
     */
    @Parameter(alias = "Subsystem-Icon")
    private String subsystemIcon;

    /**
     * The Subsystem-ImportService header specifies the imported services for a
     * Scoped Subsystem.
     */
    @Parameter(alias = "Subsystem-ImportService")
    private String subsystemImportService;

    /**
     * The Subsystem-License header provides an optional machine readable form
     * of license information.
     * <p>
     * The purpose of this header is to automate some of the license processing
     * required by many organizations like for example license acceptance before
     * a subsystem is used.
     * <p>
     * The header is structured to provide the use of unique license naming to
     * merge acceptance requests, as well as links to human readable information
     * about the included licenses. This header is purely informational for
     * management agents and must not be processed by the Subsystems
     * implementation.
     * <p>
     * eg: Subsystem-License: http://www.opensource.org/licenses/jabberpl.php
     */
    @Parameter(alias = "Subsystem-License")
    private String subsystemLicense;

    /**
     * The Subsystem-Localization header identifies the default base name of the
     * localization properties files contained in the subsystem archive.
     * <p>
     * The default value is:
     * 
     * <pre>
     * OSGI - INF / l10n / subsystem
     * </pre>
     * 
     * Translations are therefore, by default,
     * OSGI-INF/l10n/subsystem_de.properties , OSGI-INF/l10n/
     * subsystem_nl.properties , and so on. <br>
     * The location is relative to the root of the subsystem archive.
     * <p>
     * eg: Subsystem-Localization: OSGI-INF/l10n/subsystem
     */
    @Parameter(alias = "Subsystem-Localization",
            defaultValue = "OSGI-INF/l10n/subsystem")
    private String subsystemLocalization;

    /**
     * The version of the Subsystem manifest spec version.
     */
    @Parameter(alias = "Subsystem-ManifestVersion", readonly = true)
    private final String subsystemManifestVersion = SubsystemManifestVersionHeader.DEFAULT_VALUE
            .toString();

    /**
     * The Subsystem-Name header defines a short, human-readable name for this
     * Subsystem which may be localized. This should be a short, human-readable
     * name that can contain spaces.
     */
    @Parameter(alias = "Subsystem-Name")
    private String subsystemName;

    /**
     * The Subsystem-SymbolicName header specifies a non-localizable name for
     * this Subsystem. The Sub- system symbolic name together with a version
     * identify a Subsystem Definition though a Subsystem can be installed
     * multiple times in a framework. The Subsystem symbolic name should be
     * based on the reverse domain name convention.
     */
    @Parameter(alias = "Subsystem-SymbolicName")
    private String subsystemSymbolicName;

    /**
     * The Subsystem-Type header specifies the type for this Subsystem.
     * <p>
     * Three types of Subsystems must be supported: subsystem-application,
     * subsystem-composite and subsystem-feature. This value is set
     * automatically using the plugin configuration value.
     * 
     */
    private String subsystemType;

    /**
     * The Subsystem-Vendor header contains a human-readable description of the
     * subsystem vendor.
     * <p>
     * eg: Subsystem-Vendor: OSGi Alliance
     */
    @Parameter(alias = "Subsystem-Vendor")
    private String subsystemVendor;

    /**
     * The Subsystem-Version header specifies the version of this Subsystem.
     */
    @Parameter(alias = "Subsystem-Version")
    private String subsystemVersion;

    public String getExportPackage() {
        return exportPackage;
    }

    public String getImportPackage() {
        return importPackage;
    }

    public String getPreferredProvider() {
        return preferredProvider;
    }

    public String getProvideCapability() {
        return provideCapability;
    }

    public ProvisionPolicy getProvisionPolicy() {
        return provisionPolicy;
    }

    public String getRequireBundle() {
        return requireBundle;
    }

    public String getRequireCapability() {
        return requireCapability;
    }

    public String getSubsystemCategory() {
        return subsystemCategory;
    }

    public String getSubsystemContactAddress() {
        return subsystemContactAddress;
    }

    public String getSubsystemContent() {
        return subsystemContent;
    }

    public String getSubsystemCopyright() {
        return subsystemCopyright;
    }

    public String getSubsystemDescription() {
        return subsystemDescription;
    }

    public String getSubsystemDocURL() {
        return subsystemDocURL;
    }

    public String getSubsystemExportService() {
        return subsystemExportService;
    }

    public String getSubsystemIcon() {
        return subsystemIcon;
    }

    public String getSubsystemImportService() {
        return subsystemImportService;
    }

    public String getSubsystemLicense() {
        return subsystemLicense;
    }

    public String getSubsystemLocalization() {
        return subsystemLocalization;
    }

    public String getSubsystemManifestVersion() {
        return subsystemManifestVersion;
    }

    public String getSubsystemName() {
        return subsystemName;
    }

    public String getSubsystemSymbolicName() {
        return subsystemSymbolicName;
    }

    public String getSubsystemType() {
        return subsystemType;
    }

    public String getSubsystemVendor() {
        return subsystemVendor;
    }

    public String getSubsystemVersion() {
        return subsystemVersion;
    }

    public void setExportPackage(String exportPackage) {
        this.exportPackage = exportPackage;
    }

    public void setImportPackage(String importPackage) {
        this.importPackage = importPackage;
    }

    public void setPreferredProvider(String preferredProvider) {
        this.preferredProvider = preferredProvider;
    }

    public void setProvideCapability(String provideCapability) {
        this.provideCapability = provideCapability;
    }

    public void setProvisionPolicy(String provisionPolicyStr) {
        this.provisionPolicy = ProvisionPolicy
                .convertFromString(provisionPolicyStr);
    }

    public void setRequireBundle(String requireBundle) {
        this.requireBundle = requireBundle;
    }

    public void setRequireCapability(String requireCapability) {
        this.requireCapability = requireCapability;
    }

    public void setSubsystemCategory(String subsystemCategory) {
        this.subsystemCategory = subsystemCategory;
    }

    public void setSubsystemContactAddress(String subsystemContactAddress) {
        this.subsystemContactAddress = subsystemContactAddress;
    }

    public void setSubsystemContent(String subsystemContent) {
        this.subsystemContent = subsystemContent;
    }

    public void setSubsystemCopyright(String subsystemCopyright) {
        this.subsystemCopyright = subsystemCopyright;
    }

    public void setSubsystemDescription(String subsystemDescription) {
        this.subsystemDescription = subsystemDescription;
    }

    public void setSubsystemDocURL(String subsystemDocURL) {
        this.subsystemDocURL = subsystemDocURL;
    }

    public void setSubsystemExportService(String subsystemExportService) {
        this.subsystemExportService = subsystemExportService;
    }

    public void setSubsystemIcon(String subsystemIcon) {
        this.subsystemIcon = subsystemIcon;
    }

    public void setSubsystemImportService(String subsystemImportService) {
        this.subsystemImportService = subsystemImportService;
    }

    public void setSubsystemLicense(String subsystemLicense) {
        this.subsystemLicense = subsystemLicense;
    }

    public void setSubsystemLocalization(String subsystemLocalization) {
        this.subsystemLocalization = subsystemLocalization;
    }

    public void setSubsystemName(String subsystemName) {
        this.subsystemName = subsystemName;
    }

    public void setSubsystemSymbolicName(String subsystemSymbolicName) {
        this.subsystemSymbolicName = subsystemSymbolicName;
    }

    public void setSubsystemType(String subsystemType) {
        this.subsystemType = subsystemType;
    }

    public void setSubsystemVendor(String subsystemVendor) {
        this.subsystemVendor = subsystemVendor;
    }

    public void setSubsystemVersion(String subsystemVersion) {
        this.subsystemVersion = subsystemVersion;
    }

}
