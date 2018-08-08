/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package club.capture.solr.monitor.encryption;

 

import java.io.File;
import java.util.Hashtable;
 
import net.cytopia.tofu.util.FileUtil;
 
import solrmonitor.util.UUID;

/**
 *
 * @author admin
 */
public class Keystore {

    private static Hashtable<String, Encryptable> keys = null;
    private static Hashtable<EncryptionType, Encryptable> managers = null;
    private static String currentKey = null;
    private static Encryptable currentManager = null;
    private static EncryptionType currentEncryptionType = null;

    public static void addKey(String passkey) {
        if (keys == null) {
            init();
        }
        
      
        keys.put(passkey, currentManager);
    }

    public static void init() {
          currentManager = (Encryptable)AESEncryptionManager.getInstance(DEFAULT_KEY);//DESEncryptionManager.getInstance(DEFAULT_KEY);//

        if (keys == null) {
            keys = new Hashtable<String, Encryptable>();
           // keys.put(DEFAULT_KEY, AESEncryptionManager.getInstance(DEFAULT_KEY));
             keys.put(DEFAULT_KEY, currentManager);
            currentKey = DEFAULT_KEY;

            // load the managers
            managers = new Hashtable<EncryptionType,Encryptable>();
            managers.put(EncryptionType.DES, (Encryptable)DESEncryptionManager.getInstance(currentKey));
            managers.put(EncryptionType.AES, (Encryptable)AESEncryptionManager.getInstance(currentKey));
            managers.put(EncryptionType.BASE64, (Encryptable)Base64EncryptionManager.getInstance(currentKey));
            managers.put(EncryptionType.NO_ENCRYPTION, (Encryptable)NoEncryptionManager.getInstance(currentKey));


            try {

                // load the history.
                File dir = new File(KEYSTORE_PATH);
                if (dir.exists()
                        && dir.isDirectory()
                        && dir.listFiles() != null) {
                    File[] files = dir.listFiles();

                    for (int i = 0; i < files.length; i++) {
                        //
                        File file = files[i];
                        if (file.exists()
                                && file.isFile()
                                && file.getName().indexOf(KEYSTORE_MIME) > -1) {
                            String key = getDefaultEncryption().decrypt(new String(FileUtil.getBytesFromFile(file)));
                           // keys.put(key, AESEncryptionManager.getInstance(key));
                            keys.put(key, currentManager.getInstanceForKey(key));
                        }
                    }

                   
                }

                 // now load the currentKey
                    File file = new File(KEYSTORE_FILE);
                    if (file.exists()) {
                        String key = getDefaultEncryption().decrypt(new String(FileUtil.getBytesFromFile(file)));
                      //  keys.put(key, AESEncryptionManager.getInstance(key));
                         keys.put(key, currentManager.getInstanceForKey(key));
                        System.out.println(  "Loaded Default key: "+key);
                        Keystore.setCurrentKey(key);

                    } else {
                        System.out.println(  "No Default key: ");
                    }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

 
    public static void save() throws Exception {
        String newKey = getCurrentKey();
        currentKey = DEFAULT_KEY;
        String content = new String(getDefaultEncryption().encrypt(newKey));
        File dir = new File(KEYSTORE_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String filePath = KEYSTORE_PATH + UUID.genUUIDOfLength(14) + KEYSTORE_MIME;
        // write to history
        FileUtil.writeBytesToFile(content.getBytes(), filePath);

        // write to current
        FileUtil.writeBytesToFile(content.getBytes(), KEYSTORE_FILE);
        currentKey = newKey;

    }

    public static Encryptable getDefaultEncryption(){
        return AESEncryptionManager.getInstance(DEFAULT_KEY);
    }

    public static boolean changeKey(String newKey) {
        try {
            addKey(newKey);
            currentKey = newKey;
            save();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * @return the keys
     */
    public static Hashtable<String, Encryptable> getKeys() {
        if (keys == null) {
            init();
        }
        return keys;
    }

    /**
     * @return the currentKey
     */
    public static String getCurrentKey() {
        if (currentKey == null) {
            currentKey = DEFAULT_KEY;
        }
        return currentKey;
    }

    public static Encryptable getCurrentEncryption() {
        if (keys == null) {
            init();
        }
        return keys.get(getCurrentKey());
    }

    public static boolean changeEncryptionManager(){

        return true;
    }
    /**
     * @param aCurrentKey the currentKey to set
     */
    public static void setCurrentKey(String aCurrentKey) {
        currentKey = aCurrentKey;
    }
    public static final String DEFAULT_KEY = "Aedon NightINGale";
    public static EncryptionType DEFAULT_TYPE = EncryptionType.BASE64;
    public static final String KEYSTORE_FILE = "keystore.aeky";
    public static final String KEYSTORE_PATH = "keys/";
    public static final String KEYSTORE_MIME = ".aeky";

    /**
     * @return the managers
     */
    public static Hashtable<EncryptionType, Encryptable> getManagers() {
        return managers;
    }

    /**
     * @return the currentManager
     */
    public static Encryptable getCurrentManager() {
        return currentManager;
    }

    /**
     * @return the currentEncryptionType
     */
    public static EncryptionType getCurrentEncryptionType() {
        return currentEncryptionType;
    }

    public static enum EncryptionType {
        AES,
        DES,
        BASE64,
        NO_ENCRYPTION;
    }
}
