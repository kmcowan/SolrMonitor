package club.capture.solr.monitor.encryption;

import java.security.spec.*;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.*;

/**@devnote use AESEncryptionManager instead for greatest security
 *
 * @author admin
 */
public class DESEncryptionManager implements Encryptable {
        static Cipher ecipher;
        static Cipher dcipher;


        // 8-byte Salt
        byte[] salt = {
            (byte)0xA9, (byte)0x9B, (byte)0xC8, (byte)0x32,
            (byte)0x56, (byte)0x35, (byte)0xE3, (byte)0x03
        };

        // Iteration count
        int iterationCount = 19;

        DESEncryptionManager(String passPhrase) {
           setup(passPhrase);
        }

        public void setup(String passPhrase){
             try {
                // Create the key
                KeySpec keySpec = new PBEKeySpec(passPhrase.toCharArray(), salt, iterationCount);
                SecretKey key = SecretKeyFactory.getInstance(
                    "PBEWithMD5AndDES").generateSecret(keySpec);
                ecipher = Cipher.getInstance(key.getAlgorithm());
                dcipher = Cipher.getInstance(key.getAlgorithm());

                // Prepare the parameter to the ciphers
                AlgorithmParameterSpec paramSpec = new PBEParameterSpec(salt, iterationCount);

                // Create the ciphers
                ecipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
                dcipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
            } catch (java.security.InvalidAlgorithmParameterException e) {
            } catch (java.security.spec.InvalidKeySpecException e) {
            } catch (javax.crypto.NoSuchPaddingException e) {
            } catch (java.security.NoSuchAlgorithmException e) {
            } catch (java.security.InvalidKeyException e) {
            }
        }

        public  String encrypt(String str) {
            try {
                // Encode the string into bytes using utf-8
                // @todo: plug into our own Base64Coder class
                byte[] utf8 = str.getBytes("UTF8");

                // Encrypt
                byte[] enc = ecipher.doFinal(utf8);

                // Encode bytes to base64 to get a string
                return  new String(Base64.getEncoder().encode(enc));
            } catch (javax.crypto.BadPaddingException e) {
                 e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                 e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                 e.printStackTrace();
           
            } catch (Exception e) {
                  e.printStackTrace();
            }
            return null;
        }

        public  String decrypt(String str) {
            try {
                // Decode base64 to get bytes
                byte[] dec = Base64.getDecoder().decode(str);

                // Decrypt
                byte[] utf8 = dcipher.doFinal(dec);

                // Decode using utf-8
                return new String(utf8, "UTF8");
            } catch (javax.crypto.BadPaddingException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                  e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                  e.printStackTrace();
           
            } catch (Exception e) {
                  e.printStackTrace();
            }
            return null;
        }

        public static DESEncryptionManager getInstance(String passphrase){
            return new DESEncryptionManager(passphrase);

        }

      public Encryptable getInstanceForKey(String key){
        return new DESEncryptionManager(key);
      }

        public static final String DES_PASSPH = "Aedon NightINGale";
    }
