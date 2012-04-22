package michaels.spirit4android;

import java.security.MessageDigest;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class mainActivity extends Activity {
	public static String USERAGENT;
	public static SharedPreferences saveFile;
	public static JSONObject newsObject = null;
	public static String preJSON = "";
	public static SQLiteDatabase database = null;
	public static long last_news_update = 0;
	boolean pause;
	boolean showCompletePlan;
	static FHSSchedule schedule;
	static TimeStreamView tsv;
	
	public void onStart(){
		super.onStart();
		
		// Setting Useragent: "Spirit4Android v<versionName> (disp <displayWidth>*<displayHeight>)"
		try {
			DisplayMetrics dm = new DisplayMetrics();
			this.getWindowManager().getDefaultDisplay().getMetrics(dm);
			PackageInfo pi = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
			USERAGENT = "Spirit4Android v"+pi.versionName+" (disp "+dm.widthPixels+"*"+dm.heightPixels+")";
		} catch(Exception e){
			USERAGENT = "Spirit4Android";
		}
		if(saveFile == null)
			saveFile = this.getSharedPreferences("s4apref",MODE_WORLD_READABLE); //lesen der Einstellungen
		if(saveFile.getInt("letzterSemesterWechsel", Calendar.getInstance().get(Calendar.MONTH)) != Calendar.getInstance().get(Calendar.MONTH) && (Calendar.getInstance().get(Calendar.MONTH) == Calendar.OCTOBER || Calendar.getInstance().get(Calendar.MONTH) == Calendar.APRIL)){
			AlertDialog.Builder ab = new AlertDialog.Builder(this);
			ab.setTitle(R.string.LANG_SEMESTERCHANGE)
				.setMessage(R.string.LANG_SEMESTERCHANGELONG)
				.setPositiveButton(R.string.LANG_SCHEDULESETTINGS, new OnClickListener(){

					public void onClick(DialogInterface arg0, int arg1) {
						Intent intent = new Intent(mainActivity.this,settingsActivity.class);
						mainActivity.this.startActivity(intent);
						Editor e = saveFile.edit();
						e.putInt("letzterSemesterWechsel", Calendar.getInstance().get(Calendar.MONTH));
						e.commit();
					}
					
				});
			ab.create().show();
		}
		
		if(!saveFile.contains("letzterSemesterWechsel")){
			Editor e = saveFile.edit();
			e.putInt("letzterSemesterWechsel", Calendar.getInstance().get(Calendar.MONTH));
			e.commit();
		}
		
		if(schedule == null){
			if(!saveFile.getString("scheduleJSON", "[]").equals("[]")){
				try {
					schedule = new FHSSchedule(new JSONArray(saveFile.getString("scheduleJSON", "[]")),saveFile);
				} catch (JSONException e) {}
			}
		}
		
		// Load Database
		openDB();
		
		// Set last_news_update for dispRefresh
		last_news_update = saveFile.getLong("lastUpdate", 0);
		
		//Oberfläche
		this.setContentView(R.layout.main);
		
		this.refreshNews();
		/*final ListView newsListe = (ListView) this.findViewById(R.id.newsListe);
		try {
			final JSONArray newsJSON = new JSONArray(saveFile.getString("newsJSON", "[]"));
			String[] dispInList = new String[newsJSON.length()];
			for(int i = 0; i<newsJSON.length(); i++){
				dispInList[newsJSON.length()-1-i] = newsJSON.getJSONObject(i).getString("subject");	
			}
			ArrayAdapter<String> aas = new ArrayAdapter<String>(this, R.layout.listelement, dispInList);
			newsListe.setAdapter(aas);
			newsListe.setOnItemClickListener(new OnItemClickListener(){

				public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
					try {
						newsObject = newsJSON.getJSONObject(newsJSON.length()-1-pos);
						Intent i = new Intent(mainActivity.this,newsActivity.class);
						mainActivity.this.startActivity(i);
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
				
			});
		} catch (JSONException e) {
			Log.e("Display Creation Error",e.getClass().getName()+": "+e.getMessage());
		}*/
		
		preJSON = mainActivity.saveFile.getString("newsJSON", "")+mainActivity.saveFile.getString("scheduleJSON", "");
		//Handler countdown = new Handler();
		//countdown.post(new dispRefresh());
		Handler updateHandler = new Handler();
		updateHandler.postDelayed(new Updater(),10000);
	}
	
	public void onPause(){
		pause = true;
		super.onPause();
	}
	
	public void onResume(){
		try {
			schedule = new FHSSchedule(new JSONArray(saveFile.getString("scheduleJSON", "[]")),saveFile);
		} catch(Exception e){}
		pause = false;
		Handler h = new Handler();
		h.post(new dispRefresh());
		super.onResume();
		
		// AlarmManager
		setAlarm(this,false);
	}
	
	public boolean onCreateOptionsMenu(Menu m){
		this.getMenuInflater().inflate(R.menu.main_menu, m);
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem mi){
		if(mi.getItemId() == R.id.settings){ // Einstellungen
			Intent EinstellungenStarten = new Intent(this,settingsActivity.class);
			this.startActivity(EinstellungenStarten);
			return true;
		} else if(mi.getItemId() == R.id.update){ // News-Update
			Editor e = saveFile.edit();
			e.putLong("lastUpdate", 0);
			e.commit();
			Toast.makeText(this, R.string.LANG_UPDATING, Toast.LENGTH_LONG).show();
			Handler updateHandler = new Handler();
			updateHandler.post(new Updater());
			return true;
		} else if(mi.getItemId() == R.id.about){ // Über
			Intent about_intent = new Intent(this,about.class);
			this.startActivity(about_intent);
			return true;
		} else if(mi.getItemId() == R.id.lastUpdate){ //letztes Update
			long lastUpdate = saveFile.getLong("lastUpdate", 0);
			Toast.makeText(this, getString(R.string.LANG_LASTSUCCESSFULLUPDATE)+
					(lastUpdate == 0 ? getString(R.string.LANG_NEVER) : DateFormat.getDateTimeInstance().format(new Date(lastUpdate))), 
					Toast.LENGTH_LONG).show();
		}
		return super.onOptionsItemSelected(mi);
	}
	
	public void openDB(){
		if(database == null)
			database = this.openOrCreateDatabase("Spirit4Android", Context.MODE_WORLD_READABLE, null);
		database.execSQL("CREATE TABLE IF NOT EXISTS news (id INTEGER NOT NULL, title TEXT, author TEXT, receivers TEXT, date INTEGER, content TEXT)");
		database.execSQL("CREATE TABLE IF NOT EXISTS schedule (time INTEGER NOT NULL, length INTEGER, title TEXT, docent TEXT, room TEXT, week INTEGER, type INTEGER)");
	}

	public static void setAlarm(Context c, boolean today_too){
		if(saveFile == null){
			saveFile = c.getSharedPreferences("s4apref",MODE_WORLD_READABLE);
			try {
				schedule = new FHSSchedule(new JSONArray(saveFile.getString("scheduleJSON", "[]")), saveFile);
			} catch (JSONException e) {}
		}
		if(saveFile.getLong("alarmtimeBeforeEvent", -1) > -1){
			PendingIntent pending_intent = PendingIntent.getActivity(c, 0, new Intent(c,AlarmActivity.class), PendingIntent.FLAG_ONE_SHOT);
			AlarmManager alarmator = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
			
			JSONObject jo = schedule.getNextEvent();
			Calendar jo_calendar = schedule.getNextCalendar(jo);
			int current_day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
			if(today_too && jo_calendar.get(Calendar.DAY_OF_YEAR) == current_day){
				JSONObject[] days_objects = schedule.getEventsAtDay(jo_calendar);
				for(char i=0; i<days_objects.length; i++){
					if(days_objects[i] != null){
						if(days_objects[i] == jo){
							alarmator.set(AlarmManager.RTC_WAKEUP,jo_calendar.getTimeInMillis()-saveFile.getLong("alarmtimeBeforeEvent", -1), pending_intent);
							return;
						} else
							break;
					}
				}
			}
			while(jo_calendar.get(Calendar.DAY_OF_YEAR) == current_day){
				jo = schedule.getNextEvent(jo_calendar);
				jo_calendar = schedule.getNextCalendar(jo);
			}
			alarmator.set(AlarmManager.RTC_WAKEUP, jo_calendar.getTimeInMillis()-saveFile.getLong("alarmtimeBeforeEvent", -1), pending_intent);
		}
	}
	
	public static String MD5(String str){
		try {
			String rtn = "";
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.reset();
			byte[] resBytes = md.digest(str.getBytes());
			for(byte b:resBytes){
				rtn += String.format("%02x", (((int)b) & 0xFF));
			}
			return rtn;
		} catch (Exception e) {
			Log.e("MD5", e.getClass().getName()+": "+e.getMessage());
		}
		return "";
	}
	
	public void refreshNews(){
		ListView news_list = (ListView) this.findViewById(R.id.newsListe);
		Cursor c = database.rawQuery("SELECT id, title, receivers FROM news ORDER BY date DESC", null);
		c.moveToFirst();
		String news_semester = saveFile.getString("semester", "");
		news_semester.toLowerCase();
		news_semester = (news_semester.startsWith("ba") ? news_semester.substring(2) : news_semester);
		ArrayList<String> news_to_show = new ArrayList<String>();
		final ArrayList<Integer> news_ids = new ArrayList<Integer>();
		while(!c.isAfterLast()){
			String[] semester = c.getString(c.getColumnIndex("receivers")).split(" ");
			boolean show = news_semester.equals("");
			for(int i = 0; i<semester.length && !show; i++){
				String s = semester[i].toLowerCase();
				if(s.equals(news_semester) || s.equals("") || s.equals("semester"))
					show = true;
			}
			if(show){
				news_to_show.add(c.getString(c.getColumnIndex("title")));
				news_ids.add(c.getInt(c.getColumnIndex("id")));
			}
			c.moveToNext();
		}
		c.close();
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,R.layout.listelement,news_to_show);
		news_list.setAdapter(adapter);
		
		news_list.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				newsActivity.id = news_ids.get(arg2);
				Intent intent = new Intent(mainActivity.this,newsActivity.class);
				mainActivity.this.startActivity(intent);
			}
		});
		
	}
	
	class dispRefresh implements Runnable {
		
		public void run() {
			if(!pause){
				if(schedule.length() == 0){
					((LinearLayout)mainActivity.this.findViewById(R.id.main_plan)).setVisibility(View.VISIBLE);
					((LinearLayout)mainActivity.this.findViewById(R.id.main_plan_alternative)).setVisibility(View.GONE);
					((TextView)mainActivity.this.findViewById(R.id.countdown)).setVisibility(View.GONE);
					((TextView)mainActivity.this.findViewById(R.id.nächstesEvent)).setText(R.string.LANG_NOSCHEDULELOADED);
				} else {
					TextView nevent = (TextView)mainActivity.this.findViewById(R.id.nächstesEvent);
					((LinearLayout)mainActivity.this.findViewById(R.id.main_plan_alternative)).setVisibility(View.VISIBLE);
					if(tsv == null || tsv.switchDayAutomatically){							
						TextView countdown = (TextView)mainActivity.this.findViewById(R.id.countdown);
						JSONObject c = schedule.getNextEvent();
						Calendar nCalendar = schedule.getNextCalendar(c);
						long zeitDifferenz = nCalendar.getTimeInMillis()- new GregorianCalendar().getTimeInMillis();
						long stunden  = (zeitDifferenz / (3600*1000));
						long minuten  = (zeitDifferenz / 60000) - (stunden * 60);
						long sekunden = (zeitDifferenz / 1000) - (stunden*3600+minuten*60);
						GregorianCalendar bzt = (GregorianCalendar) nCalendar.clone();
						bzt.set(Calendar.HOUR_OF_DAY, 0);
						bzt.set(Calendar.MINUTE, 0);
						long tage = (bzt.getTimeInMillis()-new GregorianCalendar().getTimeInMillis())/(24*60*60*1000) +1;
						countdown.setText(stunden>23 ? 
								String.format(
										getString( tage==1 ? R.string.LANG_COUNTDOWNDAY : R.string.LANG_COUNTDOWNDAYS),
										tage, 
										DateFormat.getTimeInstance(DateFormat.SHORT).format(schedule.getNextCalendar(schedule.getNextEvent()).getTime())) : 
								String.format("%02d:%02d:%02d", stunden,minuten,sekunden));
						countdown.setVisibility(View.VISIBLE);
						nevent.setText(String.format(getString(R.string.LANG_NEXTEVENTIN), c.optString("eventType")));
						if(tsv == null || tsv.schedule != schedule){
							tsv = new TimeStreamView(mainActivity.this,schedule);
							((LinearLayout) mainActivity.this.findViewById(R.id.main_plan_alternative)).removeAllViews();
							((LinearLayout) mainActivity.this.findViewById(R.id.main_plan_alternative)).addView(tsv);
						}
						nevent.setClickable(false);
						nevent.setTextColor(0xffffffff);
					} else {
						((TextView)mainActivity.this.findViewById(R.id.countdown)).setVisibility(View.GONE);
						nevent.setTextColor(0xffcccccc);
						nevent.setText(R.string.LANG_CLICKTOGETBACK);
						nevent.setOnClickListener(new View.OnClickListener(){

							public void onClick(View v) {
								tsv.switchDayAutomatically = true;
							}
							
						});
						nevent.setClickable(true);
					}
					tsv.invalidate();
				}
			}
			if(!mainActivity.this.pause){
				// Activity is in foreground
				if(mainActivity.saveFile.getLong("lastUpdate", 0) != mainActivity.last_news_update) // News have been updated
					mainActivity.this.refreshNews();
				Handler h = new Handler();
				h.removeCallbacks(this);
				h.postDelayed(this, 300);
			}
				
				
			preJSON = mainActivity.saveFile.getString("newsJSON", "")+mainActivity.saveFile.getString("scheduleJSON", "");
		}
	}
}
