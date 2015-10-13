package com.example.goahead.makemyday.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by spartan300 on 13/10/15.
 */
public class SunshineAuthenticatorService extends Service {

    // Instance field that stores the authenticator object
    private SunshineAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        // Create a new authenticator object
        mAuthenticator = new SunshineAuthenticator(this);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
