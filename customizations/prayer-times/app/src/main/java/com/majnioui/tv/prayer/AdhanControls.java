package com.majnioui.tv.prayer;
import android.app.*;
import android.content.*;
import android.os.Build;
import android.widget.*;

final class AdhanControls {
    static LinearLayout create(Activity c){
        LinearLayout bar=new LinearLayout(c);bar.setGravity(android.view.Gravity.CENTER_VERTICAL);
        Switch enabled=new Switch(c);enabled.setText("Azan otomatis  ");enabled.setTextColor(0xFFFFFFFF);enabled.setTextSize(16);
        enabled.setChecked(AdhanScheduler.enabled(c));
        enabled.setOnCheckedChangeListener((b,on)->{
            if(on && !android.provider.Settings.canDrawOverlays(c)){
                b.setChecked(false);
                c.startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,android.net.Uri.parse("package:"+c.getPackageName())));return;
            }
            if(on && Build.VERSION.SDK_INT>=31 && !c.getSystemService(AlarmManager.class).canScheduleExactAlarms()){
                b.setChecked(false);c.startActivity(new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    android.net.Uri.parse("package:"+c.getPackageName())));return;
            }
            AdhanScheduler.setEnabled(c,on);
            Toast.makeText(c,on?"Azan otomatis aktif. Mengambil jadwal…":"Azan otomatis dimatikan",Toast.LENGTH_SHORT).show();
        });
        bar.addView(enabled,new LinearLayout.LayoutParams(0,-2,1));
        Button test=new Button(c);test.setText("Coba azan");test.setTextSize(14);
        test.setOnClickListener(v->new AlertDialog.Builder(c).setTitle("Pilih uji azan")
            .setItems(new String[]{"Azan biasa (Zuhur, Asar, Magrib, Isya)","Azan Subuh"},(dialog,which)->{
                if(!android.provider.Settings.canDrawOverlays(c)){
                    c.startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,android.net.Uri.parse("package:"+c.getPackageName())));return;
                }
                c.startForegroundService(new Intent(c,AdhanService.class).putExtra("prayer",which==1?0:1).putExtra("test",true));
            }).show());bar.addView(test);
        Button stop=new Button(c);stop.setText("Hentikan");stop.setTextSize(14);
        stop.setOnClickListener(v->c.stopService(new Intent(c,AdhanService.class)));bar.addView(stop);
        Button info=new Button(c);info.setText("Info audio");info.setTextSize(14);
        info.setOnClickListener(v->new AlertDialog.Builder(c).setTitle("Audio azan milik pengguna")
            .setMessage("Subuh: adzan-subuh.mp3 (sekitar 3 menit 7 detik).\nZuhur, Asar, Magrib, Isya: adzan.mp3 (sekitar 3 menit 39 detik).\n\nBerkas disimpan utuh secara offline. Volume mengikuti media STB. Layar azan muncul selama pemutaran lalu menutup otomatis.\n\nHadis: HR. al-Bukhari no. 645, dari Abdullah bin Umar; keutamaan salat berjamaah. Sumber: https://sunnah.com/bukhari:645. Terjemahan Indonesia dari makna matan.\n\nJadwal Aladhan metode Kemenag, Lokasi Anda, WIT. Cocokkan dengan masjid setempat.")
            .setPositiveButton("Tutup",null).show());bar.addView(info);
        return bar;
    }
}
