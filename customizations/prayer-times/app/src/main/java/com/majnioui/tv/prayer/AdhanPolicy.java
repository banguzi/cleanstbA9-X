package com.majnioui.tv.prayer;
/** Ignore stale, early, invalid and duplicate events. Never replay a missed prayer after boot. */
final class AdhanPolicy {
    static boolean shouldPlay(long now,long due,int prayer,String event,String last){
        return due>0 && prayer>=0 && prayer<5 && now>=due && now-due<=90000 && !event.equals(last);
    }
}
