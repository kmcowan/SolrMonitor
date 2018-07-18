/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solrmonitor.tasks;

import java.util.TimerTask;
import org.json.JSONObject;
import solrmonitor.util.Log;

/**
 *
 * @author kevin
 */
public class StatsRollupTask extends TimerTask implements Runnable {
    
    public void run(){
        JSONObject stats = Log.getLogRollup();
        
    }
}
