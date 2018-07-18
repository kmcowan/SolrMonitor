/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solrmonitor.tasks;

import java.util.TimerTask;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import solrmonitor.SolrMonitor;
import solrmonitor.util.Log;
import static solrmonitor.util.Utils.streamToString;

/**
 *
 * @author kevin
 */
public class APICheckTask extends TimerTask implements Runnable {
    
    private HttpClient client = null;
    private HttpResponse response = null;
    private HttpGet request = null;
    
   
    
    @Override
    public void run(){
        if(client == null){
            init();
        }
        
        try{
            
            request = new HttpGet(SolrMonitor.getProperties().getProperty("api.url.to.monitor"));
            response = client.execute(request);
            if(response.getStatusLine().getStatusCode() < 399){
                Log.logRollup(SolrPingTimerTask.Status.API_OKAY, System.currentTimeMillis());
            } else {
                int status = response.getStatusLine().getStatusCode();
                if(status > 399 && status < 499){
                     Log.logRollup(SolrPingTimerTask.Status.API_CLIENT_ERROR, System.currentTimeMillis());
                } else if(status > 499){
                     Log.logRollup(SolrPingTimerTask.Status.API_SERVER_ERROR, System.currentTimeMillis());
                } else {
                     Log.logRollup(SolrPingTimerTask.Status.API_DOWN, System.currentTimeMillis());
                }
            }
              
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
       private  void init() {
        try {
            CredentialsProvider provider = new BasicCredentialsProvider();
            String user = SolrMonitor.getProperties().getProperty("api.basic.auth.user");
            String pwd = SolrMonitor.getProperties().getProperty("api.basic.auth.pwd");;
            String fusionUrl = SolrMonitor.getProperties().getProperty("api.basic.auth.login.url");;

           Log.log( getClass(), "User: " + user + " pwd: " + pwd + " fusion: " + fusionUrl);

             String authJson = "{\"username\":\"" + user + "\", \"password\":\"" + pwd + "\"}";
            String authUrl = fusionUrl;// + "/api/session?realmName=native";

            UsernamePasswordCredentials credentials
                    = new UsernamePasswordCredentials(user, pwd);
            provider.setCredentials(AuthScope.ANY, credentials);

           
            client = HttpClientBuilder.create()
                    .setDefaultCredentialsProvider(provider)
                    .build();

            String result = "";

            HttpPost request = new HttpPost(authUrl);
            StringEntity params = new StringEntity(authJson);
            request.addHeader("content-type", "application/json");
            request.setEntity(params);
             response = client.execute(request);
            // result will be empty on success
            result = streamToString(response.getEntity().getContent());
            System.out.println("auth reponse: " + result);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
