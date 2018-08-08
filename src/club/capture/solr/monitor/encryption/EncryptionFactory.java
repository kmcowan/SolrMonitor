package club.capture.solr.monitor.encryption;

import javax.crypto.Cipher;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import java.security.Key;
import java.security.InvalidKeyException;

public class EncryptionFactory {

    private static String algorithm = "DESede";
    private static Key key = null;
    private static Cipher cipher = null;
    private static Encryptable desMgr = null;

    public static void setUp() throws Exception {
        key = KeyGenerator.getInstance(algorithm).generateKey();
        cipher = Cipher.getInstance(algorithm);
    }

    public static void main(String[] args)  {
        try{
        setUp();
        if (args.length != 1) {
            System.out.println(
                    "USAGE: java LocalEncrypter "
                    + "[String]");
          //  System.exit(1);
          args = new String[1];
          args[0] = "This is a test string...";
        }
        byte[] encryptionBytes = null;
        String input = args[0];
        System.out.println("Entered: " + input);
        encryptionBytes = encrypt(input);
        System.out.println(
                "Recovered: " + decrypt(encryptionBytes));
        }catch(Exception e){
            e.printStackTrace();
        }catch(Error err){
            err.printStackTrace();
        } finally {
            System.exit(0);
        }
    }

    public static byte[] encrypt(String input) throws InvalidKeyException,
            BadPaddingException,
            IllegalBlockSizeException {
        /*
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] inputBytes = input.getBytes();
            return cipher.doFinal(inputBytes);
             * */
        // if(desMgr == null){
        //  desMgr = DESEncryptionManager.getInstance(DESEncryptionManager.DES_PASSPH);
        desMgr = Keystore.getCurrentEncryption();
        // }
        return desMgr.encrypt(input).getBytes();
    }

    public static String decrypt(byte[] encryptionBytes)
            throws InvalidKeyException,
            BadPaddingException,
            IllegalBlockSizeException {
        /*
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] recoveredBytes = 
              cipher.doFinal(encryptionBytes);
            String recovered = 
              new String(recoveredBytes);
            return recovered;
             * */
        //   if(desMgr == null){
        // desMgr = DESEncryptionManager.getInstance(DESEncryptionManager.DES_PASSPH);
        desMgr = Keystore.getCurrentEncryption();
        //}
        return desMgr.decrypt(new String(encryptionBytes));
    }
}
