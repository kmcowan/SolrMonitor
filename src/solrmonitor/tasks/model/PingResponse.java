/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solrmonitor.tasks.model;

import java.io.Serializable;
import org.apache.solr.client.solrj.response.SolrPingResponse;

/**
 *
 * @author kevin
 */
public class PingResponse extends SolrPingResponse implements Serializable {
    private int status = 0;
    public PingResponse(int status){
       this.status = status;   
    }
    
      public PingResponse(SolrPingResponse ping){
       this.status = ping.getStatus();   
    }
    
    @Override
    public int getStatus(){
        return status;
    }
    
   
}
