package com.example.geofit;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

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
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Scanner;

public class ResultWorkoutActivity extends FragmentActivity implements OnMapReadyCallback {

    private TextView tvMode;
    private TextView tvDuration;

    private GoogleMap mMap;

    private PolylineOptions line = new PolylineOptions();
    private Polyline polyline = null;
    private LatLngBounds.Builder builder;

    private ArrayList<LatLng> points = new ArrayList<>();

    private static final int DEFAULT_ZOOM = 18;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_workout);
        //Связь с разметкой
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.workout_map_res);
        mapFragment.getMapAsync(this);
        tvMode = findViewById(R.id.workout_mode_res);
        tvDuration = findViewById(R.id.workout_duration_res);

        //Обработка элементов разметки
        SharedPreferences sp = getSharedPreferences("status", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        tvMode.setText(sp.getString("mode", "default"));
        tvDuration.setText(String.format("%d минут", sp.getInt("duration", 0)));

        line.width(7f).color(R.color.purple_200);
        builder = new LatLngBounds.Builder();

        ReadPoints();
    }

    private void ReadPoints(){
        SharedPreferences sp = getSharedPreferences("status", MODE_PRIVATE);
        try {
            String JSONString = Read();
            Log.d("points", "string: "+JSONString);
            JSONArray workouts = new JSONArray("123");
            /*JSONArray workouts = new JSONArray(JSONString);
            JSONObject object = new JSONObject(workouts.get(workouts.length()-1).toString());
            JSONArray array = new JSONArray(object.getJSONArray(sp.getString("stopRunning", "def")).toString());
            for (int i = 0; i < array.length(); i++){
                JSONObject latLngObject = new JSONObject(array.get(i).toString());
                Log.d("points", latLngObject.toString());
                LatLng latLng = new LatLng(Float.parseFloat(String.valueOf(latLngObject.getDouble("latitude"))),
                        Float.parseFloat(String.valueOf(latLngObject.getDouble("longitude"))));
                points.add(latLng);
            }*/
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void PaintRoute(){
        mMap.addMarker(new MarkerOptions().position(points.get(0)).title("Start")).setIcon(BitmapDescriptorFactory.fromResource(R.drawable.flag));
        for(LatLng latLng: points){
            line.add(latLng);
            builder.include(latLng);
        }
        mMap.addPolyline(line);
        builder.build();
        mMap.addMarker(new MarkerOptions().position(points.get(points.size()-1)).title("Finish")).setIcon(BitmapDescriptorFactory.fromResource(R.drawable.flag));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(points.get(points.size()-1), DEFAULT_ZOOM));
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //Отображение своего местоположения и перемещение камеры туда
        //PaintRoute();
    }

    private String Read(){
        try {
            FileReader reader = new FileReader("D:\\android_res\\storage.json");
            Scanner scanner = new Scanner(reader);
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = scanner.next()) != null) {
                Log.d("points", line);
                stringBuilder.append(line);
            }
            reader.close();
            return stringBuilder.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}