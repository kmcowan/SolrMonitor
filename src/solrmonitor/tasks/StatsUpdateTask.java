/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solrmonitor.tasks;

import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import org.json.JSONObject;
import solrmonitor.util.Log;

/**
 *
 * @author kevin
 */
public class StatsUpdateTask extends TimerTask implements Runnable {
    
    @SuppressWarnings("unchecked")
    private static final LinkedBlockingQueue<JSONObject> queue = new LinkedBlockingQueue();
    public StatsUpdateTask(){}
    
    public void run(){
        while(queue.size() > 0){
            Log.log(queue.peek());
        }
    }
    
    public static void addToQueue(JSONObject json){
        queue.add(json);
    }
    
    
}
