package com.example.safetyapp;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    PreviewView previewView;
    VideoCapture<Recorder> videoCapture;
    Recording recording;
    Button sosBtn;
    String videoPath;

    SessionManager session;

    // our actual server URL
    private static final String GET_URL = "http://10.178.117.191/safety_app/get_contacts.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        session     = new SessionManager(this);
        sosBtn      = findViewById(R.id.sosBtn);
        previewView = findViewById(R.id.previewView);

        //  Check session — if not logged in, go to Login
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        //  SOS pulse animation
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(sosBtn, "scaleX", 1f, 1.1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(sosBtn, "scaleY", 1f, 1.1f);
        scaleX.setDuration(800);
        scaleY.setDuration(800);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setRepeatMode(ValueAnimator.REVERSE);
        scaleY.setRepeatMode(ValueAnimator.REVERSE);
        scaleX.start();
        scaleY.start();

        //  SOS button click
        sosBtn.setOnClickListener(v -> {
            getLiveLocation();
            startVideoRecording();
            new android.os.Handler().postDelayed(() -> {
                stopVideoRecording();
                sendVideoToAllContacts();
            }, 5000);
        });

        //  Logout
        LinearLayout logoutBtn = findViewById(R.id.logoutLayout);
        logoutBtn.setOnClickListener(v -> {
            session.logout();
            Intent i = new Intent(MainActivity.this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });

        // Add Contact
        LinearLayout addBtn = findViewById(R.id.addLayout);
        addBtn.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AddContactActivity.class))
        );

        //  View Contacts
        LinearLayout viewBtn = findViewById(R.id.contactLayout);
        viewBtn.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ContactListActivity.class))
        );

        //  Map
        TextView mapBtn = findViewById(R.id.mapBtn);
        mapBtn.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, MapActivity.class))
        );

        // Emergency Contacts
        androidx.cardview.widget.CardView emergencyBtn = findViewById(R.id.cardContacts);
        emergencyBtn.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, EmergencyContactsActivity.class))
        );

        //  Guidelines
        androidx.cardview.widget.CardView guidelinesBtn = findViewById(R.id.cardGuide);
        guidelinesBtn.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, GuidelinesActivity.class))
        );

        // Self Defense
        androidx.cardview.widget.CardView selfDefenseBtn = findViewById(R.id.cardTips);
        selfDefenseBtn.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SelfDefenseActivity.class))
        );

        //  Request permissions
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA
                }, 1);
    }

    //  Fetch contacts from MySQL, then send SOS to all
    private void fetchContactsAndSendSOS(String location, boolean shouldCall) {
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest request = new StringRequest(Request.Method.POST, GET_URL,
                response -> {
                    try {
                        JSONArray arr = new JSONArray(response);
                        ArrayList<String> phones = new ArrayList<>();

                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            if (obj.getInt("user_id") == session.getUserId()) {
                                phones.add(obj.getString("phone"));
                            }
                        }

                        if (phones.isEmpty()) {
                            Toast.makeText(this, "No contacts found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        //  Send SMS to all contacts
                        for (String phone : phones) {
                            sendSMS(phone, "HELP! My location: " + location);
                        }

                        // Call the first contact
                        if (shouldCall &&
                                ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                                        == PackageManager.PERMISSION_GRANTED) {
                            Intent callIntent = new Intent(Intent.ACTION_CALL);
                            callIntent.setData(Uri.parse("tel:" + phones.get(0)));
                            startActivity(callIntent);
                        }

                    } catch (Exception e) {
                        Toast.makeText(this, "Error reading contacts", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Network error fetching contacts", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(session.getUserId()));
                return params;
            }
        };

        queue.add(request);
    }

    //  Send video via WhatsApp to all contacts
    private void sendVideoToAllContacts() {
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest request = new StringRequest(Request.Method.POST, GET_URL,
                response -> {
                    try {
                        JSONArray arr = new JSONArray(response);
                        boolean sent = false;

                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            if (obj.getInt("user_id") == session.getUserId()) {
                                sendVideoWhatsApp(obj.getString("phone"));
                                sent = true;
                                break; // Send to first contact only (WhatsApp limitation)
                            }
                        }

                        if (!sent) {
                            Toast.makeText(this, "No contacts found", Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        Toast.makeText(this, "Error sending video", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(session.getUserId()));
                return params;
            }
        };

        queue.add(request);
    }

    // Send video via WhatsApp
    private void sendVideoWhatsApp(String phone) {
        try {
            File file = new File(videoPath);
            if (!file.exists()) {
                Toast.makeText(this, "Video not found", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    file
            );

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("video/*");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setPackage("com.whatsapp");
            intent.putExtra("jid", phone + "@s.whatsapp.net");
            startActivity(intent);

        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp error or not installed", Toast.LENGTH_SHORT).show();
        }
    }

    // Start video recording
    private void startVideoRecording() {
        previewView.setVisibility(View.VISIBLE);

        ProcessCameraProvider.getInstance(this).addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = ProcessCameraProvider.getInstance(this).get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                Recorder recorder = new Recorder.Builder().build();
                videoCapture = VideoCapture.withOutput(recorder);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture);

                File file = new File(getExternalFilesDir(null),
                        "video_" + System.currentTimeMillis() + ".mp4");
                videoPath = file.getAbsolutePath();

                FileOutputOptions options = new FileOutputOptions.Builder(file).build();

                recording = videoCapture.getOutput()
                        .prepareRecording(this, options)
                        .start(ContextCompat.getMainExecutor(this), event -> {});

                Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    //  Stop video recording
    private void stopVideoRecording() {
        if (recording != null) {
            recording.stop();
            recording = null;
            previewView.setVisibility(View.GONE);
            Toast.makeText(this, "Video saved", Toast.LENGTH_SHORT).show();
        }
    }

    //  Get live GPS location then trigger SOS
    @android.annotation.SuppressLint("MissingPermission")
    private void getLiveLocation() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);

        fetchContactsAndSendSOS("Location fetching...", false);

        boolean gpsEnabled     = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!gpsEnabled && !networkEnabled) {
            Toast.makeText(this, "Please turn ON Location", Toast.LENGTH_SHORT).show();
            return;
        }

        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                String loc = "https://maps.google.com/?q="
                        + location.getLatitude() + ","
                        + location.getLongitude();

                fetchContactsAndSendSOS(loc, true);
                lm.removeUpdates(this);
            }

            @Override public void onStatusChanged(String s, int i, Bundle b) {}
            @Override public void onProviderEnabled(String s) {}
            @Override public void onProviderDisabled(String s) {}
        };

        if (networkEnabled) lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
        if (gpsEnabled)     lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
    }

    //  Send SMS
    private void sendSMS(String phone, String msg) {
        android.telephony.SmsManager sms = android.telephony.SmsManager.getDefault();
        sms.sendTextMessage(phone, null, msg, null, null);
    }
}
