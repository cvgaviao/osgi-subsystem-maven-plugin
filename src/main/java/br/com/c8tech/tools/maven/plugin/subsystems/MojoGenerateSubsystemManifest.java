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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.aries.subsystem.core.archive.ExportPackageHeader;
import org.apache.aries.subsystem.core.archive.GenericHeader;
import org.apache.aries.subsystem.core.archive.ImportPackageHeader;
import org.apache.aries.subsystem.core.archive.PreferredProviderHeader;
import org.apache.aries.subsystem.core.archive.ProvideCapabilityHeader;
import org.apache.aries.subsystem.core.archive.RequireBundleHeader;
import org.apache.aries.subsystem.core.archive.RequireCapabilityHeader;
import org.apache.aries.subsystem.core.archive.SubsystemContentHeader;
import org.apache.aries.subsystem.core.archive.SubsystemContentHeader.Clause;
import org.apache.aries.subsystem.core.archive.SubsystemExportServiceHeader;
import org.apache.aries.subsystem.core.archive.SubsystemImportServiceHeader;
import org.apache.aries.subsystem.core.archive.SubsystemManifest;
import org.apache.aries.subsystem.core.archive.SubsystemManifest.Builder;
import org.apache.aries.subsystem.core.archive.SubsystemManifestVersionHeader;
import org.apache.aries.subsystem.core.archive.SubsystemTypeHeader;
import org.apache.aries.subsystem.core.archive.VersionRangeAttribute;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.osgi.framework.Constants;
import org.osgi.service.subsystem.SubsystemConstants;

import br.com.c8tech.tools.maven.osgi.lib.mojo.CommonMojoConstants;
import br.com.c8tech.tools.maven.osgi.lib.mojo.beans.VersionConverter;
import br.com.c8tech.tools.maven.osgi.lib.mojo.handlers.AbstractSubsystemArtifactHandler;
import br.com.c8tech.tools.maven.osgi.lib.mojo.handlers.BundleArtifactHandler;
import br.com.c8tech.tools.maven.osgi.lib.mojo.incremental.ArtifactTracker;
import br.com.c8tech.tools.maven.osgi.lib.mojo.incremental.ArtifactTrackerManager;
import br.com.c8tech.tools.maven.osgi.lib.mojo.incremental.ArtifactTrackerManagerBuilder;
import br.com.c8tech.tools.maven.plugin.subsystems.PojoSubsystemManifestConfiguration.ProvisionPolicy;
import io.takari.incrementalbuild.Output;
import io.takari.incrementalbuild.aggregator.AggregatorBuildContext;
import io.takari.incrementalbuild.aggregator.InputSet;

/**
 *
 * This mojo is aimed to generate an OSGi Subsystem manifest file for the
 * current project based on the cached artifacts and the instructions passed to
 * the plugin.
 *
 */
@Mojo(name = "generateSubsystemManifest", defaultPhase = LifecyclePhase.PROCESS_RESOURCES,
        requiresProject = true, inheritByDefault = true, threadSafe = true,
        requiresDependencyCollection = ResolutionScope.COMPILE,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class MojoGenerateSubsystemManifest extends AbstractSubsystemMojo {

    public static final String EXPORTPACKAGE = Constants.EXPORT_PACKAGE;
    public static final String EXPORTSERVICE = SubsystemConstants.SUBSYSTEM_EXPORTSERVICE;
    public static final String IMPORTPACKAGE = Constants.IMPORT_PACKAGE;
    public static final String IMPORTSERVICE = SubsystemConstants.SUBSYSTEM_IMPORTSERVICE;
    public static final String PREFERRED_PROVIDER = SubsystemConstants.PREFERRED_PROVIDER;
    public static final String PROVIDE_CAPABILITY = Constants.PROVIDE_CAPABILITY;
    public static final String REQUIRE_BUNDLE = Constants.REQUIRE_BUNDLE;
    public static final String REQUIRE_CAPABILITY = Constants.REQUIRE_CAPABILITY;
    public static final String TYPE_APPLICATION = SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION;
    public static final String TYPE_COMPOSITE = SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE;
    public static final String TYPE_FEATURE = SubsystemConstants.SUBSYSTEM_TYPE_FEATURE;

    public static final String ZERO = "0.0.0";

    /**
     * Allows the plugin to traverse all dependencies (in the allowed scope) and
     * compute the Require-Bundle of this subsystem.
     * 
     * This will work ONLY for a osgi.subsystem.composite subsystem.
     */
    @Parameter(required = true, defaultValue = "false")
    private boolean allowComputeRequireBundle;

    /**
     * Allows the plugin to traverse all dependencies (in the allowed scope) and
     * compute the Export-Package of this subsystem.
     * 
     * This will work ONLY for a osgi.subsystem.composite subsystem.
     */
    @Parameter(required = true, defaultValue = "false")
    private boolean allowComputeExportPackages;

    /**
     * Allows the plugin to traverse all dependencies (in the allowed scope) and
     * compute the Export-Package of this subsystem.
     * 
     * This will work ONLY for a osgi.subsystem.composite subsystem.
     */
    @Parameter(required = true, defaultValue = "false")
    private boolean allowComputeExportServices;

    /**
     * Allows the plugin to traverse all dependencies (in the allowed scope) and
     * compute Provide-Capability of this subsystem.
     * 
     * This will work ONLY for a osgi.subsystem.composite subsystem.
     */
    @Parameter(required = true, defaultValue = "false")
    private boolean allowComputeGenericCapabilities;

    /**
     * Allows the plugin to traverse all dependencies (in the allowed scope) and
     * compute the Require-Capability of this subsystem.
     * 
     * This will work ONLY for a osgi.subsystem.composite subsystem.
     */
    @Parameter(required = true, defaultValue = "false")
    private boolean allowComputeGenericRequirements;

    /**
     * Allows the plugin to traverse all dependencies (in the allowed scope) and
     * compute the Import-Package of this subsystem.
     * 
     * This will work ONLY for a osgi.subsystem.composite subsystem.
     */
    @Parameter(required = true, defaultValue = "false")
    private boolean allowComputeImportPackages;

    /**
     * Allows the plugin to traverse all dependencies (in the allowed scope) and
     * compute the Subsystem-ImportService of this subsystem.
     * 
     * This will work ONLY for a osgi.subsystem.composite subsystem.
     */
    @Parameter(required = true, defaultValue = "false")
    private boolean allowComputeImportServices;

    /**
     * Allows the plugin to traverse all dependencies (in the allowed scope) and
     * compute the Subsystem-Content of this subsystem.
     */
    @Parameter(required = true, defaultValue = "true")
    private boolean allowComputeSubsystemContent;

    /**
     * Subsystem manifest generation instructions.
     * <p>
     * 
     */
    @Parameter(required = false)
    private PojoSubsystemManifestConfiguration manifest;

    protected final AggregatorBuildContext manifestAggregatorBuildContext;

    @Inject
    public MojoGenerateSubsystemManifest(final MavenProject project,
            AggregatorBuildContext pManifestAggregatorBuildContext) {
        super(project);
        manifestAggregatorBuildContext = pManifestAggregatorBuildContext;
    }

    private void buidSubsystemManifestFromParametersOnly(Output<File> pOutput)
            throws IOException {

        Set<File> files = new HashSet<>();

        final String subsystemContent = manifest.getSubsystemContent();
        if (subsystemContent != null && !subsystemContent.isEmpty()) {
            generateSubsystemManifestOutput(pOutput, files, null);
        } else {
            getLog().warn("No content was set for the subsystem ");
        }
        getLog().info(
                "OSGi Subsystem manifest file was successfully generated from fixed content header parameter at:"
                        + pOutput.getResource());
    }

    private SubsystemContentHeader buildSubsystemContentHeader(
            Collection<SubsystemContentHeader.Clause> pSubsystemContentHeaderClauses,
            Collection<SubsystemContentHeader.Clause> pCollection) {

        SubsystemContentHeader subsystemContentHeader = null;
        Set<SubsystemContentHeader.Clause> newSet = new HashSet<>(
                pSubsystemContentHeaderClauses);
        if (pCollection != null) {
            newSet.addAll(pCollection);
        }
        String joined = newSet.stream().map(Object::toString)
                .collect(Collectors.joining(", "));
        if (!joined.isEmpty())
            subsystemContentHeader = new SubsystemContentHeader(joined);
        return subsystemContentHeader;
    }

    private ExportPackageHeader buildSubsystemExportPackageHeader(
            Set<String> pExportPackageHeaderClauses) {
        ExportPackageHeader exportPackageHeader = null;
        if (isCompositeSubsystemProject()) {
            String exportPackage = manifest.getExportPackage();
            if (exportPackage != null && !exportPackage.isEmpty()) {
                ExportPackageHeader header = new ExportPackageHeader(
                        exportPackage);
                pExportPackageHeaderClauses.add(header.toString());
            }

            String joined = pExportPackageHeaderClauses.stream()
                    .map(Object::toString).collect(Collectors.joining(", "));

            if (!joined.isEmpty())
                exportPackageHeader = new ExportPackageHeader(joined);
        }
        return exportPackageHeader;
    }

    private SubsystemExportServiceHeader buildSubsystemExportServiceHeader(
            Set<String> pExportServiceHeaderClauses) {
        SubsystemExportServiceHeader subsystemExportServiceHeader = null;
        if (isCompositeSubsystemProject()) {
            String exportService = manifest.getSubsystemExportService();
            if (exportService != null && !exportService.isEmpty()) {
                SubsystemExportServiceHeader clause = new SubsystemExportServiceHeader(
                        exportService);
                pExportServiceHeaderClauses.add(clause.toString());
            }

            String joined = pExportServiceHeaderClauses.stream()
                    .map(Object::toString).collect(Collectors.joining(", "));
            if (!joined.isEmpty())
                subsystemExportServiceHeader = new SubsystemExportServiceHeader(
                        joined);
        }
        return subsystemExportServiceHeader;
    }

    private ImportPackageHeader buildSubsystemImportPackageHeader(
            Set<String> pImportPackageHeaderClauses) {
        ImportPackageHeader importPackageHeader = null;
        if (isCompositeSubsystemProject()) {
            String importPackage = manifest.getImportPackage();
            if (importPackage != null && !importPackage.isEmpty()) {
                ImportPackageHeader clause = new ImportPackageHeader(
                        importPackage);
                pImportPackageHeaderClauses.add(clause.toString());
            }

            String joined = pImportPackageHeaderClauses.stream()
                    .map(Object::toString).collect(Collectors.joining(", "));
            if (!joined.isEmpty())
                importPackageHeader = new ImportPackageHeader(joined);
        }
        return importPackageHeader;
    }

    private SubsystemImportServiceHeader buildSubsystemImportServiceHeader(
            Set<String> pImportServiceHeaderClauses) {
        SubsystemImportServiceHeader subsystemImportServiceHeader = null;
        if (isCompositeSubsystemProject()) {
            String importService = manifest.getSubsystemImportService();
            if (importService != null && !importService.isEmpty()) {
                SubsystemImportServiceHeader clause = new SubsystemImportServiceHeader(
                        importService);
                pImportServiceHeaderClauses.add(clause.toString());
            }

            String joined = pImportServiceHeaderClauses.stream()
                    .map(Object::toString).collect(Collectors.joining(", "));
            if (!joined.isEmpty())
                subsystemImportServiceHeader = new SubsystemImportServiceHeader(
                        joined);
        }

        return subsystemImportServiceHeader;
    }

    private void buildSubsystemManifestCategory(
            Builder subsystemManifestBuilder) {
        if (manifest.getSubsystemCategory() != null)
            subsystemManifestBuilder.header(new GenericHeader(
                    "Subsystem-Category", manifest.getSubsystemCategory()));

    }

    private void buildSubsystemManifestCopyright(
            Builder subsystemManifestBuilder) {
        if (manifest.getSubsystemCopyright() != null)
            subsystemManifestBuilder.header(new GenericHeader(
                    "Subsystem-Copyright", manifest.getSubsystemCopyright()));

    }

    private void buildSubsystemManifestLicense(
            Builder subsystemManifestBuilder) {
        if (manifest.getSubsystemLicense() != null)
            subsystemManifestBuilder.header(new GenericHeader(
                    "Subsystem-License", manifest.getSubsystemLicense()));
    }

    private void buildSubsystemManifestLocalization(
            Builder subsystemManifestBuilder) {
        if (manifest.getSubsystemLocalization() != null)
            subsystemManifestBuilder
                    .header(new GenericHeader("Subsystem-Localization",
                            manifest.getSubsystemLocalization()));

    }

    private void buildSubsystemManifestMainBody(
            final SubsystemManifest.Builder subsystemManifestBuilder) {

        subsystemManifestBuilder.header(new SubsystemManifestVersionHeader());

        buildSubsystemManifestType(subsystemManifestBuilder);

        subsystemManifestBuilder.version(manifest.getSubsystemVersion() != null
                ? manifest.getSubsystemVersion()
                : ZERO);

        buildSubsystemManifestCategory(subsystemManifestBuilder);

        buildSubsystemManifestCopyright(subsystemManifestBuilder);

        buildSubsystemManifestLicense(subsystemManifestBuilder);

        buildSubsystemManifestLocalization(subsystemManifestBuilder);

        if (manifest.getSubsystemContactAddress() != null) {
            subsystemManifestBuilder
                    .header(new GenericHeader("Subsystem-ContactAddress",
                            manifest.getSubsystemContactAddress()));
        }
        if (manifest.getSubsystemDescription() != null) {
            subsystemManifestBuilder
                    .header(new GenericHeader("Subsystem-Description",
                            manifest.getSubsystemDescription()));
        }
        if (manifest.getSubsystemDocURL() != null) {
            subsystemManifestBuilder.header(new GenericHeader(
                    "Subsystem-DocURL", manifest.getSubsystemDocURL()));
        }

        if (manifest.getSubsystemIcon() != null) {
            subsystemManifestBuilder.header(new GenericHeader("Subsystem-Icon",
                    manifest.getSubsystemIcon()));
        }

        if (manifest.getSubsystemName() != null) {
            subsystemManifestBuilder.header(new GenericHeader("Subsystem-Name",
                    manifest.getSubsystemName()));
        }

        if (manifest.getSubsystemSymbolicName() != null) {
            subsystemManifestBuilder
                    .symbolicName(manifest.getSubsystemSymbolicName());
        }

        if (manifest.getSubsystemVendor() != null) {
            subsystemManifestBuilder.header(new GenericHeader(
                    "Subsystem-Vendor", manifest.getSubsystemVendor()));
        }

    }

    private void buildSubsystemManifestType(
            final SubsystemManifest.Builder subsystemManifestBuilder) {
        StringBuilder clauseStr = new StringBuilder("Subsystem-Type:");
        clauseStr.append(getProject().getPackaging());
        if (manifest.getProvisionPolicy() == null
                || manifest.getProvisionPolicy()
                        .equals(ProvisionPolicy.REJECT_DEPENDENCIES)) {
            clauseStr.append(";provision-policy:=rejectDependencies");
        } else {
            clauseStr.append(";provision-policy:=")
                    .append(manifest.getProvisionPolicy().getName());
        }
        subsystemManifestBuilder.type(new SubsystemTypeHeader(
                new SubsystemTypeHeader.Clause(clauseStr.toString())));
    }

    private PreferredProviderHeader buildSubsystemPreferredProviderHeader(
            Set<String> pPreferredProviderHeaderClauses) {
        PreferredProviderHeader preferredProviderHeader = null;

        if (isCompositeSubsystemProject()) {
            String requireBundle = manifest.getPreferredProvider();
            if (requireBundle != null && !requireBundle.isEmpty()) {
                PreferredProviderHeader clause = new PreferredProviderHeader(
                        requireBundle);
                pPreferredProviderHeaderClauses.add(clause.toString());
            }

            String joined = pPreferredProviderHeaderClauses.stream()
                    .map(Object::toString).collect(Collectors.joining(", "));
            if (!joined.isEmpty())
                preferredProviderHeader = new PreferredProviderHeader(joined);
        }
        return preferredProviderHeader;
    }

    private ProvideCapabilityHeader buildSubsystemProvideCapabilityHeader(
            Set<String> pProvideCapabilityHeaderClauses) {
        ProvideCapabilityHeader provideCapabilityHeader = null;
        if (isCompositeSubsystemProject()) {
            String provideCapability = manifest.getProvideCapability();
            if (provideCapability != null && !provideCapability.isEmpty()) {
                ProvideCapabilityHeader clause = new ProvideCapabilityHeader(
                        provideCapability);
                pProvideCapabilityHeaderClauses.add(clause.toString());
            }
            String joined = pProvideCapabilityHeaderClauses.stream()
                    .map(Object::toString).collect(Collectors.joining(", "));
            if (!joined.isEmpty())
                provideCapabilityHeader = new ProvideCapabilityHeader(joined);
        }
        return provideCapabilityHeader;
    }

    private RequireBundleHeader buildSubsystemRequireBundleHeader(
            Set<String> pRequireBundleHeaderClauses) {
        RequireBundleHeader requireBundleHeader = null;

        if (isCompositeSubsystemProject()) {
            String requireBundle = manifest.getRequireBundle();
            if (requireBundle != null && !requireBundle.isEmpty()) {
                RequireBundleHeader clause = new RequireBundleHeader(
                        requireBundle);
                pRequireBundleHeaderClauses.add(clause.toString());
            }
            String joined = pRequireBundleHeaderClauses.stream()
                    .map(Object::toString).collect(Collectors.joining(", "));
            if (!joined.isEmpty())
                requireBundleHeader = new RequireBundleHeader(joined);
        }
        return requireBundleHeader;
    }

    private RequireCapabilityHeader buildSubsystemRequireCapabilityHeader(
            Set<String> pRequireCapabilityHeaderClauses) {
        RequireCapabilityHeader requireCapabilityHeader = null;
        if (isCompositeSubsystemProject()) {
            String requireCapability = manifest.getRequireCapability();
            if (requireCapability != null && !requireCapability.isEmpty()) {
                RequireCapabilityHeader clause = new RequireCapabilityHeader(
                        requireCapability);
                pRequireCapabilityHeaderClauses.add(clause.toString());
            }
            String joined = pRequireCapabilityHeaderClauses.stream()
                    .map(Object::toString).collect(Collectors.joining(", "));
            if (!joined.isEmpty())
                requireCapabilityHeader = new RequireCapabilityHeader(joined);
        }
        return requireCapabilityHeader;
    }

    @Override
    public void executeMojo()
            throws MojoExecutionException, MojoFailureException {

        if (manifest == null) {
            getLog().info(
                    "Manifest parameter was not informed, using default values.");
            setManifest(new PojoSubsystemManifestConfiguration());
        }

        getLog().info(
                "Setting up generation of the Subsystem manifest file for project "
                        + getProject().getArtifactId());

        Path outputFile = getSubsystemManifestFile();

        if (getManifestScopes().isEmpty() || getManifestScopes()
                .contains(CommonMojoConstants.EMPTY_VALUE)) {

            // no dependencies will be evaluated
            getLog().warn("The parameter manifestScopes was not declared. "
                    + "Building subsystem manifest based on the subsystemContent parameter only.");
            prepareGenerateSubsystemManifestFromContentParameterOnly(
                    manifestAggregatorBuildContext.newInputSet(), outputFile);
            return;
        }

        ArtifactTrackerManager artifactTrackerManager = ArtifactTrackerManagerBuilder
                .newBuilder(getMavenSession(), getCacheDirectory())
                .withGroupingByTypeDirectory(true).withVerbose(isVerbose())
                .withPreviousCachingRequired(true).mavenSetup()
                .withDependenciesHelper(getDependenciesHelper())
                .withRepositorySystem(getRepositorySystem()).workspaceSetup()
                .withAssemblyUrlProtocolAllowed(isWorkspaceResolutionAllowed())
                .withPackOnTheFlyAllowed(isWorkspaceResolutionAllowed())
                .endWorkspaceSetup().mavenFiltering()
                .withOptional(isOptionalConsidered())
                .withTransitive(isTransitiveConsidered())
                .withScopes(getManifestScopes())
                .withScopes(getEmbeddableScopes())
                .withMavenArtifactSet(getMavenArtifactSet())
                .withExcludedDependencies(getExcludedArtifacts())
                .endMavenFiltering().endMavenSetup().build();

        artifactTrackerManager.resolveMavenArtifacts(getEmbeddableScopes());

        if (isVerbose()) {
            getLog().info(
                    "Registering the artifacts into the OSGi Subsystem manifest generation incremental build context.");
        }
        prepareForSubsystemManifestGeneration(outputFile,
                artifactTrackerManager);
    }

    /**
     *
     * @param pArtifactTrackerManager
     * @param subsystemContentConfig
     * @param path
     * @return
     */
    private Set<SubsystemContentHeader.Clause> extractContentClausesFromString(
            ArtifactTrackerManager pArtifactTrackerManager,
            final String subsystemContentConfig) {
        Set<SubsystemContentHeader.Clause> clauses = new HashSet<>();

        if (subsystemContentConfig != null
                && !subsystemContentConfig.isEmpty()) {
            SubsystemContentHeader header = new SubsystemContentHeader(
                    subsystemContentConfig);
            for (Clause clause : header.getClauses()) {
                String version = clause.getVersionRange().toString();
                if (ZERO.equals(version)) {
                    ArtifactTracker tracker = pArtifactTrackerManager
                            .searchByArtifactId(clause.getSymbolicName());
                    if (tracker == null) {
                        throw new IllegalArgumentException("The version '"
                                + version + "', for artifact '"
                                + clause.getSymbolicName()
                                + "', is not valid. A subsystem content must have a valid fixed version range.");
                    } else {
                        version = tracker.getVersion();
                    }
                }
                StringBuilder newClause = new StringBuilder(clause.getPath());
                newClause.append(";version=");
                newClause.append(computeFixedRange(version));
                newClause.append(";");
                newClause.append(
                        clause.getDirectives().stream().map(Object::toString)
                                .collect(Collectors.joining(";")));
                newClause.append(";");
                newClause.append(clause.getAttributes().stream()
                        .filter(p -> !(p instanceof VersionRangeAttribute))
                        .map(Object::toString)
                        .collect(Collectors.joining(";")));
                Clause processedClause = new SubsystemContentHeader.Clause(
                        newClause.toString());
                clauses.add(processedClause);
            }
        }
        return clauses;
    }

    private void extractExportPackageHeader(final String pHeaderValue,
            Set<String> pExportPackageHeaderClauses) {
        if (!isCompositeSubsystemProject())
            return;
        if (allowComputeExportPackages && pHeaderValue != null) {

            String processed = pHeaderValue.replaceAll("(;uses:=\"[^\\\"]*\")",
                    "");

            ExportPackageHeader header = new ExportPackageHeader(processed);
            header.getClauses().forEach(
                    c -> pExportPackageHeaderClauses.add(c.toString()));
        }
    }

    private void extractImportPackageHeader(final String pHeaderValue,
            Set<String> pImportPackageHeaderClauses) {
        if (!isCompositeSubsystemProject())
            return;
        if (allowComputeImportPackages && pHeaderValue != null) {
            ImportPackageHeader header = new ImportPackageHeader(pHeaderValue);
            header.getClauses().forEach(
                    c -> pImportPackageHeaderClauses.add(c.toString()));
        }
    }

    // private void extractPreferredProviderHeader(String pHeaderValue,
    // Set<String> pPreferedProviderHeaderClauses) {
    //
    // if (!isCompositeSubsystemProject())
    // return;
    // if (allowComputePreferredProvider && pHeaderValue != null) {
    // PreferredProviderHeader header = new PreferredProviderHeader(
    // pHeaderValue);
    // header.getClauses().forEach(
    // c -> pPreferedProviderHeaderClauses.add(c.toString()));
    // }
    // }

    private void extractRequireBundleHeader(String pHeaderValue,
            Set<String> pRequireBundleHeaderClauses) {
        if (!isCompositeSubsystemProject())
            return;
        if (allowComputeRequireBundle && pHeaderValue != null) {
            RequireBundleHeader header = new RequireBundleHeader(pHeaderValue);
            header.getClauses().forEach(
                    c -> pRequireBundleHeaderClauses.add(c.toString()));
        }
    }

    private void extractSubsystemContentHeader(
            final ArtifactTracker pArtifactTracker,
            final Map<String, String> pManifestHeaders,
            final Set<SubsystemContentHeader.Clause> pContentsFromConfiguration,
            final Map<String, SubsystemContentHeader.Clause> pSubsystemContentHeaderClauses) {

        SubsystemContentHeader.Clause clause = null;

        if (pArtifactTracker
                .getTypeHandler() instanceof BundleArtifactHandler) {

            String bsn = pManifestHeaders
                    .get(CommonMojoConstants.OSGI_BUNDLE_HEADER_SN);
            String bversion = pManifestHeaders
                    .get(CommonMojoConstants.OSGI_BUNDLE_HEADER_VERSION);
            if (bsn != null && bversion != null) {
                String key = bsn.split(";")[0];
                clause = fullfillBundleDependencyClause(bsn, bversion,
                        pArtifactTracker.isOptional(),
                        pManifestHeaders.get(
                                CommonMojoConstants.OSGI_BUNDLE_HEADER_FRAGMENT_HOST) != null,
                        pContentsFromConfiguration.stream()
                                .filter(t -> t.getSymbolicName().equals(key))
                                .findFirst());
                pSubsystemContentHeaderClauses.put(key, clause);
            }
        } else
            if (pArtifactTracker
                    .getTypeHandler() instanceof AbstractSubsystemArtifactHandler) {

                String ssn = pManifestHeaders
                        .get(CommonMojoConstants.OSGI_SUBSYSTEM_SN);
                String sversion = pManifestHeaders
                        .get(CommonMojoConstants.OSGI_SUBSYSTEM_VERSION);
                if (ssn != null && sversion != null) {
                    String key = ssn.split(";")[0];

                    String stype = pManifestHeaders
                            .get(CommonMojoConstants.OSGI_SUBSYSTEM_TYPE);
                    if (stype == null) {
                        stype = pArtifactTracker.getType();
                    }
                    clause = fullfillSubsystemDependencyClause(ssn, sversion,
                            stype, pArtifactTracker.isOptional(),
                            pContentsFromConfiguration.stream().filter(
                                    t -> t.getSymbolicName().equals(key))
                                    .findFirst());

                    pSubsystemContentHeaderClauses.put(key, clause);
                }
            }
    }

    private void extractSubsystemExportServiceHeader(String pHeaderValue,
            Set<String> pExportServiceHeaderClauses) {
        if (!isCompositeSubsystemProject())
            return;
        if (allowComputeExportServices && pHeaderValue != null) {
            SubsystemExportServiceHeader header = new SubsystemExportServiceHeader(
                    pHeaderValue);
            header.getClauses().forEach(
                    c -> pExportServiceHeaderClauses.add(c.toString()));
        }
    }

    private void extractSubsystemImportServiceHeader(final String pHeaderValue,
            Set<String> pImportServiceHeaderClauses) {
        if (!isCompositeSubsystemProject())
            return;
        if (allowComputeImportServices && pHeaderValue != null) {
            SubsystemImportServiceHeader header = new SubsystemImportServiceHeader(
                    pHeaderValue);
            header.getClauses().forEach(
                    c -> pImportServiceHeaderClauses.add(c.toString()));
        }

    }

    private void extractSubsystemProvideCapabilityHeader(String pHeaderValue,
            Set<String> pProvideCapabilityHeaderClauses) {
        if (!isCompositeSubsystemProject())
            return;
        if (allowComputeGenericCapabilities && pHeaderValue != null) {
            ProvideCapabilityHeader header = new ProvideCapabilityHeader(
                    pHeaderValue);

            header.getClauses().forEach(
                    c -> pProvideCapabilityHeaderClauses.add(c.toString()));
        }
    }

    private void extractSubsystemRequireCapabilityHeader(String pHeaderValue,
            Set<String> pRequireCapabilityHeaderClauses) {
        if (!isCompositeSubsystemProject())
            return;
        if (allowComputeGenericRequirements && pHeaderValue != null) {
            RequireCapabilityHeader header = new RequireCapabilityHeader(
                    pHeaderValue);
            header.getClauses().forEach(
                    c -> pRequireCapabilityHeaderClauses.add(c.toString()));
        }
    }

    private void generateSubsystemManifestOutput(Output<File> pOutputFile,
            Iterable<File> pInputFiles,
            ArtifactTrackerManager pArtifactTrackerManager) throws IOException {

        final SubsystemManifest.Builder subsystemManifestBuilder = new SubsystemManifest.Builder();

        // build body of the manifest. it doesn't need have any contents
        buildSubsystemManifestMainBody(subsystemManifestBuilder);

        Set<SubsystemContentHeader.Clause> contentsFromConfiguration = extractContentClausesFromString(
                pArtifactTrackerManager, manifest.getSubsystemContent());
        Map<String, SubsystemContentHeader.Clause> subsystemContentHeaderClauses = new HashMap<>();
        Set<String> exportPackageHeaderClauses = new HashSet<>();
        Set<String> subsystemExportServiceHeaderClauses = new HashSet<>();
        Set<String> importPackageHeaderClauses = new HashSet<>();
        Set<String> subsystemImportServiceHeaderClauses = new HashSet<>();
        Set<String> provideCapabilityHeaderClauses = new HashSet<>();
        Set<String> requireCapabilityHeaderClauses = new HashSet<>();
        Set<String> requireBundleHeaderClauses = new HashSet<>();
        Set<String> preferredProviderHeaderClauses = new HashSet<>();

        for (File processingArtifactFile : pInputFiles) {
            ArtifactTracker artifactProperty = pArtifactTrackerManager
                    .searchByPath(processingArtifactFile.getPath());
            if (artifactProperty == null) {
                getLog().warn("Ignoring file '" + processingArtifactFile
                        + "' due a missing metadata.");
                continue;
            }
            Map<String, String> manifestHeaders = artifactProperty
                    .getManifestHeaders();

            if (allowComputeSubsystemContent)
                extractSubsystemContentHeader(artifactProperty, manifestHeaders,
                        contentsFromConfiguration,
                        subsystemContentHeaderClauses);

            if (isCompositeSubsystemProject()) {

                extractImportPackageHeader(manifestHeaders.get(IMPORTPACKAGE),
                        importPackageHeaderClauses);

                extractExportPackageHeader(manifestHeaders.get(EXPORTPACKAGE),
                        exportPackageHeaderClauses);

                extractSubsystemImportServiceHeader(
                        manifestHeaders.get(IMPORTSERVICE),
                        subsystemImportServiceHeaderClauses);

                extractSubsystemExportServiceHeader(
                        manifestHeaders.get(EXPORTSERVICE),
                        subsystemExportServiceHeaderClauses);

                extractSubsystemRequireCapabilityHeader(
                        manifestHeaders.get(REQUIRE_CAPABILITY),
                        requireCapabilityHeaderClauses);

                extractSubsystemProvideCapabilityHeader(
                        manifestHeaders.get(PROVIDE_CAPABILITY),
                        provideCapabilityHeaderClauses);

                extractRequireBundleHeader(manifestHeaders.get(REQUIRE_BUNDLE),
                        requireBundleHeaderClauses);

                // extractPreferredProviderHeader(
                // manifestHeaders.get(PREFERRED_PROVIDER),
                // preferredProviderHeaderClauses);

            }
        }
        subsystemManifestBuilder.header(buildSubsystemContentHeader(
                subsystemContentHeaderClauses.values(),
                contentsFromConfiguration));

        if (isCompositeSubsystemProject()) {
            subsystemManifestBuilder.header(buildSubsystemExportServiceHeader(
                    subsystemExportServiceHeaderClauses));

            subsystemManifestBuilder.header(buildSubsystemImportServiceHeader(
                    subsystemImportServiceHeaderClauses));

            subsystemManifestBuilder
                    .header(buildSubsystemProvideCapabilityHeader(
                            provideCapabilityHeaderClauses));

            subsystemManifestBuilder
                    .header(buildSubsystemRequireCapabilityHeader(
                            requireCapabilityHeaderClauses));

            subsystemManifestBuilder.header(buildSubsystemImportPackageHeader(
                    importPackageHeaderClauses));

            subsystemManifestBuilder.header(buildSubsystemExportPackageHeader(
                    exportPackageHeaderClauses));

            subsystemManifestBuilder.header(buildSubsystemRequireBundleHeader(
                    requireBundleHeaderClauses));

            subsystemManifestBuilder
                    .header(buildSubsystemPreferredProviderHeader(
                            preferredProviderHeaderClauses));
        }
        SubsystemManifest subsystemManifestPojo = subsystemManifestBuilder
                .build();
        subsystemManifestPojo.write(pOutputFile.newOutputStream());

    }

    private boolean isCompositeSubsystemProject() {
        return TYPE_COMPOSITE.equals(getProject().getPackaging());
    }

    private void prepareForSubsystemManifestGeneration(final Path outputFile,
            final ArtifactTrackerManager pArtifactTrackerManager)
            throws MojoExecutionException {
        InputSet manifestInputSet = registerArtifactsIntoAggregatorBuildContext(
                pArtifactTrackerManager.getAllArtifactTrackers(),
                manifestAggregatorBuildContext, true);

        try {
            // build the contents only when necessary
            if (manifestInputSet.aggregateIfNecessary(outputFile.toFile(),
                    (output, inputs) -> {
                        getLog().info(
                                "Starting generation of the Subsystem manifest file for project "
                                        + getProject().getArtifactId());
                        generateSubsystemManifestOutput(output, inputs,
                                pArtifactTrackerManager);
                    })) {
                getLog().info(String.format(
                        "OSGi Subsystem manifest file was successfully generated at : %s",
                        outputFile.toAbsolutePath()));
            }
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "An error occurred while generating an Subsystem manifest file.",
                    e);
        }
    }

    private void prepareGenerateSubsystemManifestFromContentParameterOnly(
            InputSet pInputSet, Path outputFile) throws MojoExecutionException {

        try {
            pInputSet.aggregateIfNecessary(outputFile.toFile(),
                    (pOutput,
                            pInputs) -> buidSubsystemManifestFromParametersOnly(
                                    pOutput));
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Failure occurred while generating the Subsystem manifest",
                    e);
        }
    }

    /**
     *
     * This method is used to set default values to subsystem manifest by sisu.
     *
     * @param manifest The subsystem manifest configuration xml tag.
     */
    public void setManifest(final PojoSubsystemManifestConfiguration manifest) {
        this.manifest = manifest;
        if (manifest.getSubsystemType() == null) {
            manifest.setSubsystemType(getProject().getPackaging());
        }
        if (manifest.getSubsystemName() == null) {
            manifest.setSubsystemName(getProject().getName());
        }
        if (manifest.getSubsystemDescription() == null) {
            manifest.setSubsystemDescription(getProject().getDescription());
        }
        if (manifest.getSubsystemSymbolicName() == null) {
            manifest.setSubsystemSymbolicName(getProject().getArtifactId());
        }
        if (manifest.getSubsystemVersion() == null) {
            manifest.setSubsystemVersion(
                    VersionConverter.fromMavenVersion(getProject().getVersion())
                            .toOSGi().getVersionString());
        }
    }

}
