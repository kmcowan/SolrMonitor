/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solrmonitor.util;

import java.text.SimpleDateFormat;
import java.util.Iterator;
import org.json.JSONObject;

/**
 *
 * @author kevin
 */
public class JsonUtils {
    
    public static JSONObject clone(JSONObject obj){
        return new JSONObject(obj.toString());
    }
    
    public static String jsonObjectToHtmlList(JSONObject json){
        String result = "<ul>";
        if(json != null){
            Iterator<String> keys = json.keys();
            String key;
            Object value;
            while(keys.hasNext()){
                key = keys.next();
                value = json.get(key);
               result += "<li>";
               if(value instanceof JSONObject ){
                   result += jsonObjectToHtmlList((JSONObject)value);
               } else {
                   result += key + ": "+ value.toString();
               }
               result += "</li>";
            }
            
        } else {
            result += "<li>No Content to Render</li>";
        }
        result += "</ul>";
        return result;
    }
    
    public final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
}
