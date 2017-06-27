/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solrmonitor.util;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Properties;
import org.json.JSONObject;
import solrmonitor.tasks.SolrPingTimerTask;

/**
 *
 * @author kevin
 */
public class SolrClusterStateHelper {

    static ArrayList<JSONObject> replicas = new ArrayList<>();

    public static ArrayList<JSONObject> getReplicas(JSONObject jsono) {
        ArrayList<JSONObject> arr = new ArrayList<>();
        Iterator<?> keys = jsono.keys();

        while (keys.hasNext()) {
            String key = (String) keys.next();
            Object obj = jsono.get(key);
            String state = "";
            String core = "";
            if (jsono.get(key) instanceof JSONObject) {
                JSONObject j2 = (JSONObject) obj;
                //    state = j2.getString("state");
                arr.add(j2);
                //  Log.log("** REPLICA **  key: "+key+" name: "+j2.getString("core")+" state: "+j2.getString("state"));
            }
        }
        // Log.log("Replicas returns: "+arr.size()+" replicas...");
        return arr;
    }

    public static ArrayList<JSONObject> getShards(JSONObject jsono) {
        ArrayList<JSONObject> arr = new ArrayList<>();
        Iterator<?> keys = jsono.keys();
        JSONObject replica = null;

        while (keys.hasNext()) {
            String key = (String) keys.next();
            Object obj = jsono.get(key);
            String state = "";

            if (jsono.get(key) instanceof JSONObject) {
                JSONObject j2 = (JSONObject) obj;
                state = j2.getString("state");
                //   Log.log("** SHARD ** key: "+key+" state: "+state);
                replica = j2.getJSONObject("replicas");
                if (replica != null) {
                    arr.addAll(getReplicas(replica));
                }

            }
        }
        return arr;
    }

    public static ArrayList<JSONObject> getStatesOfCluster(JSONObject jsono) {
        ArrayList<LinkedHashMap<String, String>> map = new ArrayList<>();

        Iterator<?> keys = jsono.keys();

        while (keys.hasNext()) {
            String key = (String) keys.next();
            Object obj = jsono.get(key);
            if (key.equals("shards")) {
                //  Log.log("Shards: "+obj.getClass().getSimpleName());
                ArrayList<JSONObject> reps = getShards((JSONObject) obj);
                //  Log.log("REPs has: "+reps.size()+" items...  ["+replicas.size()+"]");
                /*  for(int j=0; j<reps.size(); j++){
                    replicas.add(reps.get(j));
                }*/
                replicas.addAll(reps);

            }

            if (key.equals("replicas")) {
                //  getReplicas((JSONObject)obj);
            }
            if (jsono.get(key) instanceof JSONObject) {
                //  Log.log("JSONObject: key: " + key );

                getStatesOfCluster((JSONObject) obj);

            } else //  Log.log("Object: key: " + key +  " value: "+obj.toString()+" class type: " + obj.getClass().getSimpleName());
            {
                if (key.equals("state")) {
                    // Log.log("State:  " + obj );
                }
            }
        }
        return replicas;
    }

    public static String checkShardState(String host) {
        replicas = new ArrayList<>();
        String message = "";
        String solrBaseUrl = "";
        try {
            Properties props = new Properties();
            props.load(new FileReader(new File(SolrPingTimerTask.propsFileName)));
             if (props.getProperty("solr.ssl.enabled").equals("true")) {
                solrBaseUrl = "https://";
            } else {
                solrBaseUrl = "http://";
            }
             if(host != null){
                 solrBaseUrl += host;
             } else {
               solrBaseUrl += props.getProperty("solr.host.port");
             }
            String json = Utils.getURLContent(solrBaseUrl+"/solr/admin/zookeeper?wt=json&detail=true&path=%2Fclusterstate.json&view=graph&_=1496932225331");// Utils.streamToString(SolrClusterStateHelper.class.getResourceAsStream("test.json"));
            JSONObject jsono = new JSONObject(json);
            JSONObject data = new JSONObject(jsono.getJSONObject("znode").getString("data"));
            // Log.log(data.toString());
            ArrayList<JSONObject> replicas = getStatesOfCluster(data);
            Log.log("Replicas has: " + replicas.size() + " items");
            JSONObject j2 = null;
            String core, state;

            boolean allActive = true;

            for (int i = 0; i < replicas.size(); i++) {
                j2 = replicas.get(i);
                core = j2.getString("core");
                state = j2.getString("state");
                if (!state.equals("active")) {
                    allActive = false;
                    message += core + " is " + state + " \n";
                }
                Log.log("** REPLICA **  name: " + j2.getString("core") + " state: " + j2.getString("state"));
            }

            if (!allActive) {
                Log.log("FOUND PROBLEMS: " + message);
            } else {
                Log.log("Add shards are active...");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return message;
    }

    public static void main(String[] args) {
        checkShardState(null);
    }
}
