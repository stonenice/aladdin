
import java.util.Map;

/**
 * Encryptor defines some generic abstract concise methods about
 * encryption. You can invoke encode() to encrypt your plain text
 * and decode() to decrypt your cipher text. Besides, some algorithms
 * need to specify IV(Initial Variables). Encryptor supports to encrypt
 * or decrypt the content contained chinese. There are some tips that
 * you know about keygen(). keygen() will returns a map contained keys.
 * Before invoking it, you can put some initial variables for improving
 * the strong of encryption.
 * <p>
 * <p>
 * Examples:
 * <code>
 * //RSA
 * Encryptor rsa=Encrypts.newRSAEncryptor(); // Create a concreted encryptor with RSA.
 *
 * Map<String,String> keys=rsa.keygen(); //Generate the key pair.
 * String pubKey=keys.get("publicKey");
 * String priKey=keys.get("privateKey");
 *
 * String plainText="Hello World!";
 * String cipherText=rsa.encode(plainText,pubKey);
 * String content=rsa.decode(cipherText,priKey);
 *
 * //AES
 * Encryptor aes=Encrypts.newAESEncryptor(); // Create a concreted encryptor with AES.
 *
 * String seed="tuofeng";
 * Map<String,String> initVars=new HaspMap<String,String>();
 * initVars.put("seed",seed);
 * Map<String,String> keys=aes.keygen(initVars); //Generate the key pair.
 * String key=keys.get("key");
 *
 * String plainText="Hello World!";
 * String cipherText=aes.encode(plainText,key,seed);
 * String content=aes.decode(cipherText,key,seed);
 *
 * </code>
 
 * Created by stonenice on 2017/7/12.
 *
 * @author stonenice
 */
public interface Encryptor {

    /**
     * Specify the charset when encrypting and decrypting
     * and the default charset is UTF-8. Therefore, Encyptor
     * supports chinese.
     *
     * @param charset Default to UTF-8
     */
    void setCharset(String charset);

    /**
     * Get the current charset
     *
     * @return Default to UTF-8
     */
    String getCharset();

    /**
     * @see #keygen(Map)
     *
     * @return Keys
     */
    Map<String, String> keygen();

    /**
     * Generate the keys and you can set some initial variables.
     * RSA do not needs any variables. the returns must contains
     * the names of key in returned map("privateKey", "publicKey").
     *
     * AES needs set a seed when generating automatically, so you
     * must to put value as key "seed" in initVars. the returns must
     * contains the names of key in returned map("key")
     *
     *
     * @param initVars The initial variables
     * @return Keys The keys dictionary
     */
    Map<String, String> keygen(Map<String, String> initVars);

    /**
     *
     * @see #encode(String, String, String)
     *
     * @param plainText
     * @param key
     * @return
     */
    String encode(String plainText, String key);

    /**
     * Encrypt the plain text to the cipher text.
     *
     * @param plainText The plain text
     * @param key The key of encrypting
     * @param iv Initial Variables, for AES
     * @return The cipher text
     */
    String encode(String plainText, String key, String iv);

    /**
     * @see #decode(String, String, String)
     *
     * @param cipherText
     * @param key
     * @return
     */
    String decode(String cipherText, String key);

    /**
     * Decrypt the cipher text to the plain text.
     *
     * @param cipherText The cipher text
     * @param key The key of decrypting
     * @param iv Initial Variables, for AES
     * @return The plain text
     */
    String decode(String cipherText, String key, String iv);
}
