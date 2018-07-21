/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solrmonitor.tasks.model;

import org.apache.http.HttpResponse;
import org.apache.solr.client.solrj.response.QueryResponse;

 

/**
 *
 * @author kevin
 */
public class MonitoredQueryResponse  {
    
    private int status = 0;
    
    public MonitoredQueryResponse(HttpResponse resp){
        this.status = resp.getStatusLine().getStatusCode();
    }
    
    public MonitoredQueryResponse(QueryResponse resp){
        this.status = resp.getStatus();
    }
    
     public MonitoredQueryResponse(int resp){
        this.status = resp;
    }
    
    public int getStatus(){
        return status;
    }
    
    
}
