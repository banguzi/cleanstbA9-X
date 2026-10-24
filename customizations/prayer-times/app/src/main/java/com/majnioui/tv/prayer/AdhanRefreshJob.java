package com.majnioui.tv.prayer;
import android.app.job.*;
public class AdhanRefreshJob extends JobService {
    @Override public boolean onStartJob(JobParameters p){
        new Thread(()->{boolean retry=false;try{AdhanScheduler.refresh(this);}catch(Exception e){
            android.util.Log.e("Adhan","refresh failed; retaining cache",e);retry=true;}
            jobFinished(p,retry && AdhanScheduler.enabled(this));}).start();return true;
    }
    @Override public boolean onStopJob(JobParameters p){return AdhanScheduler.enabled(this);}
}
