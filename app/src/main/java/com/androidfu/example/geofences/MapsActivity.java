package com.androidfu.example.geofences;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
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

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private MyPlaces happyPlace;
    private MyPlaces home;
    private List<Geofence> myFences = new ArrayList<Geofence>();
    private GoogleApiClient googleApiClient;
    private PendingIntent geofencePendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        ImageButton happyPlaceBtn = (ImageButton) findViewById(R.id.ib_happy_place);
        happyPlaceBtn.setOnClickListener(this);

        ImageButton homeBtn = (ImageButton) findViewById(R.id.ib_home);
        homeBtn.setOnClickListener(this);

        setUpMapIfNeeded();
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ib_happy_place:
                Toast.makeText(this, "You Clicked Happy Place", Toast.LENGTH_SHORT).show();
                moveToLocation(happyPlace);
                break;
            case R.id.ib_home:
                Toast.makeText(this, "You Clicked Home", Toast.LENGTH_SHORT).show();
                moveToLocation(home);
                break;
            default:
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onPause() {
        this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
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
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.setBuildingsEnabled(true);

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

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                //TODO set our Mock Location to the clicked coordinates
                Toast.makeText(getApplicationContext(), String.format("You clicked @ %1$s, %2$s", latLng.latitude, latLng.longitude), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Add a map marker at the place specified.
     *
     * @param place
     */
    private void addPlaceMarker(MyPlaces place) {
        mMap.addMarker(new MarkerOptions()
                .position(place.getCoordinates())
                .title(place.getTitle())
                .snippet(place.getSnippet())
                .icon(BitmapDescriptorFactory.fromResource(place.getIconResourceId())));

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
        mMap.addCircle(circleOptions);
    }

    /**
     * Update our map's location to the place specified.
     *
     * @param place
     */
    private void moveToLocation(MyPlaces place) {
        // Move the camera instantly to "place" with a zoom of 15.
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(place.getCoordinates(), 5));
        // Zoom in, animating the camera.
        mMap.animateCamera(CameraUpdateFactory.zoomTo(place.getDefaultZoomLevel()), 2000, null);
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
        if (status.isSuccess()) {
            Toast.makeText(this, "We Added Our Fences", Toast.LENGTH_SHORT).show();
        }
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
            //TODO implement our receiver rather than coming back to MapsActivity.class
            Intent intent = new Intent(this, MapsActivity.class);
            return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }
}
