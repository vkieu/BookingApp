package com;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.properties.EncryptableProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EncryptorUtilTest {
	private static final String theString = "SDHLKSHUWEHDKSLKLJKSALJDLKA00IUAY98273492JLKASJDLKASJDKLAJSD-02102018-1815";

	private StandardPBEStringEncryptor encryptor;

	@Before
	public void setUp() throws Exception {
		encryptor = EncryptorUtil.getEncryptor();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void getEncryptor() {
		assertNotNull(encryptor);
	}

	@Test
	public void encryptor() {
		String encryptedMessage = encryptor.encrypt(theString);
		String decryptedMessage = encryptor.decrypt(encryptedMessage);

		assertNotEquals(theString, encryptedMessage);
		assertNotEquals(encryptedMessage, decryptedMessage);
		assertEquals(theString, decryptedMessage);
	}

	@Test
	public void generateEncryptResources() throws Exception {
		File rawResourceFile = new File("src/main/resources/app.properties");
		assertTrue(rawResourceFile.exists());

		Properties properties = new Properties();
		properties.load(new FileReader(rawResourceFile));
		assertTrue(!properties.isEmpty());

		File outputFile = new File("src/main/resources/app.properties.enc");
		try (
				FileWriter fw = new FileWriter(outputFile, false);
		) {
			for (String name : properties.stringPropertyNames()) {
				String value = properties.getProperty(name);
				fw.write(name + "=ENC(" + encryptor.encrypt(value) + ")\n");
			}
		}
		//reload properties file just written
		Properties encryptorProperties = new EncryptableProperties(encryptor);
		encryptorProperties.load(new FileReader(outputFile));

		Properties properties2 = new Properties();
		properties2.load(new FileReader(outputFile));

		assertEquals(properties.get("7.password"), encryptorProperties.getProperty("7.password"));
		assertNotEquals(properties2.get("7.password"), properties.getProperty("7.password"));

	}

}