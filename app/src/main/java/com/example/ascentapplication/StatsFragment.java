package com.example.ascentapplication;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.solver.state.State;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentViewHolder;
import androidx.viewpager2.widget.ViewPager2;

import android.provider.ContactsContract;
import android.service.autofill.Dataset;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.widget.FrameLayout;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.DefaultValueFormatter;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.LargeValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

//fragment class to display a graph of user's completed and unfinished boulder problems by grade
public class StatsFragment extends Fragment {

    //automatically generated code not used
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    public StatsFragment() {
        // Required empty public constructor
    }

    //automatically generated code, not used
    public static StatsFragment newInstance(String param1, String param2) {
        StatsFragment fragment = new StatsFragment();
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

    /* Private Properties */
    private static final int MAX_X_VALUE = 26;                                                      //the max number of items on x axis (i.e the number of grades to show on the graph)
    private static final int GROUPS = 2;                                                            //the number of bars to show per grade
    private static final String GROUP_1_LABEL = "Complete";                                         //the label for the first bar
    private static final String GROUP_2_LABEL = "Unfinished";                                       //the label for the second bar
    private static final float BAR_SPACE = 0.02f;                                                   //value for the space between bars in a group
    private static final float BAR_WIDTH = 0.45f;                                                   //value for the width of each bar in a group
    private static final float GROUP_SPACE = 0.06f;                                                 //value for the space between each bar group
    //array containing all grades to display on the graph
    private static final String[] GRADES = { "1", "2", "3", "4", "4+", "5", "5+", "6A", "6A+", "6B", "6B+", "6C", "6C+", "7A", "7A+", "7B", "7B+", "7C", "7C+", "8A", "8A+", "8B", "8B+", "8C", "8C+", "9A" };
    private HorizontalBarChart chart;                                                               //the chart view to display bar chart on
    private ArrayList<HashMap<String, Boolean>> resultsByGrade = new ArrayList<HashMap<String, Boolean>>();                //an arraylist containing the user's results for each grade
    //each hashmap in the arraylist represents a grade
    //each hashmap is laid out to contain the name of a boulder problem that the user has attempted, and a boolean indicating whether the user has finished the bp or not

    private View view;                                                                              //the highest level layout of the fragment

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view =  inflater.inflate(R.layout.fragment_stats, container, false);

        //get the chart view from the layout
        chart = view.findViewById(R.id.StatisticsChart);

        //call method to initially populate the results by grade arraylist with empty hashmaps for each grade
        PopulateGradesHashMaps();
        //call method to retrieve user's record from the database update the bar chart accordingly
        GetResultsData();
        //call method to attach listener to detect boulder problems when they are deleted from the database
        DeletionListener();
        return view;
    }

    //method to initially populate the results by grade arraylist with empty hashmaps for each grade
    public void PopulateGradesHashMaps()
    {
        //loop through the number of grades
        for(int i  = 0; i < MAX_X_VALUE; i++)
        {
            //create empty hashmap and add to results by grade array list
            HashMap<String, Boolean> hm = new HashMap<>();
            resultsByGrade.add(hm);
        }
    }

    //method to retrieve the user's record from the database and update bar chart accordingly
    public void GetResultsData()
    {
        //get the current firebase user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        //create database reference
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        //create reference of location for user's record
        DatabaseReference recordRef = dbRef.child("ascentusers/" + currentUser.getUid() + "/record");
        //create listener to get user's boulder problem record
        ChildEventListener recordListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if(snapshot.exists())
                {
                    //get the snapshot key as the boulder problem name
                    String bpName = snapshot.getKey();
                    BPRecord bpRecord = snapshot.getValue(BPRecord.class);
                    String complete = bpRecord.GetComplete();
                    //attach listener to list of all boulder problems to find the boulder problem from the user's record
                    dbRef.child("BoulderProblems/UserCreated").child(bpName).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(snapshot.exists())
                            {
                                //get the snapshot value as a boulder problem object
                                BoulderProblem bp = snapshot.getValue(BoulderProblem.class);
                                //check the grade of the boulder problem
                                for(int i = 0; i < GRADES.length; i++)
                                {
                                    if(bp.GetGrade().equals(GRADES[i]))
                                    {
                                        //check if the user completed the problem
                                        if(complete.equals("Yes"))
                                        {
                                            //put in the hashmap at the correct grade index, the boulder problem name and boolean indicating user has completed it
                                            resultsByGrade.get(i).put(bpName, true);
                                        }
                                        else
                                        {
                                            //put in the hashmap at the correct grade index, the boulder problem name and boolean indicating user has not finished it yet
                                            resultsByGrade.get(i).put(bpName, false);
                                        }
                                    }
                                }
                                //prepare the chart data, configure the chart appearance and display it
                                BarData data = CreateChartData();
                                ConfigureChartAppearance();
                                PrepareChartData(data);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        //attach listener to location of user's record in the database
        recordRef.addChildEventListener(recordListener);
    }

    //method to check for boulder problems when they are deleted from the database
    private void DeletionListener()
    {
        ChildEventListener deletionListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                //listens to boulder problems being removed

                //get the snapshot value as a bp object
                BoulderProblem bp = snapshot.getValue(BoulderProblem.class);

                //get the grade index of the bp
                for(int i = 0; i < GRADES.length; i++)
                {
                    if(GRADES[i].equals(bp.GetGrade()))
                    {
                        //check if the bp is in the user's record, if so delete from the record and update the bar chart accordingly
                        for(String bpName : resultsByGrade.get(i).keySet())
                        {
                            if(bpName.equals(bp.GetName()))
                            {
                                resultsByGrade.get(i).remove(bpName);
                                BarData data = CreateChartData();
                                ConfigureChartAppearance();
                                PrepareChartData(data);
                                break;
                            }
                        }
                        break;
                    }
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        //create reference of location for user created boulder problems in the database
        DatabaseReference bpRef = FirebaseDatabase.getInstance().getReference().child("BoulderProblems").child("UserCreated");
        //attach listener for deleted boulder problems
        bpRef.addChildEventListener(deletionListener);
    }

    //method to configure the appearance/style of the bar chart
    private void ConfigureChartAppearance() {

        //enable the chart description and display bar values above each bar
        chart.getDescription().setEnabled(false);
        chart.setDrawValueAboveBar(true);

        //get the chart x axis
        XAxis xAxis = chart.getXAxis();
        //set the axis value format using the array of all grades (i.e set the labels for the x axis)
        xAxis.setValueFormatter(new IndexAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return GRADES[(int) value];
            }
        });
        //set the label colors and size
        xAxis.setTextColor(Color.WHITE);
        xAxis.setTextSize(16);

        //get the left y axis
        YAxis leftAxis = chart.getAxisLeft();
        //enable left y axis labels
        leftAxis.setDrawLabels(true);
        //set the labels for the y axis based on data put into bar charts
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return super.getFormattedValue(value);
            }
        });
        //disable chart grid lines
        leftAxis.setDrawGridLines(false);
        //set space above the y axis
        leftAxis.setSpaceTop(30f);
        leftAxis.setAxisMinimum(0f);
        //set the color of y axis labels and text size
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setTextSize(16);
        leftAxis.setGranularity(1f);
        leftAxis.setGranularityEnabled(true);
        //disable chart right y axis
        chart.getAxisRight().setEnabled(false);
    }

    //method to create the bar chart data using the results by grade arraylist
    private BarData CreateChartData() {
        //create list of bar entries for complete and unfinished boulder problems
        ArrayList<BarEntry> values1 = new ArrayList<>();
        ArrayList<BarEntry> values2 = new ArrayList<>();

        //populate the list with entries for each grade
        for (int i = 0; i < GRADES.length; i++) {
            int completeCount = 0;
            int unfinishedCount = 0;
            //check the results by grade arraylist form complete and unfinished bp's by the grade
            for(String key : resultsByGrade.get(i).keySet())
            {
                if(resultsByGrade.get(i).get(key))
                {
                    completeCount += 1;
                }
                else
                {
                    unfinishedCount += 1;
                }
            }
            //add the entries to the list of bar entries
            values1.add(new BarEntry(i, completeCount));
            values2.add(new BarEntry(i, unfinishedCount));
        }

        //crate bar datasets using the bar entry lists. set the labels to complete and unfinished respectively
        BarDataSet set1 = new BarDataSet(values1, GROUP_1_LABEL);
        BarDataSet set2 = new BarDataSet(values2, GROUP_2_LABEL);
        //set the color of the labels for the bars
        set1.setValueTextColor(Color.WHITE);
        set2.setValueTextColor(Color.WHITE);

        //set the color for the bars (green for complete, red for unfinished)
        set1.setColor(Color.GREEN);
        set2.setColor(Color.RED);

        //put the bar data sets in array list of datasets
        ArrayList<IBarDataSet> dataSets = new ArrayList<>();
        dataSets.add(set1);
        dataSets.add(set2);

        //create bar data using the datasets list
        BarData data = new BarData(dataSets);

        return data;
    }

    //method put chart data into the bar chart view and display it
    /* Parameters
    * - data: the data to be displayed in the bar chart
     */
    private void PrepareChartData(BarData data) {
        //pass the data to the bar chart view
        chart.setData(data);
        //create an on value select listener for each bar
        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                //get the grade index of the selected bar
                int gradeIndex = (int) e.getX();
                //get the dataset related to selected bar
                IBarDataSet selectedDS = chart.getData().getDataSetForEntry(e);
                //get the label of the selected bar
                String selectedLbl = selectedDS.getLabel();
                Entry unfinishedE = e;
                Entry completeE = e;
                //if the label is a complete bar
                if(selectedLbl.equals(GROUP_1_LABEL))
                {
                    //then the completed entry equals the selected bar and the unfinished entry is the other bar in the group
                    completeE = e;
                    unfinishedE = chart.getData().getDataSetByIndex(h.getDataSetIndex()+1).getEntryForIndex(gradeIndex);
                }
                //if label is unfinished bar
                else if(selectedLbl.equals(GROUP_2_LABEL))
                {
                    //then the unfinished entry equals the selected bar and the complete entry is the other bar in the group
                    unfinishedE = e;
                    completeE = chart.getData().getDataSetByIndex(h.getDataSetIndex()-1).getEntryForIndex(gradeIndex);
                }
                //pass the complete and unfinished entries to a new pie chart fragment
                BPPieChartFragment pcFragment = BPPieChartFragment.newInstance(completeE.getY(), unfinishedE.getY(), gradeIndex);
                //replace current bar chart fragment with the pie chart fragment
                getParentFragmentManager().beginTransaction().replace(R.id.StatsParentContainer, pcFragment, "BPPieChartFragment").addToBackStack(null).commit();
            }

            @Override
            public void onNothingSelected() {

            }
        });
        //set space at top of bar chart
        chart.setExtraTopOffset(10f);
        //set the width of each bar
        chart.getBarData().setBarWidth(BAR_WIDTH);

        //set the legend for the bar chart
        chart.getLegend().setTextColor(Color.WHITE);
        chart.getLegend().setTextSize(16);
        //set the spacing between bars in groups and the spacing between groups of bars
        chart.getXAxis().setAxisMinimum(0.0f);
        chart.getXAxis().setAxisMinimum(-2 + chart.getBarData().getGroupWidth(GROUP_SPACE, BAR_SPACE) * GROUPS);
        chart.groupBars(0, GROUP_SPACE, BAR_SPACE);

        //set the max number of groups(grades) available on screen at one time
        chart.setVisibleXRangeMaximum(6);
        //show bar chart from the first grade
        chart.moveViewToX(0);
        //display the bar chart
        chart.invalidate();
    }

}