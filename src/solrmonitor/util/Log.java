/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solrmonitor.util;

import javax.swing.JTextPane;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;

/**
 *
 * @author kevin
 */
public class Log {
    private static JTextPane textPane = null;
    private static StyledDocument doc = null;
    private static boolean loggingEnabled = true;
    
    private static void logMessage(String message){
       if(loggingEnabled){
        System.out.println(message);
        if(textPane != null  ){
            try{
            doc.insertString(doc.getLength(), "\n"+message, null );
            }catch(Exception e){}
        }
       }
    }
    
    public static void log(String message){
       logMessage(message);
    }
    
    public static void log(Class clsName, String message){
       logMessage("["+clsName.getSimpleName()+"] "+message);
    }

    /**
     * @return the textPane
     */
    public static JTextPane getTextPane() {
        return textPane;
    }

    /**
     * @param aTextPane the textPane to set
     */
    public static void setTextPane(JTextPane aTextPane) {
        textPane = aTextPane;
        doc = textPane.getStyledDocument();
    }

    /**
     * @return the loggingEnabled
     */
    public static boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    /**
     * @param aLoggingEnabled the loggingEnabled to set
     */
    public static void setLoggingEnabled(boolean aLoggingEnabled) {
        loggingEnabled = aLoggingEnabled;
    }
}
