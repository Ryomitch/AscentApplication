package com.example.ascentapplication;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

//Record Fragment for displaying either complete or unfinished boulder problems for a user
public class RecordFragment extends Fragment {

    //automatically created code. Ignore for now
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    public RecordFragment() {
        // Required empty public constructor
    }

    //automatically created code. Ignore for now
    public static RecordFragment newInstance(String param1, String param2) {
        RecordFragment fragment = new RecordFragment();
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

    /* private properties */
    private View view;                                                                              //the highest level view/layout of the fragment
    private List<BoulderProblem> bps = new ArrayList<>();                                           //contains the list of boulder problems to be displayed
    private FirebaseDatabase database;                                                              //instance of the firebase database
    private DatabaseReference recordRef;                                                            //reference of location where to find complete or unfinished boulder problems for the user
    private String tabType = "Complete";                                                            //contains the tab type of the fragment either complete or unfinished
    private FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();                 //contains the current user
    private ChildEventListener recordListener;                                                      //listener for the record boulder problems in the database

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view =  inflater.inflate(R.layout.fragment_record, container, false);

        //get the database instance and location of the user's record
        database = FirebaseDatabase.getInstance();
        recordRef = database.getReference().child("ascentusers/" + currentUser.getUid() + "/record");

        //if tag is f0 then tab type is complete
        if(this.getTag().equals("f0"))
        {
            tabType = "Complete";
        }
        //if tag is f1 then tab type is unfinished
        else if(this.getTag().equals("f1"))
        {
            tabType = "Unfinished";
        }

        //call method to get the user's record
        GetRecord();
        //call method to attach listener for deleted boulder problems
        CheckDeleteListener();

        return view;
    }

    //method to populate the recycler view with the boulder problems
    public void PopulateRV()
    {
        RecyclerView rv = (RecyclerView) view.findViewById(R.id.RecordFragmentRV);
        rv.setHasFixedSize(true);

        rv.setLayoutManager(new LinearLayoutManager(view.getContext()));
        rv.setAdapter(new RecordRVAdapter(bps));
    }

    //method to get a user's record from the database
    public void GetRecord()
    {
        //clear all previous bps from the display list
        bps.clear();
        //check if listener is null
        if(recordListener != null)
        {
            //if not then remove the listener from the record database reference
            recordRef.removeEventListener(recordListener);
        }
        //check if tab type is for complete boulder problems
        if(tabType.equals("Complete"))
        {
            recordListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    if(snapshot.exists())
                    {
                        String bpName = snapshot.getKey();
                        recordRef.child(bpName + "/complete").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot nSnapshot) {
                                if(nSnapshot.exists())
                                {
                                    DatabaseReference bpRef = database.getReference().child("BoulderProblems/UserCreated").child(bpName);
                                    if(nSnapshot.getValue().toString().equals("Yes"))
                                    {
                                        //listener for the boulder problem in the database that was found in the user's complete record
                                        bpRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                if(snapshot.exists())
                                                {
                                                    BoulderProblem bp = snapshot.getValue(BoulderProblem.class);
                                                    //listener for the setter name
                                                    ValueEventListener setterListener = new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                            bp.SetSetter(snapshot.getValue().toString());
                                                            bps.add(bp);
                                                            PopulateRV();
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError error) {

                                                        }
                                                    };
                                                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("ascentusers").child(bp.GetSetterId());
                                                    userRef.child("DisplayName").addListenerForSingleValueEvent(setterListener);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {

                                            }
                                        });
                                    }
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
            recordRef.addChildEventListener(recordListener);
        }
        //check if tab type is for unfinished boulder problems
        else if(tabType.equals("Unfinished"))
        {
            recordListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    if(snapshot.exists())
                    {
                        String bpName = snapshot.getKey();
                        recordRef.child(bpName + "/complete").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot nSnapshot) {
                                if(nSnapshot.exists())
                                {
                                    DatabaseReference bpRef = database.getReference().child("BoulderProblems/UserCreated").child(bpName);
                                    if(nSnapshot.getValue().toString().equals("No"))
                                    {
                                        bpRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                if(snapshot.exists())
                                                {
                                                    BoulderProblem bp = snapshot.getValue(BoulderProblem.class);
                                                    ValueEventListener setterListener = new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                            bp.SetSetter(snapshot.getValue().toString());
                                                            bps.add(bp);
                                                            PopulateRV();
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError error) {

                                                        }
                                                    };
                                                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("ascentusers").child(bp.GetSetterId());
                                                    userRef.child("DisplayName").addListenerForSingleValueEvent(setterListener);
                                                }
                                            }
                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                            }
                                        });
                                    }
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
            recordRef.addChildEventListener(recordListener);
        }
    }

    //method to check for deleted boulder problems and remove them from display
    private void CheckDeleteListener()
    {
        ChildEventListener removalListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                BoulderProblem bp = snapshot.getValue(BoulderProblem.class);
                for(int i = 0; i < bps.size(); i++)
                {
                    if(bps.get(i).GetName().equals(bp.GetName()))
                    {
                        bps.remove(i);
                        GetRecord();
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
        DatabaseReference bpRef = database.getReference().child("BoulderProblems/UserCreated");
        bpRef.addChildEventListener(removalListener);
    }
}