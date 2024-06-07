/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package package_manager;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 *
 * @author hexaredecimal
 */
public class Cryptic {

	public static String encrypt(String text) {
		try {
			// SecretKey
			KeyGenerator keygenerator
							= KeyGenerator.getInstance("DES");
			SecretKey myDesKey = keygenerator.generateKey();

			// Creating object of Cipher
			Cipher desCipher;
			desCipher = Cipher.getInstance("DES");
			// Creating byte array to store string
			byte[] _text
							= text.getBytes("UTF8");

			// Encrypting text
			desCipher.init(Cipher.ENCRYPT_MODE, myDesKey);
			byte[] textEncrypted = desCipher.doFinal(_text);
			return new String(textEncrypted, StandardCharsets.UTF_8);
		} catch (Exception ex) {
			System.out.println("Error: Invalid encryption");
			System.exit(101);
		}
			return null;
	}
}
