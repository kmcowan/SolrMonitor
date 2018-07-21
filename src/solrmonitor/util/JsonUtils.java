/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solrmonitor.util;

import org.json.JSONObject;

/**
 *
 * @author kevin
 */
public class JsonUtils {
    
    public static JSONObject clone(JSONObject obj){
        return new JSONObject(obj.toString());
    }
}
