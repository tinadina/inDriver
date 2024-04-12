package com.example.union;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback{

    private GoogleMap myMap;
    Location myLocation = null;
    Location destinationLocation = null;
    protected LatLng start = null;
    protected LatLng end = null;

    private final static int LOCATION_REQUEST_CODE = 23;
    boolean locationPermission = false;

    private List<Polyline> polylines = null;

    TextView txt;
    TextToSpeech textToSpeech;
    String stat;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent prev = getIntent();
        stat = prev.getStringExtra("status");
        LayoutInflater inflater = LayoutInflater.from(this);
        Handler handler = new Handler();

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Locale russian = new Locale("ru", "RU");
                int result = textToSpeech.setLanguage(russian);
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language not supported");
                }
            } else {
                Log.e("TTS", "Initialization failed");
            }
        });
        if(stat.equals("fail")){
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    textToSpeech.speak("Верификация не пройдена. Поиск другого водителя", TextToSpeech.QUEUE_FLUSH, null, null);

                }
            }, 10);

            View dialogView = inflater.inflate(R.layout.failed_match, null);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(dialogView);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.show();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent nextIntent = new Intent(MainActivity.this, FaceRecognitionActivity.class);
                    startActivity(nextIntent);
                }
            }, 3000);
        }
        else{

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    textToSpeech.speak("Верификация пройдена. Желаем безопасной поездки!", TextToSpeech.QUEUE_FLUSH, null, null);

                }
            }, 10);
            View dialogView = inflater.inflate(R.layout.successful_match, null);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(dialogView);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.show();
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }


    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

        myMap = googleMap;
        if(stat.equals("ok")) direction();


    }

    private void direction(){
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        String url = Uri.parse("https://maps.googleapis.com/maps/api/directions/json")
                .buildUpon()
                .appendQueryParameter("destination", "Astana+Arena")
                .appendQueryParameter("origin", "Nazarbayev+University")
                .appendQueryParameter("mode", "driving")
                .appendQueryParameter("key", "AIzaSyCWuDpzBgP9Y3AyRB3Lqu40FxymxOnU1CA")
                .toString();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try{
                    String status = response.getString("status");
                    if(status.equals("OK")){
                        JSONArray routes = response.getJSONArray("routes");
                        ArrayList<LatLng> points;
                        PolylineOptions polylineOptions = null;
                        for(int i = 0; i< routes.length(); i++){
                            points = new ArrayList<>();
                            polylineOptions = new PolylineOptions();
                            JSONArray legs = routes.getJSONObject(i).getJSONArray("legs");
                            for(int j = 0; j<legs.length();j++){
                                JSONArray steps = legs.getJSONObject(j).getJSONArray("steps");
                                for(int k = 0; k<steps.length();k++){
                                    String polyline = steps.getJSONObject(k).getJSONObject("polyline").getString("points");
                                    List<LatLng> list = decodePoly(polyline);
                                    for(int l=0; l < list.size(); l++){
                                        LatLng position = new LatLng((list.get(l)).latitude, (list.get(l).longitude));
                                        polylineOptions.add(position);
                                    }
                                }
                            }

                            polylineOptions.width(20);
                            polylineOptions.color(ContextCompat.getColor(MainActivity.this, R.color.main_dark));
                            polylineOptions.geodesic(true);


                        }

                        Bitmap bitmapA = BitmapFactory.decodeResource(getResources(), R.drawable.marker_a);
                        Bitmap bitmapB = BitmapFactory.decodeResource(getResources(), R.drawable.marker_b);


                        BitmapDescriptor markerA = BitmapDescriptorFactory.fromBitmap(bitmapA);
                        BitmapDescriptor markerB = BitmapDescriptorFactory.fromBitmap(bitmapB);
                        myMap.addPolyline(polylineOptions);
                        myMap.addMarker(new MarkerOptions().position(new LatLng(51.108290913403685, 71.40262689682105)).title("B").icon(markerB));
                        myMap.addMarker(new MarkerOptions().position(new LatLng(51.090631352002326, 71.39813241031341)).title("A").icon(markerA));


                        LatLngBounds bounds = new LatLngBounds.Builder()
                                .include(new LatLng(51.108290913403685, 71.40262689682105))
                                .include(new LatLng(51.090631352002326, 71.39813241031341)).build();
                        Point point  = new Point();
                        getWindowManager().getDefaultDisplay().getSize(point);
                        myMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, point.x, 150, 10));


                        }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        RetryPolicy retryPolicy = new DefaultRetryPolicy(30000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_MAX_RETRIES);
        jsonObjectRequest.setRetryPolicy(retryPolicy);
        requestQueue.add(jsonObjectRequest);
    }
    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);

            int dLat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dLat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);

            int dLng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dLng;

            double finalLat = lat / 1e5;
            double finalLng = lng / 1e5;

            LatLng p = new LatLng(finalLat, finalLng);
            poly.add(p);
        }
        return poly;
    }
    }

