package com.majnioui.tv.prayer;

import android.app.*;
import android.app.job.*;
import android.content.*;
import android.os.Build;
import org.json.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

/** Local, opt-in calendar cache. Only one future prayer alarm is active. */
public final class AdhanScheduler {
    static final String[] KEYS={"Fajr","Dhuhr","Asr","Maghrib","Isha"};
    static final String[] LABELS={"Subuh","Zuhur","Asar","Magrib","Isya"};
    static final TimeZone WIT=TimeZone.getTimeZone("Asia/Jayapura");
    static final int REFRESH=421, NOW=422;
    static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("adhan",0);}
    static boolean enabled(Context c){return prefs(c).getBoolean("enabled",false);}
    static PendingIntent alarm(Context c,long when,int prayer){
        Intent i=new Intent(c,AdhanReceiver.class).setAction("PRAYER").putExtra("when",when).putExtra("prayer",prayer);
        return PendingIntent.getBroadcast(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    static void setEnabled(Context c,boolean value){
        prefs(c).edit().putBoolean("enabled",value).commit();
        if(value){scheduleNext(c);requestRefresh(c);}
        else{c.getSystemService(AlarmManager.class).cancel(alarm(c,0,0));
            JobScheduler j=c.getSystemService(JobScheduler.class);j.cancel(REFRESH);j.cancel(NOW);
            c.stopService(new Intent(c,AdhanService.class));}
    }
    static void requestRefresh(Context c){
        if(!enabled(c))return;
        JobScheduler j=c.getSystemService(JobScheduler.class);
        ComponentName service=new ComponentName(c,AdhanRefreshJob.class);
        j.schedule(new JobInfo.Builder(REFRESH,service).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(24*60*60*1000L).setPersisted(true).build());
        j.schedule(new JobInfo.Builder(NOW,service).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(0).build());
    }
    static synchronized void scheduleNext(Context c){
        AlarmManager a=c.getSystemService(AlarmManager.class);
        a.cancel(alarm(c,0,0));
        if(!enabled(c))return;
        long now=System.currentTimeMillis(),best=Long.MAX_VALUE;int prayer=-1;
        try{
            JSONArray events=new JSONArray(prefs(c).getString("events","[]"));
            for(int i=0;i<events.length();i++){
                JSONObject e=events.getJSONObject(i);long t=e.getLong("time");
                if(t>now && t<best){best=t;prayer=e.getInt("prayer");}
            }
            if(prayer>=0){
                if(Build.VERSION.SDK_INT>=31 && !a.canScheduleExactAlarms())return;
                a.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,best,alarm(c,best,prayer));
                prefs(c).edit().putLong("next",best).putInt("prayer",prayer).apply();
            }else prefs(c).edit().remove("next").apply();
        }catch(Exception e){android.util.Log.e("Adhan","schedule",e);}
    }
    static String summary(Context c){
        if(!enabled(c))return "Azan otomatis nonaktif";
        long t=prefs(c).getLong("next",0);
        if(t<System.currentTimeMillis())return "Menyiapkan jadwal azan…";
        SimpleDateFormat f=new SimpleDateFormat("dd MMM HH:mm",new Locale("id","ID"));f.setTimeZone(WIT);
        return "Azan berikut: "+LABELS[prefs(c).getInt("prayer",0)]+" · "+f.format(new Date(t))+" WIT";
    }
    static void refresh(Context c)throws Exception{
        if(!enabled(c))return;
        // Keep a complete current and following month; a failed fetch never erases the old cache.
        Calendar month=Calendar.getInstance(WIT);
        String key=month.get(Calendar.YEAR)+"-"+month.get(Calendar.MONTH);
        if(key.equals(prefs(c).getString("cacheMonth",""))){scheduleNext(c);return;}
        JSONArray all=new JSONArray();
        SimpleDateFormat parser=new SimpleDateFormat("dd-MM-yyyy HH:mm",Locale.ROOT);parser.setTimeZone(WIT);parser.setLenient(false);
        for(int m=0;m<2;m++){
            String url="https://api.aladhan.com/v1/calendar/"+month.get(Calendar.YEAR)+"/"+(month.get(Calendar.MONTH)+1)
                +"?latitude="+c.getString(R.string.latitude)+"&longitude="+c.getString(R.string.longitude)
                +"&method="+c.getString(R.string.method)+"&timezonestring=Asia%2FJayapura";
            HttpURLConnection conn=(HttpURLConnection)new URL(url).openConnection();conn.setConnectTimeout(15000);conn.setReadTimeout(20000);
            StringBuilder text=new StringBuilder();
            try(BufferedReader r=new BufferedReader(new InputStreamReader(conn.getInputStream(),"UTF-8"))){
                String line;while((line=r.readLine())!=null)text.append(line);
            }finally{conn.disconnect();}
            JSONObject response=new JSONObject(text.toString());
            if(response.getInt("code")!=200)throw new IOException("Calendar unavailable");
            JSONArray days=response.getJSONArray("data");
            if(days.length()<28)throw new IOException("Incomplete calendar");
            for(int d=0;d<days.length();d++){
                JSONObject day=days.getJSONObject(d),times=day.getJSONObject("timings");
                String date=day.getJSONObject("date").getJSONObject("gregorian").getString("date");
                for(int p=0;p<KEYS.length;p++){
                    String hhmm=times.getString(KEYS[p]).substring(0,5);
                    all.put(new JSONObject().put("time",parser.parse(date+" "+hhmm).getTime()).put("prayer",p));
                }
            }
            month.add(Calendar.MONTH,1);
        }
        prefs(c).edit().putString("events",all.toString()).putString("cacheMonth",key).commit();
        scheduleNext(c);
    }
}
