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
package br.com.c8tech.tools.maven.plugin.subsystems.ut;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Assert;

import org.junit.Rule;

import br.com.c8tech.tools.maven.osgi.lib.mojo.CommonMojoConstants;
import br.com.c8tech.tools.maven.plugin.subsystems.PojoSubsystemManifestConfiguration.ProvisionPolicy;
import io.takari.incrementalbuild.maven.testing.IncrementalBuildRule;
import io.takari.maven.testing.TestProperties;
import io.takari.maven.testing.TestResources;

public abstract class AbstractSubsystemUnitTest {

    public static class Header {
        String key;
        String value;

        private Header() {

        }

        private Header(String pName, String pValue) {
            key = pName;
            value = pValue;
        }

        public static Header newHeader(String pName, String pValue) {
            return new Header(pName, pValue);
        }

        public Xpp3Dom build() {
            Xpp3Dom child = new Xpp3Dom(key);
            child.setValue(value);
            return child;
        }

    }

    public static class Manifest {
        private final List<Header> headers = new ArrayList<>();

        private Manifest() {

        }

        public Manifest addHeader(Header pHeader) {
            headers.add(pHeader);
            return this;
        }

        public Manifest addHeader(String name, String value) {
            headers.add(Header.newHeader(name, value));
            return this;
        }

        public Manifest provisioninPolicy(ProvisionPolicy provisionPolicy) {
            headers.add(Header.newHeader("provisionPolicy",
                    provisionPolicy != null ? provisionPolicy.getName()
                            : null));
            return this;
        }

        public Manifest subsystemContent(String value) {
            headers.add(Header.newHeader("Subsystem-Content", value));
            return this;
        }

        public Manifest preferredProvider(String value) {
            headers.add(Header.newHeader("Preferred-Provider", value));
            return this;
        }
        
        public Manifest subsystemContent(String... values) {
            StringBuilder contentBuilder = new StringBuilder();
            if (values != null) {
                for (int i = 0; i < values.length; i++) {
                    contentBuilder.append(values[i]);
                    if (i < values.length - 1) {
                        contentBuilder.append(",");
                    }
                }
            }
            headers.add(
                    Header.newHeader("Subsystem-Content", contentBuilder.toString()));
            return this;
        }

        public Manifest exportService(String value) {
            headers.add(Header.newHeader("Subsystem-ExportService", value));
            return this;
        }

        public Manifest requireBundle(String value) {
            headers.add(Header.newHeader("Require-Bundle", value));
            return this;
        }

        public Manifest importService(String value) {
            headers.add(Header.newHeader("Subsystem-ImportService", value));
            return this;
        }

        public Manifest exportPackage(String value) {
            headers.add(Header.newHeader("Export-Package", value));
            return this;
        }

        public Manifest importPackage(String value) {
            headers.add(Header.newHeader("Import-Package", value));
            return this;
        }

        public Xpp3Dom build() {
            final Xpp3Dom manifest = new Xpp3Dom("manifest");
            for (Header header : headers) {
                manifest.addChild(header.build());
            }
            return manifest;
        }

        public List<Header> getHeaders() {
            return headers;
        }

        public static Manifest newManifest() {
            return new Manifest();
        }

        public static Manifest newManifest(Collection<Header> pHeaders) {
            Manifest manifest = new Manifest();
            pHeaders.forEach(t -> manifest.addHeader(t));
            return manifest;
        }

        public static Manifest newManifest(Manifest pBaseManifest) {
            Manifest manifest = new Manifest();
            pBaseManifest.getHeaders().forEach(t -> manifest.addHeader(t));
            return manifest;
        }
    }

    public class TestExecution {
    }

    @Rule
    public final IncrementalBuildRule incrementalBuildRule = new IncrementalBuildRule();

    protected final TestProperties testProperties = new TestProperties();

    @Rule
    public final TestResources testResources = new TestResources();

    public final void addDependency(MavenProject project, String artifactName,
            boolean direct, String scope, String type, boolean optional)
            throws Exception {
        URL fileUrl = testProperties.getClass()
                .getResource(!artifactName.startsWith("/") ? "/" + artifactName
                        : artifactName);
        if (fileUrl == null) {
            throw new IllegalArgumentException(
                    "A dependency file was not found at:" + artifactName);
        }
        File file = new File(fileUrl.getFile());
        String id = file.getName().substring(0, file.getName().indexOf('.'));
        incrementalBuildRule.newDependency(new File(fileUrl.getFile()))
                .setArtifactId(id).setType(type).setScope(scope)
                .setOptional(optional).addTo(project, direct);
    }

    public final void addDependencyDir(MavenProject project,
            String pArtifactName, String pVersion, boolean pDirect,
            String pScope, String pType, boolean pOptional) throws Exception {
        addDependencyDir(project, "test", pArtifactName, pArtifactName, pDirect,
                pScope, pType, pVersion, pOptional);
    }

    public final void addDependencyDir(MavenProject project, String pGroupId,
            String pArtifactId, String pArtifactDirName, boolean pDirect,
            String pScope, String pVersion, String pType, boolean pOptional)
            throws Exception {

        URL file = testProperties.getClass()
                .getResource(!pArtifactDirName.startsWith("/")
                        ? "/" + pArtifactDirName
                        : pArtifactDirName);
        if (file == null) {
            throw new IllegalArgumentException(
                    "A dependency directory was not found with this name '"
                            + pArtifactDirName + "'");
        }
        File dir = new File(file.getFile());
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException(
                    "The specified directory '" + pArtifactDirName
                            + "' do not points to a dependency directory.");
        }
        incrementalBuildRule
                .newDependency(new File(file.getFile(),
                        CommonMojoConstants.MAVEN_TARGET_CLASSES_FOLDER))
                .setType(pType).setScope(pScope).setOptional(pOptional)
                .setVersion(pVersion).setGroupId(pGroupId)
                .setArtifactId(pArtifactId).addTo(project, pDirect);
    }

    public Path assertAndGetBuildOutput(MavenProject project,
            String outputPath) {
        Path fileOutput = Paths.get(project.getBasedir().getPath(), outputPath);
        Assert.assertTrue("File was not found at " + outputPath,
                fileOutput.toFile().exists() && fileOutput.toFile().isFile()
                        && fileOutput.toFile().canRead());
        return fileOutput;
    }

    protected Manifest newManifest() {
        return Manifest.newManifest();
    }

    protected Manifest newManifest(Manifest pManifestToMerge) {
        return Manifest.newManifest(pManifestToMerge);
    }
    
    protected Manifest newManifest(Collection<Header> pHeaders) {
        return Manifest.newManifest(pHeaders);
    }
    
    protected Xpp3Dom newChild(String name, String value) {
        Xpp3Dom child = new Xpp3Dom(name);
        child.setValue(value);
        return child;
    }

    protected Xpp3Dom newParameterMavenArtifactConfigSet(String... artifacts) {
        final Xpp3Dom bundleSet = new Xpp3Dom("mavenArtifactSet");
        for (String artifact : artifacts) {
            bundleSet.addChild(newChild("artifact", artifact));
        }
        return bundleSet;
    }
}
