/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package club.capture.solr.monitor.encryption;

import java.util.Base64;



/**
 *
 * @author admin
 */
public class Base64EncryptionManager implements Encryptable {

    private static Base64EncryptionManager manager = null;

    private Base64EncryptionManager(){

    }
    public String decrypt(String str){
        String result = "";
        try{
        result = new String(Base64.getDecoder().decode(result));
        
        }catch(Exception e){
            e.printStackTrace();
        }
        return result;
    }

    public String encrypt(String str){
        String result = "";
        result = new String(Base64.getEncoder().encode(str.getBytes()));
        return result;
    }

    public void setup(String passPhrase){

    }

    public Encryptable getInstanceForKey(String key){
        return  Base64EncryptionManager.getInstance(key);
    }

    public static Base64EncryptionManager getInstance(String emptyString){
       if(manager == null){
        manager = new Base64EncryptionManager();
       }
        return manager;
    }
}
