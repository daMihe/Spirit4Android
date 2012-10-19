package michaels.spirit4android;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class HSWidgetProvider extends AppWidgetProvider {
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		FHSSchedule schedule = new FHSSchedule(context);
		Calendar search_cal = schedule.getNextCalendar(schedule.getNextEvent());
		search_cal.add(Calendar.SECOND, -1);
		FHSSchedule.Event events[] = schedule.getDaysComingEvents(search_cal);
		int day = (int)(events[0].time/(24*60*60)-1);
		if(mainActivity.saveFile == null)
			mainActivity.saveFile = context.getSharedPreferences("s4apref",context.MODE_WORLD_READABLE);
		String eventList = context.getResources().getStringArray(R.array.LANG_WEEKDAYS_SHORT)[day]+(mainActivity.saveFile.getLong("alarmtimeBeforeEvent", -1) < 0 ? "" : " ("+context.getString(R.string.LANG_ALARMENABLED_SHORT)+")");
		for(FHSSchedule.Event e : events){
			short time_hours = (short) ((e.time-24*60*60*(1+day))/(60*60));
			short time_minutes = (short) ((e.time-24*3600*(1+day)-time_hours*3600)/60);
			boolean ses = mainActivity.saveFile.getBoolean("shortEventDisplay", false);
			eventList += String.format("\n%02d:%02d,%s: %s (%s)", time_hours, time_minutes, e.room, e.title, context.getString(e.type == FHSSchedule.EVENT_LECTURE ? (ses ? R.string.LANG_LECTURE_S : R.string.LANG_LECTURE) : (ses ? R.string.LANG_EXERCISE_S : R.string.LANG_EXERCISE)));
		}	
		
		for(int cid:appWidgetIds){
			RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.hswidget);
			rv.setTextViewText(R.id.hsw_label, eventList);
			Intent i = new Intent(context,HSWidgetProvider.class);
			i.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
			i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
			am.set(AlarmManager.RTC, System.currentTimeMillis()+10000, PendingIntent.getBroadcast(context,0,i,PendingIntent.FLAG_ONE_SHOT));
			rv.setOnClickPendingIntent(R.id.hsw_label, PendingIntent.getActivity(context,0,new Intent(context,mainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT));
			appWidgetManager.updateAppWidget(cid, rv);
		}
	}
}
