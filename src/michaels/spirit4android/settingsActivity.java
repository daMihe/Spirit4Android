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
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class settingsActivity extends Activity {
	
	HashMap<String,Integer> groups = new HashMap<String,Integer>();
	DefaultHttpClient client = new DefaultHttpClient();
	
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
		final ArrayAdapter<String> aa = new ArrayAdapter<String>(this,R.layout.listelement,array4list);
		ListView lw = (ListView) this.findViewById(R.id.groupFilters);
		lw.setAdapter(aa);
		
		lw.setOnItemClickListener(new OnItemClickListener(){

			public void onItemClick(AdapterView<?> arg0, View arg1, final int pos, long arg3) {
				final Dialog d = new Dialog(settingsActivity.this);
				d.setContentView(R.layout.groupdialog);
				d.setTitle("Gruppe ändern");
				final EditText et = (EditText) d.findViewById(R.id.groupInput);
				final TextView tv = (TextView) d.findViewById(R.id.groupDesc);
				final Button ok = (Button) d.findViewById(R.id.saveGroup);
				final int maxValue = settingsActivity.this.groups.get(aa.getItem(pos));
				tv.setText("Gib eine Gruppennummer [min. 0 (= Event deaktiviert), max. "+maxValue+"] für "+aa.getItem(pos)+" ein:");
				et.setText(	mainActivity.saveFile.getInt("group"+mainActivity.MD5(aa.getItem(pos)), 0)+"");
				ok.setOnClickListener(new OnClickListener(){

					public void onClick(View arg0) {
						if(Integer.parseInt(et.getText().toString()) <= maxValue && Integer.parseInt(et.getText().toString())>=0){
							Editor e = mainActivity.saveFile.edit();
							e.putInt("group"+mainActivity.MD5(aa.getItem(pos)), Integer.parseInt(et.getText().toString()));
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
	}
}
