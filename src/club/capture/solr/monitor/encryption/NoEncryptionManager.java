/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package club.capture.solr.monitor.encryption;

/**
 *
 * @author admin
 */
public class NoEncryptionManager implements Encryptable {

    private static NoEncryptionManager manager = null;

    private NoEncryptionManager(){

    }
    public String decrypt(String str){
        
        return str;
    }

    public String encrypt(String str){
       
        return str;
    }

    public void setup(String passPhrase){

    }

    public Encryptable getInstanceForKey(String key){
        return  NoEncryptionManager.getInstance(key);
    }

    public static NoEncryptionManager getInstance(String emptyString){
       if(manager == null){
        manager = new NoEncryptionManager();
       }
        return manager;
    }
}
