package com.example.geofit;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity {

    private TextView tvWalk;
    private TextView tvRun;
    private TextView tvDistance;
    private TextView tvDuration;
    private TextView tvDate;
    private TextView tvSpeed;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions();
        //Связь с разметкой
        tvWalk = findViewById(R.id.tv_walk);
        tvRun = findViewById(R.id.tv_run);
        tvSpeed = findViewById(R.id.tv_speed);
        tvDistance = findViewById(R.id.all_distance);
        tvDuration = findViewById(R.id.tv_duration_workout);
        tvDate = findViewById(R.id.tv_date_workout);

        SharedPreferences sp = getSharedPreferences("status", MODE_PRIVATE);

        tvDate.setText(sp.getString("date", "01.01.2021"));
        tvDistance.setText(String.format("%.1f м", sp.getFloat("distance", 12)));

        tvWalk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, MapsActivity.class));
            }
        });
        tvRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, MapsActivity.class));
            }
        });
    }

    //Запрос разрешения на доступ к геолокации пользователя
    public void requestPermissions(){
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED ||
                ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED){
            Dexter.withActivity(MainActivity.this)
                    .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    .withListener(new PermissionListener() {
                        @Override
                        public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                            Toast.makeText(MainActivity.this, "Permission granted", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                            if (permissionDeniedResponse.isPermanentlyDenied()){
                                AlertDialog.Builder adb = new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("Permission is permanently denied")
                                        .setMessage("You need to turn it on in settigs by yourself")
                                        .setNegativeButton("No", null)
                                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                Intent intent = new Intent();
                                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                                intent.setData(Uri.fromParts("package", getPackageName(), null));
                                            }
                                        });
                                adb.show();
                            }
                            else {
                                Toast.makeText(MainActivity.this, "Permission denied", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                            permissionToken.continuePermissionRequest();
                        }
                    }).check();
        }
    }

}