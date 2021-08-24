package com.example.ascentapplication;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

// custom tab adapter class for boulder problem search fragments
public class ProblemSearchTabsAdapter extends FragmentStateAdapter
{
    public ProblemSearchTabsAdapter(AppCompatActivity fragment)
    {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position)
    {
        switch(position)
        {
            case 0:
                return new GeneralFragment();
            case 1:
                return new GeneralFragment();
            case 2:
                return new GeneralFragment();
            default:
                return new GeneralFragment();
        }
    }

    //return the number of problem search tabs
    @Override
    public int getItemCount()
    {
        return 3;
    }
}
