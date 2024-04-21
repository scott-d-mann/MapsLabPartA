package au.scottmann.mapslabparta;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import android.Manifest;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;


import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;

// change the package name to suit your app i.e. the first 3 strings
import au.scottmann.mapslabparta.databinding.ActivityMapsBinding;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    //Required permissions array
    final String[] PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    private GoogleMap mMap;
    private ActivityMapsBinding binding;

    //Variable for debugging in logcat
    String TAG = "MapAct";

    //Location variables
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest mLocationRequest;
    private ArrayList<LatLng> points;

    //Draw on map
    Polyline line;
    private static final float SMALLEST_DISPLACEMENT = 0.5F;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //get the initial location
        if (hasPermissions()) {
            Log.d(TAG, "App has required permissions, getting last location and creating location request");
            getLastLocation(); //get start location
            createLocationRequest(); // set up the location tracking

        } else {
            Log.d(TAG, "App does not have required permissions, asking now");
            askPermissions();
        }
        // to hold our location readings
        points = new ArrayList<LatLng>();



        // This is run EVERYTIME a location update is received
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        LatLng latLng = new LatLng(latitude, longitude);
                        Log.d(TAG, "Adding location to points ArrayList");
                        points.add(latLng);
                        //redrawing the line with the new location
                        redrawLine();
                        //moving the map to the new location
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    }
                }
            }
        };

    }

    //helper function to check permission status
    private boolean hasPermissions() {
        boolean permissionStatus = true;
        for (String permission : PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission is granted: " + permission);
            } else {
                Log.d(TAG, "Permission is not granted: " + permission);
                permissionStatus = false;
            }
        }
        return permissionStatus;

    }

    //helper function to ask user permissions
    private void askPermissions() {
        if (!hasPermissions()) {
            Log.d(TAG, "Launching multiple contract permission launcher for ALL required permissions");
            multiplePermissionActivityResultLauncher.launch(PERMISSIONS);
        } else {
            Log.d(TAG, "All permissions are already granted");
        }
    }

    //Result launcher for permissions
    private final ActivityResultLauncher<String[]> multiplePermissionActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), isGranted -> {
        Log.d(TAG, "Launcher result: " + isGranted.toString()); //permissions are granted lets get to work!
        getLastLocation(); //get start location
        createLocationRequest(); // set up the location tracking
        if (isGranted.containsValue(false)) {
            Log.d(TAG, "At least one of the permissions was not granted, please enable permissions to ensure app functionality");
        }
    });

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            Log.d(TAG, "Last Location detected " + location.getLatitude() + " " + location.getLongitude());
                            LatLng loc = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 19));
                            createLocationRequest();
                        } else {
                            Toast.makeText(getApplicationContext(), "no_location_detected", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Location obj is null");
                            createLocationRequest();
                        }
                    }
                });
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest.Builder(500)
                .setMinUpdateDistanceMeters(SMALLEST_DISPLACEMENT)
                .build();
    }

    private void redrawLine() {
        mMap.clear(); //clears all overlays

        PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);

        for (int i = 0; i < points.size(); i++) {
            LatLng point = points.get(i);
            options.add(point);
        }
        Log.d(TAG, "Adding polyline");
        line = mMap.addPolyline(options); //adds Polyline
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("MissingPermission")
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        Log.d(TAG, "Map ready");
        if (!mFusedLocationClient.getLastLocation().isSuccessful()) {
            Log.d(TAG, "Setting up location tracking");
            createLocationRequest(); // set up the location tracking
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopLocationUpdates();
    }

}