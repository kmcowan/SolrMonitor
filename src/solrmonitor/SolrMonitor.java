/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solrmonitor;

import solrmonitor.tasks.SolrPingTimerTask;

/**
 *
 * @author kevin
 */
public class SolrMonitor {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        SolrPingTimerTask task = new SolrPingTimerTask();
        try{
         while(true){
             System.out.println("PING SOLR...");
             task.run();
             Thread.sleep(1000);
         }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
}
