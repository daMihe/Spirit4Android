package michaels.spirit4android;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
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
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class settingsActivity extends Activity {
	
	HashMap<String,Integer> groups = new HashMap<String,Integer>();
	DefaultHttpClient client = new DefaultHttpClient();
	Cursor alarm_cursor;
	
	public void onCreate(Bundle b){
		super.onCreate(b);
		this.setContentView(R.layout.settings);
		
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
				ProgressDialog pd = new ProgressDialog(settingsActivity.this);
				pd.setMessage("Bitte Warten, Stundenplan wird heruntergeladen...");
				pd.setCancelable(true);
				pd.show();
				try {
					HttpGet hg = new HttpGet("http://spirit.fh-schmalkalden.de/rest/1.0/schedule?classname="+mainActivity.saveFile.getString("semester", "bai1"));
					hg.setHeader("User-Agent", mainActivity.USERAGENT);
					HttpResponse r = client.execute(hg);
					String response = EntityUtils.toString(r.getEntity());
					if(new JSONArray(response).length() != 0){
						Editor e = mainActivity.saveFile.edit();
						e.putString("scheduleJSON", response);
						
						e.commit();
						pd.dismiss();
						Toast.makeText(settingsActivity.this, "Stundenplan wurde aktualisiert.", Toast.LENGTH_LONG).show();
						try {
							analyseScheduleForGroups();
						} catch(Exception e1){}
					}
				} catch (Exception e) {
					pd.dismiss();
					Toast.makeText(settingsActivity.this, "Es gab einen Fehler beim Herunterladen.\n\n"+e.getClass().getName()+": "+e.getMessage(), Toast.LENGTH_LONG).show();
				}
			}
			
		});
		try {
			analyseScheduleForGroups();
		} catch(Exception e){
			Log.e("DEBUG",e.getClass().getName()+": "+e.getMessage());
		}
		
		final Spinner alarm_spinner = (Spinner) this.findViewById(R.id.alarmMedia);
		final ToggleButton alarm_penetrant = (ToggleButton) this.findViewById(R.id.alarmPenetrant);
		final Button alarmActivated = (Button) this.findViewById(R.id.alarmActivated);
		alarmActivated.setText("Erinnerung ist "+(mainActivity.saveFile.getLong("alarmtimeBeforeEvent", -1) > -1 ? "ein" : "aus")+"geschaltet");
		alarmActivated.setOnClickListener(new OnClickListener(){

			public void onClick(View arg0) {
				AlertDialog.Builder adb = new AlertDialog.Builder(settingsActivity.this);
				adb.setMessage("Bitte setzen Sie die Zeit (in Minuten) fest, die Sie vor dem nächsten Event errinnert werden möchten:");
				adb.setTitle("Erinnerung...");
				final EditText tf = new EditText(settingsActivity.this);
				tf.setKeyListener(new DigitsKeyListener());
				tf.setText(mainActivity.saveFile.getLong("alarmtimeBeforeEvent", 20*60*1000)/(60*1000)+"");
				adb.setView(tf);
				adb.setPositiveButton("Aktivieren", new DialogInterface.OnClickListener() {
					
					public void onClick(DialogInterface dialog, int which) {
						long number = Long.parseLong(tf.getText().toString());
						Editor e = mainActivity.saveFile.edit();
						e.putLong("alarmtimeBeforeEvent", number*60*1000);
						e.commit();
						alarmActivated.setText("Erinnerung ist eingeschaltet");
						dialog.dismiss();
						alarm_penetrant.setEnabled(true);
						alarm_spinner.setEnabled(alarm_cursor != null);
						if(!mainActivity.saveFile.getBoolean("alarmPenetrantModeWarning", false) && alarm_penetrant.isChecked()){
							AlertDialog.Builder b = new AlertDialog.Builder(settingsActivity.this);
							b.setTitle(R.string.LANG_WARNING);
							b.setMessage(R.string.LANG_PENETRANTMODEWARNING);
							b.setPositiveButton(R.string.LANG_OK, new DialogInterface.OnClickListener() {
								
								public void onClick(DialogInterface dialog, int which) {
									Editor e = mainActivity.saveFile.edit();
									e.putBoolean("alarmPenetrantModeWarning", true);
									e.commit();
									dialog.dismiss();
								}
							});
							b.show();
						}
					}
				});
				adb.setNegativeButton("Deaktivieren", new DialogInterface.OnClickListener() {
					
					public void onClick(DialogInterface dialog, int which) {
						Editor e = mainActivity.saveFile.edit();
						e.putLong("alarmtimeBeforeEvent", -1);
						e.commit();
						alarmActivated.setText("Erinnerung ist ausgeschaltet");
						dialog.dismiss();
						alarm_penetrant.setEnabled(false);
						alarm_spinner.setEnabled(false);
					}
				});
				adb.show();
			}
			
		});
		// Penetranter Modus Button
		alarm_penetrant.setChecked(mainActivity.saveFile.getBoolean("alarmPenetrantMode", true));
		alarm_penetrant.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Editor e = mainActivity.saveFile.edit();
				e.putBoolean("alarmPenetrantMode", isChecked);
				e.commit();
			}
		});
		alarm_penetrant.setEnabled(mainActivity.saveFile.getLong("alarmtimeBeforeEvent", -1) >= 0);
		
		// MediaSpinner Daten sammeln
		ArrayList<String> music = new ArrayList<String>();
		String[] proj = {
			MediaStore.Audio.Media.ARTIST,
			MediaStore.Audio.Media.TITLE,
			MediaStore.Audio.Media.DATA
		};
		alarm_cursor = this.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, null, null, MediaStore.Audio.Media.ARTIST);
		int current = 0;
		if(alarm_cursor != null || alarm_cursor.getCount() != 0){
			music.add(getString(R.string.LANG_RANDOMSOUND));
			alarm_cursor.moveToFirst();
			String alarm_music = mainActivity.saveFile.getString("alarmMusic", "");
			while(!alarm_cursor.isAfterLast()){
				//Log.i("Info", c.getColumnIndex(MediaStore.Audio.Media.ARTIST)+","+c.getColumnIndex(MediaStore.Audio.Media.TITLE));
				music.add((alarm_cursor.isNull(alarm_cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)) ? "" : alarm_cursor.getString(alarm_cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST))+" - ")+alarm_cursor.getString(alarm_cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
				if(alarm_cursor.getString(alarm_cursor.getColumnIndex(Media.DATA)).contentEquals(alarm_music))
					current = alarm_cursor.getPosition();
				alarm_cursor.moveToNext();
			}
		} else
			music.add(getString(R.string.LANG_NOSOUNDSFOUND));
		ArrayAdapter<String> alarm_spinner_adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,music);
		alarm_spinner.setEnabled(alarm_cursor != null);
		alarm_spinner.setAdapter(alarm_spinner_adapter);
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
		
	}
	
	public void onDestroy(){
		if(alarm_cursor != null)
			alarm_cursor.close();
		super.onDestroy();
	}
	
	public void analyseScheduleForGroups() throws JSONException{
		groups.clear();
		ArrayList<String> array4list = new ArrayList<String>();
		JSONArray stunden = new JSONArray(mainActivity.saveFile.getString("scheduleJSON", "[]"));
		for(int i = 0; i<stunden.length(); i++){
			JSONObject current = stunden.getJSONObject(i);
			if(current.optString("group").replaceAll("[^0-9]*", "").length() != 0){
				if(!groups.containsKey(current.optString("eventType")+"/"+current.optString("titleShort")) || groups.get(current.optString("eventType")+"/"+current.optString("titleShort")) < Integer.parseInt(current.optString("group").replaceAll("[^0-9]*", ""))){
					groups.put(current.optString("eventType")+"/"+current.optString("titleShort"), Integer.parseInt(current.optString("group").replaceAll("[^0-9]*", "")));
					if(!array4list.contains(current.optString("eventType")+"/"+current.optString("titleShort")))
						array4list.add(current.optString("eventType")+"/"+current.optString("titleShort"));
				}
			}
		}
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
						final TextView curtv = (TextView) v; 
						final Dialog d = new Dialog(settingsActivity.this);
						d.setContentView(R.layout.groupdialog);
						d.setTitle("Gruppe ändern");
						final EditText et = (EditText) d.findViewById(R.id.groupInput);
						final TextView tv = (TextView) d.findViewById(R.id.groupDesc);
						final Button ok = (Button) d.findViewById(R.id.saveGroup);
						final int maxValue = settingsActivity.this.groups.get(curtv.getText());
						tv.setText("Gib eine Gruppennummer [min. 0 (= Event deaktiviert), max. "+maxValue+"] für "+curtv.getText()+" ein:");
						et.setText(	mainActivity.saveFile.getInt("group"+mainActivity.MD5((String) curtv.getText()), 0)+"");
						ok.setOnClickListener(new OnClickListener(){
		
							public void onClick(View arg0) {
								if(Integer.parseInt(et.getText().toString()) <= maxValue && Integer.parseInt(et.getText().toString())>=0){
									Editor e = mainActivity.saveFile.edit();
									e.putInt("group"+mainActivity.MD5((String) curtv.getText()), Integer.parseInt(et.getText().toString()));
									e.commit();
									d.dismiss();
									Toast.makeText(settingsActivity.this, "Gruppe gesetzt.", Toast.LENGTH_LONG).show();
								} else
									Toast.makeText(settingsActivity.this, "Eingabe ist ungültig.", Toast.LENGTH_LONG).show();
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
}
