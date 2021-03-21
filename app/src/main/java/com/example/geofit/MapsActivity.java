package com.example.geofit;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.gson.Gson;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LocationManager locationManager;
    private boolean isStartLocationKnown = false;
    private boolean isRunStarted = false;
    private PolylineOptions line = new PolylineOptions();
    private Polyline polyline = null;
    private LatLngBounds.Builder builder;
    private ArrayList<LatLng> points = new ArrayList<>();

    private TextView tvDistance;
    private Button finish;
    private Button buttonStart;
    private NestedScrollView nestedScrollView;
    private BottomSheetBehavior behavior;
    private TextView modeWalk;
    private TextView modeFastWalk;
    private TextView modeScandinavianWalk;
    private TextView tvTimer;

    private float summaryDistance = 0;
    private Date startRunning;
    private Date stopRunning;

    private static final int DEFAULT_ZOOM = 18;
    private static final int START_TIMER_VALUE = 4;

    //Перевод камеры в текущее местоположение
    private GoogleMap.OnMyLocationChangeListener myLocationChangeListener = new GoogleMap.OnMyLocationChangeListener() {
        @Override
        public void onMyLocationChange(Location location) {
            if (polyline != null) polyline.remove();

            //Рассчет пройденной дистанции
            if (points.size() > 1 && isRunStarted){
                float[] result = new float[1];
                Location.distanceBetween(points.get(points.size() - 1).latitude, points.get(points.size()-1).longitude, points.get(points.size() - 2).latitude, points.get(points.size() - 2).longitude, result);
                summaryDistance += result[0];
                String s = String.format("%.1f м", summaryDistance);
                tvDistance.setText(s);
            }

            if (!isStartLocationKnown){
                LatLng myStartPosition = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.addMarker(new MarkerOptions().position(myStartPosition).title("Start")).setIcon(BitmapDescriptorFactory.fromResource(R.drawable.flag));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myStartPosition, DEFAULT_ZOOM));
                points.add(myStartPosition);
                isStartLocationKnown = true;
            }

            LatLng loc = new LatLng(location.getLatitude(), location.getLongitude());
            points.add(loc);
            line.add(loc);
            builder.include(loc);
            polyline = mMap.addPolyline(line);
            builder.build();
        }
    };

    private GoogleMap.OnMyLocationClickListener myLocationClickListener = new GoogleMap.OnMyLocationClickListener() {
        @Override
        public void onMyLocationClick(@NonNull Location location) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), DEFAULT_ZOOM));
        }
    };

    private void onRunStart(){
        isRunStarted = true;
        behavior.setPeekHeight(0, true);
        nestedScrollView.setVisibility(View.INVISIBLE);
        tvTimer.setVisibility(View.VISIBLE);
        tvTimer.setText(String.valueOf(START_TIMER_VALUE));
        CountDownTimer countDownTimer = new CountDownTimer((START_TIMER_VALUE + 1)*1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int currentValue = Integer.parseInt(tvTimer.getText().toString());
                if (currentValue != 0)tvTimer.setText(String.valueOf(currentValue - 1));
                else tvTimer.setText("Start!");
            }

            @Override
            public void onFinish() {
                tvTimer.setVisibility(View.INVISIBLE);
                Toast.makeText(MapsActivity.this, "Run!", Toast.LENGTH_SHORT).show();
                finish.setVisibility(View.VISIBLE);
            }
        }.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        //Связь с разметкой
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        tvDistance = findViewById(R.id.tv_distance);
        finish = findViewById(R.id.finish_workout);
        buttonStart = findViewById(R.id.start_button);
        nestedScrollView = findViewById(R.id.modes_of_walk_nested);
        modeFastWalk = findViewById(R.id.mode_fast_walk);
        modeScandinavianWalk = findViewById(R.id.mode_scandinavian_walk);
        modeWalk = findViewById(R.id.mode_walk);
        tvTimer = findViewById(R.id.timer);

        //Подготовка элементов карты
        SharedPreferences sp = getSharedPreferences("status", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        modeWalk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRunStart();
                editor.putString("mode", "Ходьба");
                editor.apply();
            }
        });
        modeScandinavianWalk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRunStart();
                editor.putString("mode", "Скандинавская ходьба");
                editor.apply();
            }
        });
        modeFastWalk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRunStart();
                editor.putString("mode", "Быстрая ходьба");
                editor.apply();
            }
        });
        nestedScrollView.setVisibility(View.INVISIBLE);
        behavior = BottomSheetBehavior.from(nestedScrollView);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        behavior.setPeekHeight(0);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        line.width(7f).color(R.color.purple_200);
        builder = new LatLngBounds.Builder();
        finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRunning = new Date();
                editor.putInt("duration", (int)(stopRunning.getTime() - startRunning.getTime()) / (1000 * 60));
                editor.putFloat("distance", summaryDistance);
                editor.putString("date", new SimpleDateFormat("dd.MM.yyyy в HH:mm").format(new Date()));
                editor.apply();
                createJSON();
                startActivity(new Intent(MapsActivity.this, ResultWorkoutActivity.class));
            }
        });
        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRunning = new Date();
                buttonStart.setVisibility(View.INVISIBLE);
                nestedScrollView.setVisibility(View.VISIBLE);
                behavior.setPeekHeight(nestedScrollView.getHeight(), true);
            }
        });

    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //Отображение своего местоположения и перемещение камеры туда
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.setOnMyLocationChangeListener(myLocationChangeListener);
        mMap.setOnMyLocationClickListener(myLocationClickListener);
        mMap.addPolyline(line);
    }

    private void createJSON(){
        String FILENAME = "storage.json";
        Gson gson = new Gson();
        SharedPreferences sp = getSharedPreferences("status", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("stopRunning", String.valueOf(startRunning.getTime()));
        editor.apply();
        String JSONString = String.format("{\"%s\":%s}", String.valueOf(stopRunning.getTime()) , String.valueOf(gson.toJson(points)));
        try {
            FileWriter fileWriter = new FileWriter("D:\\android_res\\storage.json", true);
            fileWriter.append(JSONString);
            fileWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}