package michaels.spirit4android;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.widget.TextView;

public class about extends Activity {

	public void onCreate(Bundle bundle){
		super.onCreate(bundle);
		this.setContentView(R.layout.about);
		TextView tv = (TextView) this.findViewById(R.id.aboutVersion);
		try {
			PackageInfo pi = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
			tv.setText(String.format("Version: %s (Build %d)",pi.versionName,pi.versionCode));
		} catch (NameNotFoundException e) {}
	}
	
}
