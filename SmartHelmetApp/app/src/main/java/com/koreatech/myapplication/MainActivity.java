package com.koreatech.myapplication;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity  {
    SmartHelmetService mService;
    boolean mBound = false;
    Button cBtn, start, stop;
    EditText etext;
    String ip;
    private int i = 0;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            SmartHelmetService.SmartHelmetBinder binder = (SmartHelmetService.SmartHelmetBinder) iBinder;

            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etext = (EditText) findViewById(R.id.ipText);
        cBtn = (Button) findViewById(R.id.cancel);
        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);

    }

    public boolean isServiceRunningCheck() {
        ActivityManager manager = (ActivityManager) this.getSystemService(Activity.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.koreatech.myapplication.SmartHelmetService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(isServiceRunningCheck()) {
            SharedPreferences settings = getSharedPreferences("MyPrefs", MODE_PRIVATE);
            ip = settings.getString("ip", "");

            Intent intent = new Intent(this, SmartHelmetService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            mBound = true;
            etext.setText(ip);
        }
    }
    @Override
    protected void onPause(){
        super.onPause();
        if(mBound) {
            SharedPreferences settings = getSharedPreferences("MyPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();

            editor.putString("ip", ip);
            editor.apply();

            etext.setText(ip);

            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(mBound) {
            unbindService(mConnection);
        }
        Log.i("MobileProgramming", "onDestory()");
    }


    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.start:
                if(mBound == false) {
                    if(etext.length() != 0) {
                        ip = etext.getText().toString();
                        Intent intent = new Intent(this, SmartHelmetService.class);
                        intent.putExtra("IP", ip);
                        startService(intent);
                        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
                    }
                }
                break;
            case R.id.stop:
                if(mBound == true) {
                    unbindService(mConnection);
                    stopService(new Intent(this, SmartHelmetService.class));
                    mBound = false;
                }
                break;
            case R.id.cancel:
                if(mBound == true) {
                    mService.musicPause();
                    mService.cancelAccident();
                }
                break;
        }
    }
}

