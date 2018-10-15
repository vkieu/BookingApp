package com;

import java.util.Properties;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.properties.EncryptableProperties;

public class EncryptorUtil {

	private static final String PWD = "SDHLKSHUWEHDKSLKLJKSALJDLKA00IUAY98273492JLKASJDLKASJDKLAJSD-02102018-1815";

	public static StandardPBEStringEncryptor getEncryptor() {
		StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
		encryptor.setAlgorithm("PBEWithMD5AndDES");
		encryptor.setPassword(PWD);
		return encryptor;
	}

	public static Properties getPropertiesEncryptor() {
		return new EncryptableProperties(getEncryptor());
	}

	public static void main(String[] args) {
		String theString = PWD;
		StandardPBEStringEncryptor encryptor = getEncryptor();

		String encryptedMessage = encryptor.encrypt(theString);
		String decryptedMessage = encryptor.decrypt(encryptedMessage);

		//System.out.println("encryptedMessage? " + encryptedMessage);
		//System.out.println("decryptedMessage? " + decryptedMessage);
		assertNotEquals(PWD, encryptedMessage);
		assertNotEquals(encryptedMessage, decryptedMessage);
		assertEquals(PWD, decryptedMessage);

		AppProperties ins = AppProperties.getInstance();

		for (String name : ins.getProperties().stringPropertyNames()) {
			String value = ins.getProperty(name);
			System.out.println(name + "=" + value);
			System.out.println(name + "=ENC(" + encryptor.encrypt(value) + ")");
		}
	}

	public static void assertEquals(Object expected, Object observed) {
		if ((expected == null && observed == null)
				|| (expected != null && expected.equals(expected))) {
			return;
		}
		throw new RuntimeException("Expecting " + expected + " but got " + observed);
	}

	public static void assertNotEquals(Object expected, Object observed) {
		if (expected != null && observed == null) {
			return;
		}
		if (expected == null && observed != null) {
			return;
		}
		if (!expected.equals(observed)) {
			return;
		}
		throw new RuntimeException("Expecting " + expected + " but got " + observed);
	}

}
