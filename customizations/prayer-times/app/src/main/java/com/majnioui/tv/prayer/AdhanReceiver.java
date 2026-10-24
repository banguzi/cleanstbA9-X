package com.majnioui.tv.prayer;
import android.content.*;
public class AdhanReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){
        if(!AdhanScheduler.enabled(c))return;
        if("PRAYER".equals(i.getAction())){
            long t=i.getLongExtra("when",0),now=System.currentTimeMillis();
            int p=i.getIntExtra("prayer",-1);
            String event=t+":"+p;
            if(AdhanPolicy.shouldPlay(now,t,p,event,AdhanScheduler.prefs(c).getString("last",""))){
                // Commit before playback, so duplicate broadcasts cannot replay the same prayer.
                AdhanScheduler.prefs(c).edit().putString("last",event).commit();
                c.startForegroundService(new Intent(c,AdhanService.class).putExtra("prayer",p));
            }
            AdhanScheduler.scheduleNext(c);
        }else{AdhanScheduler.scheduleNext(c);AdhanScheduler.requestRefresh(c);}
    }
}
