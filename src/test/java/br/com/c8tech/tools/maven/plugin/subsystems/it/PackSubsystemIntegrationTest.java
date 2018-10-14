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
package br.com.c8tech.tools.maven.plugin.subsystems.it;

import static org.assertj.core.api.Assertions.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.aries.subsystem.core.archive.SubsystemContentHeader.Clause;
import org.apache.aries.subsystem.core.archive.SubsystemManifest;
import org.junit.Test;

import io.takari.maven.testing.executor.MavenExecutionResult;
import io.takari.maven.testing.executor.MavenRuntime.MavenRuntimeBuilder;

public class PackSubsystemIntegrationTest extends AbstractIntegrationTest {

    public PackSubsystemIntegrationTest(MavenRuntimeBuilder builder)
            throws Exception {
        super(builder);
    }

    @Test
    public void testFailureWithWrongPackage() throws Exception {
        File basedir = resources.getBasedir("it-project--fail-wrong-packaging");

        MavenExecutionResult result = mavenRuntime.forProject(basedir)
                .execute("clean", "package");
        result.assertLogText("[ERROR] Failed to execute goal");
        result.assertLogText(String.format(
                "The project '%s' has a packaging not allowed by this plugin. Allowed packagings are '%s'.",
                "br.com.c8tech.tools:test-subsystem-generation-default:jar:0.1.0",
                "[osgi.subsystem.composite, osgi.subsystem.application, osgi.subsystem.feature]"));
    }
    @Test
    public void testFailurePackEsaWhenNoManifestConfigIsFound()
            throws Exception {
        File basedir = resources
                .getBasedir("it-project--subsystem-no-manifest-to-pack");

        MavenExecutionResult result = mavenRuntime.forProject(basedir)
                .execute("package");
        result.assertLogText(
                "A subsystem manifest file was not found for the project test-subsystem-generation-default");
    }

    @Test
    public void testPackingSubsystemApplication() throws Exception {
        File basedir = resources
                .getBasedir("it-project--subsystem-application");

        MavenExecutionResult result = mavenRuntime.forProject(basedir)
                .execute("package");
        result.assertErrorFreeLog();
        result.assertLogText(
                "Setting up download and caching of artifacts for project test-subsystem-application-default");
        result.assertLogText(
                "Finished copying of 1 artifact from maven repositories");
        result.assertLogText(
                "Setting up generation of the Subsystem manifest file for project");
        result.assertLogText(
                "Starting generation of the Subsystem manifest file for project");
        result.assertLogText(
                "OSGi Subsystem manifest file was successfully generated at");
        result.assertLogText(
                "Setting up generation of the Subsystem archive for project");
        result.assertLogText(
                "Starting to pack the items of the OSGi Subsystem archive for project");
        result.assertLogText(
                "OSGi Subsystem archive was successfully generated at");

        File workdir = new File(basedir, "target/work/esa");
        assertThat(new File(workdir, "OSGI-INF/SUBSYSTEM.MF").canRead())
                .isEqualTo(true);

        File esaFile = new File(basedir,
                "target/test-subsystem-application-default-0.1.0.esa");
        assertThat(esaFile.canRead()).isEqualTo(true);
        try (JarFile esajar = new JarFile(esaFile);
                InputStream in = new BufferedInputStream(esajar.getInputStream(
                        esajar.getEntry("OSGI-INF/SUBSYSTEM.MF")));) {
            assertThat(esajar).isNotNull();
            assertThat(in).isNotNull();
            assertThat(esajar.size()).isEqualTo(3);
            SubsystemManifest sm = new SubsystemManifest(in);
            assertThat(sm.getSubsystemTypeHeader().getType())
                    .isEqualTo("osgi.subsystem.application");
        }
    }

    @Test
    public void testPackingSubsystemApplicationWithEmbeddedResources()
            throws Exception {
        File basedir = resources.getBasedir(
                "it-project--subsystem-application-with-embed-resources");

        MavenExecutionResult result = mavenRuntime.forProject(basedir)
                .execute("package");
        result.assertErrorFreeLog();
        result.assertLogText("Manifest parameter was not informed, using default values.");
        result.assertLogText("Copying 2 resources");
        result.assertLogText(
                "Setting up download and caching of artifacts for project");
        result.assertLogText(
                "Finished copying of 1 artifact from maven repositories");
        result.assertLogText(
                "Setting up generation of the Subsystem manifest file for project");
        result.assertLogText(
                "Starting generation of the Subsystem manifest file for project");
        result.assertLogText(
                "OSGi Subsystem manifest file was successfully generated at");
        result.assertLogText(
                "Setting up generation of the Subsystem archive for project");
        result.assertLogText(
                "Starting to pack the items of the OSGi Subsystem archive for project");
        result.assertLogText(
                "OSGi Subsystem archive was successfully generated at");

        File workdir = new File(basedir, "target/work/esa");
        assertThat(new File(workdir, "OSGI-INF/SUBSYSTEM.MF").canRead())
                .isEqualTo(true);

        File esaFile = new File(basedir,
                "target/test-subsystem-application-with-embedded-resources-0.1.0.esa");
        assertThat(esaFile.canRead()).isEqualTo(true);
        try (JarFile esajar = new JarFile(esaFile);
                InputStream in = new BufferedInputStream(esajar.getInputStream(
                        esajar.getEntry("OSGI-INF/SUBSYSTEM.MF")));) {
            assertThat(esajar).isNotNull();
            assertThat(in).isNotNull();
            assertThat(esajar.size()).isEqualTo(5);
            ZipEntry embed1 = esajar.getEntry("files/resource1.txt");
            assertThat(embed1).isNotNull();
            ZipEntry embed2 = esajar.getEntry("files/resource2.txt");
            assertThat(embed2).isNotNull();
            ZipEntry embed3 = esajar.getEntry("slf4j-api-1.7.25.jar");
            assertThat(embed3).isNotNull();
            ZipEntry excluded = esajar.getEntry("package.html");
            assertThat(excluded).isNull();
            SubsystemManifest sm = new SubsystemManifest(in);
            assertThat(sm.getSubsystemTypeHeader().getType())
                    .isEqualTo("osgi.subsystem.application");
        }

    }

    @Test
    public void testPackingSubsystemComposite() throws Exception {
        File basedir = resources.getBasedir("it-project--subsystem-composite");

        MavenExecutionResult result = mavenRuntime.forProject(basedir)
                .execute("package");
        result.assertErrorFreeLog();
        result.assertLogText("skip non existing resourceDirectory");
        result.assertLogText(
                "Setting up download and caching of artifacts for project");
        result.assertLogText(
                "Finished copying of 1 artifact from maven repositories");
        result.assertLogText(
                "Setting up generation of the Subsystem manifest file for project");
        result.assertLogText(
                "Starting generation of the Subsystem manifest file for project");
        result.assertLogText(
                "OSGi Subsystem manifest file was successfully generated at");
        result.assertLogText(
                "Setting up generation of the Subsystem archive for project");
        result.assertLogText(
                "Starting to pack the items of the OSGi Subsystem archive for project");
        result.assertLogText(
                "OSGi Subsystem archive was successfully generated at");

        File workdir = new File(basedir, "target/work/esa");
        assertThat(new File(workdir, "OSGI-INF/SUBSYSTEM.MF").canRead())
                .isEqualTo(true);

        File esaFile = new File(basedir,
                "target/test-subsystem-composite-default-0.1.0.esa");
        assertThat(esaFile.canRead()).isEqualTo(true);

        try (JarFile esajar = new JarFile(esaFile);
                InputStream in = new BufferedInputStream(esajar.getInputStream(
                        esajar.getEntry("OSGI-INF/SUBSYSTEM.MF")));) {
            assertThat(esajar).isNotNull();
            assertThat(esajar.size()).isEqualTo(3);
            assertThat(in).isNotNull();
            SubsystemManifest sm = new SubsystemManifest(in);
            assertThat(sm.getSubsystemTypeHeader().getType())
                    .isEqualTo("osgi.subsystem.composite");
        }
    }

    @Test
    public void testPackingSubsystemCompositeWithEmbeddedContents()
            throws Exception {
        File basedir = resources.getBasedir(
                "it-project--subsystem-composite-with-embed-contents");

        MavenExecutionResult result = mavenRuntime.forProject(basedir)
                .execute("package");
        result.assertErrorFreeLog();
        result.assertLogText("skip non existing resourceDirectory");
        result.assertLogText(
                "Setting up download and caching of artifacts for project");
        result.assertLogText(
                "Finished copying of 2 artifacts from maven repositories");
        result.assertLogText(
                "Setting up generation of the Subsystem manifest file for project");
        result.assertLogText(
                "Starting generation of the Subsystem manifest file for project");
        result.assertLogText(
                "OSGi Subsystem manifest file was successfully generated at");
        result.assertLogText(
                "Setting up generation of the Subsystem archive for project");
        result.assertLogText(
                "Starting to pack the items of the OSGi Subsystem archive for project");
        result.assertLogText(
                "OSGi Subsystem archive was successfully generated at");

        File workdir = new File(basedir, "target/work/esa");
        assertThat(workdir.list().length).isEqualTo(1);
        assertThat(new File(workdir, "OSGI-INF/SUBSYSTEM.MF").canRead())
                .isEqualTo(true);

        File esa = new File(basedir,
                "target/test-subsystem-composite-with-embedded-contents-0.1.0.esa");
        assertThat(esa.canRead()).isEqualTo(true);
        try (JarFile esajar = new JarFile(esa);
                InputStream in = new BufferedInputStream(esajar.getInputStream(
                        esajar.getEntry("OSGI-INF/SUBSYSTEM.MF")));) {
            assertThat(esajar).isNotNull();
            assertThat(esajar.size()).isEqualTo(3);
            assertThat(in).isNotNull();
            ZipEntry embed1 = esajar.getEntry("slf4j-api-1.7.25.jar");
            assertThat(embed1).isNotNull();
            SubsystemManifest sm = new SubsystemManifest(in);
            assertThat(sm.getSubsystemTypeHeader().getType())
                    .isEqualTo("osgi.subsystem.composite");
            assertThat(sm.getSubsystemContentHeader().getClauses().size())
                    .isEqualTo(3);

            List<Clause> list = new ArrayList<>(
                    sm.getSubsystemContentHeader().getClauses());
            Collections.sort(list, new Comparator<Clause>() {

                @Override
                public int compare(Clause o1, Clause o2) {
                    return o1.getSymbolicName().compareTo(o2.getSymbolicName());
                }
            });
            assertThat(list.toString()).isEqualTo("[ch.qos.logback.classic;type=osgi.bundle;version=\"[1.2.1,1.2.1]\";resolution:=optional, "
                    + "com.c8tech.runtime.kernel.lib;version=\"[0.1.1.20170424014114,0.1.1.20170424014114]\";start-order:=0;resolution:=mandatory;type=osgi.bundle, "
                    + "slf4j.api;type=osgi.bundle;version=\"[1.7.25,1.7.25]\";resolution:=mandatory]");
        }
    }

    @Test
    public void testPackingSubsystemFeature() throws Exception {
        File basedir = resources.getBasedir("it-project--subsystem-feature");

        MavenExecutionResult result = mavenRuntime.forProject(basedir)
                .execute("package");
        result.assertErrorFreeLog();
        result.assertLogText(
                "Setting up download and caching of artifacts for project");
        result.assertLogText(
                "Finished copying of 1 artifact from maven repositories");
        result.assertLogText(
                "Setting up generation of the Subsystem manifest file for project");
        result.assertLogText(
                "Starting generation of the Subsystem manifest file for project");
        result.assertLogText(
                "OSGi Subsystem manifest file was successfully generated at");
        result.assertLogText(
                "Setting up generation of the Subsystem archive for project");
        result.assertLogText(
                "Starting to pack the items of the OSGi Subsystem archive for project");
        result.assertLogText(
                "OSGi Subsystem archive was successfully generated at");

        File workdir = new File(basedir, "target/work/esa");
        assertThat(new File(workdir, "OSGI-INF/SUBSYSTEM.MF").canRead())
                .isEqualTo(true);
        File esaFile = new File(basedir,
                "target/test-subsystem-feature-default-0.1.0.esa");
        assertThat(esaFile.canRead()).isEqualTo(true);
        try (JarFile esajar = new JarFile(esaFile);
                InputStream in = new BufferedInputStream(esajar.getInputStream(
                        esajar.getEntry("OSGI-INF/SUBSYSTEM.MF")));) {
            assertThat(esajar).isNotNull();
            assertThat(esajar.size()).isEqualTo(3);
            assertThat(in).isNotNull();
            SubsystemManifest sm = new SubsystemManifest(in);
            assertThat(sm.getSubsystemTypeHeader().getType())
                    .isEqualTo("osgi.subsystem.feature");
        }
    }

    /**
     * The plugin won't consider the dependencies. It will use only the plugin
     * parameter to build the manifest.
     *
     * @throws Exception
     */
    @Test
    public void testPackingSubsystemFeatureWithFixedContentHeader()
            throws Exception {
        File basedir = resources.getBasedir(
                "it-project--subsystem-feature-fixed-content-header");

        MavenExecutionResult result = mavenRuntime.forProject(basedir)
                .execute("package");
        result.assertErrorFreeLog();
        result.assertLogText("skip non existing resourceDirectory");
        result.assertLogText(
                "Setting up download and caching of artifacts for project");
        result.assertLogText(
                "Skipping downloading artifacts from maven repositories");
        result.assertLogText(
                "Setting up generation of the Subsystem manifest file for project");
        result.assertLogText(
                "The parameter manifestScopes was not declared. Building subsystem manifest based on the subsystemContent parameter only.");
        result.assertLogText(
                "OSGi Subsystem manifest file was successfully generated from fixed content header parameter at");
        result.assertLogText(
                "Setting up generation of the Subsystem archive for project");
        result.assertLogText(
                "Starting to pack the items of the OSGi Subsystem archive for project");
        result.assertLogText(
                "OSGi Subsystem archive was successfully generated at");

        File workdir = new File(basedir, "target/work/esa");
        assertThat(new File(workdir, "OSGI-INF/SUBSYSTEM.MF").canRead())
                .isEqualTo(true);
        File esaFile = new File(basedir,
                "target/test-subsystem-feature-fixed-content-header-0.1.0.esa");
        assertThat(esaFile.canRead()).isEqualTo(true);
        try (JarFile esajar = new JarFile(esaFile);
                InputStream in = new BufferedInputStream(esajar.getInputStream(
                        esajar.getEntry("OSGI-INF/SUBSYSTEM.MF")));) {
            assertThat(esajar).isNotNull();
            assertThat(esajar.size()).isEqualTo(2);
            assertThat(in).isNotNull();
            SubsystemManifest sm = new SubsystemManifest(in);
            assertThat(sm.getSubsystemTypeHeader().getType())
                    .isEqualTo("osgi.subsystem.feature");
            assertThat(sm.getSubsystemContentHeader().getClauses().size())
                    .isEqualTo(1);
            Clause content = sm.getSubsystemContentHeader().getClauses()
                    .iterator().next();
            assertThat(content.toString()).isEqualTo(
                    "org.ops4j.pax.url.mvn;version=\"[1.0.0,1.0.0]\";start-order:=5;resolution:=optional;type=osgi.bundle");
        }
    }

    @Test
    public void testPackingSubsystemFeatureWithMixedContentHeader()
            throws Exception {
        File basedir = resources.getBasedir(
                "it-project--subsystem-feature-mixed-content-header");

        MavenExecutionResult result = mavenRuntime.forProject(basedir)
                .execute("package");
        result.assertErrorFreeLog();
        result.assertLogText("skip non existing resourceDirectory");
        result.assertLogText(
                "Setting up download and caching of artifacts for project");
        result.assertLogText(
                "Finished copying of 1 artifact from maven repositories");
        result.assertLogText(
                "Setting up generation of the Subsystem manifest file for project");
        result.assertLogText(
                "Starting generation of the Subsystem manifest file for project");
        result.assertLogText(
                "OSGi Subsystem manifest file was successfully generated at");
        result.assertLogText(
                "Setting up generation of the Subsystem archive for project");
        result.assertLogText(
                "Starting to pack the items of the OSGi Subsystem archive for project");
        result.assertLogText(
                "OSGi Subsystem archive was successfully generated at");

        File workdir = new File(basedir, "target/work/esa");
        assertThat(new File(workdir, "OSGI-INF/SUBSYSTEM.MF").canRead())
                .isEqualTo(true);
        File esaFile = new File(basedir,
                "target/test-subsystem-feature-mixed-content-header-0.1.0.esa");
        assertThat(esaFile.canRead()).isEqualTo(true);
        try (JarFile esajar = new JarFile(esaFile);
                InputStream in = new BufferedInputStream(esajar.getInputStream(
                        esajar.getEntry("OSGI-INF/SUBSYSTEM.MF")));) {
            assertThat(esajar).isNotNull();
            assertThat(esajar.size()).isEqualTo(3);
            assertThat(in).isNotNull();
            SubsystemManifest sm = new SubsystemManifest(in);
            assertThat(sm.getSubsystemTypeHeader().getType())
                    .isEqualTo("osgi.subsystem.feature");
            assertThat(sm.getSubsystemContentHeader().getClauses().size())
                    .isEqualTo(1);
            Clause content = sm.getSubsystemContentHeader().getClauses()
                    .iterator().next();
            assertThat(content.getSymbolicName()).isEqualTo("slf4j.api");
            assertThat(content.getVersionRange().toString())
                    .isEqualTo("[1.7.25,1.7.25]");
            assertThat(content.getStartOrder()).isEqualTo(5);
            assertThat(content.getType()).isEqualTo("osgi.bundle");
            assertThat(content.isMandatory()).isEqualTo(true);

        }
    }

    @Test
    public void testSkippingGeneration() throws Exception {
        File basedir = resources.getBasedir("it-project--skip");

        MavenExecutionResult result = mavenRuntime.forProject(basedir)
                .execute("package");
        result.assertErrorFreeLog();
        result.assertLogText(
                "Skipping goal downloadAndCacheArtifacts for project br.com.c8tech.tools:test-subsystem-generation-default:osgi.subsystem.composite");
        result.assertLogText(
                "Skipping goal generateSubsystemManifest for project br.com.c8tech.tools:test-subsystem-generation-default:osgi.subsystem.composite");
        result.assertLogText(
                "Skipping goal packESA for project br.com.c8tech.tools:test-subsystem-generation-default:osgi.subsystem.composite");
    }
}
