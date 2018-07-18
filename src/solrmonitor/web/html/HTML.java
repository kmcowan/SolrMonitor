/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solrmonitor.web.html;

import solrmonitor.util.Utils;

 

 

/**
 *
 * @author kevin
 */
public class HTML {
 
    public static String getPage(Page p) {
        String page = "";
        try{
            if(p == Page.notfound){
                page = Utils.streamToString(HTML.class.getResourceAsStream("404.html"));
            } else {
             page = Utils.streamToString(HTML.class.getResourceAsStream(p.name()+".html"));
            }
        }catch(Exception e){
            e.printStackTrace();
            page = Utils.streamToString(HTML.class.getResourceAsStream("404.html"));
        }
                
        return page;
    }
    
    public static String getHTMLContent(String content){
         String page = "";
        try{
        page = Utils.streamToString(HTML.class.getResourceAsStream(Page.top_half.name()+".html"));
        page += content;
        page += Utils.streamToString(HTML.class.getResourceAsStream(Page.bottom_half.name()+".html"));
        }catch(Exception e){
            e.printStackTrace();
            page = Utils.streamToString(HTML.class.getResourceAsStream("404.html"));
        }
                
        return page;
    }
    
    public static enum Page {
        index,
        users,
        permissions,
        roles,
        permissions_params,
        top_half,
        bottom_half,
        notfound
    }
    
}
