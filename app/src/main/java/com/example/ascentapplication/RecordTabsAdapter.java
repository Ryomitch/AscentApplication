package com.example.ascentapplication;

import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;

//custom adapter for the record screen tabs. creates the correct fragment for each tab on the record screen
public class RecordTabsAdapter extends FragmentStateAdapter {
    //constructor
    public RecordTabsAdapter(AppCompatActivity fragment)
    {
        super(fragment);
    }

    //list of all fragments in the adapter
    public ArrayList<Fragment> fragmentList = new ArrayList<>();
    @NonNull
    @Override
    public Fragment createFragment(int position)
    {
        //check the position of the current tab
        switch(position)
        {
            case 0:
                RecordFragment rf = new RecordFragment();                                           //create a record fragment (for complete boulder problems)
                fragmentList.add(rf);
                return rf;
            case 1:
                RecordFragment rf2 = new RecordFragment();                                          //create a record fragment (for unfinished boulder problems)
                fragmentList.add(rf2);
                return rf2;
            case 2:
                StatsParentFragment sf = new StatsParentFragment();                                 //create a stats fragment (for the bar chart display)
                fragmentList.add(sf);
                return sf;
            default:
                RecordFragment rfD = new RecordFragment();
                fragmentList.add(rfD);
                return rfD;
        }
    }

    //returns the number of tabs on the record screen
    @Override
    public int getItemCount()
    {
        return 3;
    }
}
