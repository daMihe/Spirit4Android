package michaels.spirit4android;

import java.security.MessageDigest;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
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
	final static double VERSION = 0.4;
	public static String USERAGENT;
	public static SharedPreferences saveFile;
	public static JSONObject newsObject = null;
	public static String preJSON = "";
	boolean stop;
	boolean pause;
	boolean showCompletePlan;
	static FHSSchedule schedule;
	static TimeStreamView tsv;
	public void onStart(){
		super.onStart();
		DisplayMetrics dm = new DisplayMetrics();
		this.getWindowManager().getDefaultDisplay().getMetrics(dm);
		USERAGENT = "Spirit4Android v"+VERSION+" (disp "+dm.widthPixels+"*"+dm.heightPixels+")";
		stop = false;
		pause = false;
		saveFile = this.getSharedPreferences("s4apref",MODE_WORLD_READABLE); //lesen der Einstellungen
		if(saveFile.getInt("letzterSemesterWechsel", Calendar.getInstance().get(Calendar.MONTH)) != Calendar.getInstance().get(Calendar.MONTH) && (Calendar.getInstance().get(Calendar.MONTH) == Calendar.OCTOBER || Calendar.getInstance().get(Calendar.MONTH) == Calendar.APRIL)){
			AlertDialog.Builder ab = new AlertDialog.Builder(this);
			ab.setTitle("Semesterwechsel")
				.setMessage("Es sieht so aus, als würde ein neues Semester anfangen. Passe also die Einstellungen an, damit dein Stundenplan auch weiterhin stimmt...")
				.setPositiveButton("Plan-Einstellungen...", new OnClickListener(){

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
		if(!saveFile.getString("scheduleJSON", "[]").equals("[]")){
			try {
				schedule = new FHSSchedule(new JSONArray(saveFile.getString("scheduleJSON", "[]")),saveFile);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//Oberfläche
		this.setContentView(R.layout.main);
		
		final ListView newsListe = (ListView) this.findViewById(R.id.newsListe);
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
		}
		
		preJSON = mainActivity.saveFile.getString("newsJSON", "")+mainActivity.saveFile.getString("scheduleJSON", "");
		Handler countdown = new Handler();
		countdown.post(new dispRefresh());
		Handler updateHandler = new Handler();
		updateHandler.post(new Updater());
	}
	
	public void onDestroy(){
		stop = true;
		super.onDestroy();
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
		super.onResume();
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
			Toast.makeText(this, "Update läuft...", Toast.LENGTH_LONG).show();
			Handler updateHandler = new Handler();
			updateHandler.post(new Updater());
			return true;
		} else if(mi.getItemId() == R.id.about){ // Über
			Intent ÜberStarten = new Intent(this,about.class);
			this.startActivity(ÜberStarten);
			return true;
		}
		return super.onOptionsItemSelected(mi);
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
	
	class dispRefresh implements Runnable {
		public void run() {
			if(!pause){
				if(schedule.length() == 0){
					((LinearLayout)mainActivity.this.findViewById(R.id.main_plan)).setVisibility(View.VISIBLE);
					((LinearLayout)mainActivity.this.findViewById(R.id.main_plan_alternative)).setVisibility(View.GONE);
					((TextView)mainActivity.this.findViewById(R.id.countdown)).setVisibility(View.GONE);
					((TextView)mainActivity.this.findViewById(R.id.nächstesEvent)).setText("z.Z. keine Stundenplan-Daten vorhanden. Lade deinen Plan über [Menü-Taste] > \"Stundenplan-Einstellungen\" herunter und stell dort auch deine Gruppen-Filter ein.");
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
						countdown.setText(stunden>23 ? "~ "+tage+" Tag"+(tage==1?"":"en")+" um "+DateFormat.getTimeInstance(DateFormat.SHORT).format(schedule.getNextCalendar(schedule.getNextEvent()).getTime()) : String.format("%02d:%02d:%02d", stunden,minuten,sekunden));
						countdown.setVisibility(View.VISIBLE);
						nevent.setText("Nächstes Event ("+c.optString("eventType")+") in");
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
						nevent.setText("Hier \"klicken\" um zurück zum Countdown zu kommen...");
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
			if(!mainActivity.this.stop && preJSON.equals(mainActivity.saveFile.getString("newsJSON", "")+mainActivity.saveFile.getString("scheduleJSON", ""))){
				Handler h = new Handler();
				h.removeCallbacks(this);
				h.postDelayed(this, 100);
			} else if(!mainActivity.this.stop && !mainActivity.this.pause){
				Intent restart = new Intent(mainActivity.this,mainActivity.class);
				mainActivity.this.startActivity(restart);
				mainActivity.this.finish();
			} else if(!mainActivity.this.stop){
				Handler h = new Handler();
				h.removeCallbacks(this);
				h.postDelayed(this, 100);
			}
				
				
			preJSON = mainActivity.saveFile.getString("newsJSON", "")+mainActivity.saveFile.getString("scheduleJSON", "");
		}
	}
}
