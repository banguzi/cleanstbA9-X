package com.majnioui.tv.prayer;
import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;

/** Runs only for the duration of a recording; no permanent background player. */
public class AdhanService extends Service {
    private MediaPlayer player;
    private AdhanDisplay display;
    private AudioFocusRequest focus;
    private AudioManager audio;
    private boolean foreground;
    @Override public IBinder onBind(Intent i){return null;}
    @Override public int onStartCommand(Intent i,int flags,int id){
        if(i==null || "STOP".equals(i.getAction())){stopSelf();return START_NOT_STICKY;}
        if(player!=null)return START_NOT_STICKY;
        int prayer=i.getIntExtra("prayer",-1);
        String title=(i.getBooleanExtra("test",false)?"Uji azan ":"Azan ")+(prayer>=0 && prayer<5?AdhanScheduler.LABELS[prayer]:"biasa");
        NotificationManager nm=getSystemService(NotificationManager.class);
        NotificationChannel channel=new NotificationChannel("adhan_playback","Pemutaran azan",NotificationManager.IMPORTANCE_LOW);
        channel.setSound(null,null);nm.createNotificationChannel(channel);
        PendingIntent stop=PendingIntent.getService(this,8,new Intent(this,AdhanService.class).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE);
        PendingIntent open=PendingIntent.getActivity(this,9,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE);
        startForeground(7,new Notification.Builder(this,"adhan_playback").setSmallIcon(R.drawable.ic_fajr)
            .setContentTitle(title).setContentText("Lokasi Anda · tekan Hentikan untuk berhenti")
            .setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,"Hentikan",stop).build()).build());
        foreground=true;
        audio=getSystemService(AudioManager.class);
        AudioAttributes attr=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
        focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(attr)
            .setOnAudioFocusChangeListener(change->{if(change<0)stopSelf();}).build();
        if(audio.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){stopSelf();return START_NOT_STICKY;}
        try{
            player=new MediaPlayer();player.setAudioAttributes(attr);player.setWakeMode(this,PowerManager.PARTIAL_WAKE_LOCK);
            try(android.content.res.AssetFileDescriptor fd=getResources().openRawResourceFd(prayer==0 ? R.raw.adhan_subuh : R.raw.adhan_standard)){
                player.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());
            }
            player.setOnCompletionListener(p->{android.util.Log.i("Adhan","recording completed");stopSelf();});
            player.setOnErrorListener((p,what,extra)->{stopSelf();return true;});
            player.prepare();
            try{display=new AdhanDisplay(this,prayer,player.getDuration(),()->stopSelf());}
            catch(Exception e){android.util.Log.e("Adhan","display unavailable",e);}
            player.start();android.util.Log.i("Adhan","playing "+title+" audio="+(prayer==0?"adzan-subuh.mp3":"adzan.mp3")+" duration="+player.getDuration());
        }catch(Exception e){android.util.Log.e("Adhan","playback",e);stopSelf();}
        return START_NOT_STICKY;
    }
    @Override public void onDestroy(){
        if(display!=null){display.close();display=null;android.util.Log.i("Adhan","display closed");}
        if(player!=null){player.release();player=null;}
        if(audio!=null && focus!=null)audio.abandonAudioFocusRequest(focus);
        if(foreground)stopForeground(true);
        super.onDestroy();
    }
}
