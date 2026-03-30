package com.example.safetyapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

public class AddContactActivity extends AppCompatActivity {

    EditText name, phone;
    Button saveBtn;


    private static final String ADD_URL = "http://10.178.117.191/safety_app/add_contact.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);

        name    = findViewById(R.id.name);
        phone   = findViewById(R.id.phone);
        saveBtn = findViewById(R.id.saveBtn);

        SessionManager session = new SessionManager(this);

        saveBtn.setOnClickListener(v -> {
            String n = name.getText().toString().trim();
            String p = phone.getText().toString().trim();


            if (n.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, "Enter all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!p.matches("\\d{10}")) {
                Toast.makeText(this, "Enter valid 10-digit phone number", Toast.LENGTH_SHORT).show();
                return;
            }

            saveContact(String.valueOf(session.getUserId()), n, p);
        });
    }

    private void saveContact(String userId, String n, String p) {
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest request = new StringRequest(Request.Method.POST, ADD_URL,
                response -> {
                    if (response.trim().equals("success")) {
                        Toast.makeText(this, "Contact Saved!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to save: " + response, Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", userId);
                params.put("name", n);
                params.put("phone", p);
                return params;
            }
        };

        queue.add(request);
    }
}
