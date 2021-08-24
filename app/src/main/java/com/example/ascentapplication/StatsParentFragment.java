package com.example.ascentapplication;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

//fragment acts as a container for the stats fragment so that it can switch between displaying graph of all grades and pie charts for each grade
public class StatsParentFragment extends Fragment {

    //automatically created code, not used
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    public StatsParentFragment() {
        // Required empty public constructor
    }

    //automatically generated code, not used
    public static StatsParentFragment newInstance(String param1, String param2) {
        StatsParentFragment fragment = new StatsParentFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    //Private property contains the highest level layout of the fragment
    private View  view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view =  inflater.inflate(R.layout.fragment_stats_parent, container, false);
        //create a stats fragment to attach to this parent fragment
        StatsFragment fragment = new StatsFragment();
        FragmentManager fragmentManager = new FragmentManager() {
            @NonNull
            @Override
            public FragmentTransaction beginTransaction() {
                return super.beginTransaction();
            }
        };
        getChildFragmentManager().beginTransaction().setReorderingAllowed(true).add(R.id.StatsParentContainer, fragment, "statsFragment").commit();
        return view;
    }
}