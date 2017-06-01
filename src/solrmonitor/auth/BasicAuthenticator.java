/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solrmonitor.auth;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

/**
 *
 * @author kevin
 */
 public class BasicAuthenticator extends Authenticator {
        String baName;
        String baPassword;
        public BasicAuthenticator(String baName1, String baPassword1) {
            baName = baName1;
            baPassword = baPassword1;
        }
        @Override
            public PasswordAuthentication getPasswordAuthentication() {
                System.out.println("Authenticating...");
                return new PasswordAuthentication(baName, new String(baPassword.toCharArray()));
            }
    };    
