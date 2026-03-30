package com.example.safetyapp;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ContactListActivity extends AppCompatActivity {

    ListView listView;
    ArrayList<ContactModel> contactList;
    SessionManager session;

    //  our actual server URLs
    private static final String GET_URL    = "http://192.168.43.33/safety_app/get_contacts.php";
    private static final String DELETE_URL = "http://192.168.43.33/safety_app/delete_contact.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_list);

        session     = new SessionManager(this);
        listView    = findViewById(R.id.listView);
        contactList = new ArrayList<>();

        loadContacts();
    }

    private void loadContacts() {
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest request = new StringRequest(Request.Method.POST, GET_URL,
                response -> {
                    try {
                        contactList.clear();
                        JSONArray arr = new JSONArray(response);

                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            // ✅ Only load contacts for current user
                            if (obj.getInt("user_id") == session.getUserId()) {
                                contactList.add(new ContactModel(
                                        obj.getString("name"),
                                        obj.getString("phone")
                                ));
                            }
                        }

                        ContactAdapter adapter = new ContactAdapter(this, contactList);
                        listView.setAdapter(adapter);

                    } catch (Exception e) {
                        Toast.makeText(this, "Error loading contacts", Toast.LENGTH_SHORT).show();
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

        // 🔥 Click to Delete
        listView.setOnItemClickListener((parent, view, position, id) -> {
            ContactModel selected = contactList.get(position);

            new android.app.AlertDialog.Builder(this)
                    .setTitle("Delete Contact")
                    .setMessage("Are you sure you want to delete " + selected.name + "?")
                    .setPositiveButton("Yes", (dialog, which) -> deleteContact(selected.phone))
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    private void deleteContact(String phone) {
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest request = new StringRequest(Request.Method.POST, DELETE_URL,
                response -> {
                    Toast.makeText(this, "Contact Deleted", Toast.LENGTH_SHORT).show();
                    loadContacts(); // Refresh list
                },
                error -> Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("phone", phone);
                return params;
            }
        };

        queue.add(request);
    }
}
