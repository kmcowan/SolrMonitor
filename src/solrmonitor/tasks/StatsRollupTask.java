/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solrmonitor.tasks;

import java.util.Calendar;
import java.util.Iterator;
import java.util.TimerTask;
import org.json.JSONObject;
import solrmonitor.SolrMonitor;
import solrmonitor.util.Log;
import solrmonitor.util.Log.Months;
import solrmonitor.tasks.SolrPingTimerTask.Status;

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

        JSONObject stats = Log.getLogRollup();

        String[] daysToMonitor = SolrMonitor.getProperties().getProperty("stats.days.to.monitor").split(",");
        int count = 0;
        String key = "";

        JSONObject data = null;
        int year = Calendar.getInstance().get(Calendar.YEAR);
        for (int m = 0; m < Months.values().length; m++) {
            data = new JSONObject();
            Months month = Months.values()[m];
            double monthTotal = 0;
            double monthSuccess = 0;
            double monthFailure = 0;
            double monthRollup = 0;

            JSONObject days = stats.getJSONObject(month.name());
            Iterator<String> keys = days.keys();
            while (keys.hasNext()) {

                double dayTotal = 0;
                double daySuccess = 0;
                double dayFailure = 0;
                double dayRollup = 0;
                String tkey = keys.next();
                int day = Integer.parseInt(tkey.substring(tkey.indexOf("_"), tkey.length()));

                JSONObject hours = days.getJSONObject(tkey);
                Iterator<String> hourKeys = hours.keys();
                JSONObject dayData = new JSONObject();

                while (hourKeys.hasNext()) {
                    String hourKey = hourKeys.next();
                    JSONObject hourData = hours.getJSONObject(hourKey);
                    JSONObject tdata = new JSONObject();
                    tdata.put("raw", hours);
                    tdata.put("tkey", tkey);
                    tdata.put("rollup", getRollupTotal(hourData));

                    int hour = Integer.parseInt(hourKey.substring(hourKey.indexOf("_"), hourKey.length()));
                    if ((isDayToMonitor(daysToMonitor, getDayOfWeek(year, month.ordinal(), day))
                            && (hour >= startHour && hour <= stopHour)) || startHour < 0) {
                        dayTotal = dayTotal + tdata.getJSONObject("rollup").getDouble("total");
                        daySuccess = daySuccess + tdata.getJSONObject("rollup").getDouble("success");
                        dayFailure = dayFailure + tdata.getJSONObject("rollup").getDouble("failure");

                    }
                }
                dayRollup = Math.round((dayTotal / daySuccess) * 100);
                dayData.put("rollup", dayRollup);
                dayData.put("success", daySuccess);
                dayData.put("failure", dayFailure);

                monthTotal = monthTotal + dayTotal;
                monthSuccess = monthSuccess + daySuccess;
                monthFailure = monthFailure + dayFailure;
                // data.put(tkey, tdata);
            }

            if (m > 0) {
                key += "-";
            }
            key += month.name();
            count++;
            if (count == quantity) {
                json.put(key, data);
                data = new JSONObject();
                key = "";
                count = 0;
            }
            monthRollup = Math.round((monthSuccess / monthTotal) * 100);
            data.put("rollup", monthRollup);
        }
        if (data != null) {
            json.put(key, data);
        }
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
        JSONObject result = new JSONObject();
        int success = 0;
        int failure = 0;
        int total = 0;

        result.put("total", total);
        result.put("success", success);
        result.put("failure", failure);

        double uptime = Math.round((success / total) * 100);
        double downtime = Math.round((failure / total) * 100);

        result.put("uptime", uptime);
        result.put("downtime", downtime);

        return result;
    }

    private static String getDayOfWeek(int year, int month, int day) {
        now.set(year, month, day);
        return DAYS_OF_WEEK[now.get(Calendar.DAY_OF_WEEK) - 1];
    }

    public static void main(String[] args) {
        System.out.println("Day of week: " + getDayOfWeek(2018, 6, 18));
    }

}
