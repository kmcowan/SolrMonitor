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
import solrmonitor.auth.BasicAuthenticator;

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
    private final String propsFileName = "solr_monitor.properties";
    private long maxResponseTime = 100000;
    private boolean firstRun = true;

    public SolrPingTimerTask() {
        init();
    }

    public void run() {
     
        System.out.println("Ping SOLR...");
        String toEmail = props.getProperty("mail.to.email");
        String msg = "";
        String subject = "";
        try {
            if(!firstRun){
            cloudClient.connect();
            } else {
                firstRun = false;
            }
            boolean online = true;
            SolrPingResponse resp = isOnline();

            if (resp == null) {
                System.out.println("Ping Response was NULL");
                online = false;
                subject = "ZOOKEEPER CONNECTION FAILED";
                msg = "An attempt to connect to zookeeper at " + solrBaseUrl + " was failed. ";
            } else if (resp.getStatus() != 0) {
                online = false;
                System.out.println("Ping Response was STATUS was: " + resp.getStatus());
                subject = "ZOOKEEPER RETURNED ERROR CODE";
                msg = "An attempt to connect to zookeeper at " + solrBaseUrl + " was failed. Error Code received:  " + resp.getStatus() + " response time: " + resp.getElapsedTime();
            } else if (resp.getElapsedTime() > maxResponseTime) {
                subject = "ZOOKEEPER RESPONSE TIME EXCEED MAX";
                msg = "An attempt to connect to zookeeper at " + solrBaseUrl + " was successful, but slow.  Response time was:  " + resp.getElapsedTime();
            } else {
                // level two.  Run a query. 
                if(props.get("solr.test.query") != null){
                    System.out.println("Running Solr Query Check with query: "+props.getProperty("solr.test.query")+"...");
                SolrQuery query = new SolrQuery();
                   query.setQuery(props.getProperty("solr.test.query"));
                   query.setRows(10);
                   QueryResponse qresp = cloudClient.query(query);
                   if(qresp.getStatus() > 0){
                       online = false;
                       subject = "ERROR EXECUTING SOLR QUERY ";
                       msg = "An error occurred when executing the query: ";
                   }
                }
                
               
            }

            if (!online) {

                System.out.println("SOLR OFFLINE... SEND EMAIL! ");
                sendmail(toEmail, subject, msg);
            } else {
                 System.out.println("Solr is ONLINE..." + resp.getElapsedTime());
            }

             cloudClient.close();
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
        System.out.println("Sending: "+subject);
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
}
