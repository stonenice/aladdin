
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by stonenice on 2017/7/12.
 *
 */
@Component("AES")
public class AESEncryptor implements Encryptor {
    public static final String AES_IV = "tuofenguaccatsdk";
    public static final String AES_KEY_ALGORITHM = "AES";
    public final static String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
    public static final int KEYSIZE = 256;

    private String charset = "utf-8";

    @Override
    public Map<String, String> keygen() {
        return keygen(null);
    }

    private IvParameterSpec getIV(String iv) {
        iv = StringUtils.isNotBlank(iv) ? iv : AES_IV;
        byte[] ivBytes = iv.getBytes();
        int len = ivBytes.length;

        //16bit
        byte[] bytes = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        for (int i = 0; i < len && i < 8; ++i) {
            bytes[i] = ivBytes[i];
            bytes[16 - i - 1] = ivBytes[len - i - 1];
        }
        IvParameterSpec ivSpec = new IvParameterSpec(bytes);
        return ivSpec;
    }

    @Override
    public Map<String, String> keygen(Map<String, String> initVars) {
        try {
            String seed;
            if (initVars != null && initVars.containsKey("seed")) {
                seed = initVars.get("seed");
            } else {
                seed = String.valueOf(System.currentTimeMillis());
            }

            Map<String, String> map = new HashMap<String, String>();
            KeyGenerator keygen = KeyGenerator.getInstance(AES_KEY_ALGORITHM);
            SecureRandom random = new SecureRandom(seed.getBytes());
            keygen.init(KEYSIZE, random);
            Key key = keygen.generateKey();
            String keyStr = Base64.encodeBase64String(key.getEncoded());
            map.put("key", keyStr);
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
        return this.charset;
    }

    @Override
    public String encode(String plainText, String key) {
        return encode(plainText, key, null);
    }


    @Override
    public String encode(String plainText, String key, String iv) {
        if (StringUtils.isBlank(plainText) || StringUtils.isBlank(key)) {
            return plainText;
        }
        iv = StringUtils.isBlank(iv) ? AES_IV : iv;
        try {
            IvParameterSpec ivSpec = getIV(iv);
            byte[] keyBytes = Base64.decodeBase64(key);
            byte[] plainTextBytes = plainText.getBytes(getCharset());
            SecretKeySpec skeySpec = new SecretKeySpec(keyBytes, AES_KEY_ALGORITHM);
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);
            byte[] cipherBytes = cipher.doFinal(plainTextBytes);
            return Base64.encodeBase64String(cipherBytes);
        } catch (Exception e) {
            return plainText;
        }
    }

    @Override
    public String decode(String cipherText, String key) {
        return decode(cipherText, key, null);
    }

    @Override
    public String decode(String cipherText, String key, String iv) {
        if (StringUtils.isBlank(cipherText) || StringUtils.isBlank(key)) {
            return cipherText;
        }
        iv = StringUtils.isBlank(iv) ? AES_IV : iv;
        try {
            IvParameterSpec ivSpec = getIV(iv);
            byte[] keyBytes = Base64.decodeBase64(key);
            byte[] cipherBytes = Base64.decodeBase64(cipherText);
            SecretKeySpec skeySpec = new SecretKeySpec(keyBytes, AES_KEY_ALGORITHM);
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, getCharset());
        } catch (Exception e) {
            return cipherText;
        }
    }
}
