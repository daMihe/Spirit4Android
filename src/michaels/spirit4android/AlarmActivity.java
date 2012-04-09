package michaels.spirit4android;

import java.util.Calendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AlarmActivity extends Activity {
	
	private WakeLock wake_lock;
	private FHSSchedule schedule;
	private SharedPreferences settings;
	private static Intent alarm_service;
	
	public void onCreate(android.os.Bundle savedInstanceState){
		super.onCreate(savedInstanceState);		
		this.setContentView(R.layout.alarm);
		PowerManager ps = (PowerManager) this.getSystemService(POWER_SERVICE);
		wake_lock = ps.newWakeLock(PowerManager.FULL_WAKE_LOCK, "Spirit4Android: Wecker");
		settings = this.getSharedPreferences("s4apref", Context.MODE_WORLD_READABLE);
		
		if(settings.getLong("alarmtimeBeforeEvent", -1) < 0){
			this.finish();
			return;
		}
		
		// Lets play some music!
		if(alarm_service == null)
			alarm_service = new Intent(this,AlarmService.class);
		this.startService(alarm_service);
		
		// get schedule and put eventinfo to the view
		try {
			schedule = new FHSSchedule(new JSONArray(settings.getString("scheduleJSON", "[]")),settings);
		} catch (JSONException e) {}		
		TextView event_info = (TextView) this.findViewById(R.id.alarm_eventinfo);
		JSONObject event = schedule.getNextEvent();
		JSONObject eventPlace = event.optJSONObject("appointment").optJSONObject("location").optJSONObject("place");
		Calendar event_time = schedule.getNextCalendar(event);
		event_info.setText(event.optString("titleShort")+"\n"+event.optString("eventType")+"\nin "+eventPlace.optString("building")+eventPlace.optString("room")+" um "+String.format("%02d:%02d", event_time.get(Calendar.HOUR_OF_DAY),event_time.get(Calendar.MINUTE)));
		
		// Set penetrant mode
		if(settings.getBoolean("alarmPenetrantMode", true))
			generateQuestion();
		else
			this.findViewById(R.id.alarm_penetrant_section).setVisibility(View.GONE);
	}
	
	public void onResume(){
		wake_lock.acquire();
		super.onResume();
	}
	
	public void generateQuestion(){
		int left = (int)((Math.random()*20)-10), right = (int)((Math.random()*20)-10);
		boolean plus = Math.random() >= 0.5;
		((TextView)this.findViewById(R.id.alarm_question)).setText(left+" "+((plus && right >= 0) || (!plus && right < 0) ? "+" : "-")+" "+Math.abs(right)+" =");
		LinearLayout answer_container = ((LinearLayout)this.findViewById(R.id.alarm_answers));
		int count = answer_container.getChildCount();
		int truth = (int)Math.round(Math.random()*(count-1));
		for(int i = 0; i<count; i++){
			Button current_button = (Button)answer_container.getChildAt(i);
			if(truth == i){
				current_button.setText((plus ? left + right : left - right)+"");
				current_button.setOnClickListener(new OnClickListener() {
					
					public void onClick(View v) {
						LinearLayout parent = (LinearLayout)v.getParent();
						for(int i=0; i<parent.getChildCount(); i++)
							((Button)parent.getChildAt(i)).setEnabled(false);
						AlarmActivity.this.stopService(alarm_service);
					}
				});
			} else {
				current_button.setText((int)(Math.random()*40-20)+"");
				current_button.setOnClickListener(new OnClickListener() {
					
					public void onClick(View v) {
						AlarmActivity.this.generateQuestion();
					}
				});
			}
		}
		
	}
	
	public void onPause(){
		super.onPause();
		wake_lock.release();
	}
	
	public void onStop(){
		if(!settings.getBoolean("alarmPenetrantMode", true))
			this.stopService(alarm_service);
		mainActivity.setAlarm(this,false);
		super.onStop();
	}
}
