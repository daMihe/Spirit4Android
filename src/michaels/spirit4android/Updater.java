package michaels.spirit4android;

import java.text.SimpleDateFormat;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class Updater implements Runnable {

	public void run(){
		final HttpClient client = new DefaultHttpClient();
		long lastUpdate = mainActivity.saveFile.getLong("lastUpdate", 0);
		if(lastUpdate < System.currentTimeMillis()-24*60*60*1000){
			Thread thread = new Thread(new Runnable(){
				public void run(){
					try {
						String responseText = "";
						HttpGet hg = new HttpGet("http://spirit.fh-schmalkalden.de/rest/1.0/news");
						HttpConnectionParams.setSoTimeout(client.getParams(), 10000);
						hg.setHeader("User-Agent", mainActivity.USERAGENT);
						HttpResponse response = client.execute(hg); //News
						responseText = EntityUtils.toString(response.getEntity()); //Antwort in einen String wandeln.
						SQLiteDatabase db = mainActivity.database;
						if(db != null){
							try {
								JSONArray news = new JSONArray(responseText);
								SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss");
								for(int i=0; i<news.length(); i++){
									JSONObject current = news.optJSONObject(i);
									Cursor c = db.rawQuery("SELECT * FROM news WHERE id = "+current.optString("nr"),null);
									int count = c.getCount();
									c.close();
									if(count == 0){
										db.execSQL("INSERT INTO news (id,title,content,author,receivers,date) VALUES ("+current.optString("nr")+",'"+addSlashes(current.optString("subject"))+"','"+addSlashes(current.optString("news"))+"','"+addSlashes(current.optString("writer"))+"','"+addSlashes(current.optString("semester").trim())+"',"+format.parse(current.optString("date")).getTime()+")");
									} else {
										db.execSQL("UPDATE news SET title = '"+addSlashes(current.optString("subject"))+"', content = '"+addSlashes(current.optString("news"))+"',author = '"+addSlashes(current.optString("writer"))+"', receivers = '"+addSlashes(current.optString("semester").trim())+"', date = "+format.parse(current.optString("date")).getTime()+" WHERE id = "+current.optString("nr"));
									}
								}
								Editor e = mainActivity.saveFile.edit();
								e.putLong("lastUpdate", System.currentTimeMillis());
								e.commit();
							} catch(Exception e){
								Log.e("Spirit4Android - Newsupdater",e.getClass()+": "+e.getMessage());
							}
						}
						/*
						try {
							
							new JSONArray(responseText);
							Editor e = mainActivity.saveFile.edit();
							e.putString("newsJSON", responseText);
							e.putLong("lastUpdate", System.currentTimeMillis());
							e.commit();
						} catch(JSONException e){
							Log.e("a","E:"+e.getClass().getName());
						}*/
						
						
					} catch (Exception e) {
						Log.e("Spirit4Android - Client", e.getClass().getName()+": "+e.getMessage());
					}
				}
			});
			thread.run();
		}
	}
	
	public String addSlashes(String pre){
		return pre.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r").replace("\'", "\\\'");
	}

}
