package michaels.spirit4android;

import java.text.DateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.TextView;

public class newsActivity extends Activity {
	
	public static int id;
	
	public void onCreate(Bundle b){
		super.onCreate(b);
		this.setContentView(R.layout.news);
		Cursor c = mainActivity.database.rawQuery("SELECT * FROM news WHERE id = "+id,null);
		if(c.getCount() != 0){
			c.moveToFirst();
			// Title
			TextView newsTitle = (TextView) this.findViewById(R.id.newsTitle);
			newsTitle.setText(c.getString(c.getColumnIndex("title")));
			// written by <author> at Receivers
			TextView newsDesc = (TextView) this.findViewById(R.id.newsDesc);
			String who = "";
			String[] befWho = c.getString(c.getColumnIndex("receivers")).split(" ");
			for(String bw:befWho)
				who += " "+(bw.equals("") ? "" : (bw.equals("semester") ? getString(R.string.LANG_ALLSEMESTERS) : bw))+",";
			if(who.length() != 0)
				who = who.substring(0, who.length()-1);
			if(who.length() == 0)
				who = getString(R.string.LANG_ALLSEMESTERS); 
			newsDesc.setText(String.format(getString(R.string.LANG_WRITTENBYFOR), c.getString(c.getColumnIndex("author")), DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(c.getLong(c.getColumnIndex("date")))),who));
			WebView newsDisp = (WebView) this.findViewById(R.id.newsDisp);
			newsDisp.setBackgroundColor(android.R.color.black);
			String html = "";
			String[] preHTML = c.getString(c.getColumnIndex("content")).split("%\\{");
			for(String s:preHTML){
				html += (s.indexOf("%")!=-1 ? "<span style=\""+s.replaceAll("\\}", "\">").replaceAll("%", "</span>") : s).replaceAll("\\r\\n","<br />");
			}
			while(html.matches("(.*\".*(\":http)s?(://).*)")){
				Pattern p = Pattern.compile("(\"[^<>]*(\":http)s?(://)[^\\s]+)");
				Matcher m = p.matcher(html);
				m.find();
				int sPosition = m.start();
				int ePosition = m.end();
				String s = html.substring(sPosition, ePosition);
				String newS = "<a href=\""+s.substring(s.indexOf(":")+1)+"\">"+s.substring(1, s.indexOf("\"",2))+"</a>";
				html = html.replace(s, newS);	
			}
			while(html.matches(".*[\\*]{2}.*[\\*]{2}.*")){
				html = html.replaceFirst("[\\*]{2}", "<strong>");
				html = html.replaceFirst("[\\*]{2}", "</strong>");
			}
			html = html.replace("\\r\\n", "<br />");
			html = "<html><head><meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" /></head><body style=\"color:#fff\">"+html+"</body></html>";
			newsDisp.loadData(html, "text/html", "UTF-8");
		}
		c.close();		
	}
}
