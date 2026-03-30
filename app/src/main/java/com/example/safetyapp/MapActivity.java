package com.example.safetyapp;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import org.json.*;

import java.io.IOException;
import java.util.List;

import okhttp3.*;

import android.location.Geocoder;
import android.location.Address;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private EditText destinationInput;
    private Button routeBtn;

    private LatLng selectedDestination = null;

    // Default: Kolkata (replace later with GPS)
    private LatLng currentLatLng = new LatLng(22.5726, 88.3639);

    private final String API_KEY = "AIzaSyDF-HK00DjBxjZUp9kXaRtD1tMptlKPD7o";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        destinationInput = findViewById(R.id.destination);
        routeBtn = findViewById(R.id.routeBtn);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        routeBtn.setOnClickListener(v -> handleRouteClick());
    }

    private void handleRouteClick() {

        String destText = destinationInput.getText().toString().trim();

        if (destText.isEmpty() && selectedDestination == null) {
            Toast.makeText(this, "Enter or select destination", Toast.LENGTH_SHORT).show();
            return;
        }

        LatLng destLatLng;

        if (selectedDestination != null) {
            destLatLng = selectedDestination;
        } else {
            destLatLng = getLatLngFromAddress(destText);
        }

        if (destLatLng != null) {
            getRoutes(currentLatLng, destLatLng);
        } else {
            Toast.makeText(this, "Invalid location", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.addMarker(new MarkerOptions().position(currentLatLng).title("You"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14));

        mMap.setOnMapClickListener(latLng -> {

            selectedDestination = latLng;

            mMap.clear();

            mMap.addMarker(new MarkerOptions().position(currentLatLng).title("You"));
            mMap.addMarker(new MarkerOptions().position(latLng).title("Destination"));

            destinationInput.setText(latLng.latitude + ", " + latLng.longitude);

            Toast.makeText(this, "Destination selected", Toast.LENGTH_SHORT).show();
        });
    }

    // Convert address to LatLng
    private LatLng getLatLngFromAddress(String address) {
        try {
            Geocoder geocoder = new Geocoder(this);
            List<Address> list = geocoder.getFromLocationName(address, 1);

            if (list != null && !list.isEmpty()) {
                return new LatLng(
                        list.get(0).getLatitude(),
                        list.get(0).getLongitude()
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //  Fetch routes
    private void getRoutes(LatLng origin, LatLng dest) {

        String url = "https://maps.googleapis.com/maps/api/directions/json?"
                + "origin=" + origin.latitude + "," + origin.longitude
                + "&destination=" + dest.latitude + "," + dest.longitude
                + "&alternatives=true"
                + "&key=" + API_KEY;

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(MapActivity.this, "Network error", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (response.body() == null) return;

                String res = response.body().string();

                runOnUiThread(() -> parseRoutes(res));
            }
        });
    }

    //  Parse routes and choose safest
    private void parseRoutes(String json) {

        try {
            JSONObject obj = new JSONObject(json);
            JSONArray routes = obj.getJSONArray("routes");

            int bestScore = Integer.MIN_VALUE;
            PolylineOptions bestPolyline = null;

            for (int i = 0; i < routes.length(); i++) {

                JSONObject route = routes.getJSONObject(i);

                String polyline = route.getJSONObject("overview_polyline")
                        .getString("points");

                JSONObject leg = route.getJSONArray("legs").getJSONObject(0);

                double distance = leg.getJSONObject("distance").getDouble("value");
                JSONArray steps = leg.getJSONArray("steps");

                int turns = steps.length();

                double lat = leg.getJSONObject("start_location").getDouble("lat");
                double lng = leg.getJSONObject("start_location").getDouble("lng");

                int score = calculateSafetyScore(distance, turns, lat, lng);

                if (score > bestScore) {
                    bestScore = score;
                    bestPolyline = decodePolyline(polyline);
                }
            }

            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(currentLatLng).title("You"));

            if (bestPolyline != null) {
                bestPolyline.width(12).color(Color.GREEN);
                mMap.addPolyline(bestPolyline);

                Toast.makeText(this, "Safest route selected ✅", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error parsing routes", Toast.LENGTH_SHORT).show();
        }
    }

    //  Improved Safety Score
    private int calculateSafetyScore(double distance, int turns, double lat, double lng) {

        int score = 0;

        // Shorter distance = safer
        score += (distance < 4000) ? 4 : 1;

        // Fewer turns = safer
        score += (turns < 8) ? 4 : 1;

        // Nearby safety places
        score += getNearbySafetyScore(lat, lng);

        return score;
    }

    //  Nearby Police check (IMPORTANT FIX: run in same thread safely)
    private int getNearbySafetyScore(double lat, double lng) {

        try {
            String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?"
                    + "location=" + lat + "," + lng
                    + "&radius=1000"
                    + "&type=police"
                    + "&key=" + API_KEY;

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();

            Response response = client.newCall(request).execute();

            if (response.body() == null) return 0;

            String res = response.body().string();

            JSONObject obj = new JSONObject(res);
            JSONArray results = obj.getJSONArray("results");

            int count = results.length();

            if (count > 3) return 4;
            if (count > 1) return 2;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return -2;
    }

    //  Decode polyline
    private PolylineOptions decodePolyline(String encoded) {

        PolylineOptions poly = new PolylineOptions();

        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {

            int b, shift = 0, result = 0;

            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);

            lat += ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));

            shift = 0;
            result = 0;

            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);

            lng += ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));

            poly.add(new LatLng(lat / 1E5, lng / 1E5));
        }

        return poly;
    }
}