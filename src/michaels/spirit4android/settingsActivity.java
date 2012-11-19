package michaels.spirit4android;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Media;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class settingsActivity extends TabActivity {
	
	HashMap<String,Integer> groups = new HashMap<String,Integer>();
	DefaultHttpClient client = new DefaultHttpClient();
	Cursor alarm_cursor, plan_cursor, spare_cursor;
	ProgressDialog pd;
	
	public void onCreate(Bundle b){
		/*
		 * onCreate - settingsActivity initialization. See Comments for what is
		 * done here exactly. Android API needs a call to Activity.onCreate.
		 */
		super.onCreate(b);
		this.setContentView(R.layout.settings);
		
		//initialize Tabs
		TabHost th = (TabHost) this.findViewById(android.R.id.tabhost);
		th.setup();
		
		TabSpec tab_simple = th.newTabSpec("tab1");
		tab_simple.setContent(R.id.simple_settings);
		tab_simple.setIndicator(getString(R.string.LANG_SETTINGS_SIMPLE),getResources().getDrawable(R.drawable.settings_simple));
		
		TabSpec tab_alarm = th.newTabSpec("tab2");
		tab_alarm.setContent(R.id.alarm_settings);
		tab_alarm.setIndicator(getString(R.string.LANG_SETTINGS_ALARM),getResources().getDrawable(R.drawable.settings_alarm));
		
		TabSpec tab_sparetime = th.newTabSpec("tab3");
		tab_sparetime.setContent(R.id.spare_time_editor);
		tab_sparetime.setIndicator(getString(R.string.LANG_SETTINGS_SPARETIMEEDITOR),getResources().getDrawable(R.drawable.settings_spare_time));
		
		TabSpec tab_advanced = th.newTabSpec("tab4");
		tab_advanced.setContent(R.id.advanced_plan_editor);
		tab_advanced.setIndicator(getString(R.string.LANG_SETTINGS_ADVANCEDSCHEDULEEDITOR),getResources().getDrawable(R.drawable.settings_advanced));
		
		
		
		th.addTab(tab_simple);
		th.addTab(tab_alarm);
		th.addTab(tab_sparetime);
		th.addTab(tab_advanced);
		
		// Fill with contents
		// Simple Settings
		
		@SuppressWarnings("rawtypes")
		final ArrayAdapter aas = ArrayAdapter.createFromResource(this, R.array.studiengang, android.R.layout.simple_spinner_item);
		aas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		Spinner s = ((Spinner)this.findViewById(R.id.studiengangspinner));
		s.setAdapter(aas);
		s.setOnItemSelectedListener(new OnItemSelectedListener(){

			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				Editor e = mainActivity.saveFile.edit();
				e.putString("semester", (String)aas.getItem(arg2));
				e.commit();
				
			}
	
			public void onNothingSelected(AdapterView<?> arg0) {}
			
		});
		List<String> semester = Arrays.asList(this.getResources().getStringArray(R.array.studiengang));
		s.setSelection(semester.indexOf(mainActivity.saveFile.getString("semester", "bai1")));
		
		Button updateNow = (Button) this.findViewById(R.id.downloadPlanNow);
		updateNow.setOnClickListener(new OnClickListener(){

			public void onClick(View arg0) {
				pd = new ProgressDialog(settingsActivity.this);
				pd.setMessage(getString(R.string.LANG_DOWNLOADING));
				pd.setCancelable(true);
				pd.show();
				new Handler().postDelayed(new ScheduleLoader(), 500);
			}
			
		});
		try {
			analyseScheduleForGroups();
		} catch(Exception e){
			Log.e("DEBUG",e.getClass().getName()+": "+e.getMessage());
		}

		CheckBox ses_check = (CheckBox) this.findViewById(R.id.short_style);
		ses_check.setChecked(mainActivity.saveFile.getBoolean("shortEventDisplay", false));
		ses_check.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Editor e = mainActivity.saveFile.edit();
				e.putBoolean("shortEventDisplay", isChecked);
				e.commit();
			}
		});
		
		// alarm settings		
		final Spinner alarm_spinner = (Spinner) this.findViewById(R.id.alarmMedia);
		final ToggleButton alarm_penetrant = (ToggleButton) this.findViewById(R.id.alarmPenetrant);
		final ToggleButton alarmActivated = (ToggleButton) this.findViewById(R.id.alarmActivated);
		alarmActivated.setChecked(mainActivity.saveFile.getLong("alarmtimeBeforeEvent", -1) > -1);
		alarmActivated.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					AlertDialog.Builder adb = new AlertDialog.Builder(settingsActivity.this);
					adb.setMessage(R.string.LANG_ALARMDIALOGTEXT);
					adb.setTitle(R.string.LANG_ALARMDIALOGTITLE);
					final EditText tf = new EditText(settingsActivity.this);
					adb.setView(tf);
					tf.setKeyListener(new DigitsKeyListener());
					tf.setText(mainActivity.saveFile.getLong("alarmtimeBeforeEvent", 20*60*1000)/(60*1000)+"");
					adb.setPositiveButton(R.string.LANG_SAVE, new android.content.DialogInterface.OnClickListener(){

						public void onClick(DialogInterface dialog, int which) {
							Editor e = mainActivity.saveFile.edit();
							e.putLong("alarmtimeBeforeEvent", Long.parseLong(tf.getText().toString())*60*1000);
							e.commit();
							dialog.dismiss();
							alarm_penetrant.setEnabled(true);
							alarm_spinner.setEnabled(true);
						}
						
					});
					adb.setOnCancelListener(new DialogInterface.OnCancelListener() {
						
						public void onCancel(DialogInterface dialog) {
							alarmActivated.setChecked(mainActivity.saveFile.getLong("alarmtimeBeforeEvent", -1) > -1);
							dialog.dismiss();
						}
					});
					adb.create().show();
				} else {
					Editor e = mainActivity.saveFile.edit();
					e.putLong("alarmtimeBeforeEvent", -1);
					e.commit();
					alarm_penetrant.setEnabled(false);
					alarm_spinner.setEnabled(false);
				}
			}
		});
		// Penetrant Mode Button
		alarm_penetrant.setChecked(mainActivity.saveFile.getBoolean("alarmPenetrantMode", true));
		alarm_penetrant.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Editor e = mainActivity.saveFile.edit();
				e.putBoolean("alarmPenetrantMode", isChecked);
				e.commit();
			}
		});
		alarm_penetrant.setEnabled(mainActivity.saveFile.getLong("alarmtimeBeforeEvent", -1) >= 0);
		
		// search music for alarm
		ArrayList<String> music = new ArrayList<String>();
		String[] proj = {
			MediaStore.Audio.Media.ARTIST,
			MediaStore.Audio.Media.TITLE,
			MediaStore.Audio.Media.DATA
		};
		alarm_cursor = this.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, null, null, MediaStore.Audio.Media.ARTIST);
		int current = 0;
		if(alarm_cursor != null && alarm_cursor.getCount() != 0){
			music.add(getString(R.string.LANG_RANDOMSOUND));
			alarm_cursor.moveToFirst();
			String alarm_music = mainActivity.saveFile.getString("alarmMusic", "");
			while(!alarm_cursor.isAfterLast()){
				//Log.i("Info", c.getColumnIndex(MediaStore.Audio.Media.ARTIST)+","+c.getColumnIndex(MediaStore.Audio.Media.TITLE));
				music.add(alarm_cursor.getString(alarm_cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST))+" - "+alarm_cursor.getString(alarm_cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
				if(alarm_cursor.getString(alarm_cursor.getColumnIndex(Media.DATA)).contentEquals(alarm_music))
					current = alarm_cursor.getPosition()+1;
				alarm_cursor.moveToNext();
			}
		} else
			music.add(getString(R.string.LANG_NOSOUNDSFOUND));
		ArrayAdapter<String> alarm_spinner_adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,music);
		alarm_spinner.setEnabled(alarm_cursor != null && alarm_cursor.getCount() > 0 && alarmActivated.isChecked());
		alarmActivated.setEnabled(alarm_cursor != null && alarm_cursor.getCount() > 0);
		alarm_spinner.setAdapter(alarm_spinner_adapter);
		if(current != 0)
			alarm_spinner.setSelection(current);
		alarm_spinner.setOnItemSelectedListener(new OnItemSelectedListener(){

			public void onItemSelected(AdapterView<?> arg0, View arg1, int pos, long arg3) {
				if(alarm_cursor != null){
					Editor e = mainActivity.saveFile.edit();
					if(pos != 0){
						alarm_cursor.moveToPosition(pos-1);
						e.putString("alarmMusic", alarm_cursor.getString(alarm_cursor.getColumnIndex(MediaStore.Audio.Media.DATA)));
					} else
						e.putString("alarmMusic", "");
					e.commit();					
				}
			}

			public void onNothingSelected(AdapterView<?> arg0) {}
			
		});
		
		// Advanced Schedule Editor
		final TextView advsen = (TextView) this.findViewById(R.id.about_advandced_schedule_editor);
		advsen.setOnClickListener(new OnClickListener(){

			public void onClick(View arg0) {
				advsen.setVisibility(View.GONE);
			}
			
		});
		analyseEvents();
		((Button) this.findViewById(R.id.advanced_schedule_add)).setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				Cursor c = mainActivity.database.rawQuery("select time from schedule where time = 86400 and length = 5400 and title = \"\" and " +
						"room = \"\" and docent = \"\" and week = "+FHSSchedule.EVENT_RYTHM_WEEKLY+" and egroup = 0 and type = "+FHSSchedule.EVENT_LECTURE, null);
				if(c.getCount() == 0){
					mainActivity.database.execSQL("insert or replace into schedule (time, length, title, room, docent, week, egroup, type) values" +
							"("+24*60*60+","+90*60+",\"\",\"\",\"\","+FHSSchedule.EVENT_RYTHM_WEEKLY+",0,"+FHSSchedule.EVENT_LECTURE+")");
					settingsActivity.this.analyseEvents();
				} else {
					Toast.makeText(settingsActivity.this,R.string.LANG_ASE_ALREADYCREATED,Toast.LENGTH_LONG).show();
				}
				c.close();
			}
		});
		
		// Sparetime Editor
		// should sparetime-data be loaded from third-party servers?
		CheckBox sparetimeload = (CheckBox) this.findViewById(R.id.spare_time_load_from_acl5m);
		sparetimeload.setChecked(mainActivity.saveFile.getBoolean("loadsparetimefromacl5m", false));
		sparetimeload.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// save the new state
				Editor e = mainActivity.saveFile.edit();
				e.putBoolean("loadsparetimefromacl5m", isChecked);
				e.commit();
			}
		});
		
		analyseSpareTime();
		
		Button sparetimeadd = (Button) this.findViewById(R.id.spare_time_add);
		sparetimeadd.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				Cursor c = mainActivity.database.rawQuery("select * from sparetime where start = 0 and stop = 0", null);
				int count = c.getCount();
				c.close();
				if(count == 0){
					mainActivity.database.execSQL("insert into sparetime (desc,start,stop) values (\"new sparetime\",0,0)");
					settingsActivity.this.analyseSpareTime();
				}
			}
		});
	}
	
	public void onDestroy(){
		if(alarm_cursor != null)
			alarm_cursor.close();
		if(plan_cursor != null)
			plan_cursor.close();
		if(spare_cursor != null)
			spare_cursor.close();
		super.onDestroy();
	}
	
	public void analyseSpareTime(){
		/*
		 * analyseSpareTime - analyses Sparetime's in database and updating list
		 * in Sparetime-tab.
		 */
		
		// clear database-cursor and layout
		if(spare_cursor != null)
			spare_cursor.close();		
		LinearLayout spare_layout = (LinearLayout) this.findViewById(R.id.spare_time_list);
		spare_layout.removeAllViews();
		
		// find all sparetime's
		spare_cursor = mainActivity.database.rawQuery("select desc, start, stop from sparetime order by start asc",null);
		spare_cursor.moveToFirst();
		
		// add them to the UI
		LayoutInflater inflater = this.getLayoutInflater();
		while(!spare_cursor.isAfterLast()){
			TextView inflated = (TextView) inflater.inflate(R.layout.listelement, null);
			inflated.setText(DateFormat.getDateInstance().format(new Date(spare_cursor.getLong(spare_cursor.getColumnIndex("start"))))+
					" "+spare_cursor.getString(spare_cursor.getColumnIndex("desc")));
			inflated.setTag(spare_cursor.getPosition());
			inflated.setOnClickListener(new OnClickListener() {
				
				public void onClick(View v) {
					/*
					 * this function is called when a sparetime should be 
					 * edited. It creates a dialog for it and implements the
					 * saving and deletion of the sparetime.
					 */
					spare_cursor.moveToPosition((Integer) v.getTag());
					
					final Dialog dialog = new Dialog(settingsActivity.this);
					dialog.setContentView(R.layout.sparetimeeditor);
					
					final String description = spare_cursor.getString(spare_cursor.getColumnIndex("desc"));
					
					final EditText desc = (EditText) dialog.findViewById(R.id.ste_desc);
					desc.setText(description);
					
					final long start = spare_cursor.getLong(spare_cursor.getColumnIndex("start"));
					final long stop = spare_cursor.getLong(spare_cursor.getColumnIndex("stop"));
					
					final EditText start_day = (EditText) dialog.findViewById(R.id.ste_start_day);
					start_day.setText(DateFormat.getDateInstance().format(new Date(start)));
					
					final EditText start_time = (EditText) dialog.findViewById(R.id.ste_start_time);
					start_time.setText(DateFormat.getTimeInstance().format(new Date(start)));
					
					final EditText end_day = (EditText) dialog.findViewById(R.id.ste_end_day);
					end_day.setText(DateFormat.getDateInstance().format(new Date(stop)));
					
					final EditText end_time = (EditText) dialog.findViewById(R.id.ste_end_time);
					end_time.setText(DateFormat.getTimeInstance().format(new Date(stop)));
					
					dialog.setTitle(R.string.LANG_STE_DTITLE);
					
					((Button) dialog.findViewById(R.id.ste_save)).setOnClickListener(new OnClickListener() {
						
						public void onClick(View v) {
							DateFormat df = DateFormat.getDateInstance();
							DateFormat tf = DateFormat.getTimeInstance();
							try {								
								long newstart = df.parse(start_day.getEditableText().toString()).getTime() + 
										tf.parse(start_time.getEditableText().toString()).getTime();
								newstart += TimeZone.getDefault().getOffset(newstart);
								long newstop = df.parse(end_day.getEditableText().toString()).getTime() + 
										tf.parse(end_time.getEditableText().toString()).getTime();
								newstop += TimeZone.getDefault().getOffset(newstop);
								// check if end-time is before start-time (if both input can be valid)
								if(newstop < newstart)
									throw new ParseException("End-time is before Start-time.", 0);
								// insert data to the database
								mainActivity.database.execSQL("update sparetime set desc = ?, start = "+newstart+
										", stop = "+newstop+" where desc = ? and start = "+start+" and stop = "+stop,
										new String[]{
											desc.getEditableText().toString(),
											description
								});
								dialog.dismiss();
								settingsActivity.this.analyseSpareTime();
							} catch (ParseException e) {
								Toast.makeText(settingsActivity.this, R.string.LANG_INVALIDINPUT, Toast.LENGTH_LONG).show();
							}
						}
					});
					((Button) dialog.findViewById(R.id.ste_delete)).setOnClickListener(new OnClickListener() {
						public void onClick(View arg0) {
							
							mainActivity.database.execSQL("delete from sparetime where desc = ? and start = ? and stop = ?",
									new Object[]{
										spare_cursor.getString(spare_cursor.getColumnIndex("desc")),
										spare_cursor.getLong(spare_cursor.getColumnIndex("start")),
										spare_cursor.getLong(spare_cursor.getColumnIndex("stop"))
							});
							dialog.dismiss();
							settingsActivity.this.analyseSpareTime();
						}
					});
					
					dialog.show();
				}
			});
			spare_layout.addView(inflated);
			spare_cursor.moveToNext();
		}
	}
	
	@SuppressLint({ "ParserError", "ParserError", "ParserError" })
	public void analyseEvents(){
		if(plan_cursor != null)
			plan_cursor.close();
		plan_cursor = mainActivity.database.rawQuery("select * from schedule order by time asc",null);
		plan_cursor.moveToFirst();
		LayoutInflater layoutifl = this.getLayoutInflater();
		LinearLayout parent = (LinearLayout) this.findViewById(R.id.advanced_schedule_list);
		parent.removeAllViews();
		while(!plan_cursor.isAfterLast()){
			TextView tv = (TextView) layoutifl.inflate(R.layout.listelement,null);
			int day_ = plan_cursor.getInt(plan_cursor.getColumnIndex("time"))/(24*60*60)-1;
			short week = plan_cursor.getShort(plan_cursor.getColumnIndex("week"));
			String day = this.getResources().getStringArray(R.array.LANG_WEEKDAYS_SHORT)[day_] + (week != FHSSchedule.EVENT_RYTHM_WEEKLY ? " ("+this.getString(FHSSchedule.EVENT_RYTHM_EVEN == week ?  R.string.LANG_WEEK_EVEN : R.string.LANG_WEEK_ODD)+")":"");
			long hour = (plan_cursor.getLong(plan_cursor.getColumnIndex("time"))-(day_+1)*24*60*60)/(60*60);
			long min = (plan_cursor.getLong(plan_cursor.getColumnIndex("time"))-(day_+1)*24*60*60-hour*60*60)/60;
			tv.setText(String.format("%s,%02d:%02d %s", day, hour, min, plan_cursor.getString(plan_cursor.getColumnIndex("title"))));
			tv.setTag(plan_cursor.getPosition());
			tv.setOnClickListener(new OnClickListener() {
				
				public void onClick(View v) {
					plan_cursor.moveToPosition((Integer) v.getTag());
					final Dialog d = new Dialog(settingsActivity.this);
					d.setContentView(R.layout.eventeditor);
					d.setTitle(R.string.LANG_ASE_EVENTEDITOR);
					final String title = plan_cursor.getString(plan_cursor.getColumnIndex("title"));
					final TextView ctrl_title = (TextView) d.findViewById(R.id.ase_title);
					ctrl_title.setText(title);
					final TextView ctrl_docent = (TextView) d.findViewById(R.id.ase_docent);
					ctrl_docent.setText(plan_cursor.getString(plan_cursor.getColumnIndex("docent")));
					final Spinner ctrl_weekday = (Spinner) d.findViewById(R.id.ase_day);
					ctrl_weekday.setAdapter(new ArrayAdapter<String>(settingsActivity.this,android.R.layout.simple_spinner_item,settingsActivity.this.getResources().getStringArray(R.array.LANG_WEEKDAYS_SHORT)));
					final int time = plan_cursor.getInt(plan_cursor.getColumnIndex("time"));
					int day = time/(24*60*60)-1;
					int hour = (time-(day+1)*24*60*60)/(60*60);
					int min = (time-(day+1)*24*60*60-hour*60*60)/60;
					ctrl_weekday.setSelection(time/(24*60*60)-1);
					final CheckBox ctrl_evenweek = (CheckBox) d.findViewById(R.id.ase_evenweek);
					ctrl_evenweek.
						setChecked(FHSSchedule.EVENT_RYTHM_EVEN == (plan_cursor.getShort(plan_cursor.getColumnIndex("week")) & FHSSchedule.EVENT_RYTHM_EVEN));
					final CheckBox ctrl_oddweek = (CheckBox) d.findViewById(R.id.ase_oddweek);
					ctrl_oddweek.
						setChecked(FHSSchedule.EVENT_RYTHM_ODD == (plan_cursor.getShort(plan_cursor.getColumnIndex("week")) & FHSSchedule.EVENT_RYTHM_ODD));
					final TextView ctrl_time = (TextView) d.findViewById(R.id.ase_time);
					ctrl_time.setText(String.format("%02d:%02d", hour, min));
					final TextView ctrl_length = (TextView) d.findViewById(R.id.ase_length);
					ctrl_length.setText(plan_cursor.getInt(plan_cursor.getColumnIndex("length"))+"");
					final TextView ctrl_room = (TextView) d.findViewById(R.id.ase_room);
					ctrl_room.setText(plan_cursor.getString(plan_cursor.getColumnIndex("room")));
					final short group = plan_cursor.getShort(plan_cursor.getColumnIndex("egroup"));
					final TextView ctrl_group = (TextView) d.findViewById(R.id.ase_group);
					ctrl_group.setText(group+"");
					final RadioGroup ctrl_type = (RadioGroup) d.findViewById(R.id.ase_type);
					ctrl_type.check(plan_cursor.getShort(plan_cursor.getColumnIndex("type")) == FHSSchedule.EVENT_LECTURE ? R.id.ase_rad_lecture : R.id.ase_rad_exercise);
					((Button) d.findViewById(R.id.ase_save)).setOnClickListener(new OnClickListener() {
						
						public void onClick(View v) {
							Pattern text_pattern = Pattern.compile("^[^\n\r\t\"]+$");
							Pattern time_pattern = Pattern.compile("^((([0-1]?[0-9]|2[0-3]):[0-5]?[0-9])|((0?[0-9]|1[0-2]):[0-5]?[0-9]\\s?[ap]m))$");
							if(text_pattern.matcher(ctrl_title.getText()).matches() && // Title
									text_pattern.matcher(ctrl_docent.getText()).matches() && //Docent
									text_pattern.matcher(ctrl_room.getText()).matches() && //Room
									(ctrl_evenweek.isChecked() || ctrl_oddweek.isChecked()) && //Minimum week-requirement
									time_pattern.matcher(ctrl_time.getText()).matches()){
								String tc_text = ctrl_time.getText().toString();
								int new_time = (ctrl_weekday.getSelectedItemPosition()+1)*24*60*60 + 
										Integer.parseInt(tc_text.substring(0, tc_text.indexOf(":")))*60*60 +
										Integer.parseInt(tc_text.substring(tc_text.indexOf(":")+1))*60 +
										(tc_text.endsWith("pm") ? 12*60*60 : 0);
								mainActivity.database.execSQL("update schedule set " +
										"time = "+new_time+", " +
										"length = "+(ctrl_length.getText().length() == 0 ? 90*60 : Integer.parseInt(ctrl_length.getText().toString()))+","+
										"egroup = "+(ctrl_group.getText().length() == 0 ? 0 : Integer.parseInt(ctrl_group.getText().toString()))+","+
										"week = "+((ctrl_evenweek.isChecked() ? FHSSchedule.EVENT_RYTHM_EVEN : 0)|(ctrl_oddweek.isChecked() ? FHSSchedule.EVENT_RYTHM_ODD : 0))+","+
										"room = \""+ctrl_room.getText().toString()+"\","+
										"title = \""+ctrl_title.getText().toString()+"\","+
										"docent = \""+ctrl_docent.getText()+"\","+
										"type = "+(ctrl_type.getCheckedRadioButtonId() == R.id.ase_rad_lecture ? FHSSchedule.EVENT_LECTURE : FHSSchedule.EVENT_EXERCISE)+" "+
										"where time = "+time+" and title = \""+title+"\" and egroup = "+group);
								d.dismiss();
								settingsActivity.this.analyseEvents();
								try {
									settingsActivity.this.analyseScheduleForGroups();
								} catch (JSONException e) {}
							} else
								Toast.makeText(settingsActivity.this, R.string.LANG_INVALIDINPUT, Toast.LENGTH_LONG).show();
						}
					});
					((Button) d.findViewById(R.id.ase_delete)).setOnClickListener(new OnClickListener() {
						
						public void onClick(View v) {
							mainActivity.database.execSQL("delete from schedule where time = "+time+" and title = \""+title+"\" and egroup = "+group);
							d.dismiss();
							settingsActivity.this.analyseEvents();
							try {
								settingsActivity.this.analyseScheduleForGroups();
							} catch (JSONException e) {}
						}
					});
					d.show();
				}
			});
			parent.addView(tv);
			plan_cursor.moveToNext();
		}
	}
	
	public void analyseScheduleForGroups() throws JSONException {
		groups.clear();
		ArrayList<String> array4list = new ArrayList<String>();
		
		Cursor c = mainActivity.database.rawQuery("SELECT egroup, type, title FROM schedule",null);
		c.moveToFirst();
		while(!c.isAfterLast()){
			if(c.getInt(c.getColumnIndex("egroup"))!= 0){
				String index = getString(c.getInt(c.getColumnIndex("type")) == FHSSchedule.EVENT_LECTURE ? R.string.LANG_LECTURE : R.string.LANG_EXERCISE)+"/"+
						c.getString(c.getColumnIndex("title"));
				if(!groups.containsKey(index) || groups.get(index) < c.getInt(c.getColumnIndex("egroup")))
					groups.put(index, c.getInt(c.getColumnIndex("egroup")));
				if(!array4list.contains(index))
					array4list.add(index);
			}
			c.moveToNext();
		}
		c.close();
		if(array4list.size() != 0){
			this.findViewById(R.id.grpFilterDesc).setVisibility(View.VISIBLE);
			LinearLayout pseudo_list = (LinearLayout) this.findViewById(R.id.groupFilters);
			pseudo_list.removeAllViews();
			LayoutInflater inflater = this.getLayoutInflater();
			for(int i=0; i<array4list.size();i++){
				TextView curtv = (TextView) inflater.inflate(R.layout.listelement, null);
				curtv.setText(array4list.get(i));
				curtv.setOnClickListener(new OnClickListener() {
					
					public void onClick(View v) {
						TextView curtv = (TextView) v; 
						final Dialog d = new Dialog(settingsActivity.this);
						d.setContentView(R.layout.groupdialog);
						d.setTitle("Gruppe Ändern");
						final EditText et = (EditText) d.findViewById(R.id.groupInput);
						final TextView tv = (TextView) d.findViewById(R.id.groupDesc);
						final Button ok = (Button) d.findViewById(R.id.saveGroup);
						final int maxValue = settingsActivity.this.groups.get(curtv.getText());
						final String[] grp_str = curtv.getText().toString().split("/");
						tv.setText(String.format(getString(R.string.LANG_SETGROUPFOREVENT),maxValue,curtv.getText()));
						final byte type = (grp_str[0].equals(getString(R.string.LANG_LECTURE)) ? FHSSchedule.EVENT_LECTURE : FHSSchedule.EVENT_EXERCISE);
						Cursor c = mainActivity.database.rawQuery("SELECT egroup FROM groups WHERE type = "+type+" AND title = '"+grp_str[1]+"'", null);
						c.moveToFirst();
						final boolean create = c.isAfterLast();
						et.setText(create ? "0": c.getInt(c.getColumnIndex("egroup"))+"");
						c.close();
						ok.setOnClickListener(new OnClickListener(){
		
							public void onClick(View arg0) {
								int group = Integer.parseInt(et.getText().toString());
								if(group <= maxValue && group>=0){
									if(create)
										mainActivity.database.execSQL("INSERT INTO groups (title, type, egroup) VALUES ('"+grp_str[1]+"',"+type+","+group+")");
									else
										mainActivity.database.execSQL("UPDATE groups SET egroup = "+group+" WHERE title = '"+grp_str[1]+"' AND type = "+type);
									d.dismiss();
									Toast.makeText(settingsActivity.this, getString(R.string.LANG_GROUPSETSUCCESSFULLY), Toast.LENGTH_LONG).show();
								} else
									Toast.makeText(settingsActivity.this, getString(R.string.LANG_INVALIDINPUT), Toast.LENGTH_LONG).show();
							}
							
						});
						d.show();
					}
				});
				pseudo_list.addView(curtv);
			}
		} else {
			// Liste ist leer...
			this.findViewById(R.id.grpFilterDesc).setVisibility(View.GONE);
		}
	}
	
	public class ScheduleLoader implements Runnable {

		public void run() {
			try {
				HttpGet hg = new HttpGet("http://spirit.fh-schmalkalden.de/rest/1.0/schedule?classname="+mainActivity.saveFile.getString("semester", "bai1"));
				hg.setHeader("User-Agent", mainActivity.USERAGENT);
				HttpResponse r = client.execute(hg);
				String response = EntityUtils.toString(r.getEntity());
				JSONArray result = new JSONArray(response);
				if(result.length() != 0){
					pd.setMessage(settingsActivity.this.getString(R.string.LANG_ANALYSING));
					FHSSchedule.parsePlan(result);
					Toast.makeText(settingsActivity.this, getString(R.string.LANG_SCHEDULEDOWNLOADEDSUCCESSFULLY), Toast.LENGTH_LONG).show();
					try {
						analyseScheduleForGroups();
					} catch(Exception e1){}
					analyseEvents();
					pd.dismiss();
				} else 
					throw new Exception();
			} catch (Exception e) {
				pd.dismiss();
				Toast.makeText(settingsActivity.this, String.format(getString(R.string.LANG_ERRORWHILELOADINGPLAN), e.getClass().getName(), e.getMessage()), Toast.LENGTH_LONG).show();
			}
		}
	}
}
