package com.banguzi.tv.cctv;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Receives config.json over ADB via `content write` (guarded by WRITE_SECURE_SETTINGS),
 * imports it into SharedPreferences, and triggers an immediate preview refresh.
 */
public class ConfigProvider extends ContentProvider {
    private static final String NAME = "config.json";

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (mode == null || !mode.contains("w") || !NAME.equals(uri.getLastPathSegment())) {
            throw new FileNotFoundException("unsupported uri");
        }
        Context context = getContext();
        if (context == null) {
            throw new FileNotFoundException("no context");
        }
        File target = new File(context.getFilesDir(), CctvConfig.PUSHED_FILE);
        try {
            return ParcelFileDescriptor.open(
                    target,
                    ParcelFileDescriptor.MODE_WRITE_ONLY
                            | ParcelFileDescriptor.MODE_CREATE
                            | ParcelFileDescriptor.MODE_TRUNCATE,
                    new Handler(Looper.getMainLooper()),
                    err -> {
                        if (err == null && CctvConfig.importIfPresent(context)) {
                            HomeWatchService.reloadConfig(context);
                        }
                    });
        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }
}
