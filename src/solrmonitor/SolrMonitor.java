/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solrmonitor;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
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
        String hosts = props.getProperty("solr.hosts.to.monitor");
        ArrayList<Runnable> tasks = new ArrayList<>();
        if(hosts.contains(",")){
             System.out.println("RUNNING MULTIPLE MONITOR...");
            String[] solrHosts = hosts.split(",");
            for(int j=0; j<solrHosts.length; j++){
                 SolrPingTimerTask ttask = new SolrPingTimerTask(solrHosts[j]);
                 tasks.add(ttask);
            }
        } else {
            System.out.println("RUNNING SINGLE MONITOR...");
            tasks.add(task);
        }
        long loopTime = Long.parseLong(props.getProperty("monitor.loop.time"));
         while(true){
             System.out.println("Monitor Loop Run...");
             for(int i=0; i<tasks.size(); i++){
               //task.run();
               run(tasks.get(i));
             }
             Thread.sleep(loopTime);
         }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    private static synchronized void run(Runnable task){
        task.run();
    }
    
}
