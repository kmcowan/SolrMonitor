/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solrmonitor;

import club.capture.solr.monitor.encryption.EncryptionFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.StringReader;
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
 
import solrmonitor.util.Utils;
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
    private static boolean isEncrypting = false;
    private static boolean isDecrypting = false;
    private static boolean isReInit = false;
    private static final long START_DATE = System.currentTimeMillis();
    private static final Calendar START_DATE_CAL = Calendar.getInstance();
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(10);
    private static boolean USING_ENCRYPTION = false;

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
        } else if (isEncrypting) {
            encryptProperties();
        } else if (isDecrypting) {
            decryptProperties();
        } else if (isReInit) {
            Log.reinit();
        }
    }

    private static void printHelp(String[] args) {
        Log.log(SolrMonitor.class, "Start in HELP Mode...");
        System.out.println("Welcome to the Solr Monitor. ");

        System.out.println("An Open Source Offering by Capture Club, LLC ");
        System.out.println("Options:  ");
        System.out.println("****************************** ");
        System.out.println(" ");
        System.out.println("\t -report: Generates a Report from current statistics. ");
        System.out.println("\t -server: Stars Solr/Fusion Monitor in Web Server Mode");
        System.out.println("\t -help: Prints this dialog. ");
        System.out.println("\t -encrypt: Encrypts your propeties file");
        System.out.println("\t -decrypt: Decrypts your properties file");
        System.out.println("\t -reinit: creates a new JSON statistics DB. ");
        System.out.println(" ");

        System.out.println("****************************** ");

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

            getProperties();
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

                    case "-encrypt":
                        isRunningTermMode = false;
                        isEncrypting = true;
                        break;

                    case "-decrypt":
                        isRunningTermMode = false;
                        isDecrypting = true;
                        break;

                    case "-reinit":

                        isRunningTermMode = false;
                        isReInit = true;
                        break;
                }
            }
        }
    }

    private static void loadProperties() throws Exception {
        if (USING_ENCRYPTION) {
            String encStr = Utils.streamToString(new FileInputStream(new File(PROPS_FILE_NAME)));
            String decStr = EncryptionFactory.decrypt(encStr.getBytes());
            PROPERTIES.load(new StringReader(decStr));
        }
    }

    private static void encryptProperties() {
        try {
            Log.log("Begin Property encrytpion...");
            String strToEnc = Utils.streamToString(new FileInputStream(new File(PROPS_FILE_NAME)));
            byte[] encStr = EncryptionFactory.encrypt(strToEnc);
            Utils.writeBytesToFile(PROPS_FILE_NAME, new String(encStr));

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Log.log("END Property encrytpion...");
            Log.log("Please note that you will now have to decrypt your properties file in order to edit. ");
        }

    }

    private static void decryptProperties() {
        try {
           Log.log("Begin Property decrytpion...");
            String strToEnc = Utils.streamToString(new FileInputStream(new File(PROPS_FILE_NAME)));
            String encStr = EncryptionFactory.decrypt(strToEnc.getBytes());
            System.out.println("Encrypted..."+encStr);
             writeBytesToFile(PROPS_FILE_NAME, encStr);
             

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("END Property decrytpion...");
            System.out.println("You properties file is now human readable!  ");
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
                if(isPropertiesEncrypted()){
                    USING_ENCRYPTION = true;
                    System.out.println("Properties are encrypted...");
                }
                if (USING_ENCRYPTION) {
                    loadProperties();
                } else {
                    PROPERTIES.load(new FileReader(new File(PROPS_FILE_NAME)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return PROPERTIES;
    }
    
    private static boolean isPropertiesEncrypted() throws Exception{
        String testEnc = Utils.streamToString(new FileInputStream(new File(PROPS_FILE_NAME)));
        if(!testEnc.contains("stats.file=")){
            return true;
        }
        return false;
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
    
     public static String writeBytesToFile(String filePath, String content) {
        String path = "";
        try {
            File file = new File(filePath);
            FileWriter fw = new FileWriter(file);
            System.out.println( "write file: " + filePath);
            fw.write(content);
            //  IOUtils.write(content, fw);
            //  IOUtils.write
            path = file.getAbsolutePath();
            fw.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }

}
