package com.example.ascentapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.icu.text.UnicodeFilter;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

//RecordActivity class for creating the complete, unfinished and statistics tabs on the record screen
public class RecordActivity extends AppCompatActivity {

    /* private tab names property */
    private final String[] tabNames = new String[] {"Complete", "Unfinished", "Statistics"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        //call method to display user details
        PopulateUserDetails();

        //creating record tabs adapter object and creating the complete, unfinished and stats tabs/fragments
        RecordTabsAdapter recordTabsAdapter = new RecordTabsAdapter(this);
        recordTabsAdapter.createFragment(0);
        recordTabsAdapter.createFragment(1);
        recordTabsAdapter.createFragment(2);

        //getting the viewpager and attaching the tabs to it
        ViewPager2 viewPager = findViewById(R.id.RecordViewPager);
        viewPager.setAdapter(recordTabsAdapter);
        TabLayout tabLayout = findViewById(R.id.RecordTabLayout);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) ->
                tab.setText(tabNames[position])).attach();
    }

    //method to display user avatar on screen
    public void PopulateUserDetails()
    {
        ImageView avatarIV = findViewById(R.id.RecordAvatarIV);
        avatarIV.setImageBitmap(HomeActivity.appUser.GetAvatar());
    }

    //on click method to access profile screen
    public void ProfileOnClick(View view)
    {
        Intent intent = new Intent(RecordActivity.this, ProfileActivity.class);
        startActivity(intent);
    }

    //onclick method to return to the home screen. Clears all previous activities as on back pressed on home activity returns to the sign in screen
    public void HomeOnClick(View view)
    {
        Intent intent = new Intent(RecordActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}