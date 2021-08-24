package com.example.ascentapplication;

import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// BPPieChartFragment class used to represent Completed and unfinished boulder problems for specific grade in a pie chart
public class BPPieChartFragment extends Fragment {

    /*Private properties */

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_COMPLETE = "complete";
    private static final String ARG_UNFINISHED = "unfinished";
    private static final String ARG_GRADE = "grade";

    // fragment private properties for the number of complete and unfinished BPs to display and the grade for which pie chart represents
    private float complete;
    private float unfinished;
    private int grade;

    //properties for some views on the layout
    private PieChart pieChart;          //the chart view that will shown on the screen
    private Button closeBtn;            //the close button to close the pie chart

    //array of all the possible grades
    private static final String[] GRADES = { "1", "2", "3", "4", "4+", "5", "5+", "6A", "6A+", "6B", "6B+", "6C", "6C+", "7A", "7A+", "7B", "7B+", "7C", "7C+", "8A", "8A+", "8B", "8B+", "8C", "8C+", "9A" };

    //empty constructor
    public BPPieChartFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param complete1 Parameter 1.
     * @param unfinished2 Parameter 2.
     * @return A new instance of fragment BPPieChartFragment.
     */
    public static BPPieChartFragment newInstance(float complete1, float unfinished2, int grade3) {
        BPPieChartFragment fragment = new BPPieChartFragment();
        Bundle args = new Bundle();
        args.putFloat(ARG_COMPLETE, complete1);
        args.putFloat(ARG_UNFINISHED, unfinished2);
        args.putInt(ARG_GRADE, grade3);
        fragment.setArguments(args);
        return fragment;
    }

    //class on create method
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            complete = getArguments().getFloat(ARG_COMPLETE);
            unfinished = getArguments().getFloat(ARG_UNFINISHED);
            grade = getArguments().getInt(ARG_GRADE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_b_p_pie_chart, container, false);

        //get the piechart and close button from the layout
        pieChart = view.findViewById(R.id.BPPieChart);
        closeBtn = view.findViewById(R.id.BPPieChartBtn);

        //set the on click listener for the close button
        closeBtn.setOnClickListener(this::BPPieChartCloseOnClick);

        //call method to setup and display the pie chart
        SetupPieChart();
        return view;
    }

    //method set the display style and values for the pie chart
    public void SetupPieChart()
    {
        //disable the description
        pieChart.getDescription().setEnabled(false);
        //set the center text of the pie chart using the grade of the pie chart
        pieChart.setCenterText("Grade " + GRADES[grade] + " Record");
        pieChart.setCenterTextSize(20);

        //set the hole color and center text color
        pieChart.setHoleColor(R.color.transparent);
        pieChart.setCenterTextColor(Color.WHITE);

        //set the legend text color and size
        pieChart.getLegend().setTextColor(Color.WHITE);
        pieChart.getLegend().setTextSize(16);

        //add the complete and unfinished values as entries to be displayed on the pie chart
        PieEntry completeEntry = new PieEntry(complete, "Complete");
        PieEntry unfinishedEntry = new PieEntry(unfinished, "Unfinished");
        List<PieEntry> entries = new ArrayList<>();
        entries.add(completeEntry);
        entries.add(unfinishedEntry);

        //pass the entries in to a pe chart data set
        PieDataSet set = new PieDataSet(entries, "Grade " + GRADES[grade] + " Record");

        //configure the colors for each slice in the pie chart as well as the text sizes
        List<Integer> sliceColors = Arrays.asList(Color.GREEN, Color.RED);
        set.setColors(sliceColors);
        List<Integer> valueTextColors = Arrays.asList(Color.BLUE, Color.WHITE);
        set.setValueTextSize(24);
        set.setValueTextColors(valueTextColors);

        //turn data set into piedata object and pass to the pie chart for display
        PieData data = new PieData(set);
        pieChart.setData(data);
        pieChart.invalidate();
    }

    //the on click listener for the pie chart close button
    public void BPPieChartCloseOnClick(View view)
    {
        //get the parent of this fragment
        StatsFragment statsFragment = (StatsFragment) getParentFragmentManager().findFragmentByTag("statsFragment");
        //replace this fragment with the parent fragment to show it
        getParentFragmentManager().beginTransaction().replace(R.id.StatsParentContainer, statsFragment).addToBackStack(null).commit();
    }

}