package com.koreatech.myapplication;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;


public class SmartHelmetService extends Service {
    static final String TAG = "MobileProgramming";
    static final String USERID = "userID_1";
    MediaPlayer mediaPlayer;
    String ip;
    PendingIntent pIntent;
    private String clientId = MqttClient.generateClientId();
    private MqttAndroidClient client=null;

    Notification noti;

    public class SmartHelmetBinder extends Binder {
        SmartHelmetService getService() { return SmartHelmetService.this; }
    }

    private final IBinder mBinder = new SmartHelmetBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");

        if(mediaPlayer != null){
            mediaPlayer.reset();
        } else
            mediaPlayer = MediaPlayer.create(this, R.raw.testmusic1);

        mediaPlayer.setLooping(true);

        if(client == null) {
            ip = intent.getStringExtra("IP");

            if (ip.equals("") == false) {
                client = new MqttAndroidClient(getApplicationContext(),
                        "tcp://" + ip + ":1883", clientId);

                try {
                    MqttConnectOptions options = new MqttConnectOptions();
                    options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
                    options.setUserName("USERNAME");
                    options.setPassword("PASSWORD".toCharArray());
                    client.setCallback(new MqttCallback() {
                        @Override
                        public void connectionLost(Throwable cause) {

                        }

                        @Override
                        public void messageArrived(String topic, MqttMessage message) throws Exception {
                            Toast.makeText(getApplicationContext(), "message arrived!!!", Toast.LENGTH_SHORT).show();

                            if (topic.equals("user/id/" + USERID+"/test")) {
                                musicStart();
                                Intent intent;
                                intent = new Intent(getApplicationContext(), MainActivity.class);

                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                                startActivity(intent);
                            }
                        }

                        @Override
                        public void deliveryComplete(IMqttDeliveryToken token) {

                        }
                    });
                    IMqttToken token = client.connect(options);

                    token.setActionCallback(new IMqttActionListener() {
                        String a = "abc";

                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                            Log.d(a, "onSuccess");
                            String topic = "user/id/" + USERID+"/add";
                            String payload = USERID;
                            byte[] encodedPayload = new byte[0];
                            try {
                                encodedPayload = payload.getBytes("UTF-8");
                                MqttMessage message = new MqttMessage(encodedPayload);
                                client.publish(topic, message);
                            } catch (UnsupportedEncodingException | MqttException e) {
                                e.printStackTrace();
                            }
                            topic = "user/id/" + USERID+"/test";
                            int qos = 0;
                            try {
                                IMqttToken subToken = client.subscribe(topic, qos);
                                subToken.setActionCallback(new IMqttActionListener() {
                                    @Override
                                    public void onSuccess(IMqttToken asyncActionToken) {
                                        Toast.makeText(getApplicationContext(), "onSuccess2", Toast.LENGTH_SHORT).show();

                                    }

                                    @Override
                                    public void onFailure(IMqttToken asyncActionToken,
                                                          Throwable exception) {
                                        Toast.makeText(getApplicationContext(), "onFailure2", Toast.LENGTH_SHORT).show();

                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.e("taga", e.toString());
                                Toast.makeText(getApplicationContext(), "err", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                            Log.d(a, "onFailure");
                            Toast.makeText(getApplicationContext(), "onFailure1", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    Log.e("MqttError", e.getMessage());
                    Toast.makeText(getApplicationContext(), "error", Toast.LENGTH_SHORT).show();
                }

            }


        }
        intent = new Intent(this, MainActivity.class);
        pIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        noti = new Notification.Builder(this)
                .setContentTitle(getText(R.string.app_name))
                .setContentText(ip)
                .setSmallIcon(R.drawable.ic_stop_black_24dp)
                .setContentIntent(pIntent)
                .build();

        startForeground(123, noti);

        return START_STICKY;
    }

    public void onDestroy() {
        Log.d(TAG, "onDestroy()");

        if(mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if(client != null){
            try {
                client.disconnect();
                client.close();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    public void musicPause() {
        mediaPlayer.pause();
    }

    public void cancelAccident(){
        String topic1 = "cancel";
        String payload = USERID;
        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            client.publish(topic1, message);
        } catch (UnsupportedEncodingException | MqttException e) {
            Toast.makeText(getApplicationContext(), "err", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        musicPause();
    }

    public void musicStart(){
        mediaPlayer.start();
    }

    public boolean isPlaying(){
        if(mediaPlayer != null) {
            return mediaPlayer.isPlaying();
        } else return false;
    }
}
