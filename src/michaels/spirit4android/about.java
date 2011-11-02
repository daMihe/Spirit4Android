package michaels.spirit4android;

import java.util.Locale;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class about extends Activity {

	public void onCreate(Bundle bundle){
		super.onCreate(bundle);
		this.setContentView(R.layout.about);
		TextView tv = (TextView) this.findViewById(R.id.aboutVersion);
		tv.setText(String.format(Locale.ENGLISH,"Version: %4.1f",mainActivity.VERSION));
	}
	
}
