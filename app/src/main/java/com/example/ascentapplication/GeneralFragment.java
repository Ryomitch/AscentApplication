package com.example.ascentapplication;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.renderscript.Sampler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import static android.content.ContentValues.TAG;

//fragment for display boulder problems on the boulder problem search screen
public class GeneralFragment extends Fragment {

    //automatically created code can be ignored for now
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    public GeneralFragment() {
        // Required empty public constructor
    }

    //method not used
    public static GeneralFragment newInstance(String param1, String param2) {
        GeneralFragment fragment = new GeneralFragment();
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
    public View view;                                                                               //the highest level layout of the fragment
    private ArrayList<BoulderProblem> bps = new ArrayList<>();                                      //contains the list of boulder problems to be displayed
    private ArrayList<BoulderProblem> displayBps = new ArrayList();
    private DatabaseReference bpRef;                                                                //contains the reference of the location where to retrieve boulder problems from
    private ChildEventListener mainQuery;                                                           //contains the query that is attached to the above location, changes based on what is to be displayed
    private String tabType = "General";                                                             //initially set the tab type to general (all boulder problems)
    private ArrayList<String> favorites = new ArrayList<>();                                        //list of the user's favorite boulder problems (contains the problem names)
    private FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();                 //getting the currently signed in user
    private String sortOption = "name";                                                             //contains sort option indicating how the displayed bp's should be sorted
    private String query = null;                                                                    //contains the search query used to search for specific boulder problems
    private ArrayList<Integer> filters = new ArrayList<>();                                         //contains the list of hold type filters that may be applied
    private boolean ascendingOption = true;                                                         //contains boolean indicating whether to show items in ascending or descending order

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_general, container, false);

        //get the database instance and the reference for the location for boulder problems
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        bpRef = database.getReference().child("BoulderProblems");

        //initially create an empty query and attach it to the ref location
        EmptyQuery();

        //if the tag for this fragment is f0 then this tab is to display all boulder problems
        if(this.getTag().equals("f0"))
        {
            tabType = "General";
        }
        //if the tag is f1 then display favorites
        else if(this.getTag().equals("f1"))
        {
            tabType = "Favorites";
        }
        //if the tab is f2 then display user's own problems
        else if(this.getTag().equals("f2"))
        {
            tabType = "My Problems";
        }
        //if tag is f3 then the fragment displays benchmarks
        else if(this.getTag().equals("f3"))
        {
            tabType = "Benchmarks";
            view.setBackgroundResource(R.color.black_transparent);
        }

        if(tabType.equals("General"))
        {
            //call method to search all boulder problems
            GeneralSearch();
        }
        else if(tabType.equals("Favorites"))
        {
            //call method to retrieve all the user's favorite bp's
            GetUserFavs();
        }
        else if(tabType.equals("My Problems"))
        {
            //call method to search for user's own boulder problems
            MyProblemsSearch();
        }
        else
        {
            //call method to search for benchmark bp's
            BenchmarkSearch();
        }
        return view;
    }

    //method to populate the recycler view (list view) with the list of boulder problems to be displayed
    public void PopulateRV()
    {
        //get the recycler view from the layout
        RecyclerView rv = (RecyclerView) view.findViewById(R.id.GeneralFragmentRV);
        rv.setHasFixedSize(true);
        rv.setLayoutManager(new LinearLayoutManager(view.getContext()));
        //pass the list of bps to the Recycler view via a custom adapter

        //get the list of bps sorted by user selected sort option
        ArrayList<BoulderProblem> sortedBps = SortBps();

        //return the list of bps in ascending or descending order based on user selected option
        ArrayList<BoulderProblem> displayedBps = new ArrayList<>();
        if(!ascendingOption)
        {
            for(int i = sortedBps.size(); i > 0; i--)
            {
                displayedBps.add(sortedBps.get(i-1));
            }
        }
        else
        {
            for(BoulderProblem bp : sortedBps)
            {
                displayedBps.add(bp);
            }
        }
        rv.setAdapter(new GeneralRVAdapter(displayedBps));
    }

    //method to sort the list of boulder problems being displayed by user selected sort option
    private ArrayList<BoulderProblem> SortBps()
    {
        ArrayList<BoulderProblem> sortedBps = displayBps;
        if(sortOption.equals("name"))
        {
            Collections.sort(sortedBps, bpNameComparator);
        }
        else if(sortOption.equals("grade"))
        {
            Collections.sort(sortedBps, bpGradeComparator);
        }
        else
        {
            Collections.sort(sortedBps, bpSetterComparator);
        }
        return sortedBps;
    }

    //custom comparator for sorting boulder problems by name
    private static Comparator<BoulderProblem> bpNameComparator = new Comparator<BoulderProblem>()
    {
        @Override
        public int compare(BoulderProblem bp1, BoulderProblem bp2)
        {
            return bp1.GetName().toLowerCase().compareTo(bp2.GetName().toLowerCase());
        }
    };

    //custom comparator for sorting bps by grade
    private static Comparator<BoulderProblem> bpGradeComparator = new Comparator<BoulderProblem>() {
        @Override
        public int compare(BoulderProblem bp1, BoulderProblem bp2) {
            return bp1.GetGrade().compareTo(bp2.GetGrade());
        }
    };

    //custom comparator for sorting bps by setter display name
    private static Comparator<BoulderProblem> bpSetterComparator = new Comparator<BoulderProblem>() {
        @Override
        public int compare(BoulderProblem bp1, BoulderProblem bp2) {
            return bp1.GetSetter().toLowerCase().compareTo(bp2.GetSetter().toLowerCase());
        }
    };

    //method to assign search input and selected filters, plus selected sort option and call search method
    public void Search(ArrayList<Integer> nFilters, String nQuery, String sort, boolean ascending)
    {
        sortOption = sort;
        query = nQuery;
        filters = nFilters;
        ascendingOption = ascending;

        BPSearch();
    }

    //method to check if a boulder problem matches all the selected hold type filters
    public Boolean FilterMatch(BoulderProblem bp, ArrayList<Integer> filters)
    {
        //intially set boolean to true
        Boolean allMatches = true;
        //if filters list is not empty then do the check
        if(filters.size() > 0)
        {
            //get the list of holds in the bp route
            ArrayList<Hold> bpRoute = bp.route;
            //create an array of booleans for each filter. intially set all booleans to false
            Boolean[] filterMatches = new Boolean[filters.size()];
            for(int i = 0; i < filterMatches.length; i++)
            {
                filterMatches[i] = false;
            }
            //loop through all the holds and check if they match a selected filter.
            //if so then set the respective boolean to true
            for(int i = 0; i < filters.size(); i++)
            {
                for(Hold hold : bpRoute)
                {
                    if(Integer.parseInt(hold.GetType()) == filters.get(i))
                    {
                        filterMatches[i] = true;
                        break;
                    }
                }
            }
            //check if all the booleans are true (all filters are satisfied)
            for(boolean match : filterMatches)
            {
                if(!match)
                {
                    //if not then set the match to false
                    allMatches = false;
                    break;
                }
            }
        }
        return allMatches;
    }

    //method to initialise an empty listener for boulder problems in the database
    public void EmptyQuery()
    {
        mainQuery = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

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
        bpRef.child("UserCreated").addChildEventListener(mainQuery);
        bpRef.child("Benchmarks").addChildEventListener(mainQuery);
    }

    /*
    private ArrayList<BPRecord> bpRecords = new ArrayList<>();
    private ArrayList<BoulderProblem> recommendedBps = new ArrayList<>();
    private ArrayList<BoulderProblem> recordBps = new ArrayList<>();


    private void GetRecordBPs()
    {
        DatabaseReference recordRef = FirebaseDatabase.getInstance().getReference().child("ascentusers").child(currentUser.getUid()).child("record");
        recordRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String bpName = snapshot.getKey();
                BPRecord bpRecord = snapshot.getValue(BPRecord.class);
                bpRecords.add(bpRecord);
                bpRef.child("UserCreated").child(bpName).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        BoulderProblem recordBp = snapshot.getValue(BoulderProblem.class);
                        recordBps.add(recordBp);
                        for(int i = 0; i < recommendedBps.size(); i++)
                        {
                            if(recommendedBps.get(i).GetName().equals(recordBp.GetName()))
                            {
                                recommendedBps.remove(i);
                                PopulateRV();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
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
        });
    }
    public void RecommendedSearch()
    {
        bps.clear();
        bpRef.child("UserCreated").removeEventListener(mainQuery);

        mainQuery = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                BoulderProblem bp = snapshot.getValue(BoulderProblem.class);
                boolean found = false;
                for(int i = 0; i < recordBps.size(); i++)
                {
                    if(recordBps.get(i).GetName().equals(bp.GetName()))
                    {
                        found = true;
                    }
                }
                if(!found)
                {
                    boolean goodGrade = false;
                    ArrayList<String> gradesSorted = GetGradesSorted();
                    if(gradesSorted.size() > 0)
                    {
                        for(int i = 0; i < gradesSorted.size(); i++)
                        {
                            if(i >2)
                            {
                                break;
                            }
                            else if(gradesSorted.get(i).equals(bp.GetGrade()))
                            {
                                goodGrade = true;
                                break;
                            }
                        }
                    }
                    else
                    {
                        goodGrade = true;
                    }
                    if(goodGrade)
                    {
                        recommendedBps.add(bp);
                        PopulateRV();
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                BoulderProblem bp = snapshot.getValue(BoulderProblem.class);
                for(int i = 0; i < recommendedBps.size(); i++)
                {
                    if(recommendedBps.get(i).GetName().equals(bp.GetName()))
                    {
                        recommendedBps.remove(i);
                        PopulateRV();
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
        bpRef.child("UserCreated").addChildEventListener(mainQuery);
    }

    private ArrayList<String> GetGradesSorted()
    {
        HashMap<String, Integer> gradeCounts = new HashMap<String, Integer>();
        for(BoulderProblem bp : recordBps)
        {
            if(gradeCounts.keySet().contains(bp.GetGrade()))
            {
                gradeCounts.replace(bp.GetGrade(), gradeCounts.get(bp.GetGrade()) + 1);
            }
            else
            {
                gradeCounts.put(bp.GetGrade(), 1);
            }
        }
        ArrayList<String> grades = new ArrayList<>();
        ArrayList<Integer> counts = new ArrayList<>();
        for(String grade : gradeCounts.keySet())
        {
            boolean added = false;
            for(int i = 0; i < grades.size(); i++)
            {
                if(counts.get(i) < gradeCounts.get(i))
                {
                    counts.add(i, gradeCounts.get(grade));
                    grades.add(i, grade);
                    added = true;
                    break;
                }
            }
            if(!added)
            {
                grades.add(grade);
                counts.add(gradeCounts.get(grade));
            }
        }
        return grades;
    }*/

    //method to apply search input & filters to display list of boulder problems
    private void BPSearch()
    {
        //clear the display list of any previous boulder problems
        displayBps.clear();
        //call method to populate RV in case any displayed boulder problems are not to be shown anymore
        PopulateRV();
        //loop through list of boulder problems
        for(BoulderProblem bp : bps)
        {
            //check the filters
            boolean allMatches = FilterMatch(bp, filters);
            if(allMatches)
            {
                //check if user has entered a search, if so check if the problem name/setter display name/grade contains the input search text
                if(query == null || bp.name.toLowerCase().contains(query) || bp.setter.toLowerCase().contains(query) || bp.grade.contains(query))
                {
                    //add all boulder problems to list of boulder problems to be displayed
                    displayBps.add(bp);
                }
            }
        }
        //only display max of 100 boulder problems on the tab at once
        if(displayBps.size() > 0 && displayBps.size() <= 100)
        {
            PopulateRV();
        }
    }

    //method to search for all boulder problems in the database
    public void GeneralSearch()
    {
        mainQuery = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                //get the snapshot value as a boulder problem
                BoulderProblem bp = snapshot.getValue(BoulderProblem.class);

                //create listener to get the setter display name
                ValueEventListener setterListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //display name is the value of the snapshot
                        bp.SetSetter(snapshot.getValue().toString());
                        //add the boulder problem to list of boulder problems
                        bps.add(bp);
                        //call method to display correct list
                        BPSearch();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                };
                //call method to attach listener for getting setter display name
                AttachSetterListener(bp.GetSetterId(), setterListener);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                //listens for boulder problems removed from the database

                //get the snapshot value as a boulder problem object
                BoulderProblem bp = snapshot.getValue(BoulderProblem.class);
                //loop through list of boulder problems to check if it is in the list and if so remove it and re display the list on screen
                for(int i = 0; i < bps.size(); i++)
                {
                    if(bps.get(i).GetName().equals(bp.GetName()))
                    {
                        bps.remove(i);
                        BPSearch();
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
        bpRef.child("UserCreated").orderByChild(sortOption).addChildEventListener(mainQuery);
    }

    //method to search and display the user's own boulder problems
    public void MyProblemsSearch()
    {
        //set new listener for boulder problems set by the current user
        mainQuery = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                //get the snapshot value as a boulder problem object
                BoulderProblem bp = snapshot.getValue(BoulderProblem.class);

                //check if the boulder problem was set by the current user
                if(bp.GetSetterId().equals(currentUser.getUid()))
                {
                    //set the setter display name of the boulder problem using current user's display name
                    bp.SetSetter(currentUser.getDisplayName());
                    //add to list of boulder problems
                    bps.add(bp);
                    //call method to get the correct display list
                    BPSearch();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                //listens for removed boulder problems from the database

                //get the snapshot value as a boulder problem object
                BoulderProblem bp = snapshot.getValue(BoulderProblem.class);
                //find and remove the bp from the list of boulder problems
                for(int i = 0; i < bps.size(); i++)
                {
                    if(bps.get(i).GetName().equals(bp.GetName()))
                    {
                        bps.remove(i);
                        //call method to get correct display list
                        BPSearch();
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
        bpRef.child("UserCreated").orderByChild(sortOption).addChildEventListener(mainQuery);
    }

    //method to get list of names of the user's favorite boulder problems
    public void GetUserFavs() {
        //create reference of database location
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        //attach listener to location where favorites for user are stored
        dbRef.child("ascentusers/" + currentUser.getUid() + "/Favorites").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                //checking the snapshot exists
                if (snapshot.exists()) {
                    String favoriteName = snapshot.getKey();
                    //add the snapshot key to favorites list as it will be the boulder problem name
                    //favorites.add(snapshot.getKey());
                    dbRef.child("BoulderProblems").child("UserCreated").child(favoriteName).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            BoulderProblem bp = snapshot.getValue(BoulderProblem.class);

                            //create value event listener to find display name of setter in database
                            ValueEventListener setterListener = new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    //set the setter display name of the boulder problem
                                    bp.SetSetter(snapshot.getValue().toString());
                                    //add to list of boulder problems
                                    bps.add(bp);
                                    //call method to apply searches, filters and sorts to get correct display list
                                    BPSearch();
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            };
                            AttachSetterListener(bp.GetSetterId(), setterListener);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                    //call the favorites search method
                    //FavoritesSearch();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                //listens for removed favorites

                //check the snapshot exists
                if(snapshot.exists())
                {
                    for(int i = 0; i < bps.size(); i++)
                    {
                        if(bps.get(i).GetName().equals(snapshot.getKey()))
                        {
                            bps.remove(i);
                            BPSearch();
                            break;
                        }
                    }
                    /*
                    //find and remove the boulder problem from the favorites list
                    for(int i = 0; i < favorites.size(); i++)
                    {
                        if(favorites.get(i).equals(snapshot.getKey()))
                        {
                            favorites.remove(i);
                            //find and remove the boulder problem from the bps list
                            for(int j = 0; j < bps.size(); j++)
                            {
                                if(bps.get(j).GetName().equals(snapshot.getKey()))
                                {
                                    bps.remove(j);
                                    PopulateRV();
                                    break;
                                }
                            }
                            break;
                        }
                    }*/
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    //method to search for benchmark boulder problems
    private void BenchmarkSearch()
    {
        //clear all previous bps from the display list
        bps.clear();

        //create reference for location of all boulder problems
        DatabaseReference allBpRef = FirebaseDatabase.getInstance().getReference().child("BoulderProblems");
        allBpRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName)
            {
                //checking if key is benchmarks
                if(snapshot.getKey().equals("Benchmarks"))
                {
                    //get the number of benchmarks
                    long childCount = snapshot.getChildrenCount();

                    //add event listener for the benchmarks
                    bpRef.child("Benchmarks").removeEventListener(mainQuery);
                    mainQuery = new ChildEventListener() {
                        @Override
                        public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                            //get the snapshot value as boulder problem object
                            BoulderProblem benchmark = snapshot.getValue(BoulderProblem.class);
                            //add setter display name listener
                            ValueEventListener setterListener = new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    //set the setter display name of the benchmark using the snapshot value
                                    benchmark.SetSetter(snapshot.getValue().toString());
                                    displayBps.add(benchmark);
                                    //add the boulder problem to list of displayed boulder problems
                                    bps.add(benchmark);
                                    if(bps.size() == childCount)
                                    {
                                        PopulateRV();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            };
                            AttachSetterListener(benchmark.GetSetterId(), setterListener);
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
                    bpRef.child("Benchmarks").orderByChild("grade").addChildEventListener(mainQuery);
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
        });
    }

    //method to attach a setter display name listener to setter location in the realtime database
    private void AttachSetterListener(String setterId, ValueEventListener setterListener)
    {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("ascentusers").child(setterId);
        userRef.child("DisplayName").addListenerForSingleValueEvent(setterListener);
    }
}