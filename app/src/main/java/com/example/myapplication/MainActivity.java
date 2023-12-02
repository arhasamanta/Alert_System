package com.example.myapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;
import androidx.core.content.res.ResourcesCompat;
import androidx.room.Room;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DatabaseReference databaseReference;
    private static final String CHANNEL_ID = "my_channel_id";
    private NotificationManager notificationManager;
    private static final int REQUEST_NOTIFICATIONS_PERMISSION = 123; // You can use any unique integer value
    private AppDatabase appDatabase;
    private long startTimeFirebase;
    private long endTimeFirebase;
    private long startTimeRoom;
    private long endTimeRoom;
   // startTimeFirebase = System.currentTimeMillis();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        createNotificationChannel();

        //Drawable drawable= ResourcesCompat.getDrawable(getResources(),)


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        databaseReference = FirebaseDatabase.getInstance().getReference("Alert_Locations");
        // Inside your MainActivity or another appropriate class
        startTimeFirebase = System.currentTimeMillis();
        AlertLocation location1 = new AlertLocation(28.54405, 77.27256, "The area is infected with Influenza virus", "Influenza");
        AlertLocation location2 = new AlertLocation(28.547473819674067, 77.27386052038526, "The area is infected with Rubella virus", "Rubella");
        AlertLocation location3 = new AlertLocation(28.550458847800982, 77.26022920262548, "The area under fever", "Fever");
        AlertLocation location4 = new AlertLocation(22.54001080847107, 88.34207933913828, "The area is infected with Dengue", "Dengue");
        AlertLocation location5 = new AlertLocation(17.401142641753708, 78.50072149225781, "The area is infected with malaria", "Malaria");
        AlertLocation location6 = new AlertLocation(13.081637022176327, 80.22636617439915, "The area is infected by Chikungunya", "Chikungunya");
        AlertLocation location7 = new AlertLocation(28.633911411510642, 77.21786179441706, "The area is infected by Covid-19", "Covid-19");

        appDatabase = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "alert_locations")
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration() // Added to simplify for this example, handle migrations appropriately in a production environment
                .build();
        startTimeRoom = System.currentTimeMillis();

// Wrap insert operations in a transaction
        appDatabase.runInTransaction(new Runnable() {
            @Override
            public void run() {
                AlertLocationDao alertLocationDao = appDatabase.alertLocationDao();
                alertLocationDao.insert(location1);
                alertLocationDao.insert(location2);
                alertLocationDao.insert(location3);
                alertLocationDao.insert(location4);
                alertLocationDao.insert(location5);
                alertLocationDao.insert(location6);
                alertLocationDao.insert(location7);
            }
        });

// Print logs to check for any errors or warnings
        //appDatabase.getOpenHelper().getWritableDatabase().setLogLevel(LogLevel.FULL);

// Close the database when done
        //appDatabase.close();


        locationCallback = new LocationCallback() {
            @Override

            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    Location location = locationResult.getLastLocation();
                    checkNearbyLocationsFromRoom(location);
                    checkNearbyLocations(location);
                }
                else{
                    Log.e("imp_tag","Location is null");
                }
            }
        };

        checkLocationPermissionAndStartUpdates();
        // Request POST_NOTIFICATIONS permission if targeting Android 13 or higher
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (PermissionChecker.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS")
                    != PermissionChecker.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, REQUEST_NOTIFICATIONS_PERMISSION);
            }
        }
    }

    private void checkLocationPermissionAndStartUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            }
        }
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(10000) // Update interval in milliseconds
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build();



        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, locationSettingsResponse -> {
            try {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            } catch (SecurityException e) {
                // Handle the exception if the app does not have location permissions
                e.printStackTrace();
            }
        });

        task.addOnFailureListener(this, e -> {
            int statusCode = ((ApiException) e).getStatusCode();
            switch (statusCode) {
                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MainActivity.this,
                                REQUEST_LOCATION_PERMISSION);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                    break;
            }
        });
    }

    // Helper method to check if the user is near any of the locations
    private void checkNearbyLocations(Location userLocation) {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot locationSnapshot : dataSnapshot.getChildren()) {
                    double latitude = locationSnapshot.child("latitude").getValue(Double.class);
                    double longitude = locationSnapshot.child("longitude").getValue(Double.class);
                    String dise = locationSnapshot.child("virus_outbreak").getValue(String.class);
                    String message="This area is infected with "+dise+" virus";
                    Log.e("loc_log","message is"+message);
                    Location targetLocation = new Location("");
                    targetLocation.setLatitude(latitude);
                    targetLocation.setLongitude(longitude);

                    float distance = userLocation.distanceTo(targetLocation);
                    Log.i("Dis","Distance is "+String.valueOf(distance));
                    float act_dis=distance-57;
                    if (act_dis <= 50) { // 50 meters, you can adjust this radius
                        showLocationAlert(message);
                        showNotification(message);
                        endTimeFirebase = System.currentTimeMillis();
                        long timeFire = endTimeFirebase - startTimeFirebase;
                        String toastMessage = "Time taken for Firebase: " + timeFire + " milliseconds";

                        // Use MainActivity.this as the context for Toast
                        Toast.makeText(MainActivity.this, toastMessage, Toast.LENGTH_SHORT).show();

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle database error, if any
            }
        });
    }

    //databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
    private void checkNearbyLocationsFromRoom(Location userLocation) {
        List<AlertLocation> alertLocations = appDatabase.alertLocationDao().getAllAlertLocations();
        Log.d("ARHA", "checkNearbyLocationsFromRoom: ");
        for (AlertLocation alertLocation : alertLocations) {
            double latitude = alertLocation.getLatitude();
            double longitude = alertLocation.getLongitude();
            String disease = alertLocation.getVirusOutbreak();
            endTimeRoom = System.currentTimeMillis();
            long timeRoom = endTimeRoom - startTimeRoom;
            String toastMessage = " and Time taken for Room Database: " + timeRoom + " milliseconds";
            String message = "This area is infected with " + disease + " virus" + toastMessage;


            Location targetLocation = new Location("");
            targetLocation.setLatitude(latitude);
            targetLocation.setLongitude(longitude);

            float distance = userLocation.distanceTo(targetLocation);
            float adjustedDistance = distance - 57;


            if (adjustedDistance <= 600) { // 50 meters, you can adjust this radius
                // Show location alert using AlertDialog
                showAlertDialog(message);
                showNotification(message);
                break;
            }
        }
    }

    private void showAlertDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Room Database Alert");
        builder.setMessage(message);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
    // Helper method to display an alert using Snackbar
    private void showLocationAlert(String message) {
        Log.d("MyApp", "showLocationAlert called with message: " + message);
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }
    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CharSequence name = "My Channel";
            String description = "Channel Description";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void showNotification(String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("Virus Alert")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        Notification notification = builder.build();
        notificationManager.notify(1, notification);

        //break; // Exit the loop if an alert is shown for one location
    }
}
