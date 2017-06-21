package com.tp.koreatech.maptest;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, MqttCallback{


    private GoogleApiClient mGoogleApiClient = null;
    private GoogleMap mGoogleMap = null;
    private Marker currentMarker = null;
    private Marker testMarker = null;

    //디폴트 위치, Seoul
    private static final LatLng DEFAULT_LOCATION = new LatLng(37.56, 126.97);
    private static final String TAG = "googlemap";
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 2002;
    private static final int UPDATE_INTERVAL_MS = 1000;  // 1초
    private static final int FASTEST_UPDATE_INTERVAL_MS = 1000; // 1초

    private AppCompatActivity mActivity;
    boolean askPermissionOnceAgain = false;
    private int myI = 0;
    private Button btn;
    private EditText etext;
    private HashMap<String, Marker> accidentList = new HashMap<>();
    private String clientId = MqttClient.generateClientId();
    private MqttAndroidClient client;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //----------------------------------------------------------------------------------------------------------------
        /*client = new MqttAndroidClient(getApplicationContext(),
                "tcp://192.168.0.41:1883", clientId);

        try{
            MqttConnectOptions options = new MqttConnectOptions();
            options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
            options.setUserName("USERNAME");
            options.setPassword("PASSWORD".toCharArray());
            client.setCallback(this);
            IMqttToken token = client.connect(options);

            token.setActionCallback(new IMqttActionListener() {
                String a = "abc";
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Log.d(a, "onSuccess");
                    String topic1 = "foo/bar";
                    String payload = "the payload";
                    byte[] encodedPayload = new byte[0];
                    try {
                        encodedPayload = payload.getBytes("UTF-8");
                        MqttMessage message = new MqttMessage(encodedPayload);
                        client.publish(topic1, message);
                    } catch (UnsupportedEncodingException | MqttException e) {
                        e.printStackTrace();
                    }

                    Toast.makeText(getApplicationContext(), "onSuccess1", Toast.LENGTH_SHORT).show();
                    String topic = "accident";
                    int qos = 0;
                    try {
                        IMqttToken subToken = client.subscribe(topic, qos);
                        subToken.setActionCallback(new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                // The message was published
                                Toast.makeText(getApplicationContext(), "onSuccess2", Toast.LENGTH_SHORT).show();

                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken,
                                                  Throwable exception) {
                                // The subscription could not be performed, maybe the user was not
                                // authorized to subscribe on the specified topic e.g. using wildcards
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
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.d(a, "onFailure");
                    Toast.makeText(getApplicationContext(), "onFailure1", Toast.LENGTH_SHORT).show();
                }
            });
        }catch(Exception e){
            Log.e("MqttError", e.getMessage());
            Toast.makeText(getApplicationContext(), "error", Toast.LENGTH_SHORT).show();
        }*/

        //----------------------------------------------------------------------------------------------------------------

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        mActivity = this;

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }


    @Override
    protected void onStart() {
        super.onStart();

        //if (mGoogleApiClient != null)
        // mGoogleApiClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mGoogleApiClient!=null)
            mGoogleApiClient.connect();

        //앱 정보에서 퍼미션을 허가했는지를 다시 검사해봐야 한다.
        if (askPermissionOnceAgain) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                askPermissionOnceAgain = false;

                checkPermissions();
            }
        }
    }

    @Override
    protected void onStop() {

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }


    @Override
    public void onPause() {

        //위치 업데이트 중지
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {

            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

            mGoogleApiClient.disconnect();
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {

        if (mGoogleApiClient != null) {
            mGoogleApiClient.unregisterConnectionCallbacks(this);
            mGoogleApiClient.unregisterConnectionFailedListener(this);

            if (mGoogleApiClient.isConnected()) {
                LocationServices.FusedLocationApi
                        .removeLocationUpdates(mGoogleApiClient, this);
                mGoogleApiClient.disconnect();
            }

        }

        super.onDestroy();
    }



    @Override
    public void onMapReady(GoogleMap map) {

        mGoogleMap = map;

        //런타임 퍼미션 요청 대화상자나 GPS 활성 요청 대화상자 보이기전에
        //지도의 초기위치를 서울로 이동
        setCurrentLocation(null, "위치정보 가져올 수 없음", "위치 퍼미션과 GPS 활성 요부 확인하세요");

        mGoogleMap.getUiSettings().setCompassEnabled(true);
        //mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(17));


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { //API 23 이상이면 런타임 퍼미션 처리 필요

            int hasFineLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

            if (hasFineLocationPermission == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions( mActivity, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

            } else {

                if (mGoogleApiClient == null) {
                    buildGoogleApiClient();
                }

                mGoogleMap.setMyLocationEnabled(true);

            }
        } else {

            if (mGoogleApiClient == null) {
                buildGoogleApiClient();
            }

            mGoogleMap.setMyLocationEnabled(true);

        }

        etext = (EditText) findViewById(R.id.ipText);
        btn = (Button) findViewById(R.id.btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String ip;
                /*LatLng newLocation = new LatLng(35.7, 125.5);
                //LatLng newLocation = new LatLng(126.70, 37.12);
                myI++;
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(newLocation);
                markerOptions.title("test" + myI);
                markerOptions.snippet("test1-" + myI);
                                    *//*
                                    markerOptions.draggable(true);
                                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));*//*

                testMarker = mGoogleMap.addMarker(markerOptions);*/
                if(etext.length() != 0){
                    ip = etext.getText().toString();
                    client = new MqttAndroidClient(getApplicationContext(),
                            "tcp://"+ip+":1883", clientId);

                    try{
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

                                if(topic.equals("accident")) {

                                    String[] a = message.toString().split(",");
                                    if(a.length == 3 && accidentList.containsKey(a[0]) == false) {

                                        double[] l = new double[2];
                                        l[0] = Double.parseDouble(a[1]);
                                        l[1] = Double.parseDouble(a[2]);

                                        /////
                                        try {
                                            LatLng newLocation = new LatLng(l[1], l[0]);
                                            Toast.makeText(getApplicationContext(), "message arrived(" + a[0] + "):" + newLocation.latitude + "," + newLocation.longitude, Toast.LENGTH_SHORT).show();

                                            //LatLng newLocation = new LatLng(126.70, 37.12);
                                            myI++;
                                            MarkerOptions markerOptions = new MarkerOptions();
                                            markerOptions.position(newLocation);
                                            markerOptions.title("test" + myI);
                                            markerOptions.snippet("test1-" + myI);
                                    /*
                                    markerOptions.draggable(true);
                                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));*/

                                            testMarker = mGoogleMap.addMarker(markerOptions);
                                            accidentList.put(a[0], testMarker);
                                        } catch (Exception e) {
                                            Log.e("err", e.getMessage());
                                        }
                                    }
                                    else {
                                        Toast.makeText(getApplicationContext(), "" + a.length + ", "+accidentList.containsKey(a[0]), Toast.LENGTH_SHORT).show();
                                    }
                                }
                                else if(topic.equals("remove")){

                                    if(accidentList.containsKey(message.toString()) == true){
                                        Marker temp = accidentList.get(message.toString());
                                        Toast.makeText(getApplicationContext(), "message arrived(remove):" + message.toString(), Toast.LENGTH_SHORT).show();
                                        temp.remove();
                                        accidentList.remove(message.toString());
                                    }
                                    else {
                                        Toast.makeText(getApplicationContext(), "accident is not exist", Toast.LENGTH_SHORT).show();
                                    }

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
                                // We are connected
                                Log.d(a, "onSuccess");
                                String topic1 = "foo/bar";
                                String payload = "the payload";
                                byte[] encodedPayload = new byte[0];
                                try {
                                    encodedPayload = payload.getBytes("UTF-8");
                                    MqttMessage message = new MqttMessage(encodedPayload);
                                    client.publish(topic1, message);
                                } catch (UnsupportedEncodingException | MqttException e) {
                                    e.printStackTrace();
                                }

                                Toast.makeText(getApplicationContext(), "onSuccess1", Toast.LENGTH_SHORT).show();
                                String[] topic = {"accident", "remove"};
                                int[] qos = {0, 0};
                                try {
                                    //IMqttToken subToken = client.subscribe(topic, qos);
                                    IMqttToken subToken = client.subscribe(topic, qos);
                                    subToken.setActionCallback(new IMqttActionListener() {
                                        @Override
                                        public void onSuccess(IMqttToken asyncActionToken) {
                                            // The message was published
                                            Toast.makeText(getApplicationContext(), "onSuccess2", Toast.LENGTH_SHORT).show();

                                        }

                                        @Override
                                        public void onFailure(IMqttToken asyncActionToken,
                                                              Throwable exception) {
                                            // The subscription could not be performed, maybe the user was not
                                            // authorized to subscribe on the specified topic e.g. using wildcards
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
                                // Something went wrong e.g. connection timeout or firewall problems
                                Log.d(a, "onFailure");
                                Toast.makeText(getApplicationContext(), "onFailure1", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }catch(Exception e){
                        Log.e("MqttError", e.getMessage());
                        Toast.makeText(getApplicationContext(), "error", Toast.LENGTH_SHORT).show();
                    }

                }

            }
        });
    }


    @Override
    public void onLocationChanged(Location location) {

        String markerTitle = getCurrentAddress(location);
        String markerSnippet = "위도:"+String.valueOf(location.getLatitude())
                + " 경도:"+String.valueOf(location.getLongitude());

        //현재 위치에 마커 생성
        setCurrentLocation(location, markerTitle, markerSnippet );
    }


    protected synchronized void buildGoogleApiClient() {

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();

    }


    @Override
    public void onConnected(Bundle connectionHint) {

        if (!checkLocationServicesStatus()) {
            showDialogForLocationServiceSetting();
        }

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL_MS);
        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_MS);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);

        }
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Location location = null;
        location.setLatitude(DEFAULT_LOCATION.latitude);
        location.setLongitude(DEFAULT_LOCATION.longitude);

        setCurrentLocation(location, "위치정보 가져올 수 없음",
                "위치 퍼미션과 GPS 활성 요부 확인하세요");
    }



    @Override
    public void onConnectionSuspended(int cause) {
        if ( cause ==  CAUSE_NETWORK_LOST )
            Log.e(TAG, "onConnectionSuspended(): Google Play services " +
                    "connection lost.  Cause: network lost.");
        else if (cause == CAUSE_SERVICE_DISCONNECTED )
            Log.e(TAG,"onConnectionSuspended():  Google Play services " +
                    "connection lost.  Cause: service disconnected");
    }


    public String getCurrentAddress(Location location){

        //지오코더... GPS를 주소로 변환
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses;

        try {

            addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1);
        } catch (IOException ioException) {
            //네트워크 문제
            Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show();
            return "지오코더 서비스 사용불가";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show();
            return "잘못된 GPS 좌표";

        }


        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show();
            return "주소 미발견";

        } else {
            Address address = addresses.get(0);
            return address.getAddressLine(0).toString();
        }

    }


    public boolean checkLocationServicesStatus(){
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }


    public void setCurrentLocation(Location location, String markerTitle, String markerSnippet) {

        if (currentMarker != null) currentMarker.remove();

        if (location != null) {
            LatLng currentLocation = new LatLng( location.getLatitude(), location.getLongitude());

            //마커를 원하는 이미지로 변경해줘야함
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(currentLocation);
            markerOptions.title(markerTitle);
            markerOptions.snippet(markerSnippet);
            markerOptions.draggable(true);
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            currentMarker = mGoogleMap.addMarker(markerOptions);

            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
            return;
        }

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(DEFAULT_LOCATION);
        markerOptions.title(markerTitle);
        markerOptions.snippet(markerSnippet);
        markerOptions.draggable(true);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        currentMarker = mGoogleMap.addMarker(markerOptions);

        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(DEFAULT_LOCATION));

    }


    //여기부터는 런타임 퍼미션 처리을 위한 메소드들
    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermissions() {
        boolean fineLocationRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

        if (hasFineLocationPermission == PackageManager.PERMISSION_DENIED && fineLocationRationale )
            showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");

        else if (hasFineLocationPermission == PackageManager.PERMISSION_DENIED && !fineLocationRationale ) {
            showDialogForPermissionSetting("퍼미션 거부 + Don't ask again(다시 묻지 않음) 체크 박스를 설정한 경우로 설정에서 퍼미션 허가해야합니다.");
        }

        else if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED) {

            if (mGoogleApiClient == null) {
                buildGoogleApiClient();
            }

            mGoogleMap.setMyLocationEnabled(true);


        }
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (permsRequestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION && grantResults.length > 0) {

            boolean permissionAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

            if (permissionAccepted) {

                if (mGoogleApiClient == null) {
                    buildGoogleApiClient();
                }

                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                    mGoogleMap.setMyLocationEnabled(true);
                }


            }
            else {

                checkPermissions();
            }
        }
    }


    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                ActivityCompat.requestPermissions( mActivity, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        });

        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        builder.create().show();
    }

    private void showDialogForPermissionSetting(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(true);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                askPermissionOnceAgain = true;

                Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + mActivity.getPackageName()));
                myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
                myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mActivity.startActivity(myAppSettings);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        builder.create().show();
    }


    //여기부터는 GPS 활성화를 위한 메소드들
    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
                + "위치 설정을 수정하실래요?" );
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id){
                dialog.cancel();
            }
        });
        builder.create().show();
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case GPS_ENABLE_REQUEST_CODE:

                //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }

                        if (ActivityCompat.checkSelfPermission(this,
                                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                            mGoogleMap.setMyLocationEnabled(true);
                        }

                        return;
                    }
                }
                else{
                    setCurrentLocation(null, "위치정보 가져올 수 없음", "위치 퍼미션과 GPS 활성 요부 확인하세요");
                }

                break;
        }
    }

    @Override
    public void connectionLost(Throwable cause) {

    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        if(topic.equals("accident")) {
            //Toast.makeText(getApplicationContext(), "message arrived", Toast.LENGTH_SHORT).show();
            String[] a = message.getPayload().toString().split(",");
            double[] l = new double[2];
            l[0] = Double.parseDouble(a[0]);
            l[1] = Double.parseDouble(a[1]);
            Toast.makeText(getApplicationContext(), "message arrived:"+l[0]+","+l[1]+".", Toast.LENGTH_SHORT).show();


            LatLng newLocation = new LatLng(l[0], l[1]);
            myI++;
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(newLocation);
            markerOptions.title("test" + myI);
            markerOptions.snippet("test1-" + myI);
            mGoogleMap.addMarker(markerOptions);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }
}

