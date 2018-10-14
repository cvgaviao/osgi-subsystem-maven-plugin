/**
 * ==========================================================================
 * Copyright © 2015-2018 Cristiano Gavião, C8 Technology ME.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cristiano Gavião (cvgaviao@c8tech.com.br)- initial API and implementation
 * ==========================================================================
 */
package br.com.c8tech.tools.maven.plugin.subsystems;

import javax.inject.Inject;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import br.com.c8tech.tools.maven.osgi.lib.mojo.incremental.ArtifactTrackerManager;
import br.com.c8tech.tools.maven.osgi.lib.mojo.incremental.ArtifactTrackerManagerBuilder;
import br.com.c8tech.tools.maven.osgi.lib.mojo.incremental.BuildContextWithUrl;

/**
 * This mojo is aimed to resolve the declared dependencies against the
 * registered local and remote maven repositories, filter the valid ones and
 * copy them into the cache directory in order to be processed by the subsequent
 * mojos in the lifecycle.
 * <p>
 * <br>
 * The artifacts will be filtered by:
 * <li>One or more scopes specified in the parameters: {@link #manifestScopes}
 * and {@link #embeddableScopes}.</li>
 * <li>The bundle types specified in the parameter:
 * {@link #validBundleType}</li>
 * <li>The subsystem types specified in the parameter:
 * {@link #validSubsystemType}</li>
 * <li>The presence of a valid artifact's manifest file on each filter
 * above.</li>
 * <p>
 * <br>
 * It was not designed to be used stand alone. It is an integrated part of the
 * default lifecycle of the three provided packaging types:
 * <li><b>osgi.subsystem.application</b>
 * <li><b>osgi.subsystem.composite</b>
 * <li><b>osgi.subsystem.feature</b>
 *
 */
@Mojo(name = "downloadAndCacheArtifacts",
        defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        requiresDependencyCollection = ResolutionScope.COMPILE,
        requiresProject = true, inheritByDefault = true, aggregator = false)
public class MojoDownloadAndCacheArtifacts extends AbstractSubsystemMojo {

    private final BuildContextWithUrl copyBuildContext;

    @Inject
    public MojoDownloadAndCacheArtifacts(MavenProject pProject,
            BuildContextWithUrl pCopyBuildContext) {
        super(pProject);
        copyBuildContext = pCopyBuildContext;
        addExtraSupportedPackaging("osgi.subsystem.feature");
        addExtraSupportedPackaging("osgi.subsystem.composite");
        addExtraSupportedPackaging("osgi.subsystem.application");
    }

    @Override
    protected void doBeforeSkipMojo() throws MojoExecutionException {
        copyBuildContext.markSkipExecution();
    }

    @Override
    protected void executeMojo()
            throws MojoExecutionException, MojoFailureException {

        getLog().info(
                "Setting up download and caching of artifacts for project "
                        + getProject().getArtifactId());

        ArtifactTrackerManager artifactTrackerManager = ArtifactTrackerManagerBuilder
                .newBuilder(getMavenSession(), getCacheDirectory())
                .withGroupingByTypeDirectory(true).withVerbose(isVerbose())
                .withPreviousCachingRequired(false).mavenSetup()
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

        artifactTrackerManager.copyMavenArtifactsToCache(copyBuildContext);

    }
}
