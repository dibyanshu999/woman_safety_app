package com.example.safetyapp;


import android.content.Context;
import android.view.*;
import android.widget.*;
import java.util.ArrayList;

public class ContactAdapter extends BaseAdapter {

    Context context;
    ArrayList<ContactModel> list;

    public ContactAdapter(Context context, ArrayList<ContactModel> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.contact_item, parent, false);
        }

        TextView name = convertView.findViewById(R.id.contactName);
        TextView phone = convertView.findViewById(R.id.contactPhone);
        ImageView image = convertView.findViewById(R.id.profileImage);

        ContactModel contact = list.get(position);

        name.setText(contact.name);
        phone.setText(contact.phone);

        // Default icon (we can replace later)
        image.setImageResource(R.drawable.ic_person); // create this icon

        return convertView;
    }
}