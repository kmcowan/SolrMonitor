/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solrmonitor.web;

import java.util.Calendar;
import java.util.Date;
import org.json.JSONObject;
import solrmonitor.tasks.StatsRollupTask;
import solrmonitor.util.JsonUtils;
import static solrmonitor.util.JsonUtils.DATE_FORMAT;
import solrmonitor.util.Log;
import solrmonitor.util.Log.Months;

/**
 *
 * @author kevin
 */
public class ActionProcessor {

    /**
     * @todo This needs to be moves to a class and refactored so display is
     * dynamic.
     * @return
     */
    private static final StatsRollupTask rollup = new StatsRollupTask();
    
    public static String getIndexContent() {
        return "<h2>Welcome to the Solr/Fusion Status Monitor and Reporting Tool</h2>";
    }
    
    public static String getMonthlyContent(){
        String content = "";
        Calendar cal = Calendar.getInstance();
           content += "<div>Today is: " + getDayOfWeek(cal)+"</div>";
           Months month = Months.values()[cal.get(Calendar.MONDAY)];
           JSONObject json = rollup.doRollupOneMonth(month, JsonUtils.clone(Log.getLogRollup()));
           content += JsonUtils.jsonObjectToHtmlList(json);
           
        return content;
    }
    
    private static String getDayOfWeek(Calendar cal){
         return StatsRollupTask.DAYS_OF_WEEK[cal.get(Calendar.DAY_OF_WEEK) - 1];
    }

    public static String getStatusContent() {
        String content = "";
        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        Date now = cal.getTime();
        cal.add(Calendar.HOUR, -1);
        Date oneHour = cal.getTime();
        cal.add(Calendar.HOUR, -1);
        Date twoHour = cal.getTime();

        JSONObject json = Log.getLogRollup();
        JSONObject thisDay = json.getJSONObject(Log.Months.values()[month].name()).getJSONObject("day_" + day);
        JSONObject thisHour = thisDay.getJSONObject("hour_" + hour);
        JSONObject lastHour = null;
        JSONObject twoHoursBack = null;
        if (hour > 1) {
            lastHour = thisDay.getJSONObject("hour_" + (hour - 1));
        }

        if (hour > 2) {
            twoHoursBack = thisDay.getJSONObject("hour_" + (hour - 2));
        }

        content = "<div>";
        content += "<table cellpadding=2 cellspacing=4 border=0><tbody><tr>";
        if (twoHoursBack != null) {
            content += "<td><div>" + DATE_FORMAT.format(twoHour) + "</div><div>" + JsonUtils.jsonObjectToHtmlList(twoHoursBack) + "</div></td>";
        }

        if (lastHour != null) {
            content += "<td><div>" + DATE_FORMAT.format(oneHour) + "</div><div>" + JsonUtils.jsonObjectToHtmlList(lastHour) + "</div></td>";
        }

        content += "<td><div>" + DATE_FORMAT.format(now) + "</div><div>" + JsonUtils.jsonObjectToHtmlList(thisHour) + "</div></td>";

        content += "</tr></tbody></table></div>";

        return content;
    }

}
