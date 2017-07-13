import java.util.Map;

/**
 * Created by stonenice on 2017/7/12.
 */
public interface Encryptor {
    void setCharset(String charset);

    Map<String, String> keygen();

    Map<String, String> keygen(Map<String, String> initVars);

    String getCharset();

    String encode(String plainText, String key);

    String encode(String plainText, String key, String iv);

    String decode(String cipherText, String key);

    String decode(String cipherText, String key, String iv);
}
