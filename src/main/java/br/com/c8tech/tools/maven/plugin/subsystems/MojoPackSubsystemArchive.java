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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import javax.inject.Inject;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;

import br.com.c8tech.tools.maven.osgi.lib.mojo.CommonMojoConstants;
import br.com.c8tech.tools.maven.osgi.lib.mojo.archivers.AbstractSubsystemArchiver;
import br.com.c8tech.tools.maven.osgi.lib.mojo.incremental.ArtifactTracker;
import br.com.c8tech.tools.maven.osgi.lib.mojo.incremental.ArtifactTrackerManager;
import br.com.c8tech.tools.maven.osgi.lib.mojo.incremental.ArtifactTrackerManagerBuilder;
import io.takari.incrementalbuild.Output;
import io.takari.incrementalbuild.aggregator.AggregatorBuildContext;
import io.takari.incrementalbuild.aggregator.InputSet;

/**
 * This mojo is aimed to generate a subsystem archive file (.esa) for the
 * current project and also, when specified, to embed on it the cached bundles,
 * subsystems and others allowed artifacts.
 *
 */

@Mojo(name = "packESA", defaultPhase = LifecyclePhase.PACKAGE,
        requiresDependencyCollection = ResolutionScope.COMPILE,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        requiresProject = true, inheritByDefault = true, aggregator = false,
        threadSafe = true)
public class MojoPackSubsystemArchive extends AbstractSubsystemMojo {

    private final AggregatorBuildContext aggregatorBuildContext;

    /**
     * Use this to enable the generation of the mime-type tag file entry in the
     * generated subsystem archive.
     * <p>
     * Note that currently is not supported by Apache Aries (but it is in the
     * Subsystem spec).
     */
    @Parameter(defaultValue = "false")
    protected boolean generateEsaMimeEntry;

    /**
     *
     * @param project
     * @param pAggregatorBuildContext
     */
    @Inject
    public MojoPackSubsystemArchive(MavenProject project,
            AggregatorBuildContext pAggregatorBuildContext) {
        super(project);
        aggregatorBuildContext = pAggregatorBuildContext;
    }

    protected File calculateArchiveFile() {
        Path targetFile = Paths.get(getProject().getBuild().getDirectory(),
                getFinalName() + "."
                        + CommonMojoConstants.OSGI_SUBSYSTEM_EXTENSION);

        return targetFile.toFile();
    }

    /**
     * Generate the ESA file for the current project.
     *
     * @throws MojoExecutionException
     *                                    if an error occurred while building
     *                                    the ESA file
     * @throws MojoFailureException
     */
    @Override
    public void executeMojo()
            throws MojoExecutionException, MojoFailureException {

        if (!getSubsystemManifestFile().toFile().exists()) {
            throw new MojoExecutionException(
                    "A subsystem manifest file was not found for the project "
                            + getProject().getArtifactId());
        }
        getLog().info(
                "Setting up generation of the Subsystem archive for project "
                        + getProject().getArtifactId());

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
            getLog().info("Registering artifacts into the OSGi Subsystem "
                    + "archive generation incremental build context.");
        }

        prepareForSubsystemArchiveGeneration(artifactTrackerManager);
    }

    private void generateSubsystemArchive(
            ArtifactTrackerManager pArtifactTrackers, Output<File> pOutputFile,
            Iterable<File> pInputFiles) throws IOException {
        getLog().info(
                "Starting to pack the items of the OSGi Subsystem archive for project "
                        + getProject().getArtifactId());
        Archiver esaArchiver = getDependenciesHelper()
                .lookupArchiver(getProject().getPackaging());
        for (File file : pInputFiles) {
            Path source;
            Path target;

            if (file.toPath().endsWith("SUBSYSTEM.MF")) {
                ((AbstractSubsystemArchiver) esaArchiver).setManifest(file);
                continue;
            } else {
                ArtifactTracker artifact = pArtifactTrackers
                        .searchByPath(file.getPath());
                if (artifact != null && artifact.isCached()
                        && artifact.isToBeEmbedded()) {
                    source = artifact.getCachedFilePath();
                    target = file.toPath().getFileName();
                } else {
                    source = file.toPath();
                    target = Paths
                            .get(getProject().getBuild().getOutputDirectory())
                            .relativize(file.toPath());
                }
            }
            esaArchiver.addFile(source.toFile(), target.toString());
            if (isVerbose()) {
                getLog().info("    included file: " + target);
            }
        }

        ((AbstractSubsystemArchiver) esaArchiver)
                .setGenerateEsaMimeEntry(generateEsaMimeEntry);
        esaArchiver.setIncludeEmptyDirs(false);
        esaArchiver.setDestFile(pOutputFile.getResource());
        esaArchiver.createArchive();
        getLog().info("OSGi Subsystem archive was successfully generated at "
                + pOutputFile.getResource());
    }

    private void prepareForSubsystemArchiveGeneration(
            final ArtifactTrackerManager pArtifactTrackerManager)
            throws MojoExecutionException {

        try {

            InputSet inputSet = registerArtifactsIntoAggregatorBuildContext(
                    pArtifactTrackerManager.lookupEmbeddableArtifactTrackers(),
                    aggregatorBuildContext, true);
            inputSet.addInputs(
                    getWorkSubDirectory(DEFAULT_SUBSYSTEM_DIRECTORY).toFile(),
                    null, Arrays.asList("plugins/*", "subsystems/*"));
            inputSet.addInputs(Paths
                    .get(getProject().getBuild().getOutputDirectory()).toFile(),
                    null, null);
            File esaFile = calculateArchiveFile();
            inputSet.aggregateIfNecessary(esaFile,
                    (outputFile, inputFiles) -> generateSubsystemArchive(
                            pArtifactTrackerManager, outputFile, inputFiles));

            getProject().getArtifact().setFile(esaFile);
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Failure occurred while generating the OSGi subsystem archive",
                    e);
        }

    }

}
