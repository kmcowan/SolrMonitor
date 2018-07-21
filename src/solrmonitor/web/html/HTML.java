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
 
    public final static String HEADER = Utils.streamToString(HTML.class.getResourceAsStream("header.html"));
    public final static String FOOTER = Utils.streamToString(HTML.class.getResourceAsStream("footer.html"));
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
        page = Utils.streamToString(HTML.class.getResourceAsStream(Page.header.name()+".html"));
        page += content;
        page += Utils.streamToString(HTML.class.getResourceAsStream(Page.footer.name()+".html"));
        }catch(Exception e){
            e.printStackTrace();
            page = Utils.streamToString(HTML.class.getResourceAsStream("404.html"));
        }
                
        return page;
    }
    
    public static String getPageWithContent(String content){
        return HEADER + content + FOOTER;
    }
    
    public static boolean pageExists(String page){
        for(int i=0; i<Page.values().length; i++){
            if(page.contains(Page.values()[i].name())){
                return true;
            }
        }
        return false;
    }
    
    public static enum Page {
        index,
        footer,
        header,
        notfound
    }
    
}
