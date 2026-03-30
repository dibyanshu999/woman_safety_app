package com.example.safetyapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.net.Uri;
import android.widget.ListView;

import java.util.ArrayList;

public class EmergencyContactsActivity extends AppCompatActivity {

    ListView listView;
    ArrayList<String> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_contacts);

        listView = findViewById(R.id.listView);
        list = new ArrayList<>();

        //  Emergency Numbers (India)
        list.add("Police - 100");
        list.add("Ambulance - 102");
        list.add("Women Helpline - 1091");
        list.add("Emergency - 112");
        list.add("Child Helpline - 1098");

        android.widget.ArrayAdapter<String> adapter =
                new android.widget.ArrayAdapter<>(
                        this,
                        android.R.layout.simple_list_item_1,
                        list
                );

        listView.setAdapter(adapter);

        //  Click to Call
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String item = list.get(position);
            String number = item.split(" - ")[1];

            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + number));
            startActivity(intent);
        });
    }
}

