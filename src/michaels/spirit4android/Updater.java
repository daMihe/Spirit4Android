package michaels.spirit4android;

import java.text.SimpleDateFormat;
import java.util.Locale;

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
						HttpConnectionParams.setSoTimeout(client.getParams(), 10000);
						SQLiteDatabase db = mainActivity.database;
						if(db != null){
							// News-Update
							HttpGet hg = new HttpGet("http://spirit.fh-schmalkalden.de/rest/1.0/news");
							hg.setHeader("User-Agent", mainActivity.USERAGENT);
							HttpResponse response = client.execute(hg); //News
							responseText = EntityUtils.toString(response.getEntity()); //Antwort in einen String wandeln.
							// read answer
							try {
								JSONArray news = new JSONArray(responseText);
								SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss Z",Locale.ENGLISH);
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
							
							// Sparetime-Update (only if user wants so)
							if(mainActivity.saveFile.getBoolean("loadsparetimefromacl5m", false)){
								HttpGet stg = new HttpGet("http://acl5m.org/spirit-sparetime.php");
								stg.setHeader("User-Agent", mainActivity.USERAGENT);
								HttpResponse stresponse = client.execute(stg); // Sparetime
								String stresponseText = EntityUtils.toString(stresponse.getEntity()); //Antwort in einen String wandeln.
								try {
									// parse sparetime into database
									JSONArray mainarray = new JSONArray(stresponseText);
									for(int i=0; i<mainarray.length(); ++i){
										JSONObject current = mainarray.getJSONObject(i);
										Cursor c = db.rawQuery("SELECT * FROM sparetime WHERE desc = ?", new String[]{
											current.getString("description")
										});
										
										// do not overwrite existing sparetimes (check for title)
										if(c.getCount()==0){
											db.execSQL("INSERT OR IGNORE INTO sparetime (desc,start,stop) VALUES (?,?,?)",new String[]{
												current.getString("description"),
												current.getLong("timeStart")*1000+"",
												current.getLong("timeEnd")*1000+""
											});
										}
										c.close();
									}
								} catch(Exception e){
									Log.e("Spirit4Android - Sparetime-Updater", e.getClass().getName()+": "+e.getMessage());
								}
							}
						}						
						
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
