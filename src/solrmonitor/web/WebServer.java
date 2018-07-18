/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solrmonitor.web;


import com.sun.net.httpserver.HttpServer;
import java.awt.Desktop;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import solrmonitor.util.Log;

/**
 *
 * @author kevin
 */
public class WebServer {

    private static HashMap<String, String> config = null;

    private WebServer() {

    }

    public static void main(String[] args) {
        try {
            config = argsAsMap(args);
            int port = 8081;
            String host = config.get("-s");
            
            if (config != null && config.get("-p") != null) {
                port = Integer.parseInt(config.get("-p"));
            }
            Log.log(WebServer.class, "Starting web server on port: " + port);
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.start();
            server.createContext("/", new RESTService());
             Log.log(WebServer.class,"Server Started OK..."+ new Date().toString());
            URL url = new URL("http://"+host+":"+port);
            openWebpage(url.toURI());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static HashMap<String, String> argsAsMap(String[] args) {
        String key, value;
        HashMap<String, String> params = new HashMap<>();
        params.put("-p", "8081");
        params.put("-s", "localhost");
        for (int i = 0; i < args.length; i++) {
            key = args[i];
            if (params.get(key) != null) {
                value = args[i + 1];// assumes value follows key in args list. 
                params.put(key, value);
            }
        }
        return params;
    }
    
      private static void openWebpage(URI uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(uri);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
