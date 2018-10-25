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
package br.com.c8tech.tools.maven.plugin.subsystems.ut;

import static io.takari.maven.testing.TestMavenRuntime.newParameter;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.aries.subsystem.core.archive.Attribute;
import org.apache.aries.subsystem.core.archive.Directive;
import org.apache.aries.subsystem.core.archive.Parameter;
import org.apache.aries.subsystem.core.archive.SubsystemContentHeader;
import org.apache.aries.subsystem.core.archive.SubsystemContentHeader.Clause;
import org.apache.aries.subsystem.core.archive.SubsystemManifest;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import org.junit.Test;
import org.osgi.framework.VersionRange;

import br.com.c8tech.tools.maven.osgi.lib.mojo.CommonMojoConstants;
import br.com.c8tech.tools.maven.plugin.subsystems.PojoSubsystemManifestConfiguration.ProvisionPolicy;

public class GenerateSubsystemManifestUnitTest
        extends AbstractSubsystemUnitTest {

    @Test
    public void testVersionRange() throws Exception {
        String version = "[0.1.1.20170424014114, 0.1.1.20170424014114]";
        VersionRange versionRange1 = new VersionRange(version);

        assertThat(versionRange1.isExact()).isTrue();
        assertThat(new VersionRange("0.1.1.20170424014114").isExact())
                .isFalse();

        SubsystemContentHeader contentHeader = new SubsystemContentHeader(
                "com.c8tech.runtime.kernel.lib;type=osgi.bundle;version=\"[0.1.1.20170424014114,0.1.1.20170424014114]\";resolution:=mandatory");
        Optional<Clause> clause = contentHeader.getClauses().stream()
                .findFirst();
        if (clause.isPresent()) {
            VersionRange versionRange2 = clause.get().getVersionRange();
            assertThat(versionRange1).isEqualTo(versionRange2);
        }
    }

//    @Test
    public void testGenerationCompositeWithSubsystemDepInsideWorkspace()
            throws Exception {

        MavenProject project = incrementalBuildRule.readMavenProject(
                testResources.getBasedir("ut-project--composite"));
        Path targetDir = project.getBasedir().toPath().resolve("target");
        Path rootDir = targetDir.resolve("work/esa");

        addDependencyDir(project, "teste", "extracted.composite.esa",
                "subsystems/extracted.composite.esa", true, "compile", "0.1.0",
                "osgi.subsystem.composite", false);
        addDependencyDir(project, "org.slf4j", "slf4j-api",
                "jars/extracted-slf4j-api", true, "compile", "1.7.25", "jar",
                false);

        incrementalBuildRule.executeMojo(project, "downloadAndCacheArtifacts",
                newParameterMavenArtifactConfigSet(
                        "org.slf4j:slf4j-api:1.7.25@2"),
                newParameter("manifestScopes", "compile"),
                newParameter("verbose", "true"),
                newParameter("embeddableScopes", "compile"),
                newParameter("workspaceResolutionAllowed", "true"),
                newParameter("optionalConsidered", "false"),
                newParameter("transitiveConsidered", "false"));

        incrementalBuildRule.executeMojo(project, "generateSubsystemManifest",
                newParameterMavenArtifactConfigSet(
                        "org.slf4j:slf4j-api:1.7.25@2"),
                newManifest(basicHeaders()).requireBundle(
                        "com.c8tech.runtime.kernel.lib;bundle-version=\"0.0.0\"")
                        .preferredProvider(
                                "anBundle;type=osgi.bundle;version=0.0.0")
                        .build(),
                newParameter("manifestScopes", "compile"),
                newParameter("verbose", "true"),
                newParameter("embeddableScopes", "compile"),
                newParameter("workspaceResolutionAllowed", "true"),
                newParameter("allowComputeExportPackages", "true"),
                newParameter("allowComputeSubsystemContent", "true"),
                newParameter("allowComputeImportPackages", "true"),
                newParameter("optionalConsidered", "false"),
                newParameter("transitiveConsidered", "false"));

        Path outputFile = rootDir
                .resolve(CommonMojoConstants.OSGI_SUBSYSTEM_MANIFEST_LOCATION);

        Path expected = Paths.get(getClass().getResource(
                "/files/manifest_composite_fixed_content_on_workspace.txt")
                .toURI());
        SubsystemManifest smExpected = new SubsystemManifest(expected.toFile());
        SubsystemManifest smActual = new SubsystemManifest(outputFile.toFile());
        assertThat(smActual).isEqualTo(smExpected);

        // pack the generated manifest
        incrementalBuildRule.executeMojo(project, "packESA",
                newParameterMavenArtifactConfigSet(
                        "org.slf4j:slf4j-api:1.7.25@2"),
                newParameter("manifestScopes", "compile"),
                newParameter("workspaceResolutionAllowed", "true"),
                newParameter("verbose", "true"),
                newParameter("embeddableScopes", "compile"),
                newParameter("optionalConsidered", "false"),
                newParameter("transitiveConsidered", "false"));
        Path esa0 = targetDir
                .resolve("subsystem.composite.unit.test-0.1.0.esa");
        assertThat(esa0.toFile()).as("ESA file was not created.").exists();

        ZipFile zip = new ZipFile(esa0.toFile());
        assertThat(zip.size()).isEqualTo(4);

        ZipEntry entry1 = zip.getEntry("OSGI-INF/SUBSYSTEM.MF");
        assertThat(entry1).isNotNull();

        zip.close();
    }

    public static List<Header> basicHeaders() {
        return Manifest.newManifest()
                .addHeader("Subsystem-ContactAddress", "r. X number 100")
                .addHeader("Subsystem-Copyright",
                        "Cristiano Gavião (c) 2015-2025")
                .addHeader("Subsystem-DocURL",
                        "http://www.example.com/Firewall/doc")
                .addHeader("Subsystem-Icon", "/icons/acme-logo.png; size=64")
                .addHeader("Subsystem-License",
                        "http://www.eclipse.org/org/documents/edl-v10.php")
                .addHeader("Subsystem-Localization", "OSGI-INF/l10n/subsystem2")
                .addHeader("Subsystem-Vendor", "C8Tech")
                .addHeader("Subsystem-Name", "Subsystem Archive Generator")
                .addHeader("Subsystem-Version", "1.0.0")
                .addHeader("Subsystem-SymbolicName", "subsystem.maven.plugin")
                .addHeader("Subsystem-Description",
                        "A subsystem to test the generator")
                .addHeader("Subsystem-Category", "Generator, OSGi")
                .getHeaders();
    }

//    @Test
    public void testGenerationCompositeWithSubsystemDepInsideWorkspaceWithoutEmbed()
            throws Exception {

        MavenProject project = incrementalBuildRule.readMavenProject(
                testResources.getBasedir("ut-project--composite"));
        Path targetDir = project.getBasedir().toPath().resolve("target");
        Path rootDir = targetDir.resolve("work/esa");

        addDependencyDir(project, "teste", "extracted.composite.esa",
                "subsystems/extracted.composite.esa", true, "compile", "0.1.0",
                "osgi.subsystem.composite", false);
        addDependencyDir(project, "org.slf4j", "slf4j-api",
                "jars/extracted-slf4j-api", true, "compile", "1.7.25", "jar",
                false);

        incrementalBuildRule.executeMojo(project, "downloadAndCacheArtifacts",
                newParameterMavenArtifactConfigSet(
                        "org.slf4j:slf4j-api:1.7.25@2"),
                newParameter("manifestScopes", "compile"),
                newParameter("verbose", "true"),
                newParameter("workspaceResolutionAllowed", "true"),
                newParameter("embeddableScopes", ""),
                newParameter("optionalConsidered", "false"),
                newParameter("transitiveConsidered", "false"));

        incrementalBuildRule.executeMojo(project, "generateSubsystemManifest",
                newParameterMavenArtifactConfigSet(
                        "org.slf4j:slf4j-api:1.7.25@2"),
                newParameter("manifestScopes", "compile"),
                newParameter("verbose", "true"),
                newManifest(basicHeaders()).requireBundle(
                        "com.c8tech.runtime.kernel.lib;bundle-version=\"0.0.0\"")
                        .preferredProvider(
                                "anBundle;type=osgi.bundle;version=0.0.0")
                        .build(),
                newParameter("workspaceResolutionAllowed", "true"),
                newParameter("embeddableScopes", ""),
                newParameter("allowComputeExportPackages", "true"),
                newParameter("allowComputeImportPackages", "true"),
                newParameter("allowComputeSubsystemContent", "true"),
                newParameter("optionalConsidered", "false"),
                newParameter("transitiveConsidered", "false"));

        Path outputFile = rootDir
                .resolve(CommonMojoConstants.OSGI_SUBSYSTEM_MANIFEST_LOCATION);

        Path expected = Paths.get(getClass().getResource(
                "/files/manifest_composite_fixed_content_on_workspace.txt")
                .toURI());
        SubsystemManifest smExpected = new SubsystemManifest(expected.toFile());
        SubsystemManifest smActual = new SubsystemManifest(outputFile.toFile());
        assertThat(smActual).isEqualTo(smExpected);

        // pack the generated manifest
        incrementalBuildRule.executeMojo(project, "packESA",
                newParameterMavenArtifactConfigSet(
                        "org.slf4j:slf4j-api:1.7.25@2"),
                newParameter("manifestScopes", "compile"),
                newParameter("verbose", "true"),
                newParameter("embeddableScopes", ""),
                newParameter("allowComputeExportPackages", "true"),
                newParameter("optionalConsidered", "false"),
                newParameter("transitiveConsidered", "false"));
        Path esa0 = targetDir
                .resolve("subsystem.composite.unit.test-0.1.0.esa");
        assertThat(esa0.toFile()).as("ESA file was not created.").exists();

        ZipFile zip = new ZipFile(esa0.toFile());
        assertThat(zip.size()).isEqualTo(2);

        Enumeration<? extends ZipEntry> en = zip.entries();

        while (en.hasMoreElements()) {
            ZipEntry type = en.nextElement();
            System.out.print(type.getName() + "  ");
            System.out.println(type.isDirectory());
        }

        ZipEntry entry1 = zip.getEntry("OSGI-INF/SUBSYSTEM.MF");
        assertThat(entry1).isNotNull();

        zip.close();
    }

    @Test
    public void testGenerationApplicationWithoutDependenciesAcceptDependencies()
            throws Exception {

        MavenProject project = incrementalBuildRule.readMavenProject(
                testResources.getBasedir("ut-project--application"));
        Path targetDir = project.getBasedir().toPath().resolve("target");
        Path rootDir = targetDir.resolve("work/esa");

        incrementalBuildRule.executeMojo(project, "generateSubsystemManifest",
                newParameter("optionalConsidered", "false"),
                newParameter("transitiveConsidered", "false"),
                newParameter("verbose", "true"),
                newManifest(basicHeaders()).subsystemContent(
                        "com.c8tech.anotherBundle;start-order:=0;type=osgi.bundle;version=\"[1.0.0,1.0.0]\";resolution:=mandatory")
                        .provisioninPolicy(ProvisionPolicy.ACCEPT_DEPENDENCIES)
                        .build());

        Path outputFile = rootDir
                .resolve(CommonMojoConstants.OSGI_SUBSYSTEM_MANIFEST_LOCATION);

        Path expected = Paths.get(getClass().getResource(
                "/files/manifest_application_fixed_content_accept_deps.txt")
                .toURI());
        SubsystemManifest smExpected = new SubsystemManifest(expected.toFile());
        SubsystemManifest smActual = new SubsystemManifest(outputFile.toFile());
        assertThat(smActual).isEqualTo(smExpected);

        // pack the generated manifest
        incrementalBuildRule.executeMojo(project, "packESA",
                newParameter("verbose", "true"));
        Path esa0 = targetDir
                .resolve("subsystem.application.unit.test-0.1.0.esa");
        assertThat(esa0.toFile()).as("ESA file was not created.").exists();

        ZipFile zip = new ZipFile(esa0.toFile());
        assertThat(zip.size()).isEqualTo(2);

        ZipEntry entry1 = zip.getEntry("OSGI-INF/SUBSYSTEM.MF");
        assertThat(entry1).isNotNull();

        zip.close();
    }

    @Test
    public void testGenerationCompositeWithoutDependencies() throws Exception {

        MavenProject project = incrementalBuildRule.readMavenProject(
                testResources.getBasedir("ut-project--composite"));
        Path targetDir = project.getBasedir().toPath().resolve("target");
        Path rootDir = targetDir.resolve("work/esa");

        incrementalBuildRule.executeMojo(project, "generateSubsystemManifest",
                newParameter("excludedArtifacts", "empty"),
                newParameter("manifestScopes", "empty"),
                newParameter("verbose", "true"),
                newParameter("embeddableScopes", "empty"),
                newParameter("optionalConsidered", "false"),
                newParameter("transitiveConsidered", "false"),
                newManifest(basicHeaders())
                        .preferredProvider(
                                "anBundle;type=osgi.bundle;version=0.0.0")
                        .requireBundle(
                                "com.acme.chess; version= \"[1.0, 2.0)\"")
                        .subsystemContent(
                                "com.c8tech.anotherBundle;start-order:=0;type=osgi.bundle;version=\"[1.0.0,1.0.0]\";resolution:=mandatory")
                        .build());

        Path outputFile = rootDir
                .resolve(CommonMojoConstants.OSGI_SUBSYSTEM_MANIFEST_LOCATION);

        Path expected = Paths.get(getClass()
                .getResource("/files/manifest_composite_fixed_content.txt")
                .toURI());
        SubsystemManifest smExpected = new SubsystemManifest(expected.toFile());
        SubsystemManifest smActual = new SubsystemManifest(outputFile.toFile());
        assertThat(smActual).isEqualTo(smExpected);

        // pack the generated manifest
        incrementalBuildRule.executeMojo(project, "packESA",
                newParameter("verbose", "true"));
        Path esa0 = targetDir
                .resolve("subsystem.composite.unit.test-0.1.0.esa");
        assertThat(esa0.toFile()).as("ESA file was not created.").exists();

        ZipFile zip = new ZipFile(esa0.toFile());
        assertThat(zip.size()).isEqualTo(2);

        ZipEntry entry1 = zip.getEntry("OSGI-INF/SUBSYSTEM.MF");
        assertThat(entry1).isNotNull();

        zip.close();
    }

    @Test
    public void testGenerationCompositeWithoutDependenciesAndOpenVersionRange()
            throws Exception {

        MavenProject project = incrementalBuildRule.readMavenProject(
                testResources.getBasedir("ut-project--composite"));
        Path targetDir = project.getBasedir().toPath().resolve("target");
        Path rootDir = targetDir.resolve("work/esa");

        incrementalBuildRule.executeMojo(project, "generateSubsystemManifest",
                newParameter("excludedArtifacts", "empty"),
                newParameter("manifestScopes", "empty"),
                newParameter("verbose", "true"),
                newParameter("embeddableScopes", "empty"),
                newParameter("optionalConsidered", "false"),
                newParameter("transitiveConsidered", "false"),
                newManifest(basicHeaders())
                        .preferredProvider(
                                "anBundle;type=osgi.bundle;version=0.0.0")
                        .requireBundle(
                                "com.acme.chess; version= \"[1.0, 2.0)\"")
                        .subsystemContent(
                                "com.c8tech.anotherBundle;start-order:=0;type=osgi.bundle;version=\"[1.0.0,1.0.0)\";resolution:=mandatory")
                        .build());

        Path outputFile = rootDir
                .resolve(CommonMojoConstants.OSGI_SUBSYSTEM_MANIFEST_LOCATION);

        Path expected = Paths.get(getClass()
                .getResource("/files/manifest_composite_fixed_content.txt")
                .toURI());
        SubsystemManifest smExpected = new SubsystemManifest(expected.toFile());
        SubsystemManifest smActual = new SubsystemManifest(outputFile.toFile());
        assertThat(smActual).isEqualTo(smExpected);

        // pack the generated manifest
        incrementalBuildRule.executeMojo(project, "packESA",
                newParameter("verbose", "true"));
        Path esa0 = targetDir
                .resolve("subsystem.composite.unit.test-0.1.0.esa");
        assertThat(esa0.toFile()).as("ESA file was not created.").exists();

        ZipFile zip = new ZipFile(esa0.toFile());
        assertThat(zip.size()).isEqualTo(2);

        ZipEntry entry1 = zip.getEntry("OSGI-INF/SUBSYSTEM.MF");
        assertThat(entry1).isNotNull();

        zip.close();
    }

    /**
     * Ensure a feature subsystem manifest are being created with compile and
     * provided dependencies but only compile will be embedded.
     * <p>
     *
     * @throws Exception
     */
    @Test
    public void testGenerationFeatureEmbeddingCompileAndProvidedDependencies()
            throws Exception {

        MavenProject project = incrementalBuildRule.readMavenProject(
                testResources.getBasedir("ut-project--feature"));

        addDependency(project, "jars/aBundle.jar", true, "compile", "jar",
                false);
        addDependency(project, "subsystems/aCompositeSubsystem.esa", true,
                "compile", "osgi.subsystem.composite", true);
        addDependency(project, "jars/anotherBundle.jar", true, "provided",
                "jar", false);
        addDependency(project, "jars/aNonValidBundle.jar", true, "compile",
                "jar", false);
        addDependency(project, "jars/aTransitiveDependencyBundle.jar", false,
                "compile", "jar", false);

        incrementalBuildRule.executeMojo(project, "downloadAndCacheArtifacts",
                newManifest().build(),
                newParameter("optionalConsidered", "true"),
                newParameter("embeddableScopes", "compile"),
                newParameter("verbose", "true"),
                newParameter("manifestScopes", "compile,provided"),
                newParameter("transitiveConsidered", "true"));

        incrementalBuildRule.assertBuildOutputs(
                new File(project.getBasedir(), "target"),
                "cache/plugins/aBundle-1.8.4.jar",
                "cache/plugins/aTransitiveDependencyBundle-1.0.0.jar",
                "cache/plugins/anotherBundle-1.0.0.jar",
                "cache/subsystems/aCompositeSubsystem-0.1.0.esa");

        incrementalBuildRule.executeMojo(project, "generateSubsystemManifest",
                newManifest(basicHeaders()).build(),
                newParameter("optionalConsidered", "true"),
                newParameter("embeddableScopes", "compile"),
                newParameter("verbose", "true"),
                newParameter("manifestScopes", "compile,provided"),
                newParameter("allowComputeSubsystemContent", "true"),
                newParameter("transitiveConsidered", "true"));

        incrementalBuildRule.assertBuildOutputs(
                new File(project.getBasedir(), "target"),
                "work/esa/OSGI-INF/SUBSYSTEM.MF");

        Path outputFile = assertAndGetBuildOutput(project,
                "target/work/esa/OSGI-INF/SUBSYSTEM.MF");
        Path expected = Paths.get(getClass().getResource(
                "/files/manifest_feature_deps_compile_and_provided_plus_optional_and_transitives2.txt")
                .toURI());
        SubsystemManifest smExpected = new SubsystemManifest(expected.toFile());
        SubsystemManifest smActual = new SubsystemManifest(outputFile.toFile());
        assertThat(smActual).isEqualTo(smExpected);

        // pack the generated manifest
        incrementalBuildRule.executeMojo(project, "packESA",
                newManifest(basicHeaders())
                        .importService("one.service.to.Import").build(),
                newParameter("optionalConsidered", "true"),
                newParameter("embeddableScopes", "compile"),
                newParameter("verbose", "true"),
                newParameter("manifestScopes", "compile,provided"),
                newParameter("transitiveConsidered", "true"));
        Path esa0 = project.getBasedir().toPath()
                .resolve("target/subsystem.feature.unit.test-0.1.0.esa");
        assertThat(esa0.toFile()).as("ESA file was not created.").exists();

        ZipFile zip = new ZipFile(esa0.toFile());
        assertThat(zip.size()).isEqualTo(5);

        ZipEntry entry1 = zip.getEntry("aTransitiveDependencyBundle-1.0.0.jar");
        assertThat(entry1).isNotNull();

        ZipEntry entry2 = zip.getEntry("aCompositeSubsystem-0.1.0.esa");
        assertThat(entry2).isNotNull();

        ZipEntry entry3 = zip.getEntry("aBundle-1.8.4.jar");
        assertThat(entry3).isNotNull();

        ZipEntry entry4 = zip.getEntry("OSGI-INF/SUBSYSTEM.MF");
        assertThat(entry4).isNotNull();

        zip.close();
    }

    @Test
    public void testGenerationFeatureEmbeddingProvidedDependencies()
            throws Exception {
        MavenProject project = incrementalBuildRule.readMavenProject(
                testResources.getBasedir("ut-project--feature"));

        addDependency(project, "jars/aBundle.jar", true, "compile", "jar",
                false);
        addDependency(project, "subsystems/aCompositeSubsystem.esa", true,
                "provided", "osgi.subsystem.composite", true);
        addDependency(project, "jars/anotherBundle.jar", true, "provided",
                "jar", false);
        addDependency(project, "jars/aNonValidBundle.jar", true, "compile",
                "jar", false);
        addDependency(project, "jars/aTransitiveDependencyBundle.jar", false,
                "compile", "jar", false);

        incrementalBuildRule.executeMojo(project, "downloadAndCacheArtifacts",
                newParameter("optionalConsidered", "true"),
                newParameter("verbose", "true"),
                newParameter("manifestScopes", "compile,provided"),
                newParameter("embeddableScopes", "provided"),
                newParameter("transitiveConsidered", "true"));

        incrementalBuildRule.assertBuildOutputs(
                new File(project.getBasedir(), "target"),
                "cache/plugins/anotherBundle-1.0.0.jar",
                "cache/plugins/aBundle-1.8.4.jar",
                "cache/plugins/aTransitiveDependencyBundle-1.0.0.jar",
                "cache/plugins/anotherBundle-1.0.0.jar",
                "cache/subsystems/aCompositeSubsystem-0.1.0.esa");

        incrementalBuildRule.executeMojo(project, "generateSubsystemManifest",
                newManifest(basicHeaders()).build(),
                newParameter("optionalConsidered", "true"),
                newParameter("verbose", "true"),
                newParameter("manifestScopes", "compile,provided"),
                newParameter("allowComputeSubsystemContent", "true"),
                newParameter("embeddableScopes", "provided"),
                newParameter("transitiveConsidered", "true"));

        incrementalBuildRule.assertBuildOutputs(
                new File(project.getBasedir(), "target"),
                "work/esa/OSGI-INF/SUBSYSTEM.MF");

        Path outputFile = assertAndGetBuildOutput(project,
                "target/work/esa/OSGI-INF/SUBSYSTEM.MF");
        Path expected = Paths.get(getClass()
                .getResource("/files/manifest_feature_deps_provided.txt")
                .toURI());
        SubsystemManifest smExpected = new SubsystemManifest(expected.toFile());
        SubsystemManifest smActual = new SubsystemManifest(outputFile.toFile());
        assertThat(smActual).isEqualTo(smExpected);

        SubsystemContentHeader content = smActual.getSubsystemContentHeader();
        assertThat(content.getClauses().size()).isEqualTo(4);

        Collection<Clause> clauses1 = content.getClauses();
        for (Clause clause : clauses1) {
            if (clause.getSymbolicName().equals("com.c8tech.anotherBundle")) {
                Clause mc = new Clause(clause.toString());
                assertThat(mc.getDirectives().size()).isEqualTo(2);
                assertThat(mc.getParameters().size()).isEqualTo(4);
                assertThat(mc.getAttributes().size()).isEqualTo(2);
                assertThat(mc.getPath()).isEqualTo("com.c8tech.anotherBundle");
                Parameter parameter = mc.getParameter("start-order");
                assertThat(parameter.getValue()).isEqualTo("0");

                Directive directive = mc.getDirective("resolution");
                assertThat(directive.getValue()).isEqualTo("mandatory");

                Directive directiveNonexistent = mc.getDirective("non");
                assertThat(directiveNonexistent).isNull();

                Attribute attNonexistent = mc.getAttribute("non");
                assertThat(attNonexistent).isNull();

                Clause mc2 = new Clause(clause.toString());
                assertThat(mc).isEqualTo(mc2);
            }
        }

        // pack the generated manifest
        incrementalBuildRule.executeMojo(project, "packESA",
                newParameter("optionalConsidered", "true"),
                newParameter("verbose", "true"),
                newParameter("manifestScopes", "compile,provided"),
                newParameter("embeddableScopes", "provided"),
                newParameter("transitiveConsidered", "true"));
        Path esa0 = project.getBasedir().toPath()
                .resolve("target/subsystem.feature.unit.test-0.1.0.esa");
        assertThat(esa0.toFile()).as("ESA file was not created.").exists();

        ZipFile zip = new ZipFile(esa0.toFile());
        assertThat(zip.size()).isEqualTo(4);

        ZipEntry entry1 = zip.getEntry("anotherBundle-1.0.0.jar");
        assertThat(entry1).isNotNull();

        ZipEntry entry2 = zip.getEntry("aCompositeSubsystem-0.1.0.esa");
        assertThat(entry2).isNotNull();

        ZipEntry entry4 = zip.getEntry("OSGI-INF/SUBSYSTEM.MF");
        assertThat(entry4).isNotNull();
        zip.close();
    }

    /**
     * Ensure that manifest is being generated with dependencies with compile
     * scope.
     * <p>
     * Optional, transitive and no bundle dependencies must be out of the
     * generated manifest.
     *
     * @throws Exception
     */
    @Test
    public void testGenerationCompositeWithCompileDependencies()
            throws Exception {

        MavenProject project = incrementalBuildRule.readMavenProject(
                testResources.getBasedir("ut-project--composite"));

        addDependency(project, "jars/aBundle.jar", true, "compile", "jar",
                false);
        addDependency(project, "subsystems/aCompositeSubsystem.esa", true,
                "compile", "osgi.subsystem.composite", false);
        addDependency(project, "jars/anotherBundle.jar", true, "provided",
                "jar", false);
        addDependency(project, "jars/aNonValidBundle.jar", true, "compile",
                "jar", false);
        addDependency(project, "jars/aTransitiveDependencyBundle.jar", false,
                "compile", "jar", true);

        incrementalBuildRule.executeMojo(project, "downloadAndCacheArtifacts",
                newParameter("verbose", "true"),
                newParameter("manifestScopes", "compile,provided"),
                newParameter("excludedArtifacts", "anotherBundle"),
                newParameter("embeddableScopes", "compile"));

        incrementalBuildRule.assertBuildOutputs(
                new File(project.getBasedir(), "target"),
                "cache/plugins/aBundle-1.8.4.jar",
                "cache/plugins/anotherBundle-1.0.0.jar",
                "cache/subsystems/aCompositeSubsystem-0.1.0.esa");

        incrementalBuildRule.executeMojo(project, "generateSubsystemManifest",
                newManifest(basicHeaders())
                        .exportPackage("one.package.to.export3")
                        .subsystemContent(
                                "org.acme.billing.credit.subsystem;type=osgi.subsystem.composite;version=1.0",
                                "org.acme.billing.impl;type=osgi.bundle;version=1.0")
                        .importPackage("one.package.to.import3").build(),
                newParameter("verbose", "true"),
                newParameter("manifestScopes", "compile,provided"),
                newParameter("allowComputeImportPackages", "true"),
                newParameter("allowComputeExportPackages", "true"),
                newParameter("allowComputeImportServices", "true"),
                newParameter("allowComputeExportServices", "true"),
                newParameter("allowComputeGenericCapabilities", "true"),
                newParameter("allowComputeGenericRequirements", "true"),
                newParameter("allowComputeRequireBundle", "true"),
                newParameter("allowComputeSubsystemContent", "true"),
                newParameter("excludedArtifacts", "anotherBundle"),
                newParameter("embeddableScopes", "compile"));

        incrementalBuildRule.assertBuildOutputs(
                new File(project.getBasedir(), "target"),
                "work/esa/OSGI-INF/SUBSYSTEM.MF");

        Path outputFile = assertAndGetBuildOutput(project,
                "target/work/esa/OSGI-INF/SUBSYSTEM.MF");
        Path expected = Paths.get(getClass()
                .getResource(
                        "/files/manifest_composite_deps_compile_provided.txt")
                .toURI());
        SubsystemManifest smExpected = new SubsystemManifest(expected.toFile());
        SubsystemManifest smActual = new SubsystemManifest(outputFile.toFile());
        assertThat(smActual).isEqualTo(smExpected);

        // pack the generated manifest
        incrementalBuildRule.executeMojo(project, "packESA",
                newManifest().build(), newParameter("verbose", "true"),
                newParameter("manifestScopes", "compile,provided"),
                newParameter("excludedArtifacts", "anotherBundle"),
                newParameter("embeddableScopes", "compile"));
        Path esa0 = project.getBasedir().toPath()
                .resolve("target/subsystem.composite.unit.test-0.1.0.esa");
        assertThat(esa0.toFile()).as("ESA file was not created.").exists();

        ZipFile zip = new ZipFile(esa0.toFile());
        assertThat(zip.size()).as("Zip size").isEqualTo(4);

        ZipEntry entry1 = zip.getEntry("aBundle-1.8.4.jar");
        assertThat(entry1).isNotNull();
        ZipEntry entry3 = zip.getEntry("aCompositeSubsystem-0.1.0.esa");
        assertThat(entry3).isNotNull();
        ZipEntry entry2 = zip.getEntry("OSGI-INF/SUBSYSTEM.MF");
        assertThat(entry2).isNotNull();
        zip.close();
    }

    /**
     * Ensure that manifest is being generated with dependencies with compile
     * scope.
     * <p>
     * Embeddable file control will be generated but only for compile scope.
     * <br>
     * Optional, transitive will be include in generated manifest.
     *
     * @throws Exception
     */
    @Test
    public void testGenerationFeatureWithCompileDependenciesPlusOptionalAndTransitive()
            throws Exception {

        MavenProject project = incrementalBuildRule.readMavenProject(
                testResources.getBasedir("ut-project--feature"));

        addDependency(project, "jars/aBundle.jar", true, "compile", "jar",
                false);
        addDependency(project, "subsystems/aCompositeSubsystem.esa", true,
                "compile", "osgi.subsystem.composite", true);
        addDependency(project, "jars/anotherBundle.jar", true, "provided",
                "jar", false);
        addDependency(project, "jars/aNonValidBundle.jar", true, "compile",
                "jar", false);
        addDependency(project, "jars/aTransitiveDependencyBundle.jar", false,
                "compile", "jar", false);

        incrementalBuildRule.executeMojo(project, "downloadAndCacheArtifacts",
                newParameter("optionalConsidered", "true"),
                newParameter("verbose", "true"),
                newParameter("manifestScopes", "compile"),
                newParameter("embeddableScopes", "compile"),
                newParameter("transitiveConsidered", "true"));

        incrementalBuildRule.assertBuildOutputs(
                new File(project.getBasedir(), "target"),
                "cache/plugins/aBundle-1.8.4.jar",
                "cache/plugins/aTransitiveDependencyBundle-1.0.0.jar",
                "cache/subsystems/aCompositeSubsystem-0.1.0.esa");

        incrementalBuildRule.executeMojo(project, "generateSubsystemManifest",
                newManifest(basicHeaders()).build(),
                newParameter("optionalConsidered", "true"),
                newParameter("verbose", "true"),
                newParameter("manifestScopes", "compile"),
                newParameter("embeddableScopes", "compile"),
                newParameter("allowComputeSubsystemContent", "true"),
                newParameter("transitiveConsidered", "true"));

        incrementalBuildRule.assertBuildOutputs(
                new File(project.getBasedir(), "target"),
                "work/esa/OSGI-INF/SUBSYSTEM.MF");

        Path outputFile = assertAndGetBuildOutput(project,
                "target/work/esa/OSGI-INF/SUBSYSTEM.MF");
        Path expected = Paths.get(getClass().getResource(
                "/files/manifest_feature_deps_compile_plus_optional_and_transitives.txt")
                .toURI());
        SubsystemManifest smExpected = new SubsystemManifest(expected.toFile());
        SubsystemManifest smActual = new SubsystemManifest(outputFile.toFile());
        assertThat(smActual).isEqualTo(smExpected);

        // pack the generated manifest
        incrementalBuildRule.executeMojo(project, "packESA",
                newParameter("optionalConsidered", "true"),
                newParameter("verbose", "true"),
                newParameter("manifestScopes", "compile"),
                newParameter("embeddableScopes", "compile"),
                newParameter("transitiveConsidered", "true"));
        Path esa0 = project.getBasedir().toPath()
                .resolve("target/subsystem.feature.unit.test-0.1.0.esa");
        assertThat(esa0.toFile()).as("ESA file was not created.").exists();

        ZipFile zip = new ZipFile(esa0.toFile());
        assertThat(zip.size()).isEqualTo(5);

        ZipEntry entry1 = zip.getEntry("aBundle-1.8.4.jar");
        assertThat(entry1).isNotNull();
        ZipEntry entry2 = zip.getEntry("OSGI-INF/SUBSYSTEM.MF");
        assertThat(entry2).isNotNull();
        ZipEntry entry3 = zip.getEntry("aTransitiveDependencyBundle-1.0.0.jar");
        assertThat(entry3).isNotNull();

        ZipEntry entry4 = zip.getEntry("aCompositeSubsystem-0.1.0.esa");
        assertThat(entry4).isNotNull();
        zip.close();

    }

    /**
     * Ensure that manifest is being generated with dependencies with compile
     * scope.
     * <p>
     * Embeddable file control won't be generated. Optional, transitive and no
     * bundle dependencies must be out of the generated manifest.
     *
     * @throws Exception
     */
    @Test
    public void testGenerationFeatureWithCompileDependenciesWithFixedStartOrder()
            throws Exception {

        MavenProject project = incrementalBuildRule.readMavenProject(
                testResources.getBasedir("ut-project--feature"));

        addDependency(project, "jars/aBundle.jar", true, "compile", "jar",
                false);
        addDependency(project, "subsystems/aCompositeSubsystem.esa", true,
                "compile", "osgi.subsystem.composite", true);
        addDependency(project, "jars/anotherBundle.jar", true, "provided",
                "jar", false);
        addDependency(project, "jars/aNonValidBundle.jar", true, "compile",
                "jar", false);
        addDependency(project, "jars/aTransitiveDependencyBundle.jar", false,
                "compile", "jar", false);

        incrementalBuildRule.executeMojo(project, "downloadAndCacheArtifacts",
                newParameter("optionalConsidered", "false"),
                newParameter("embeddableScopes", "compile"),
                newParameter("verbose", "true"),
                newParameter("excludedArtifacts", "anotherBundle"),
                newParameter("manifestScopes", "compile"),
                newParameter("transitiveConsidered", "false"));

        incrementalBuildRule.assertBuildOutputs(
                new File(project.getBasedir(), "target"),
                "cache/plugins/aBundle-1.8.4.jar");

        incrementalBuildRule.executeMojo(project, "generateSubsystemManifest",
                newManifest(basicHeaders()).subsystemContent(
                        "com.c8tech.bundle;start-order:=1;version=1.8.4",
                        "com.c8tech.subsystem.composite;version=1.0.0;type=osgi.subsystem.composite")
                        .build(),
                newParameter("optionalConsidered", "false"),
                newParameter("embeddableScopes", "compile"),
                newParameter("verbose", "true"),
                newParameter("excludedArtifacts", "anotherBundle"),
                newParameter("manifestScopes", "compile"),
                newParameter("transitiveConsidered", "false"));

        incrementalBuildRule.assertBuildOutputs(
                new File(project.getBasedir(), "target"),
                "work/esa/OSGI-INF/SUBSYSTEM.MF");

        Path outputFile = assertAndGetBuildOutput(project,
                "target/work/esa/OSGI-INF/SUBSYSTEM.MF");
        Path expected = Paths.get(getClass()
                .getResource(
                        "/files/manifest_feature_deps_compile_startOrder.txt")
                .toURI());
        SubsystemManifest smExpected = new SubsystemManifest(expected.toFile());
        SubsystemManifest smActual = new SubsystemManifest(outputFile.toFile());

        assertThat(smActual).isEqualTo(smExpected);

        // pack the generated manifest
        incrementalBuildRule.executeMojo(project, "packESA",
                newParameter("optionalConsidered", "false"),
                newParameter("embeddableScopes", "compile"),
                newParameter("verbose", "true"),
                newParameter("excludedArtifacts", "anotherBundle"),
                newParameter("manifestScopes", "compile"),
                newParameter("transitiveConsidered", "false"));
        Path esa0 = project.getBasedir().toPath()
                .resolve("target/subsystem.feature.unit.test-0.1.0.esa");
        assertThat(esa0.toFile()).as("ESA file was not created.").exists();

        ZipFile zip = new ZipFile(esa0.toFile());
        assertThat(zip.size()).isEqualTo(3);

        ZipEntry entry1 = zip.getEntry("aBundle-1.8.4.jar");
        assertThat(entry1).isNotNull();

        ZipEntry entry2 = zip.getEntry("OSGI-INF/SUBSYSTEM.MF");
        assertThat(entry2).isNotNull();
        zip.close();

    }

    @Test
    public void testGenerationFeatureWithProvidedDependenciesExcludedOneItem()
            throws Exception {

        MavenProject project = incrementalBuildRule.readMavenProject(
                testResources.getBasedir("ut-project--feature"));

        addDependency(project, "jars/aBundle.jar", true, "compile", "jar",
                false);
        addDependency(project, "subsystems/aCompositeSubsystem.esa", true,
                "provided", "osgi.subsystem.composite", true);
        addDependency(project, "jars/anotherBundle.jar", true, "provided",
                "jar", false);
        addDependency(project, "jars/aNonValidBundle.jar", true, "compile",
                "jar", false);
        addDependency(project, "jars/aTransitiveDependencyBundle.jar", false,
                "compile", "jar", false);

        incrementalBuildRule.executeMojo(project, "downloadAndCacheArtifacts",
                newParameter("optionalConsidered", "false"),
                newParameter("verbose", "true"),
                newParameter("manifestScopes", "compile,provided"),
                newParameter("excludedArtifacts", "aCompositeSubsystem.esa"),
                newParameter("embeddableScopes", "compile"),
                newParameter("transitiveConsidered", "false"));
        incrementalBuildRule.assertBuildOutputs(
                new File(project.getBasedir(), "target"),
                "cache/plugins/aBundle-1.8.4.jar",
                "cache/plugins/anotherBundle-1.0.0.jar");

        incrementalBuildRule.executeMojo(project, "generateSubsystemManifest",
                newManifest(basicHeaders()).build(),
                newParameter("optionalConsidered", "false"),
                newParameter("verbose", "true"),
                newParameter("manifestScopes", "compile,provided"),
                newParameter("excludedArtifacts", "aCompositeSubsystem.esa"),
                newParameter("allowComputeSubsystemContent", "true"),
                newParameter("embeddableScopes", "compile"),
                newParameter("transitiveConsidered", "false"));

        incrementalBuildRule.assertBuildOutputs(
                new File(project.getBasedir(), "target"),
                "work/esa/OSGI-INF/SUBSYSTEM.MF");

        Path outputFile = assertAndGetBuildOutput(project,
                "target/work/esa/OSGI-INF/SUBSYSTEM.MF");
        Path expected = Paths.get(getClass().getResource(
                "/files/manifest_feature_deps_provided_excluded_one.txt")
                .toURI());
        SubsystemManifest smExpected = new SubsystemManifest(expected.toFile());
        SubsystemManifest smActual = new SubsystemManifest(outputFile.toFile());
        assertThat(smActual).isEqualTo(smExpected);

        // pack the generated manifest
        incrementalBuildRule.executeMojo(project, "packESA",
                newParameter("optionalConsidered", "false"),
                newParameter("verbose", "true"),
                newParameter("manifestScopes", "compile,provided"),
                newParameter("excludedArtifacts", "aCompositeSubsystem.esa"),
                newParameter("embeddableScopes", "compile"),
                newParameter("transitiveConsidered", "false"));
        Path esa0 = project.getBasedir().toPath()
                .resolve("target/subsystem.feature.unit.test-0.1.0.esa");
        assertThat(esa0.toFile()).as("ESA file was not created.").exists();

        ZipFile zip = new ZipFile(esa0.toFile());
        assertThat(zip.size()).isEqualTo(3);

        ZipEntry entry1 = zip.getEntry("aBundle-1.8.4.jar");
        assertThat(entry1).isNotNull();

        ZipEntry entry2 = zip.getEntry("OSGI-INF/SUBSYSTEM.MF");
        assertThat(entry2).isNotNull();
        zip.close();
    }

    @Test(expected = MojoExecutionException.class)
    public void testWrongPackagingFailure() throws Exception {
        File basedir = testResources
                .getBasedir("ut-project--fail-wrong-packaging");
        incrementalBuildRule.executeMojo(basedir, "generateSubsystemManifest");

    }
}
