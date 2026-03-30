package com.example.safetyapp;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    EditText email, password, confirmPassword;
    Button signupBtn;
    TextView goLogin;

    //our actual server URL
    private static final String SIGNUP_URL = "http://192.168.43.33/safety_app/signup.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        email           = findViewById(R.id.email);
        password        = findViewById(R.id.password);
        confirmPassword = findViewById(R.id.confirmPassword);
        signupBtn       = findViewById(R.id.signupBtn);
        goLogin         = findViewById(R.id.goLogin);

        signupBtn.setOnClickListener(v -> {
            String e  = email.getText().toString().trim();
            String p  = password.getText().toString().trim();
            String cp = confirmPassword.getText().toString().trim();

            if (e.isEmpty() || p.isEmpty() || cp.isEmpty()) {
                Toast.makeText(this, "Enter all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!p.equals(cp)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (p.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            registerUser(e, p);
        });

        goLogin.setOnClickListener(v -> finish());
    }

    private void registerUser(String e, String p) {
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest request = new StringRequest(Request.Method.POST, SIGNUP_URL,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        String status = obj.getString("status");

                        if (status.equals("success")) {
                            Toast.makeText(this, "Signup Successful! Please login.", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            String msg = obj.optString("message", "Signup Failed");
                            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception ex) {
                        Toast.makeText(this, "Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("email", e);
                params.put("password", p);
                return params;
            }
        };

        queue.add(request);
    }
}
