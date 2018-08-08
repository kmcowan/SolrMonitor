/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package club.capture.solr.monitor.encryption;

/**
 *
 * @author admin
 */
public interface Encryptable {

    String decrypt(String str);

    String encrypt(String str);

    void setup(String passPhrase);

    Encryptable getInstanceForKey(String key);


 
}
