package com.kasoft.pushnot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootCompleteReceiver  extends BroadcastReceiver {
	@Override
    public void onReceive(Context context, Intent intent) {
        Intent startServiceIntent = new Intent(context, MainActivity.class);
        context.startService(startServiceIntent);
    }
}
