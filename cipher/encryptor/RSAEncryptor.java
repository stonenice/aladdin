
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by stonenice on 2017/7/13.
 */
@Component("RSA")
public class RSAEncryptor implements Encryptor {

    public static final int MAX_ENCRYPT_BLOCK = 117;
    public static final int MAX_DECRYPT_BLOCK = 128;
    public final static int KEYSIZE = 1024;
    public static final String RSA_KEY_ALGORITHM = "RSA";
    public final static String RSA_ALGORITHM = "RSA/ECB/PKCS1Padding";
    public final static String DEFUALT_CHARSET = "utf-8";
    private String charset;

    public static PublicKey getPublicKey(String key) throws Exception {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.decodeBase64(key.getBytes()));
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_KEY_ALGORITHM);
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        return publicKey;
    }

    public static PrivateKey getPrivateKey(String key) throws Exception {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.decodeBase64(key.getBytes()));
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_KEY_ALGORITHM);
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
        return privateKey;
    }

    @Override
    public Map<String, String> keygen() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("charset", DEFUALT_CHARSET);
        return keygen(map);
    }

    @Override
    public Map<String, String> keygen(Map<String, String> initVars) {
        try {
            String charset;
            if (initVars != null && initVars.containsKey("charset")) {
                charset = initVars.get("charset");
            } else {
                charset = DEFUALT_CHARSET;
            }
            SecureRandom sr = new SecureRandom();
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(RSA_KEY_ALGORITHM);

            kpg.initialize(KEYSIZE, sr);

            KeyPair kp = kpg.generateKeyPair();

            Key publicKey = kp.getPublic();
            byte[] publicKeyBytes = publicKey.getEncoded();

            String pub = new String(Base64.encodeBase64(publicKeyBytes), charset);

            Key privateKey = kp.getPrivate();
            byte[] privateKeyBytes = privateKey.getEncoded();
            String pri = new String(Base64.encodeBase64(privateKeyBytes), charset);

            Map<String, String> map = new HashMap<String, String>();
            map.put("publicKey", pub);
            map.put("privateKey", pri);
            RSAPublicKey rsp = (RSAPublicKey) kp.getPublic();
            BigInteger bint = rsp.getModulus();
            byte[] b = bint.toByteArray();
            byte[] deBase64Value = Base64.encodeBase64(b);
            String retValue = new String(deBase64Value);
            map.put("modulus", retValue);
            return map;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void setCharset(String charset) {
        this.charset = charset;
    }

    @Override
    public String getCharset() {
        return StringUtils.isNotBlank(charset) ? charset : DEFUALT_CHARSET;
    }

    @Override
    public String encode(String plainText, String key) {
        if (StringUtils.isBlank(plainText) || StringUtils.isBlank(key)) {
            return plainText;
        }
        try {
            byte[] data = plainText.getBytes(getCharset());
            byte[] keyBytes = Base64.decodeBase64(key);
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_KEY_ALGORITHM);
            Key publicK = keyFactory.generatePublic(x509KeySpec);
            // 对数据加密
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, publicK);
            int inputLen = data.length;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int offSet = 0;
            byte[] cache;
            int i = 0;
            // 对数据分段加密
            while (inputLen - offSet > 0) {
                if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
                    cache = cipher.doFinal(data, offSet, MAX_ENCRYPT_BLOCK);
                } else {
                    cache = cipher.doFinal(data, offSet, inputLen - offSet);
                }
                out.write(cache, 0, cache.length);
                i++;
                offSet = i * MAX_ENCRYPT_BLOCK;
            }
            byte[] encryptedData = out.toByteArray();
            out.close();
            return Base64.encodeBase64String(encryptedData);
        } catch (Exception e) {
            return plainText;
        }
    }

    @Override
    public String encode(String plainText, String key, String iv) {
        return encode(plainText, key);
    }

    @Override
    public String decode(String cipherText, String key) {
        if (StringUtils.isBlank(cipherText) || StringUtils.isBlank(key)) {
            return cipherText;
        }
        try {
            byte[] encryptedData = Base64.decodeBase64(cipherText);
            byte[] keyBytes = Base64.decodeBase64(key);
            PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_KEY_ALGORITHM);
            Key privateK = keyFactory.generatePrivate(pkcs8KeySpec);
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, privateK);
            int inputLen = encryptedData.length;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int offSet = 0;
            byte[] cache;
            int i = 0;

            while (inputLen - offSet > 0) {
                if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
                    cache = cipher.doFinal(encryptedData, offSet, MAX_DECRYPT_BLOCK);
                } else {
                    cache = cipher.doFinal(encryptedData, offSet, inputLen - offSet);
                }
                out.write(cache, 0, cache.length);
                i++;
                offSet = i * MAX_DECRYPT_BLOCK;
            }
            byte[] decryptedData = out.toByteArray();
            out.close();
            return new String(decryptedData, getCharset());
        } catch (Exception e) {
            return cipherText;
        }
    }

    @Override
    public String decode(String cipherText, String key, String iv) {
        return decode(cipherText, key);
    }
}
