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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.aries.subsystem.core.archive.SubsystemContentHeader.Clause;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import br.com.c8tech.tools.maven.osgi.lib.mojo.AbstractCustomPackagingMojo;
import br.com.c8tech.tools.maven.osgi.lib.mojo.CommonMojoConstants;
import br.com.c8tech.tools.maven.osgi.lib.mojo.beans.MavenArtifactSet;
import br.com.c8tech.tools.maven.osgi.lib.mojo.beans.VersionConverter;
import br.com.c8tech.tools.maven.osgi.lib.mojo.handlers.AbstractSubsystemArtifactHandler;
import br.com.c8tech.tools.maven.osgi.lib.mojo.handlers.BundleArtifactHandler;
import io.takari.incrementalbuild.Incremental;
import io.takari.incrementalbuild.Incremental.Configuration;

/**
 *
 * @author Cristiano Gavião
 *
 */
public abstract class AbstractSubsystemMojo
        extends AbstractCustomPackagingMojo {

    protected static final String DEFAULT_SUBSYSTEM_DIRECTORY = "esa";

    private static final String[] DEFAULT_SUPPORTED_PACKAGINGS = {
            CommonMojoConstants.OSGI_SUBSYSTEM_PACKAGING_COMPOSITE,
            CommonMojoConstants.OSGI_SUBSYSTEM_PACKAGING_APPLICATION,
            CommonMojoConstants.OSGI_SUBSYSTEM_PACKAGING_FEATURE };

    /**
     * The file used to control the incremental support
     */
    protected static final String INCREMENTAL_BUILD_CONTROL_FILE = "com.c8tech.tools.maven.plugin.subsystem";

    /**
     * Indicates whether the plugin should consider the projects opened in the
     * IDE for dependency resolution.
     */
    @Parameter(defaultValue = "false")
    private boolean workspaceResolutionAllowed;

    /**
     * The directory used to hold the artifacts files downloaded from the
     * repositories.
     */
    @Parameter(property = "osgi.subsystem.cacheDirectory", required = true,
            defaultValue = "${project.build.directory}/"
                    + CommonMojoConstants.DEFAULT_CACHE_DIR_NAME)
    private File cacheDirectory;

    /**
     * A list of scopes to be used to filter the project's dependencies when the
     * plugin is determining which ones will be embedded into the generated
     * <b>.esa</b> archive file.
     * <p>
     * Example:
     *
     * <pre>
     * {@code
     * <embeddableScopes>
     *     <embeddableScope>compile<embeddableScope>
     * </embeddableScopes>
     * }
     * </pre>
     * 
     */
    @Parameter()
    private final Set<String> embeddableScopes = new HashSet<>();

    /**
     * A list of <i>artifactId</i> that the plugin must ignored when resolving
     * the project's declared dependencies.
     * <p>
     * Example:
     *
     * <pre>
     * {@code
     * <excludedArtifacts>
     *      <excludedArtifact>
     *              org.slf4j:slf4j-api
     *      </excludedArtifact>
     * </excludedArtifacts>}
     * </pre>
     *
     */
    @Parameter()
    private List<String> excludedArtifacts = new ArrayList<>();

    /**
     * A list of scopes to be used to filter the project's dependencies when the
     * plugin is determining which ones will be used to generate the subsystem's
     * manifests file.
     * <p>
     * Example:
     *
     * <pre>
     * {@code
     * <manifestScopes>
     *   <manifestScope>compile</manifestScope>
     *   <manifestScope>provided</manifestScope>
     * </manifestScopes>}
     * </pre>
     *
     */
    @Parameter()
    private final Set<String> manifestScopes = new HashSet<>();

    /**
     * A set of string based maven artifact identification.
     * <p>
     * Each artifact has the following pattern:<br>
     * 
     * <code><b>groupId:artifactId:[version][@startLevel]</b></code>
     * <p>
     * 
     * It allows a developer to override and also to complement a maven
     * dependency information in the subsystem manifest, as for example, to use
     * another version or to set a bundle's start level.
     * 
     * <pre>
     * Example:
     * {@code 
     * <mavenArtifactSet>
     *     <artifacts>
     *       <artifact>org.apache.aries.subsystem:org.apache.aries.subsystem.core@5</artifact>
     *       <artifact>org.apache.aries.subsystem:org.apache.aries.subsystem.api@5</artifact>
     *       <artifact>org.apache.aries:org.apache.aries.util@5</artifact>
     *     </artifacts>
     * </mavenArtifactSet>
     * }
     * </pre>
     */
    @Parameter()
    private MavenArtifactSet mavenArtifactSet;

    /**
     * Indicates to this plugin whether it must consider any optional
     * dependencies in order to generate the subsystem archive.
     * 
     * @see #transitiveConsidered
     */
    @Parameter(defaultValue = "false",
            property = "subsystem.optionalConsidered")
    private boolean optionalConsidered;

    /**
     * Set this to <code>true</code> to skip the plugin execution.
     */
    @Parameter(defaultValue = "false", property = "subsystem.skip")
    @Incremental(configuration = Configuration.ignore)
    private boolean skip;

    /**
     * Indicates to this plugin whether it must consider the transitive
     * dependencies of those direct declared in order to generate the subsystem
     * archive.
     * 
     * @see #optionalConsidered
     */
    @Parameter(defaultValue = "false",
            property = "subsystem.transitiveConsidered")
    private boolean transitiveConsidered;

    /**
     * A list of maven packaging types considered valid to be used as a OSGi
     * bundle.
     * <p>
     * The plugin will filter the declared dependencies by it before checking
     * the existence of a valid internal manifest file when generating the
     * subsystem archive.
     * <p>
     * Any directed declared dependency or any transitive one (when allowed)
     * must have its packaging type contained in this list in order to be
     * considered a valid dependency.
     * <p>
     * Example:
     *
     * <pre>
     * {@code 
     * <validTypes>
     *   <validType>bundle</validType>
     * </validTypes>
     * }
     * </pre>
     *
     * @see #transitiveConsidered
     * @see #embeddableScopes
     * @see #manifestScopes
     * @see #validSubsystemTypes
     */
    @Parameter()
    private List<String> validBundleTypes = new ArrayList<>();

    /**
     * A list of maven packaging types considered valid to be used as an OSGi
     * Subsystem.
     * <p>
     * The plugin will filter the declared dependencies by it before checking
     * the existence of a valid internal manifest file when generating the
     * subsystem archive.
     * <p>
     * Any directed declared dependency or any transitive one (when allowed)
     * must have its packaging type contained in this list in order to be
     * considered a valid dependency.
     * <p>
     * Example:
     * 
     * <pre>
     * {@code 
     * <validSubsystemTypes>
     *      <validSubsystemType>esa</validSubsystemType>
     *      <validSubsystemType>osgi.subsystem.application</validSubsystemType>
     *      <validSubsystemType>osgi.subsystem.composite</validSubsystemType>
     *      <validSubsystemType>osgi.subsystem.feature</validSubsystemType>
     * </validSubsystemTypes>}
     * </pre>
     *
     * @see #transitiveConsidered
     * @see #embeddableScopes
     * @see #manifestScopes
     * @see #validBundleTypes
     */
    @Parameter()
    private List<String> validSubsystemTypes = new ArrayList<>();

    /**
     *
     * @param project The reference to the project being built.
     */
    public AbstractSubsystemMojo(final MavenProject project) {
        super(project, getDefaultSupportedPackagings());
        if (validSubsystemTypes.isEmpty()) {
            validSubsystemTypes.addAll(Arrays.asList(
                    AbstractSubsystemArtifactHandler.getDefaultValidTypes()));
        }
        if (validBundleTypes.isEmpty()) {
            validBundleTypes.addAll(Arrays
                    .asList(BundleArtifactHandler.getDefaultValidTypes()));
        }
    }

    public static String[] getDefaultSupportedPackagings() {
        return DEFAULT_SUPPORTED_PACKAGINGS;
    }

    /**
     *
     * @param scope
     *                  The scope to be considered to embed artifacts.
     */
    public final void addEmbeddableScope(String scope) {
        this.embeddableScopes.add(scope);
    }

    /**
     *
     * @param excludedArtifact
     *                             The artifact ID to be excluded.
     */
    public final void addExcludedArtifact(String excludedArtifact) {
        this.excludedArtifacts.add(excludedArtifact);
    }

    /**
     *
     * @param scope
     *                  The scope to be considered when computing the manifest.
     */
    public final void addManifestScope(String scope) {
        this.manifestScopes.add(scope);
    }

    /**
     *
     * @param validType
     *                      A valid package type for a bundle.
     */
    public final void addValidBundleType(String validType) {
        this.validBundleTypes.add(validType);
    }

    /**
     *
     * @param validType
     *                      A valid package type for a subsystem.
     */
    public final void addValidSubsystemType(String validType) {
        this.validSubsystemTypes.add(validType);
    }

    protected final String computeFixedRange(String pBversion) {

        String versionFixed = VersionConverter.fromOsgiVersion(pBversion)
                .toOSGi().getFixedVersionRangeString();

        return "\"" + versionFixed + "\"";
    }

    @Override
    protected void doBeforeSkipMojo() throws MojoExecutionException {
        // do nothing

    }

    @Override
    protected void executeExtraInitializationSteps()
            throws MojoExecutionException {
    }

    protected final Clause fullfillBundleDependencyClause(final String pBsn,
            final String pBversion, boolean pIsOptional, boolean pIsFragment,
            final Optional<Clause> pOptionalClause) {

        if (pOptionalClause.isPresent() && ((pBsn == null || pBsn.isEmpty()) // NOSONAR
                || (pBversion == null || pBversion.isEmpty()))) {
            return pOptionalClause.get();

        }
        StringBuilder clauseStr = new StringBuilder();
        if (!pOptionalClause.isPresent()) {
            clauseStr.append(pBsn);
        } else {
            clauseStr.append(pOptionalClause.get().getSymbolicName());
        }
        if (pIsFragment) {
            clauseStr.append(";" + Clause.ATTRIBUTE_TYPE + "="
                    + CommonMojoConstants.OSGI_FRAGMENT_TYPE);
        } else {
            clauseStr.append(";" + Clause.ATTRIBUTE_TYPE + "="
                    + CommonMojoConstants.OSGI_BUNDLES_TYPE);
        }
        clauseStr.append(";" + Clause.ATTRIBUTE_VERSION + "="
                + computeFixedRange(pBversion));
        if (pIsOptional) {
            clauseStr.append(
                    ";" + Clause.DIRECTIVE_RESOLUTION + ":=" + "optional");
        } else {
            clauseStr.append(
                    ";" + Clause.DIRECTIVE_RESOLUTION + ":=" + "mandatory");
        }
        if (pOptionalClause.isPresent()) {
            clauseStr.append(";" + Clause.DIRECTIVE_STARTORDER + ":="
                    + pOptionalClause.get().getStartOrder());
        }
        return new Clause(clauseStr.toString());
    }

    protected final Clause fullfillSubsystemDependencyClause(final String ssn,
            final String sversion, final String stype, boolean isOptional,
            final Optional<Clause> pOptionalClause) {

        if (ssn == null || ssn.isEmpty() || sversion == null
                || sversion.isEmpty()) {
            return null;
        }
        StringBuilder clause = new StringBuilder();
        if (!pOptionalClause.isPresent()) {
            clause.append(ssn);
            clause.append(
                    ";" + Clause.ATTRIBUTE_TYPE + "=" + stype.split(";")[0]);
        } else {
            clause.append(pOptionalClause.get().getSymbolicName());
            clause.append(";" + Clause.DIRECTIVE_STARTORDER + ":="
                    + pOptionalClause.get().getStartOrder());
            if (stype == null) {
                clause.append(";" + Clause.ATTRIBUTE_TYPE + "="
                        + pOptionalClause.get().getType());
            } else {
                clause.append(";" + Clause.ATTRIBUTE_TYPE + "="
                        + stype.split(";")[0]);
            }
        }
        clause.append(";" + Clause.ATTRIBUTE_VERSION + "="
                + computeFixedRange(sversion));
        if (isOptional) {
            clause.append(
                    ";" + Clause.DIRECTIVE_RESOLUTION + ":=" + "optional");
        } else {
            clause.append(
                    ";" + Clause.DIRECTIVE_RESOLUTION + ":=" + "mandatory");
        }

        return new Clause(clause.toString());
    }

    @Override
    protected final Path getCacheDirectory() {
        return cacheDirectory.toPath();
    }

    protected final Set<String> getEmbeddableScopes() {
        return embeddableScopes;
    }

    protected final List<String> getExcludedArtifacts() {
        return excludedArtifacts;
    }

    protected final Set<String> getManifestScopes() {
        if (manifestScopes.isEmpty()) {
            manifestScopes.add("compile");
        }
        return manifestScopes;
    }

    protected final MavenArtifactSet getMavenArtifactSet() {
        if (mavenArtifactSet == null) {
            mavenArtifactSet = new MavenArtifactSet();
        }
        return mavenArtifactSet;
    }

    protected final Path getSubsystemManifestFile()
            throws MojoExecutionException {
        return getWorkSubDirectory(DEFAULT_SUBSYSTEM_DIRECTORY)
                .resolve(CommonMojoConstants.OSGI_SUBSYSTEM_MANIFEST_FOLDER)
                .resolve(CommonMojoConstants.OSGI_SUBSYSTEM_MANIFEST_XML_NAME);
    }

    protected final List<String> getValidBundleTypes() {
        return validBundleTypes;
    }

    protected final List<String> getValidSubsystemTypes() {
        return validSubsystemTypes;
    }

    public boolean isWorkspaceResolutionAllowed() {
        return this.workspaceResolutionAllowed;
    }

    protected final boolean isOptionalConsidered() {
        return optionalConsidered;
    }

    @Override
    protected boolean isSkip() {
        return skip;
    }

    protected final boolean isTransitiveConsidered() {
        return transitiveConsidered;
    }

    public void setWorkspaceResolutionAllowed(
            boolean pAllowsWorkspaceResolution) {
        this.workspaceResolutionAllowed = pAllowsWorkspaceResolution;
    }

    public final void setEmbeddableScopes(List<String> scopes) {
        for (String scope : scopes) {
            addEmbeddableScope(scope);
        }
    }

    public final void setExcludedArtifacts(List<String> excludedArtifacts) {
        for (String excludedArtifact : excludedArtifacts) {
            addExcludedArtifact(excludedArtifact);
        }
    }

    public final void setManifestScopes(List<String> scopes) {
        for (String scope : scopes) {
            addManifestScope(scope);
        }
    }

    public final void setOptionalConsidered(boolean pOptionalConsidered) {
        optionalConsidered = pOptionalConsidered;
    }

    public final void setSubsystemValidTypes(List<String> validTypes) {
        for (String validType : validTypes) {
            addValidSubsystemType(validType);
        }
    }

    public final void setTransitiveConsidered(boolean pTransitiveConsidered) {
        transitiveConsidered = pTransitiveConsidered;
    }

    public final void setValidBundleTypes(List<String> validTypes) {
        for (String validType : validTypes) {
            addValidBundleType(validType);
        }
    }

    public final void setValidSubsystemTypes(
            List<String> pValidSubsystemTypes) {
        validSubsystemTypes = pValidSubsystemTypes;
    }

}
