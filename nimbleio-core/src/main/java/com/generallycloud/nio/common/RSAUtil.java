package com.generallycloud.nio.common;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;

import com.generallycloud.nio.Encoding;

public class RSAUtil {

	private static KeyFactory	keyFactory;

	static {
		try {
			keyFactory = KeyFactory.getInstance("RSA");
		} catch (NoSuchAlgorithmException e) {
		}
	}

	/**
	 * 生成公钥和私钥
	 * 
	 * @param length 1024 ...
	 * @throws NoSuchAlgorithmException
	 *
	 */
	public static RSAKeys getKeys(int length) throws NoSuchAlgorithmException {

		KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
		
		keyPairGen.initialize(length);
		
		KeyPair keyPair = keyPairGen.generateKeyPair();
		
		RSAKeys keys = new RSAKeys();
		
		keys.publicKey = (RSAPublicKey) keyPair.getPublic();
		keys.privateKey = (RSAPrivateKey) keyPair.getPrivate();
		return keys;
	}

	/**
	 * 使用模和指数生成RSA公钥
	 * 注意：【此代码用了默认补位方式，为RSA/None/PKCS1Padding，不同JDK默认的补位方式可能不同，如Android默认是RSA
	 * /None/NoPadding】
	 * 
	 * @param modulus
	 *             模
	 * @param exponent
	 *             指数
	 * @return
	 */
	public static RSAPublicKey getPublicKey(String modulus, String exponent) {
		try {
			BigInteger b1 = new BigInteger(modulus);
			BigInteger b2 = new BigInteger(exponent);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			RSAPublicKeySpec keySpec = new RSAPublicKeySpec(b1, b2);
			return (RSAPublicKey) keyFactory.generatePublic(keySpec);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * 使用模和指数生成RSA私钥
	 * 注意：【此代码用了默认补位方式，为RSA/None/PKCS1Padding，不同JDK默认的补位方式可能不同，如Android默认是RSA
	 * /None/NoPadding】
	 * 
	 * @param modulus
	 *             模
	 * @param exponent
	 *             指数
	 * @return
	 */
	public static RSAPrivateKey getPrivateKey(String modulus, String exponent) {
		try {
			BigInteger b1 = new BigInteger(modulus);
			BigInteger b2 = new BigInteger(exponent);
			RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(b1, b2);
			return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * 公钥加密
	 * 
	 * @param data
	 * @param publicKey
	 * @return
	 * @throws Exception
	 */
	public static String encryptByPublicKey(byte [] data, RSAPublicKey publicKey) throws Exception {
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.ENCRYPT_MODE, publicKey);
		// 模长
		int key_len = publicKey.getModulus().bitLength() / 8;
		// 加密数据长度 <= 模长-11
		List<byte[]> datas = splitArray(data, key_len - 11);
		StringBuilder mi = new StringBuilder();
		// 如果明文长度大于模长-11则要分组加密
		for (byte[] s : datas) {
			mi.append(bcd2Str(cipher.doFinal(s)));
		}
		return mi.toString();
	}

	/**
	 * 私钥解密
	 * 
	 * @param data
	 * @param privateKey
	 * @return
	 * @throws Exception
	 */
	public static String decryptByPrivateKey(byte [] bytes, RSAPrivateKey privateKey) throws Exception {
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.DECRYPT_MODE, privateKey);
		// 模长
		int key_len = privateKey.getModulus().bitLength() / 8;
		byte[] bcd = ASCII_To_BCD(bytes, bytes.length);
		System.err.println(bcd.length);
		// 如果密文长度大于模长则要分组解密
		StringBuilder ming = new StringBuilder();
		List<byte[]> arrays = splitArray(bcd, key_len);
		for (byte[] arr : arrays) {
			ming.append(new String(cipher.doFinal(arr)));
		}
		return ming.toString();
	}

	/**
	 * ASCII码转BCD码
	 * 
	 */
	private static byte[] ASCII_To_BCD(byte[] ascii, int asc_len) {
		byte[] bcd = new byte[asc_len / 2];
		int j = 0;
		int len = (asc_len + 1) / 2;
		for (int i = 0; i < len; i++) {
			bcd[i] = asc_to_bcd(ascii[j++]);
			bcd[i] = (byte) (((j >= asc_len) ? 0x00 : asc_to_bcd(ascii[j++])) + (bcd[i] << 4));
		}
		return bcd;
	}

	private static byte asc_to_bcd(byte asc) {
		byte bcd;

		if ((asc >= '0') && (asc <= '9'))
			bcd = (byte) (asc - '0');
		else if ((asc >= 'A') && (asc <= 'F'))
			bcd = (byte) (asc - 'A' + 10);
		else if ((asc >= 'a') && (asc <= 'f'))
			bcd = (byte) (asc - 'a' + 10);
		else
			bcd = (byte) (asc - 48);
		return bcd;
	}

	/**
	 * BCD转字符串
	 */
	private static String bcd2Str(byte[] bytes) {
		char temp[] = new char[bytes.length * 2], val;

		for (int i = 0; i < bytes.length; i++) {
			val = (char) (((bytes[i] & 0xf0) >> 4) & 0x0f);
			temp[i * 2] = (char) (val > 9 ? val + 'A' - 10 : val + '0');

			val = (char) (bytes[i] & 0x0f);
			temp[i * 2 + 1] = (char) (val > 9 ? val + 'A' - 10 : val + '0');
		}
		return new String(temp);
	}

	/**
	 * 拆分字符串
	 */
	private static List<byte[]> splitArray(byte [] array, int len) {
		
		int length = array.length;

		List<byte []> list = new ArrayList<byte[]>();
		
		if (length <= len) {
			
			list.add(array);
			
			return list;
		}
		
		int size = length / len;
		
		for (int i = 0; i < size; i++) {
			byte [] a = new byte [len] ;
			System.arraycopy(array, i * len, a, 0, len);
			list.add(a);
		}
		

		int yu = array.length % len;
		
		if (yu > 0) {
			byte [] a = new byte [yu] ;
			System.arraycopy(array, length - yu, a, 0, yu);
			list.add(a);
		}
		
		return list;
	}

	public static class RSAKeys {

		private RSAPublicKey	publicKey;
		private RSAPrivateKey	privateKey;

		public RSAPublicKey getPublicKey() {
			return publicKey;
		}

		public RSAPrivateKey getPrivateKey() {
			return privateKey;
		}

		public void setPublicKey(RSAPublicKey publicKey) {
			this.publicKey = publicKey;
		}

		public void setPrivateKey(RSAPrivateKey privateKey) {
			this.privateKey = privateKey;
		}
	}
	
	public static void generateKeys(String file,int length) throws NoSuchAlgorithmException, IOException{
		RSAKeys keys = RSAUtil.getKeys(length);
		// 生成公钥和私钥
		RSAPublicKey publicKey = keys.getPublicKey();
		RSAPrivateKey privateKey = keys.getPrivateKey();
		
		File publicKeyFile = new File(file + "/public.rsa");
		String publicKeyString = publicKey.toString();
		
		File privateKeyFile = new File(file + "/private.rsa");
		String privateKeyString = privateKey.toString();
		
		FileUtil.write(publicKeyFile, publicKeyString);
		FileUtil.write(privateKeyFile,privateKeyString);
		
		System.out.println("Public RSA File:"+publicKeyFile.getCanonicalPath());
		System.out.println(publicKeyString);
		System.out.println();
		System.out.println("Private RSA File:"+privateKeyFile.getCanonicalPath());
		System.out.println(privateKeyString);
	}
	
	private static Map<String,String> parseRSAFromContent(String content){
		String [] lines = content.split("\n");
		Map<String,String> map = new HashMap<String, String>();
		for (int i = 1; i < lines.length; i++) {
			String [] array = lines[i].split(":");
			if (array.length != 2) {
				continue;
			}
			String name = array[0].trim().replace("\r", "");
			String value = array[1].trim().replace("\r", "");
			map.put(name, value);
		}
		return map;
	}
	
	public static RSAPublicKey getRsaPublicKey(String content){
		
		if (StringUtil.isNullOrBlank(content)) {
			throw new IllegalArgumentException("null content");
		}
		
		Map<String,String> map = parseRSAFromContent(content);
		
		String modulus = map.get("modulus");
		String exponent = map.get("public exponent");
		
		return getPublicKey(modulus, exponent);
	}
	
	public static RSAPrivateKey getRsaPrivateKey(String content){
		
		if (StringUtil.isNullOrBlank(content)) {
			throw new IllegalArgumentException("null content");
		}
		
		Map<String,String> map = parseRSAFromContent(content);
		
		String modulus = map.get("modulus");
		String exponent = map.get("private exponent");
		
		return getPrivateKey(modulus, exponent);
	}

	public static void main(String[] args) throws Exception {
		RSAKeys keys = RSAUtil.getKeys(1024);
		// 生成公钥和私钥
		RSAPublicKey publicKey = keys.getPublicKey();
		RSAPrivateKey privateKey = keys.getPrivateKey();

		// 模
		String modulus = publicKey.getModulus().toString();
		// 公钥指数
		String public_exponent = publicKey.getPublicExponent().toString();
		// 私钥指数
		String private_exponent = privateKey.getPrivateExponent().toString();
		// 明文
		String ming = "你好你好你好你好你好你好你好你好你好你好你好你好你好你好你好你好你好你好你好你好你好你好你好你好你好你好你好你好你好你好你好你好";
//		ming = "22ttt2adw";
		// 使用模和指数生成公钥和私钥
		RSAPublicKey pubKey = RSAUtil.getPublicKey(modulus, public_exponent);
		RSAPrivateKey priKey = RSAUtil.getPrivateKey(modulus, private_exponent);
		// 加密后的密文
		String mi = RSAUtil.encryptByPublicKey(ming.getBytes(Encoding.UTF8), pubKey);
		System.err.println("密文："+mi);
		// 解密后的明文
		ming = RSAUtil.decryptByPrivateKey(mi.getBytes(Encoding.UTF8), priKey);
		System.err.println("明文："+ming);
	}
}
