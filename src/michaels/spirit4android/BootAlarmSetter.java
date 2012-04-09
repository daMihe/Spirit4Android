package michaels.spirit4android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootAlarmSetter extends BroadcastReceiver {

	@Override
	public void onReceive(Context arg0, Intent arg1) {
		mainActivity.setAlarm(arg0, true);
	}

}
