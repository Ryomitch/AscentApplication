package com.example.ascentapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

//BenchmarksDialogFragment class used to contain the display for benchmark boulder problems on a dialog fragment
public class BenchmarksDialogFragment extends DialogFragment
{
    /* Private Properties */
    private GeneralFragment benchmarksFragment;                     //a fragment which will display the benchmarks

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.benchmarks_dialog, container);
        //initialising the benchmarks fragment
        benchmarksFragment = new GeneralFragment();
        //attaching the fragment to this dialog fragment, pass the tag f3 to indicate it is to display benchmarks
        getChildFragmentManager().beginTransaction().setReorderingAllowed(true).add(R.id.BenchmarkDialogFragmentCL, benchmarksFragment, "f3").commit();
        return view;
    }
}
