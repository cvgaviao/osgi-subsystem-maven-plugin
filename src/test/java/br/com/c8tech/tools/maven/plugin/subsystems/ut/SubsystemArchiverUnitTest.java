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

import static org.assertj.core.api.Assertions.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.CRC32;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import br.com.c8tech.tools.maven.osgi.lib.mojo.CommonMojoConstants;
import br.com.c8tech.tools.maven.osgi.lib.mojo.archivers.SubsystemCompositeArchiver;

public class SubsystemArchiverUnitTest {

	public static class TestClass extends SubsystemCompositeArchiver {
		public String getType() {
			return getArchiveType();
		}
	}

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private File properties;

	public SubsystemArchiverUnitTest() {
	}

	@Test
	public void testArchiveType() {
		TestClass esaArchiver = new TestClass();
		assertThat(esaArchiver.getType()).isEqualTo("esa");
	}

	@Test
	public void testArchiveIsgenerated() throws Exception {
		SubsystemCompositeArchiver esaArchiver = new SubsystemCompositeArchiver();

        Path manifest = Paths.get(getClass().getResource(
                "/subsystems/extracted.composite.esa/target/esa/OSGI-INF/SUBSYSTEM.MF")
                .toURI());

		
		File esa = Paths.get(folder.getRoot().getPath(), "archive.esa").toFile();
		properties = folder.newFile("messages.properties");
		BufferedWriter infile = new BufferedWriter(new FileWriter(properties));
		infile.write("first.name = Cristiano\n");
		infile.write("last.name = Gavião\n");
		infile.close();

		esaArchiver.setManifest(manifest.toFile());
		esaArchiver.setGenerateEsaMimeEntry(true);
		esaArchiver.setIncludeEmptyDirs(false);
		esaArchiver.setDestFile(esa);
		esaArchiver.addFile(properties, properties.getName());
		esaArchiver.createArchive();

		assertThat(esa.canRead()).isTrue();

		ZipFile zipFile = new ZipFile(esa);
		ZipArchiveEntry inputMime = zipFile.getEntry(CommonMojoConstants.MIME_TYPE_ENTRY_NAME);
		assertThat(inputMime).isNotNull();
		assertThat(inputMime.getName()).isEqualTo(CommonMojoConstants.MIME_TYPE_ENTRY_NAME);
		assertThat(inputMime.getCrc()).isEqualTo(calculateCRC());
		ZipArchiveEntry otherFile = zipFile.getEntry("messages.properties");
		assertThat(otherFile).isNotNull();
		zipFile.close();
	}

	private static long calculateCRC() throws UnsupportedEncodingException {
		byte[] mimetypeBytes = CommonMojoConstants.OSGI_SUBSYSTEM_MIME_TYPE.getBytes("UTF-8");
		CRC32 crc = new CRC32();
		crc.update(mimetypeBytes);

		return crc.getValue();
	}

}
