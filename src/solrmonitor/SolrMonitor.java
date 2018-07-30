/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solrmonitor;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import solrmonitor.tasks.APICheckTask;
import solrmonitor.tasks.SolrPingTimerTask;
import solrmonitor.tasks.StatsRollupTask;
import solrmonitor.tasks.StatsUpdateTask;
import solrmonitor.util.Log;
import solrmonitor.web.WebServer;

/**
 *
 * @author kevin
 */
public class SolrMonitor {

    private static final String PROPS_FILE_NAME = "solr_monitor.properties";
    private static final Properties PROPERTIES = new Properties();
    private static boolean isLoaded = false;
    private static boolean isReportingMode = false;
    private static boolean isRunningTermMode = true;// running mode by default/ 
    private static boolean isRunningWebMode = false;
    private static boolean isPrintingHelp = false;
    private static boolean isReInit = false;
    private static final long START_DATE = System.currentTimeMillis();
    private static final Calendar START_DATE_CAL = Calendar.getInstance();
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        setFlags(args);
        START_DATE_CAL.setTime(new Date(START_DATE));
        if (isRunningTermMode) {

            initTERM(args);
        } else if (isRunningWebMode) {
            initWEB(args);
        } else if (isReportingMode) {
            initREPORT(args);
        } else if (isPrintingHelp) {
            printHelp(args);
        } else if(isReInit){
            Log.reinit();
        }
    }

    private static void printHelp(String[] args) {
        Log.log(SolrMonitor.class, "Start in HELP Mode...");
        System.out.println("Welcome to the Solr Monitor. ");

        System.out.println("An Open Source Offering by Kevin Cowan ");
    }

    private static void initREPORT(String[] args) {
        Log.log(SolrMonitor.class, "Start in REPORT Mode...");
        StatsRollupTask rollup = new StatsRollupTask();
        rollup.run();
    }

    private static void initWEB(String[] args) {
        Log.log(SolrMonitor.class, "Start in WEB Mode...");
        WebServer.main(args);
        initTERM(args);
    }

    private static void initTERM(String[] args) {
        Log.log(SolrMonitor.class, "Start in TERM Mode...");
        SolrPingTimerTask task = new SolrPingTimerTask();
        StatsUpdateTask statsTask = new StatsUpdateTask();
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(statsTask, 500, 5000);
     

        try {

            PROPERTIES.load(new FileReader(new File(PROPS_FILE_NAME)));
            isLoaded = true;
            String hosts = PROPERTIES.getProperty("solr.hosts.to.monitor");
            ArrayList<Runnable> tasks = new ArrayList<>();
            if (hosts.contains(",")) {
                Log.log(SolrMonitor.class, "RUNNING MULTIPLE MONITOR...");
                String[] solrHosts = hosts.split(",");
                for (int j = 0; j < solrHosts.length; j++) {
                    SolrPingTimerTask ttask = new SolrPingTimerTask(solrHosts[j]);
                    tasks.add(ttask);
                }
            } else {
                Log.log(SolrMonitor.class, "RUNNING SINGLE MONITOR...");
                tasks.add(task);
            }

            if (PROPERTIES.getProperty("api.monitor.enabled").equals("true")) {
                Log.log(SolrMonitor.class, "API MONITOR ENABLED...");
                APICheckTask apitask = new APICheckTask();
                tasks.add(apitask);
            }

            long loopTime = Long.parseLong(PROPERTIES.getProperty("monitor.loop.time"));
            while (true) {
                Log.log(SolrMonitor.class, "Monitor Loop Run...");
                for (int i = 0; i < tasks.size(); i++) {
                    //task.run();
                    run(tasks.get(i));
                }
                Thread.sleep(loopTime);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            initShutdownHook();
        }
    }

    private static void setFlags(String[] args) {
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                final String arg = args[i];
                switch (arg) {
                    case "-report":
                        isReportingMode = true;
                        isRunningTermMode = false;
                        break;

                    case "-server":
                        isRunningWebMode = true;
                        isRunningTermMode = false;
                        break;

                    case "-help":
                        isPrintingHelp = true;
                        isRunningTermMode = false;
                        break;
                        
                     case "-reinit":
                      
                        isRunningTermMode = false;
                        isReInit = true;
                        break;
                }
            }
        }
    }

    private static void initShutdownHook() {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    Log.log("Shutdown Hook is running !");
                    Log.shutdown();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static synchronized void run(Runnable task) {
       // task.run();
        EXECUTOR.execute(task);
    }

    public static Properties getProperties() {
        if (!isLoaded) {
            try {
                PROPERTIES.load(new FileReader(new File(PROPS_FILE_NAME)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return PROPERTIES;
    }

    /**
     * @return the START_DATE
     */
    public static long getSTART_DATE() {
        return START_DATE;
    }

    /**
     * @return the START_DATE_CAL
     */
    public static Calendar getSTART_DATE_CAL() {
        return START_DATE_CAL;
    }

}
