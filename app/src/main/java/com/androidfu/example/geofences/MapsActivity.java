package com.androidfu.example.geofences;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    public static final String TAG = MapsActivity.class.getSimpleName();

    private static final long LOCATION_ITERATION_PAUSE_TIME = 1000;
    private static final int NUMBER_OF_LOCATION_ITERATIONS = 10;

    private GoogleMap googleMap; // Might be null if Google Play services APK is not available.
    private MyPlaces happyPlace;
    private MyPlaces home;
    private List<Geofence> myFences = new ArrayList<Geofence>();
    private GoogleApiClient googleApiClient;
    private PendingIntent geofencePendingIntent;
    private UpdateLocationRunnable updateLocationRunnable;
    private LocationManager locationManager;
    private int marker = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        ImageButton happyPlaceBtn = (ImageButton) findViewById(R.id.ib_happy_place);
        happyPlaceBtn.setOnClickListener(this);

        ImageButton homeBtn = (ImageButton) findViewById(R.id.ib_home);
        homeBtn.setOnClickListener(this);

        ImageButton resetBtn = (ImageButton) findViewById(R.id.ib_reset);
        resetBtn.setOnClickListener(this);

        setUpMapIfNeeded();
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {

        MyPlaces place = null;
        switch (v.getId()) {
            case R.id.ib_happy_place:
                Toast.makeText(this, "You Clicked Happy Place", Toast.LENGTH_SHORT).show();
                place = happyPlace;
                moveToLocation(place);
                break;
            case R.id.ib_home:
                Toast.makeText(this, "You Clicked Home", Toast.LENGTH_SHORT).show();
                place = home;
                moveToLocation(place);
                break;
            case R.id.ib_reset:
                Toast.makeText(this, "Resetting Our Map", Toast.LENGTH_SHORT).show();
                googleApiClient.disconnect();
                googleMap.clear();
                myFences.clear();
                setUpMap();
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        setUpMapIfNeeded();

        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Log.i(TAG, "Setup MOCK Location Providers");
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        Log.i(TAG, "GPS Provider");
        locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false, true, false, false, false, false, false, Criteria.POWER_HIGH, Criteria.ACCURACY_FINE);
        locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);

        Log.i(TAG, "Network Provider");
        locationManager.addTestProvider(LocationManager.NETWORK_PROVIDER, true, false, true, false, false, false, false, Criteria.POWER_MEDIUM, Criteria.ACCURACY_FINE);
        locationManager.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, true);
    }

    @Override
    protected void onPause() {
        this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Interrupt our runnable if we're going into the background or exiting
        updateLocationRunnable.interrupt();

        Log.i(TAG, "Cleanup Our Fields");
        locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
        locationManager.removeTestProvider(LocationManager.NETWORK_PROVIDER);
        locationManager = null;
        updateLocationRunnable = null;

        super.onPause();
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #googleMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (googleMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            googleMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
            // Check if we were successful in obtaining the map.
            if (googleMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera.
     * <p/>
     * This should only be called once and when we are sure that {@link #googleMap} is not null.
     */
    private void setUpMap() {
        googleMap.setBuildingsEnabled(true);

        // Add a place with a Geofence
        happyPlace = new MyPlaces("Pier @ Folly Beach", "This is my Happy Place!", new LatLng(32.652411, -79.938063), 10000, 10, R.drawable.ic_palm_tree);
        addPlaceMarker(happyPlace);
        addFence(happyPlace);

        // Add a place with a Geofence
        home = new MyPlaces("Home", "This is where I live.", new LatLng(39.2697455, -84.269921), 1000, 13, R.drawable.ic_home);
        addPlaceMarker(home);
        addFence(home);

        // Add a place w/o a Geofence
        MyPlaces charleston = new MyPlaces("Charleston, SC", "This is where I want to live!", new LatLng(32.8210454, -79.9704779), 0, 10, R.drawable.ic_heart);
        addPlaceMarker(charleston);
        addFence(charleston);

        /*
            After all your places have been created and markers added you can monitor your fences.
         */
        monitorFences(myFences);

        /*
            Choose one of our locations as the default and move there.
         */
        moveToLocation(charleston);

        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {

                if (updateLocationRunnable != null && updateLocationRunnable.isAlive() && !updateLocationRunnable.isInterrupted()) {
                    updateLocationRunnable.interrupt();
                }
                updateLocationRunnable = new UpdateLocationRunnable(locationManager, latLng);
                updateLocationRunnable.start();

                MyPlaces touchedPlace = new MyPlaces(String.format("Marker %1$d", ++marker), "", latLng, 65, 12, 0);
                addPlaceMarker(touchedPlace);
            }
        });
    }

    /**
     * Add a map marker at the place specified.
     *
     * @param place
     */
    private void addPlaceMarker(MyPlaces place) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(place.getCoordinates())
                .title(place.getTitle());
        if (!TextUtils.isEmpty(place.getSnippet())) {
            markerOptions.snippet(place.getSnippet());
        }
        if (place.getIconResourceId() > 0) {
            markerOptions.icon(BitmapDescriptorFactory.fromResource(place.getIconResourceId()));
        }
        googleMap.addMarker(markerOptions);
        drawGeofenceAroundTarget(place);
    }

    /**
     * If our place has a fence radius greater than 0 then draw a circle around it.
     *
     * @param place
     */
    private void drawGeofenceAroundTarget(MyPlaces place) {
        if (place.getFenceRadius() <= 0) {
            // Nothing to draw
            return;
        }
        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(place.getCoordinates());
        circleOptions.fillColor(Color.argb(0x55, 0x00, 0x00, 0xff));
        circleOptions.strokeColor(Color.argb(0xaa, 0x00, 0x00, 0xff));
        circleOptions.radius(place.getFenceRadius());
        googleMap.addCircle(circleOptions);
    }

    /**
     * Update our map's location to the place specified.
     *
     * @param place
     */
    private void moveToLocation(final MyPlaces place) {
        // Move the camera instantly to "place" with a zoom of 5.
        if (place.getTitle().equals("Charleston, SC")) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(place.getCoordinates(), 5));
        }

        // Fly to our new location and then set the correct zoom level for the given place.
        googleMap.animateCamera(CameraUpdateFactory.newLatLng(place.getCoordinates()), new GoogleMap.CancelableCallback() {
            @Override
            public void onFinish() {
                googleMap.animateCamera(CameraUpdateFactory.zoomTo(place.getDefaultZoomLevel()), 2000, null);
            }

            @Override
            public void onCancel() {
                // Nothing to see here.
            }
        });
    }

    /**
     * If our place has a fence radius > 0 then add it to our monitored fences.
     *
     * @param place
     */
    private void addFence(MyPlaces place) {
        if (place.getFenceRadius() <= 0) {
            // Nothing to monitor
            return;
        }
        Geofence geofence = new Geofence.Builder()
                .setCircularRegion(place.getCoordinates().latitude, place.getCoordinates().longitude, place.getFenceRadius())
                .setRequestId(place.getTitle())
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build();
        myFences.add(geofence);
    }

    /**
     * Connect our GoogleApiClient so we can begin monitoring our fences.
     *
     * @param fences
     */
    private void monitorFences(List<Geofence> fences) {
        if (fences.isEmpty()) {
            throw new RuntimeException("No fences to monitor. Call addPlaceMarker() First.");
        }
        googleApiClient = new GoogleApiClient.Builder(this).addApi(LocationServices.API).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
        googleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Toast.makeText(this, "GoogleApiClient Connected", Toast.LENGTH_SHORT).show();
        geofencePendingIntent = getRequestPendingIntent();
        PendingResult<Status> result = LocationServices.GeofencingApi.addGeofences(googleApiClient, myFences, geofencePendingIntent);
        result.setResultCallback(this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, "GoogleApiClient Connection Suspended", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "GoogleApiClient Connection Failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResult(Status status) {
        String toastMessage;
        if (status.isSuccess()) {
            toastMessage = "Success: We Are Monitoring Our Fences";
        } else {
            toastMessage = "Error: We Are NOT Monitoring Our Fences";
        }
        Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
    }

    /**
     * Returns the current PendingIntent to the caller.
     *
     * @return The PendingIntent used to create the current set of geofences
     */
    public PendingIntent getRequestPendingIntent() {
        return createRequestPendingIntent();
    }

    /**
     * Get a PendingIntent to send with the request to add Geofences. Location
     * Services issues the Intent inside this PendingIntent whenever a geofence
     * transition occurs for the current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence
     * transitions.
     */
    private PendingIntent createRequestPendingIntent() {
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        } else {
            Intent intent = new Intent(this, GeofenceTransitionReceiver.class);
            return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }


    // /////////////////////////////////////////////////////////////////////////////////////////
    // // UpdateLocationRunnable                                                              //
    // /////////////////////////////////////////////////////////////////////////////////////////

    class UpdateLocationRunnable extends Thread {

        private final LocationManager locationManager;
        private final LatLng latlng;
        Location mockGpsLocation;
        Location mockNetworkLocation;

        UpdateLocationRunnable(LocationManager locationManager, LatLng latlng) {
            this.locationManager = locationManager;
            this.latlng = latlng;
        }

        /**
         * Starts executing the active part of the class' code. This method is
         * called when a thread is started that has been created with a class which
         * implements {@code Runnable}.
         */
        @Override
        public void run() {
            try {
                Log.i(TAG, String.format("Setting Mock Location to: %1$s, %2$s", latlng.latitude, latlng.longitude));
                /*
                    Location can be finicky.  Iterate over our desired location every second for
                    NUMBER_OF_LOCATION_ITERATIONS seconds to help it figure out where we want it to
                    be.
                 */
                for (int i = 0; !isInterrupted() && i <= NUMBER_OF_LOCATION_ITERATIONS; i++) {
                    mockGpsLocation = createMockLocation(LocationManager.GPS_PROVIDER, latlng.latitude, latlng.longitude);
                    locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, mockGpsLocation);
                    mockNetworkLocation = createMockLocation(LocationManager.NETWORK_PROVIDER, latlng.latitude, latlng.longitude);
                    locationManager.setTestProviderLocation(LocationManager.NETWORK_PROVIDER, mockNetworkLocation);
                    Thread.sleep(LOCATION_ITERATION_PAUSE_TIME);
                }
            } catch (InterruptedException e) {
                Log.i(TAG, "Interrupted.");
                // Do nothing.  We expect this to happen when location is successfully updated.
            } finally {
                Log.i(TAG, "Done moving location.");
            }
        }
    }


    // /////////////////////////////////////////////////////////////////////////////////////////
    // // CreateMockLocation                                                                  //
    // /////////////////////////////////////////////////////////////////////////////////////////

    private Location createMockLocation(String locationProvider, double latitude, double longitude) {
        Location location = new Location(locationProvider);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setAccuracy(1.0f);
        location.setTime(System.currentTimeMillis());
        /*
            setElapsedRealtimeNanos() was added in API 17
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        }
        try {
            Method locationJellyBeanFixMethod = Location.class.getMethod("makeComplete");
            if (locationJellyBeanFixMethod != null) {
                locationJellyBeanFixMethod.invoke(location);
            }
        } catch (Exception e) {
            // There's no action to take here.  This is a fix for Jelly Bean and no reason to report a failure.
        }
        return location;
    }
}
