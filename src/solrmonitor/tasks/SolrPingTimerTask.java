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

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import solrmonitor.auth.BasicAuthenticator;
import solrmonitor.util.Log;
import solrmonitor.util.SolrClusterStateHelper;
import solrmonitor.util.Utils;

/**
 *
 * @author kevin
 */
public class SolrPingTimerTask extends TimerTask implements Runnable {

    public final static String COLLECTION_LIST_URL_PART = "/solr/admin/collections?action=LIST&wt=json";
    public final static String STATUS_URL_PART = "/solr/admin/cores?action=STATUS&wt=json";

    private HttpClient client = null;
    private Properties props = null;
    private CloudSolrClient cloudClient = null;
    private String solrBaseUrl = "";
    public static final String propsFileName = "solr_monitor.properties";
    private long maxResponseTime = 100000;
    private boolean firstRun = true;
    private String solrHost = null;

    public SolrPingTimerTask() {
        init();
    }

    public SolrPingTimerTask(String solrHost) {
        this.solrHost = solrHost;
        init();
    }

    public void run() {

        System.out.println("Ping SOLR...");
        String toEmail = props.getProperty("mail.to.email");
        String msg = "";
        String subject = "";
        try {
            if (!firstRun) {
                cloudClient.connect();
            } else {
                firstRun = false;
            }
            boolean online = true;
            SolrPingResponse resp = isOnline();
            JSONObject json = new JSONObject();
            json.put("timestamp", System.currentTimeMillis());

            if (resp == null) {
                System.out.println("Ping Response was NULL");
                online = false;
                subject = "ZOOKEEPER CONNECTION FAILED";
                msg = "Attempt to connect to zookeeper at " + solrBaseUrl + " failed. ";
                json.put("zk_status", Status.NO_ZOOKEEPER);
                json.put("zk_status_msg", Status.NO_ZOOKEEPER.name());
            } else if (resp.getStatus() != 0) {
                online = false;
                System.out.println("Ping Response was STATUS was: " + resp.getStatus());
                subject = "ZOOKEEPER RETURNED ERROR CODE";
                msg = "An attempt to connect to zookeeper at " + solrBaseUrl + " was failed. Error Code received:  " + resp.getStatus() + " response time: " + resp.getElapsedTime();
                json.put("zk_status", Status.ZOOKEEPER_ERROR);
                json.put("zk_status_msg", Status.ZOOKEEPER_ERROR.name());
            } else if (resp.getElapsedTime() > maxResponseTime) {
                subject = "ZOOKEEPER RESPONSE TIME EXCEED MAX";
                msg = "An attempt to connect to zookeeper at " + solrBaseUrl + " was successful, but slow.  Response time was:  " + resp.getElapsedTime();
                json.put("zk_status", Status.ZOOKEEPER_TIMEOUT);
                json.put("zk_status_msg", Status.ZOOKEEPER_TIMEOUT.name());
            } else {
                String message = SolrClusterStateHelper.checkShardState(solrHost);
                if (!message.equals("")) {
                    subject = "ONE OR MORE SOLR SHARDS HAS BECOME INACTIVE";
                    msg += message;
                    online = false;
                  //  json.put("cluster_status", Status.CLUSTER_STATE_CONTAINS_INACTIVE_REPLICAS);
                  //  json.put("cluster_status_msg", Status.CLUSTER_STATE_CONTAINS_INACTIVE_REPLICAS.name());

                } else {
                    json.put("cluster_status", Status.CLUSTER_STATE_OKAY);
                    json.put("cluster_status_msg", Status.CLUSTER_STATE_OKAY.name());
                    Log.logRollup(Status.CLUSTER_STATE_OKAY, System.currentTimeMillis());
                }
                // level two.  Run a query. 
                if (props.get("solr.test.query") != null
                        && !props.getProperty("solr.test.query").equals("")) {
                    System.out.println("Running Solr Query Check with query: " + props.getProperty("solr.test.query") + "...");
                    SolrQuery query = new SolrQuery();
                    query.setQuery(props.getProperty("solr.test.query"));
                    query.setRows(10);
                    QueryResponse qresp = cloudClient.query(query);
                    if (qresp.getStatus() > 0) {
                        online = false;
                        subject = "ERROR EXECUTING SOLR QUERY ";
                        msg += "An error occurred when executing the query: ";
                        json.put("query_status", Status.QUERY_FAIL);
                        json.put("query_status_msg", Status.QUERY_FAIL.name());
                       Log.logRollup(Status.QUERY_FAIL, System.currentTimeMillis());
                    }
                }

            }

            if (!online) {

                System.out.println("SOLR OFFLINE... SEND EMAIL! ");
                sendmail(toEmail, subject, msg);
                json.put("status", Status.SOLR_DOWN);
                json.put("status_msg", Status.SOLR_DOWN.name());
            } else {
                System.out.println("Solr is ONLINE..." + resp.getElapsedTime());
                json.put("status", Status.OK);
                json.put("status_msg", Status.OK.name());
                Log.logRollup(Status.OK, System.currentTimeMillis());
            }

            cloudClient.close();
            StatsUpdateTask.addToQueue(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init() {
        PoolingClientConnectionManager cm = new PoolingClientConnectionManager();
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

            // client = new DefaultHttpClient(cm);
            client = HttpClientBuilder.create()
                    .setDefaultCredentialsProvider(provider)
                    .build();

            cloudClient = new CloudSolrClient(props.getProperty("solr.zookeeper.port"), client);
            cloudClient.setDefaultCollection(props.getProperty("solr.default.collection"));

            System.out.println("Solr Base URL: " + solrBaseUrl);

            maxResponseTime = Long.parseLong(props.getProperty("ping.max.response.time"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SolrPingResponse isOnline() {
        boolean result = true;
        SolrPingResponse resp = null;
        try {
            resp = cloudClient.ping();
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
        API_DOWN,
        API_OKAY,
        API_CLIENT_ERROR,
        API_SERVER_ERROR

    }
}
