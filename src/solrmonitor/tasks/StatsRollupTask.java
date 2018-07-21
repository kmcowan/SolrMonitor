/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solrmonitor.tasks;

import java.io.File;
import java.util.Calendar;
import java.util.Iterator;
import java.util.TimerTask;
import org.json.JSONObject;
import solrmonitor.SolrMonitor;
import solrmonitor.util.Log;
import solrmonitor.util.Log.Months;
import solrmonitor.tasks.SolrPingTimerTask.Status;
import solrmonitor.util.JsonUtils;
import solrmonitor.util.Utils;

/**
 *
 * @author kevin
 */
public class StatsRollupTask extends TimerTask implements Runnable {

    private static final Status[] OKAY = {Status.API_OKAY, Status.OK, Status.CLUSTER_STATE_OKAY, Status.ZOOKEEPER_OK};
    private static final Status[] NOT_OKAY = {Status.API_CLIENT_ERROR,
        Status.API_DOWN,
        Status.API_SERVER_ERROR,
        Status.CLUSTER_STATE_CONTAINS_INACTIVE_REPLICAS,
        Status.CLUSTER_STATE_REPLICA_DOWN,
        Status.CLUSTER_STATE_RECOVERY,
        Status.EXCEPTION_400,
        Status.EXCEPTION_500,
        Status.NO_SOLR,
        Status.NO_ZOOKEEPER,
        Status.QUERY_FAIL,
        Status.SOLR_DOWN,
        Status.SOLR_ERROR,
        Status.ZOOKEEPER_ERROR,
        Status.ZOOKEEPER_TIMEOUT};

    private static final String[] DAYS_OF_WEEK = new String[]{"Sunday", "Monday", "Tuesday", "Wednesday", "Thusday",
        "Friday", "Saturday"};

    private static final Calendar now = Calendar.getInstance();

    @Override
    public void run() {

        JSONObject result = null;
        final String period = SolrMonitor.getProperties().getProperty("stats.rollup.period");
        int quantity = Integer.parseInt(SolrMonitor.getProperties().getProperty("stats.rollup.period.quantity"));
        int startHour = Integer.parseInt(SolrMonitor.getProperties().getProperty("stats.availability.start.hour"));
        int stopHour = Integer.parseInt(SolrMonitor.getProperties().getProperty("stats.availability.stop.hour"));

        switch (period) {
            case "daily":
                result = rollupDAILY(quantity, startHour, stopHour);
                break;

            case "weekly":
                result = rollupWEEKLY(quantity, startHour, stopHour);
                break;

            case "monthly":
                result = rollupMONTHLY(quantity, startHour, stopHour);
                break;

            case "yearly":
                result = rollupYEARLY(quantity, startHour, stopHour);
                break;

            default:
                result = rollupMONTHLY(quantity, startHour, stopHour);
                break;
        }

        if (result != null) {
            try {
                File rollup = new File("rollup.json");
                if (!rollup.exists()) {
                    rollup.createNewFile();
                }
                Utils.writeBytesToFile(rollup.getAbsolutePath(), result.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private boolean isDayToMonitor(String[] days, String targetDay) {
        for (int d = 0; d < days.length; d++) {
            if (days[d].equals(targetDay)) {
                return true;
            }
        }
        return false;
    }

    private JSONObject rollupDAILY(int quantity, int startHour, int stopHour) {
        JSONObject json = new JSONObject();
        JSONObject stats = Log.getLogRollup();
        for (int m = 0; m < Months.values().length; m++) {
            Months month = Months.values()[m];
        }
        return json;
    }

    private JSONObject rollupWEEKLY(int quantity, int startHour, int stopHour) {
        JSONObject json = new JSONObject();
        JSONObject stats = Log.getLogRollup();
        for (int m = 0; m < Months.values().length; m++) {
            Months month = Months.values()[m];
        }
        return json;
    }

    private JSONObject rollupMONTHLY(int quantity, int startHour, int stopHour) {
        JSONObject json = new JSONObject();

        JSONObject stats = JsonUtils.clone(Log.getLogRollup());

        String[] daysToMonitor = SolrMonitor.getProperties().getProperty("stats.days.to.monitor").split(",");
        int count = 0;
        String key = "";

        JSONObject data = new JSONObject();
        int year = Calendar.getInstance().get(Calendar.YEAR);
        for (int m = 0; m < Months.values().length; m++) {

            JSONObject monthData = doRollupOneMonth(Months.values()[m], stats);//new JSONObject();
            
            if (m > 0) {
                key += "-";
            }

            if (count == quantity) {
                json.put(key, data);
                data = new JSONObject();
                key = Months.values()[m].name();
                count = 0;

            } else {
                key += Months.values()[m].name();

            }
            count++;

            data.put(Months.values()[m].name(), monthData);
        }

        json.put(key, data);

        return json;
    }

    private JSONObject rollupYEARLY(int quantity, int startHour, int stopHour) {
        JSONObject json = new JSONObject();
        for (int m = 0; m < Months.values().length; m++) {
            Months month = Months.values()[m];
        }
        return json;
    }

    private JSONObject getRollupTotal(JSONObject hourData) {
        //  JSONObject result = new JSONObject();
        hourData = doHourlyRollup(hourData);
        double success = hourData.getDouble("success");
      //  double failure = hourData.getDouble("failure");
        double total = hourData.getDouble("total");

        //    hourData.put("total", total);
        //    hourData.put("success", success);
        //    hourData.put("failure", failure);
        if (total > 0) {
            hourData.put("rollup", Math.round((success / total) * 100));
/*
            if (success > 0) {
                double uptime = Math.round((success / total) * 100);
                hourData.put("uptime", uptime);
            } else {
                hourData.put("uptime", 0);
            }

            if (failure > 0) {
                double downtime = Math.round((failure / total) * 100);
                hourData.put("downtime", downtime);
            } else {
                hourData.put("downtime", 0);
            }
            */
        } else {
          //  hourData.put("uptime", 0);
          //  hourData.put("downtime", 0);
        }
    

        return hourData;
    }

    private JSONObject doHourlyRollup(JSONObject hourData) {
        Iterator<String> iter = hourData.keys();
        String key;
        long value = 0;
        long success = 0;
        long failure = 0;
        long total = 0;

        while (iter.hasNext()) {
            key = iter.next();
            value = hourData.getLong(key);
            if (isOkayKey(key)) {
                success = success + value;
            } else {
                failure = failure + value;
            }

            total = total + value;
        }

        hourData.put("success", success);
        hourData.put("failure", failure);
        hourData.put("total", total);
        hourData.put("rollup", 0);

        if (total > 0) {
            hourData.put("rollup", Math.round((success / total) * 100));
        }

        return hourData;
    }

    private boolean isOkayKey(String key) {
        for (int i = 0; i < OKAY.length; i++) {
            if (OKAY[i].name().equals(key)) {
                return true;
            }
        }

        return false;
    }

    private boolean isOkayNotKey(String key) {
        for (int i = 0; i < NOT_OKAY.length; i++) {
            if (NOT_OKAY[i].name().equals(key)) {
                return true;
            }
        }

        return false;
    }

    private Status getStatusKey(String key) {
        for (int s = 0; s < Status.values().length; s++) {
            if (Status.values()[s].name().equals(key)) {
                return Status.values()[s];
            }
        }

        return Status.OK;
    }

    private static String getDayOfWeek(int year, int month, int day) {
        now.set(year, month, day);
        return DAYS_OF_WEEK[now.get(Calendar.DAY_OF_WEEK) - 1];
    }

    protected JSONObject doRollupOneMonth(Months month, JSONObject stats) {
        JSONObject json = new JSONObject();
        double monthTotal = 0;
        double monthSuccess = 0;
        double monthFailure = 0;
        double monthRollup = 0;
        if (stats == null) {
            stats = Log.getLogRollup();
        }
        JSONObject days = stats.getJSONObject(month.name());
        Iterator<String> keys = days.keys();
        while (keys.hasNext()) {
            String dayKey = keys.next();
            if (dayKey.contains("day_")) {
                JSONObject dayData = days.getJSONObject(dayKey);
                dayData = doRollupOneDay(dayData);
                if(dayData.getDouble("total") > 0){
                monthTotal = monthTotal + dayData.getDouble("total");
                monthSuccess = monthSuccess + dayData.getDouble("success");
                monthFailure = monthFailure + dayData.getDouble("failure");
                }
            }
        }
        if (monthTotal > 0) {

            monthRollup = Math.round(monthSuccess / (monthTotal) * 100);
        }

        json.put("total", monthTotal);
        json.put("success", monthSuccess);
        json.put("failure", monthFailure);
        json.put("rollup", monthRollup);

        return json;
    }

    private JSONObject doRollupOneDay(JSONObject day) {

        Iterator<String> hourKeys = day.keys();
        JSONObject hourData;
        String hourKey;
        double dayTotal = 0;
        double daySuccess = 0;
        double dayFailure = 0;
        double dayRollup = 0;

        while (hourKeys.hasNext()) {
            hourKey = hourKeys.next();
            if (hourKey.contains("hour_")) {
                hourData = day.getJSONObject(hourKey);
                hourData = getRollupTotal(hourData);

                dayTotal = dayTotal + hourData.getDouble("total");
                daySuccess = daySuccess + hourData.getDouble("success");
                dayFailure = dayFailure + hourData.getDouble("failure");
            }

        }
        if (dayTotal > 0) {

            dayRollup = Math.round((daySuccess / (dayTotal)) * 100);
        }

        day.put("rollup", dayRollup);
        day.put("success", daySuccess);
        day.put("failure", dayFailure);
        day.put("total", dayTotal);

        return day;
    }

    public static void main(String[] args) {
        Calendar cal = Calendar.getInstance();
      //  Timer timer = new Timer();

        System.out.println("Day of week: " + getDayOfWeek(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)));
        /*    StatsRollupTask task = new StatsRollupTask();
       JSONObject json2 = task.doRollupOneMonth(Months.values()[cal.get(Calendar.MONTH)], Log.getLogRollup());
        
       File rollup = new File("test_rollup_month.json");
        Utils.writeBytesToFile(rollup.getAbsolutePath(), json2.toString());
         */
        
        StatsRollupTask task2 = new StatsRollupTask();
         task2.run();
        JSONObject json = task2.doRollupOneMonth(Months.values()[cal.get(Calendar.MONTH)], JsonUtils.clone(Log.getLogRollup()));
        File rollup = new File("test_rollup_month.json");
        Utils.writeBytesToFile(rollup.getAbsolutePath(), json.toString());

        StatsRollupTask task = new StatsRollupTask();
        JSONObject json2 = task.doRollupOneMonth(Months.values()[cal.get(Calendar.MONTH)], JsonUtils.clone(Log.getLogRollup()));

        File rollup2 = new File("test_rollup_month2.json");
        Utils.writeBytesToFile(rollup2.getAbsolutePath(), json2.toString());
        System.exit(0);

        // timer.schedule(new RollOneMonth(), 500);
    }

}

class RollOneMonth extends TimerTask implements Runnable {

    public void run() {
        Calendar cal = Calendar.getInstance();
        StatsRollupTask task = new StatsRollupTask();
        JSONObject json2 = task.doRollupOneMonth(Months.values()[cal.get(Calendar.MONTH)], Log.getLogRollup());

        File rollup = new File("test_rollup_month.json");
        Utils.writeBytesToFile(rollup.getAbsolutePath(), json2.toString());
    }
}
