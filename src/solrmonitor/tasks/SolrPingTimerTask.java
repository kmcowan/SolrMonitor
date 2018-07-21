/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solrmonitor.tasks;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;
import java.util.TimerTask;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.json.JSONArray;
import org.json.JSONObject;
import solrmonitor.SolrMonitor;
import solrmonitor.auth.BasicAuthenticator;
import solrmonitor.tasks.model.MonitoredQueryResponse;
import solrmonitor.tasks.model.PingResponse;
import solrmonitor.util.Log;
import static solrmonitor.util.Log.isPlaybackLoggingEnabled;
import solrmonitor.util.SolrClusterStateHelper;
import solrmonitor.util.Utils;
import static solrmonitor.util.Utils.streamToString;

/**
 *
 * @author kevin
 */
public class SolrPingTimerTask extends TimerTask implements Runnable {

    public final static String COLLECTION_LIST_URL_PART = "/solr/admin/collections?action=LIST&wt=json";
    public final static String STATUS_URL_PART = "/solr/admin/cores?action=STATUS&wt=json";

    private CloseableHttpClient client = null;
    private static HttpResponse response = null;
    private HttpGet request = null;

    private Properties props = null;
    private CloudSolrClient cloudClient = null;
    private String solrBaseUrl = "";
    private static final SolrQuery query = new SolrQuery();
    public static final String propsFileName = "solr_monitor.properties";
    private long maxResponseTime = 100000;
    private boolean firstRun = true;
    private String solrHost = null;
    private boolean isFusionEnabled = false;

    public SolrPingTimerTask() {
        preinit();
        final String endpoint = props.getProperty("api.endpoint.type");
        if (endpoint.equals("fusion")) {
            isFusionEnabled = true;
        }
        init();
    }

    public SolrPingTimerTask(String solrHost) {
        this.solrHost = solrHost;
        init();
    }

    public void run() {

        Log.log("Ping SOLR...");

        final String toEmail = props.getProperty("mail.to.email");
        String msg = "";
        String subject = "";
        PingResponse resp = null;
        try {
            if (!firstRun && !isFusionEnabled) {
                cloudClient.connect();
            } else {
                firstRun = false;
            }
            boolean online = true;

            resp = isOnline();
            JSONObject json = new JSONObject();
            json.put("timestamp", System.currentTimeMillis());

            if (resp == null) {
                Log.log(getClass(), "Ping Response was NULL");
                online = false;
                subject = "ZOOKEEPER CONNECTION FAILED";
                msg = "Attempt to connect to zookeeper at " + solrBaseUrl + " failed. ";
                json.put("zk_status", Status.NO_ZOOKEEPER);
                json.put("zk_status_msg", Status.NO_ZOOKEEPER.name());
                Log.logRollup(Status.NO_ZOOKEEPER, System.currentTimeMillis());
            } else if (resp.getStatus() != 0) {
                online = false;
                System.out.println("Ping Response was STATUS was: " + resp.getStatus());
                subject = "ZOOKEEPER RETURNED ERROR CODE";
                msg = "An attempt to connect to zookeeper at " + solrBaseUrl + " was failed. Error Code received:  " + resp.getStatus() + " response time: " + resp.getElapsedTime();
                json.put("zk_status", Status.ZOOKEEPER_ERROR);
                json.put("zk_status_msg", Status.ZOOKEEPER_ERROR.name());
                Log.logRollup(Status.ZOOKEEPER_ERROR, System.currentTimeMillis());
            } else if (resp.getElapsedTime() > maxResponseTime) {
                subject = "ZOOKEEPER RESPONSE TIME EXCEED MAX";
                msg = "An attempt to connect to zookeeper at " + solrBaseUrl + " was successful, but slow.  Response time was:  " + resp.getElapsedTime();
                json.put("zk_status", Status.ZOOKEEPER_TIMEOUT);
                json.put("zk_status_msg", Status.ZOOKEEPER_TIMEOUT.name());
                Log.logRollup(Status.ZOOKEEPER_TIMEOUT, System.currentTimeMillis());
            } else {
                Log.logRollup(Status.ZOOKEEPER_OK, System.currentTimeMillis());
                final String message = SolrClusterStateHelper.checkShardState(solrHost);
                if (!message.equals("")) {
                    subject = "ONE OR MORE SOLR SHARDS HAS BECOME INACTIVE";
                    msg += message;
                    online = false;
                    json.put("cluster_status", Status.CLUSTER_STATE_CONTAINS_INACTIVE_REPLICAS);
                    json.put("cluster_status_msg", Status.CLUSTER_STATE_CONTAINS_INACTIVE_REPLICAS.name());
                    Log.logRollup(Status.CLUSTER_STATE_CONTAINS_INACTIVE_REPLICAS, System.currentTimeMillis());
                } else {
                    json.put("cluster_status", Status.CLUSTER_STATE_OKAY);
                    json.put("cluster_status_msg", Status.CLUSTER_STATE_OKAY.name());
                    Log.logRollup(Status.CLUSTER_STATE_OKAY, System.currentTimeMillis());
                }
                // level two.  Run a query. 
                if (props.get("solr.test.query") != null
                        && !props.getProperty("solr.test.query").equals("")) {
                    Log.log(getClass(), "Running Solr Query Check with query: " + props.getProperty("solr.test.query") + "...");

                   // query.setQuery(props.getProperty("solr.test.query"));
                  //  query.setRows(10);
                    MonitoredQueryResponse qresp = doQuery();//cloudClient.query(query);
                    Log.log("Final status: " + qresp.getStatus());
                    if (qresp.getStatus() > 0) {
                        online = false;
                        subject = "ERROR EXECUTING SOLR QUERY ";
                        msg += "An error occurred when executing the query: ";
                        json.put("query_status", Status.QUERY_FAIL);
                        json.put("query_status_msg", Status.QUERY_FAIL.name());
                        Log.logRollup(Status.QUERY_FAIL, System.currentTimeMillis());
                    } else {
                        // Log.logRollup(Status.OK, System.currentTimeMillis());
                    }
                }

            }

            if (!online) {

                Log.log("SOLR OFFLINE... SEND EMAIL! ");
                sendmail(toEmail, subject, msg);
                json.put("status", Status.SOLR_DOWN);
                json.put("status_msg", Status.SOLR_DOWN.name());
                Log.logRollup(Status.SOLR_DOWN, System.currentTimeMillis());
            } else {
                Log.log("Solr is ONLINE..." + resp.getElapsedTime());
                json.put("status", Status.OK);
                json.put("status_msg", Status.OK.name());
                Log.logRollup(Status.OK, System.currentTimeMillis());
            }

            if (cloudClient != null) {
                cloudClient.close();
            }
            if (isPlaybackLoggingEnabled()) {
                StatsUpdateTask.addToQueue(json);
            } else {
                json = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.logRollup(Status.EXCEPTION_500, System.currentTimeMillis());
        } finally {
            resp = null;

        }
    }

    private void preinit() {
        props = SolrMonitor.getProperties();
        maxResponseTime = Long.parseLong(props.getProperty("ping.max.response.time"));
        fusionClientInit();
    }

    private void init() {
        // PoolingClientConnectionManager cm = new PoolingClientConnectionManager();
        try {
            if (props == null) {
                props = new Properties();
                props.load(new FileReader(new File(propsFileName)));
            }

            if (props.getProperty("solr.ssl.enabled").equals("true")) {
                solrBaseUrl = "https://";
            } else {
                solrBaseUrl = "http://";
            }
            solrBaseUrl += props.getProperty("solr.zookeeper.port");
            //  solr.crawler.cloud.server=localhost:9983
            //admin:password123@
            CredentialsProvider provider = new BasicCredentialsProvider();

            UsernamePasswordCredentials credentials
                    = new UsernamePasswordCredentials(props.getProperty("solr.admin.user"), props.getProperty("solr.admin.pwd"));
            provider.setCredentials(AuthScope.ANY, credentials);
            if (!isFusionEnabled) {
                // client = new DefaultHttpClient(cm);
                client = HttpClientBuilder.create()
                        .setDefaultCredentialsProvider(provider)
                        .build();

                cloudClient = new CloudSolrClient(props.getProperty("solr.zookeeper.port"), client);
                cloudClient.setDefaultCollection(props.getProperty("solr.default.collection"));
            }

            System.out.println("Solr Base URL: " + solrBaseUrl);

            maxResponseTime = Long.parseLong(props.getProperty("ping.max.response.time"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PingResponse isOnline() {
        boolean result = true;
        PingResponse resp = null;
        try {
            if (!isFusionEnabled) {
                resp = new PingResponse(cloudClient.ping());
            } else {
                resp = new PingResponse(0);

                // HttpGet request = new HttpGet(props.getProperty("api.url.to.monitor"));
            }
            if (resp == null) {
                System.out.println("Ping Response was NULL");
                result = false;
            } else if (resp.getStatus() != 0) {
                result = false;
                System.out.println("Ping Response was STATUS was: " + resp.getStatus());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Ping Response returned EXCEPTION");
            result = false;
        }
        return resp;
    }

    private synchronized void sendmail(String to, String subject, String msg) {
        // Recipient's email ID needs to be mentioned.
        System.out.println("Sending: " + subject);
        Session session = null;
        Transport transport = null;
        boolean useAuth = false;

        if (props.getProperty("mail.use.auth").equals("true")) {
            useAuth = true;
        }

        // Sender's email ID needs to be mentioned
        String from = props.getProperty("mail.from.email");

        // Assuming you are sending email from localhost
        String host = props.getProperty("mail.host");

        // Get system properties
        Properties properties = System.getProperties();

        // Setup mail server
        properties.setProperty("mail.smtp.host", host);
        try {

            if (useAuth) {
                String user = "";
                String pwd = "";

                Authenticator auth = new BasicAuthenticator(user, pwd);
                session = Session.getDefaultInstance(props, auth);
                // uncomment for debugging infos to stdout
                // mailSession.setDebug(true);
                transport = session.getTransport();
            } else {

                // Get the default Session object.
                session = Session.getDefaultInstance(properties);
            }

            // Create a default MimeMessage object.
            MimeMessage message = new MimeMessage(session);

            // Set From: header field of the header.
            message.setFrom(new InternetAddress(from));

            // Set To: header field of the header.
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

            // Set Subject: header field
            message.setSubject(subject);

            // Now set the actual message
            message.setText(msg);

            // Send message
            if (useAuth && transport != null) {
                transport.connect();
                transport.sendMessage(message,
                        message.getRecipients(Message.RecipientType.TO));
                transport.close();
            } else {
                Transport.send(message);
            }
            System.out.println("Sent message successfully....");
        } catch (MessagingException mex) {
            mex.printStackTrace();
        }
    }

    public MonitoredQueryResponse doQuery() {
        MonitoredQueryResponse response = null;
        try {
            if (isFusionEnabled) {
                // dishnetwork/select?q=*:*&wt=json
               /* HttpGet request = new HttpGet(props.getProperty("fusion.query.endpoint"));
                HttpResponse resp = client.execute(request);
                Log.log(getClass(), "HttpResponse: " + resp.getStatusLine().getStatusCode());
                if (resp.getStatusLine().getStatusCode() < 300) {
                    response = new MonitoredQueryResponse(0);
                } else {
                    response = new MonitoredQueryResponse(resp);
                }*/
                response = new MonitoredQueryResponse(0);
               apiCheck();

            } else {
                query.setQuery(props.getProperty("solr.test.query"));
                query.setRows(1);
                response = new MonitoredQueryResponse(cloudClient.query(query));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    public JSONArray getCollections() {
        JSONArray collections = null;
        String collectionUrl = "";
        if (props.getProperty("solr.ssl.enabled").equals("true")) {
            collectionUrl = "https://";
        } else {
            collectionUrl = "http://";
        }

        if (solrHost != null) {
            collectionUrl += solrHost;
        } else {
            collectionUrl += props.getProperty("solr.host.port");
        }
        String response = Utils.getURLContent(collectionUrl + COLLECTION_LIST_URL_PART);
        JSONObject obj = new JSONObject(response);
        collections = obj.getJSONArray("collections");

        return collections;
    }

    private void fusionClientInit() {
        try {
            
            PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
// Increase max total connection to 200
cm.setMaxTotal(200);
// Increase default max connection per route to 20
cm.setDefaultMaxPerRoute(20);
// Increase max connections for localhost:80 to 50
HttpHost localhost = new HttpHost(props.getProperty("api.host"), Integer.parseInt(props.getProperty("api.port")));
cm.setMaxPerRoute(new HttpRoute(localhost), 50);

/*CloseableHttpClient httpClient = HttpClients.custom()
        .setConnectionManager(cm)
        .build();*/

            CredentialsProvider provider = new BasicCredentialsProvider();
            String user = SolrMonitor.getProperties().getProperty("api.basic.auth.user");
            String pwd = SolrMonitor.getProperties().getProperty("api.basic.auth.pwd");;
            String fusionUrl = SolrMonitor.getProperties().getProperty("api.basic.auth.login.url");;

            Log.log(APICheckTask.class, "User: " + user + " pwd: " + pwd + " fusion: " + fusionUrl);

            String authJson = "{\"username\":\"" + user + "\", \"password\":\"" + pwd + "\"}";
            String authUrl = fusionUrl;// + "/api/session?realmName=native";

            UsernamePasswordCredentials credentials
                    = new UsernamePasswordCredentials(user, pwd);
            provider.setCredentials(AuthScope.ANY, credentials);

            client = HttpClientBuilder.create()
                    .setDefaultCredentialsProvider(provider)
                     .setConnectionManager(cm)
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
            EntityUtils.consume(response.getEntity());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void apiCheck(){
         try{
            Log.log("API Check...");
            request = new HttpGet(SolrMonitor.getProperties().getProperty("fusion.query.endpoint"));
            response = client.execute(request);
            if(response.getStatusLine().getStatusCode() < 399){
                Log.logRollup(SolrPingTimerTask.Status.API_OKAY, System.currentTimeMillis());
            } else {
                int status = response.getStatusLine().getStatusCode();
                if(status < 299){
                     Log.logRollup(SolrPingTimerTask.Status.API_OKAY, System.currentTimeMillis());
                } else  if(status > 399 && status < 499){
                     Log.logRollup(SolrPingTimerTask.Status.API_CLIENT_ERROR, System.currentTimeMillis());
                } else if(status > 499){
                     Log.logRollup(SolrPingTimerTask.Status.API_SERVER_ERROR, System.currentTimeMillis());
                } else {
                     Log.logRollup(SolrPingTimerTask.Status.API_DOWN, System.currentTimeMillis());
                }
            }
            request.completed();
            EntityUtils.consume(response.getEntity());
              
        }catch(Exception e){
            e.printStackTrace();
             Log.logRollup(SolrPingTimerTask.Status.API_DOWN, System.currentTimeMillis());
       
        } finally {
             request = null;
             response = null;
             
        }
    }

    public static enum Status {
        OK,
        NO_ZOOKEEPER,
        ZOOKEEPER_ERROR,
        ZOOKEEPER_TIMEOUT,
        ZOOKEEPER_OK,
        NO_SOLR,
        SOLR_ERROR,
        SOLR_DOWN,
        CLUSTER_STATE_OKAY,
        CLUSTER_STATE_RECOVERY,
        CLUSTER_STATE_CONTAINS_INACTIVE_REPLICAS,
        CLUSTER_STATE_REPLICA_DOWN,
        QUERY_FAIL,
        EXCEPTION_500,
        EXCEPTION_400,
        API_DOWN,
        API_OKAY,
        API_CLIENT_ERROR,
        API_SERVER_ERROR

    }
}
