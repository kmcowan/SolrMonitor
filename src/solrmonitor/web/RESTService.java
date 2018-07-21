/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solrmonitor.web;

import solrmonitor.web.html.HTML;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import java.util.Iterator;
import java.util.List;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.cytopia.tofu.ProcessorServlet;
import org.json.JSONObject;
import solrmonitor.util.Log;
import solrmonitor.util.Utils;
import solrmonitor.web.html.HTML.Page;

/**
 *
 *
 * @author kevin
 */
public class RESTService extends GenericServlet implements MessageHandler, HttpHandler {

    private String message;
    public final static ProcessorServlet PROCESSOR = new ProcessorServlet();

    @Override
    public void init() throws ServletException {
        // Do required initialization
        message = "Hello World";
    }

    public void doGet(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {
        // Set response content type
        response.setContentType("text/html");

        // Actual logic goes here.
        PrintWriter out = response.getWriter();
        out.println("<h1>" + message + "</h1>");
    }

    public void doPost(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

    }

    @Override
    public void destroy() {
        // do nothing.
    }

    @Override
    public void service(ServletRequest req,
            ServletResponse res)
            throws ServletException,
            java.io.IOException {

        res.setContentType("text/html");
        PrintWriter out = res.getWriter();

        out.println("<h1>Hello World example using"
                + " GenericServlet class.</h1>");
        out.close();

    }

    public static void main(String[] args) {
        try {
            RESTService service = new RESTService();
            service.init();
        } catch (Exception ex) {
            System.err.println("URISyntaxException exception: " + ex.getMessage());
        }
    }

    @Override
    public void handleMessage(String message) {
        Log.log(RESTService.class, "message rec'd: " + message);
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        OutputStream out = ex.getResponseBody();

        if (isValidRequestMethod(ex)) {

            try {
                String contentType = "application/json";
                boolean isValidAction = false;
                JSONObject action = null;

                String page = ex.getRequestURI().getPath();
                String content = "";
                if (page.equals("/")) {
                    page += "index";
                    contentType = "text/html";
                } else {
                    final String pg = page;
                    switch (pg) {
                        case "/status":
                            content = ActionProcessor.getStatusContent();
                            break;
                    }
                }

                boolean isService = false;
                String actionPage = page;
                /* if (actionPage.startsWith("/")) {
                    actionPage = actionPage.substring(1);
                }
                if (actionManager.isValidAction(actionPage)) {
                    action = actionManager.getAction(actionPage);
                    contentType = action.getString(ActionKey.ContentType.name());
                    isValidAction = true;

                } else if (page.endsWith(".js")) {
                    contentType = "application/javascript";
                } else if (page.endsWith(".png")) {
                    contentType = "image/png";
                } else if (page.endsWith(".jpg") || page.endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                } else if (page.endsWith(".ico")) {
                    contentType = "image/icon";
                } else if (page.endsWith(".css")) {
                    contentType = "text/css";
                } else if (page.endsWith(".html") || page.equals("") || page.contains("index.html")) {
                    contentType = "text/html";
                } else {
                    isService = true;
                }*/
                ex.getResponseHeaders().add("Content-Type", contentType);
                ex.getResponseHeaders().add("X-Powered-by", "Capture Club HAIDIE::EMPATHS Platform");

                if (isService && !isValidAction) {
                    Log.log(RESTService.class, "Process SERVICE Request: " + page);
                    // Log.log(RESTService.class,"Process request as SERVICE");
                    byte[] outbuf = "Hello World!".getBytes();//  RequestProcessor.process(ex).getBytes();
                    ex.sendResponseHeaders(200, 0);
                    if (outbuf != null && outbuf.length > 0) {
                        out.write(outbuf);
                    } else {
                        out.write("an error occurred...".getBytes());
                    }
                } else if (!isService && isValidAction) {
                    Log.log(RESTService.class, "Process ACTION Request: " + actionPage);

                } else {
                    Log.log(RESTService.class, "Process " + ex.getRequestMethod() + " request as NON-SERVICE " + page);
                    if (page.trim().equals("/")) {
                        page = "index";
                    } else if (page.startsWith("/")) {
                        page = page.substring(1);
                    }

                    final String pg = page;
                    switch (pg) {
                        case "status":
                            ex.getResponseHeaders().add("Content-Type", "text/html");
                            ex.sendResponseHeaders(200, 0);
                            out.write(HTML.HEADER.getBytes());
                           out.write(HTML.NAV.getBytes());
                            out.write(ActionProcessor.getStatusContent().getBytes());
                            out.write(HTML.FOOTER.getBytes());
                            break;

                        case "index":
                            ex.getResponseHeaders().add("Content-Type", "text/html");
                            ex.sendResponseHeaders(200, 0);
                            out.write(HTML.HEADER.getBytes());
                            out.write(ActionProcessor.getIndexContent().getBytes());
                            out.write(HTML.NAV.getBytes());
                            out.write(HTML.FOOTER.getBytes());
                            break;

                        case "monthly":
                            ex.getResponseHeaders().add("Content-Type", "text/html");
                            ex.sendResponseHeaders(200, 0);
                            out.write(HTML.HEADER_AND_NAV.getBytes());
                            out.write(ActionProcessor.getMonthlyContent().getBytes());
                            out.write(HTML.FOOTER.getBytes());
                            break;

                        default:
                            if (page != null && !HTML.pageExists(page)) {
                                page = "404.html";
                            }
                            java.io.InputStream stream = HTML.class.getResourceAsStream(page);
                            byte bytes[] = Utils.streamToBytes(stream);
                            if (Utils.isHTML(bytes) || page.contains(".html")) {
                                ex.getResponseHeaders().add("Content-Type", "text/html");
                            }
                            ex.sendResponseHeaders(200, 0);
                            out.write(bytes);
                            break;
                    }

                }

                out.flush();
                ex.close();
            } catch (Exception e) {
                e.printStackTrace();
                out.write(HTML.getPage(Page.notfound).getBytes());
            }
        } else {
            try {
                final String method = ex.getRequestMethod();
                switch (method) {
                    case "HEAD":

                        ex.getResponseHeaders().add("X-Powered-by", "Capture Club HAIDIE::EMPATHS Platform");
                        ex.sendResponseHeaders(0, 0);
                        break;

                    case "OPTIONS":
                        ex.getResponseHeaders().add("X-Powered-by", "Capture Club HAIDIE::EMPATHS Platform");
                        break;

                    default:
                        ex.getResponseHeaders().add("message", "I don't understand the method by which you're asking me questions.  " + method + " is not a valid method for this system. ");

                        break;
                }

                out.flush();
                ex.close();
            } catch (Exception e) {

            }
        }

    }

    private boolean isValidRequestMethod(HttpExchange ex) {
        String method = ex.getRequestMethod();
        String[] methods = {"GET", "PUT", "POST", "DELETE"};
        for (int i = 0; i < methods.length; i++) {
            if (method.equals(methods[i])) {
                return true;
            }
        }
        return false;
    }

    private String headersAsString(Headers h) {
        String result = "";
        String key;
        List<String> value;

        Iterator<String> iter = h.keySet().iterator();
        while (iter.hasNext()) {
            key = iter.next();
            value = h.get(key);
            // result += header.toString() + "\n";
            result += key + ":" + String.join(",", value) + "\n";
        }
        return result;
    }
}
