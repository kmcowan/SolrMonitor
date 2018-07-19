/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solrmonitor.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.swing.JTextPane;
import javax.swing.text.StyledDocument;
import org.json.JSONObject;
import solrmonitor.SolrMonitor;
import solrmonitor.tasks.SolrPingTimerTask;
import solrmonitor.tasks.SolrPingTimerTask.Status;

/**
 *
 * @author kevin
 */
public class Log {

    private static JTextPane textPane = null;
    private static StyledDocument doc = null;
    private static boolean loggingEnabled = true;
    private static final String ROLLUP_FILE_NAME = SolrMonitor.getProperties().getProperty("stats.file");
    private static final String FILE_NAME = SolrMonitor.getProperties().getProperty("stats.playback.log.file");
    private static FileWriter fw = null;
    private static BufferedWriter bw = null;
    private static PrintWriter out = null;
    private static final File logFile = new File(FILE_NAME);
    private static final File logRollupFile = new File(ROLLUP_FILE_NAME);
    private static final long saveEveryXTimes = Long.parseLong(SolrMonitor.getProperties().getProperty("stats.file.save.every"));
    private static long currCount = 0;
    private static JSONObject logRollup = null;

    private static void logMessage(String message) {
        if (loggingEnabled) {
            System.out.println(message);
            if (textPane != null) {
                try {
                    doc.insertString(doc.getLength(), "\n" + message, null);
                } catch (Exception e) {
                }
            }
        }
    }

    public static void log(String message) {
        logMessage(message);
    }

    public static void log(Class clsName, String message) {
        logMessage("[" + clsName.getSimpleName() + "] " + message);
    }

    public static boolean isPlaybackLoggingEnabled() {
        String enabled = SolrMonitor.getProperties().getProperty("stats.playback.enabled");
        if (enabled.trim().equals("true")) {
            return true;
        }
        return false;
    }

    /**
     * @return the textPane
     */
    public static JTextPane getTextPane() {
        return textPane;
    }

    /**
     * @param aTextPane the textPane to set
     */
    public static void setTextPane(JTextPane aTextPane) {
        textPane = aTextPane;
        doc = textPane.getStyledDocument();
    }

    /**
     * @return the loggingEnabled
     */
    public static boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    /**
     * @param aLoggingEnabled the loggingEnabled to set
     */
    public static void setLoggingEnabled(boolean aLoggingEnabled) {
        loggingEnabled = aLoggingEnabled;
    }

    public static void log(JSONObject obj) {
        if (isPlaybackLoggingEnabled()) {
            try {
                if (fw == null) {
                    initLogger();
                }
                out.println(obj.toString());

            } catch (Exception e) {
                //exception handling left as an exercise for the reader
                e.printStackTrace();
            }
        }
    }

    public static void logRollup(SolrPingTimerTask.Status status, long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(time));

        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int year = cal.get(Calendar.YEAR);
        Log.log(" month: " + month + " day: " + day + " hour: " + hour);

        if (logRollup == null) {
            initJSONObject();
        }
        if (logRollup.getJSONObject(Months.values()[month].name()) != null) {

            if (logRollup.getJSONObject(Months.values()[month].name()).getJSONObject("day_" + day) != null) {

                if (logRollup.getJSONObject(Months.values()[month].name()).getJSONObject("day_" + day).getJSONObject("hour_" + hour) != null) {
                    int oldval = logRollup.getJSONObject(Months.values()[month].name()).getJSONObject("day_" + day).getJSONObject("hour_" + hour).getInt(status.name());
                    Log.log("Old value [" + status.name() + "]: " + oldval);
                    oldval++;
                    logRollup.getJSONObject(Months.values()[month].name()).getJSONObject("day_" + day).getJSONObject("hour_" + hour).put(status.name(), oldval);

                    currCount++;
                    if (currCount >= saveEveryXTimes) {
                        currCount = 0;
                        Log.log(Log.class, "SAVING...");
                        // Check start time to see if there is a year rollover, and save the old file
                       
                        Utils.writeBytesToFile(logRollupFile.getAbsolutePath(), logRollup.toString());
                        
                         if(year > SolrMonitor.getSTART_DATE_CAL().get(Calendar.YEAR)){
                            logRollupFile.renameTo(new File(year+"_"+ROLLUP_FILE_NAME));
                            initJSONObject();
                        }
                         
                    } else {
                        Log.log(Log.class, "currCount: " + currCount + " save x times: " + saveEveryXTimes);
                    }
                } else {
                    Log.log("Hour " + hour + " was null");
                }
            } else {
                Log.log("Day " + day + " was null.. ");
            }

        } else {
            Log.log("Month " + Months.values()[month].name() + " was null.. ");
        }

    }

    public static void reinit() {
        try {
            logRollupFile.delete();
            initJSONObject();

        } catch (Exception e) {

        }
    }

    private static void initLogger() throws Exception {
        if (isPlaybackLoggingEnabled()) {
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            fw = new FileWriter(logFile, true);
            bw = new BufferedWriter(fw);
            out = new PrintWriter(bw);
        }

        if (logRollupFile.exists()) {
            String historyStr = Utils.streamToString(new FileInputStream(logRollupFile));
            if (historyStr.equals("")) {
                Log.log("Load history from file...");
                logRollup = new JSONObject(historyStr);
            }

        } else {
            logRollupFile.createNewFile();
            logRollup = new JSONObject();
            initJSONObject();

        }

    }

    private synchronized static void initJSONObject() {
        try {
            if (!logRollupFile.exists()) {

                logRollupFile.createNewFile();

            
            Calendar cal = Calendar.getInstance();
            int iDay = 1;
            logRollup = new JSONObject();
            // months 
            for (int m = 0; m < Months.values().length; m++) {
                int iYear = cal.get(Calendar.YEAR);
                //  int iMonth = m; // 1 (months begin with 0)
                JSONObject month_data = new JSONObject();
                Months month = Months.values()[m];

// Create a calendar object and set year and month
                Calendar tempcal = new GregorianCalendar(iYear, m, iDay);

// Get the number of days in that month
                int daysInMonth = tempcal.getActualMaximum(Calendar.DAY_OF_MONTH);
                // days
                for (int d = 1; d < daysInMonth; d++) {
                    JSONObject day_data = new JSONObject();

                    // hours
                    for (int h = 0; h < 24; h++) {
                        JSONObject hour_data = new JSONObject();
                        Status[] stati = SolrPingTimerTask.Status.values();
                        // stati
                        for (int i = 0; i < stati.length; i++) {
                            hour_data.put(stati[i].name(), 0);
                        }
                        day_data.put("hour_" + h, hour_data);
                    }
                    month_data.put("day_" + d, day_data);
                }
                logRollup.put(month.name(), month_data);
            }

            Utils.writeBytesToFile(logRollupFile.getAbsolutePath(), logRollup.toString());
            } else {
                Log.log("Stats File Exists...");
                 String historyStr = Utils.streamToString(new FileInputStream(logRollupFile));
                 logRollup = new JSONObject(historyStr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.log(Log.class, "Init JSON OK...");
    }

    public static void main(String[] args) {
        try {
            logRollupFile.createNewFile();
            logRollup = new JSONObject();
            initJSONObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @return the logRollup
     */
    public static JSONObject getLogRollup() {
        if (logRollup == null) {
            initJSONObject();
        }
        return logRollup;
    }

    public static enum Months {
        JANUARY,
        FEBRUARY,
        MARCH,
        APRIL,
        MAY,
        JUNE,
        JULY,
        AUGUST,
        SEPTEMBER,
        OCTOBER,
        NOVEMBER,
        DECEMBER
    }

    public static void shutdown() {

        try {

            if (out != null && isPlaybackLoggingEnabled()) {
                out.close();
                bw.close();
                fw.close();
            }

        } catch (Exception e) {
        }
    }
}
