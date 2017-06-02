/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solrmonitor;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;
import solrmonitor.tasks.SolrPingTimerTask;

/**
 *
 * @author kevin
 */
public class SolrMonitor {
    private static final String propsFileName = "solr_monitor.properties";
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        SolrPingTimerTask task = new SolrPingTimerTask();
     
        
        try{
               Properties props = new Properties();
        props.load(new FileReader(new File(propsFileName)));
        long loopTime = Long.parseLong(props.getProperty("monitor.loop.time"));
         while(true){
             System.out.println("Monitor Loop Run...");
             task.run();
             Thread.sleep(loopTime);
         }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
}
