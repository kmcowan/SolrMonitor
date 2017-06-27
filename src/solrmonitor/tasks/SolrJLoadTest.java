package solrmonitor.tasks;

import java.util.ArrayList;
import java.util.Collection;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.common.SolrInputDocument;

public class SolrJLoadTest {
	public static void main(String[] argv) {
		System.out.println("--------  SolrJLoadTest Testing ------");
                
                SolrJLoadTest service = new SolrJLoadTest();
                
                service.doTest2();
                
                System.exit(0);
	}
    public static String getTimestamp(){
            return( getTimestamp(java.util.Calendar.getInstance().getTime()) );
        }
        
        public static String getTimestamp(java.util.Date date){
            String result = "";
            
            java.text.SimpleDateFormat out = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            
            result = out.format(date);

            return( result );
        }   
    
    public void doTest2(){
        CloudSolrClient server = null;
        
        try{
            server = new CloudSolrClient("localhost:9983");
            server.setDefaultCollection("__test__");

            int maxRows = 10;
            int counter = 0;
            Collection<SolrInputDocument> docList = new ArrayList<SolrInputDocument>();
            
            for(int i = 0;i < maxRows;i++,counter++){

                SolrInputDocument doc = new SolrInputDocument();
                doc.setField("id","ID" + counter);
                doc.setField("name","name"+counter);
                doc.setField("cat","cat"+counter);
                
                
                /*SolrInputDocument cDoc = new SolrInputDocument();
                cDoc.setField("id","CID" + counter);
                cDoc.setField("name","cname"+counter);
                cDoc.setField("cat","ccat"+counter);
                
                doc.addChildDocument(cDoc);*/
                
                
                docList.add(doc);
        
                   
            }
            //UpdateResponse res = server.add(docList);
            
            System.out.println("added: " + counter);
        }
        catch(Exception e){
            System.out.println("failed: " + e.toString());
        }
        finally {
            if( server != null ){
                try{
                    server.close();
                }
                catch(Exception e){
                    System.out.println("unable to close: " + e.toString());
                }
            }
        }
    }
}