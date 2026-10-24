package com.majnioui.tv.prayer;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.Locale;

/** A temporary image + readable text window, owned by the playback service. */
final class AdhanDisplay {
    private final Context context;
    private final WindowManager windows;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private FrameLayout root;
    private final Typeface font;
    private TextView remaining;
    private ProgressBar progress;
    private final long started;
    private final int duration;
    private final Runnable ticker=new Runnable(){public void run(){
        if(root==null)return;
        int elapsed=(int)(android.os.SystemClock.elapsedRealtime()-started);
        int seconds=Math.max(0,(duration-elapsed+999)/1000);
        progress.setProgress(Math.min(duration,elapsed));
        remaining.setText("Azan sedang berkumandang · "+String.format(Locale.ROOT,"%d:%02d",seconds/60,seconds%60)+" tersisa");
        handler.postDelayed(this,1000);
    }};
    AdhanDisplay(Context context,int prayer,int duration,Runnable stop){
        this.context=context;this.duration=duration;started=android.os.SystemClock.elapsedRealtime();
        windows=context.getSystemService(WindowManager.class);
        font=context.getResources().getFont(R.font.outfit);
        if(!Settings.canDrawOverlays(context))throw new IllegalStateException("Izin tampil di atas aplikasi belum aktif");
        root=new FrameLayout(context){
            @Override public boolean dispatchKeyEvent(KeyEvent event){
                if(event.getKeyCode()==KeyEvent.KEYCODE_BACK){if(event.getAction()==KeyEvent.ACTION_UP)stop.run();return true;}
                return super.dispatchKeyEvent(event);
            }
        };
        root.setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        ImageView image=new ImageView(context);image.setImageResource(R.drawable.adhan_background);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);root.addView(image,new FrameLayout.LayoutParams(-1,-1));
        View shade=new View(context);shade.setBackgroundColor(0xD508131C);root.addView(shade,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);content.setGravity(Gravity.CENTER);
        content.setPadding(dp(64),dp(28),dp(64),dp(28));root.addView(content,new FrameLayout.LayoutParams(-1,-1));
        TextClock clock=new TextClock(context);clock.setTimeZone("Asia/Jayapura");clock.setFormat12Hour("HH:mm 'WIT'");clock.setFormat24Hour("HH:mm 'WIT'");
        clock.setTextColor(0xFFDCC995);clock.setTextSize(24);clock.setTypeface(font);clock.setGravity(Gravity.CENTER);content.addView(clock);
        TextView heading=text("WAKTU SOLAT TELAH TIBA",42,0xFFFFFFFF);heading.setTypeface(font,Typeface.BOLD);heading.setPadding(0,dp(10),0,dp(4));content.addView(heading);
        String label=prayer>=0 && prayer<5?AdhanScheduler.LABELS[prayer]:"Salat";
        TextView place=text("Waktu "+label+" · Lokasi Anda",22,0xFFE4D497);content.addView(place);
        content.addView(new View(context),new LinearLayout.LayoutParams(1,0,1));
        TextView theme=text("KEUTAMAAN SALAT BERJAMAAH",17,0xFFE4D497);theme.setLetterSpacing(.12f);content.addView(theme);
        TextView narrator=text("Dari Abdullah bin Umar radhiyallahu ‘anhuma, Rasulullah ﷺ bersabda:",18,0xDDFFFFFF);narrator.setPadding(0,dp(12),0,dp(12));content.addView(narrator);
        TextView arabic=text("صَلَاةُ الْجَمَاعَةِ تَفْضُلُ صَلَاةَ الْفَذِّ بِسَبْعٍ وَعِشْرِينَ دَرَجَةً",32,0xFFFFFFFF);
        arabic.setTypeface(Typeface.DEFAULT);arabic.setPadding(0,0,0,dp(12));content.addView(arabic);
        TextView meaning=text("“Salat berjamaah lebih utama daripada salat sendirian dengan dua puluh tujuh derajat.”",26,0xFFFFFFFF);meaning.setLineSpacing(dp(5),1);content.addView(meaning);
        TextView source=text("HR. al-Bukhari no. 645 · Matan lengkap · sunnah.com/bukhari:645",16,0xCCFFFFFF);source.setPadding(0,dp(12),0,0);content.addView(source);
        content.addView(new View(context),new LinearLayout.LayoutParams(1,0,1));
        remaining=text("Azan sedang berkumandang",18,0xFFE4D497);content.addView(remaining);
        progress=new ProgressBar(context,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(duration);
        LinearLayout.LayoutParams line=new LinearLayout.LayoutParams(dp(560),dp(5));line.setMargins(0,dp(10),0,dp(10));content.addView(progress,line);
        Button button=new Button(context);button.setText("Hentikan azan");button.setOnClickListener(v->stop.run());content.addView(button);
        TextView foot=text("Tampilan ini akan menutup otomatis setelah azan selesai.",15,0xBBFFFFFF);content.addView(foot);
        WindowManager.LayoutParams params=new WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            android.graphics.PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.TOP|Gravity.START;params.setTitle("Prayer Times — layar azan");
        windows.addView(root,params);button.requestFocus();handler.post(ticker);
    }
    void close(){
        handler.removeCallbacksAndMessages(null);
        if(root!=null){try{windows.removeViewImmediate(root);}catch(IllegalArgumentException e){android.util.Log.w("Adhan","display already removed",e);}finally{root=null;}}
    }
    private TextView text(String value,int sp,int color){
        TextView v=new TextView(context);v.setText(value);v.setTextSize(sp);v.setTextColor(color);v.setTypeface(font);
        v.setGravity(Gravity.CENTER);v.setIncludeFontPadding(false);return v;
    }
    private int dp(int n){return Math.round(n*context.getResources().getDisplayMetrics().density);}
}
