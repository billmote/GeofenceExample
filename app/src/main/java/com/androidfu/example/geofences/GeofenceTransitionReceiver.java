package com.androidfu.example.geofences;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

public class GeofenceTransitionReceiver extends WakefulBroadcastReceiver {
    
    public static final String TAG = GeofenceTransitionReceiver.class.getSimpleName();
    
    private Context context;
    
    public GeofenceTransitionReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "onReceive(context, intent)");
        this.context = context;
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        if(event != null){
            if(event.hasError()){
                onError(event.getErrorCode());
            } else {
                int transition = event.getGeofenceTransition();
                if(transition == Geofence.GEOFENCE_TRANSITION_ENTER || transition == Geofence.GEOFENCE_TRANSITION_DWELL || transition == Geofence.GEOFENCE_TRANSITION_EXIT){
                    String[] geofenceIds = new String[event.getTriggeringGeofences().size()];
                    for (int index = 0; index < event.getTriggeringGeofences().size(); index++) {
                        geofenceIds[index] = event.getTriggeringGeofences().get(index).getRequestId();
                    }
                    if (transition == Geofence.GEOFENCE_TRANSITION_ENTER || transition == Geofence.GEOFENCE_TRANSITION_DWELL) {
                        onEnteredGeofences(geofenceIds);
                    } else {
                        onExitedGeofences(geofenceIds);
                    }
                }
            }
        }
    }

    protected void onEnteredGeofences(String[] geofenceIds) {
        for (String fenceId : geofenceIds) {
            Toast.makeText(context, String.format("Entered this fence: %1$s", fenceId), Toast.LENGTH_SHORT).show();
            Log.i(TAG, String.format("Entered this fence: %1$s", fenceId));
        }
    }

    protected void onExitedGeofences(String[] geofenceIds){
        for (String fenceId : geofenceIds) {
            Toast.makeText(context, String.format("Exited this fence: %1$s", fenceId), Toast.LENGTH_SHORT).show();
            Log.i(TAG, String.format("Exited this fence: %1$s", fenceId));
        }
    }

    protected void onError(int errorCode){
        Toast.makeText(context, String.format("onError(%1$d)", errorCode), Toast.LENGTH_SHORT).show();
        Log.e(TAG, String.format("onError(%1$d)", errorCode));
    }
    
}
