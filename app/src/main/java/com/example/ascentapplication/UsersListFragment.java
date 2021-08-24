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
import java.util.HashMap;
import java.util.List;

//class for retrieve lists of users to display on screen
//can get list of user's friends, users that can be added, users who have been sent requests already, or users who have sent requests to signed in user
public class UsersListFragment extends Fragment {

    //automatically created code, can ignore for now
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    public UsersListFragment() {
        // Required empty public constructor
    }

    //automatically created code, currently not used
    public static UsersListFragment newInstance(String param1, String param2) {
        UsersListFragment fragment = new UsersListFragment();
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

    //Private Properties
    private View view;                                                                              //contains the highest level layout of the fragment
    private RecyclerView rv;                                                                        //contains the fragment recycler view
    private HashMap<String, String> users = new HashMap<>();                                        //contains list of suers to be displayed (id, Display name)
    private FirebaseDatabase database;                                                              //to contain instance of firebase database
    private DatabaseReference userRef;                                                              //to contain reference of location for list of users in the database
    private ChildEventListener usersQuery;                                                          //to contain the listener that will retrieve specific users from the database
    private ValueEventListener displayUserNameQuery;                                                //to contain the listener that will retrieve the display names of users from the database
    private String tabType = "View";                                                                //indicates the type of display for this fragment. "View" for viewing friends, "Add" sending requests, etc.
    private FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();                 //contains the currently signed in user
    private String search;                                                                          //contains the current user search input

    //hashmaps contain list of specific users related to current user in database
    private HashMap<String, String> friends = new HashMap<>();                                      //contains the user's friends
    private HashMap<String, String> requestsSent = new HashMap<>();                                 //contains list of users who have been sent friend requests
    private HashMap<String, String> requestsReceived = new HashMap<>();                             //contains lsit of users who sent requests to current user

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_users_list, container, false);

        //get the instance of the database
        database = FirebaseDatabase.getInstance();
        //get reference for location of all users in database
        userRef = database.getReference().child("ascentusers");

        //get recycler view from the layout
        rv = (RecyclerView) view.findViewById(R.id.UsersFragmentRV);
        rv.setHasFixedSize(true);
        rv.setLayoutManager(new LinearLayoutManager(view.getContext()));

        //get the current fragment tag
        String tag = this.getTag();

        //if tag is f0 then this fragment is for viewing user's friends
        if(this.getTag().equals("f0"))
        {
            tabType = "View";
        }
        //if tag is f1 then fragment for viewing requests sent
        else if(this.getTag().equals("f1"))
        {
            tabType = "Requests Sent";
        }
        //if tag f2 then fragment for viewing requests received
        else if(this.getTag().equals("f2"))
        {
            tabType = "Requests Received";
        }
        //if tag f3 then fragment for viewing other users who can still be added
        else if(this.getTag().equals("f3"))
        {
            tabType = "Add";
            view.setBackgroundResource(R.color.black_transparent);
        }

        //if the tabtype is add then call method to get list of all users in the database
        if(tabType.equals("Add"))
        {
            GetUsers();
        }

        //call methods to get list of user's friends, requests sent and requests received
        RetrieveFriends();
        RetrieveRequestsReceived();
        RetrieveRequestsSent();
        return view;
    }

    //method to pass the user display list to the recycler view
    /* Parameters
    * - displayList : map of all the users to be shown on this fragment
     */
    private void PopulateRV(HashMap<String, String> displayList)
    {
        //if showing requests received create a requests received rv adapter
        //need a different rv adapter for requests received as it has two buttons ( one for accept and one for reject)
        if(tabType.equals("Requests Received"))
        {
            rv.setAdapter(new RequestsRVAdapter(displayList));
        }
        //otherwise create a users rv adapter
        else
        {
            rv.setAdapter(new UsersRVAdapter(displayList, tabType));
        }
    }

    //method to use user search input to filter display list
    public void Search(String query)
    {
        //check if the user has entered input
        if(query != null)
        {
            //search property equals user's input
            search = query;
            //create hashmap to contain list of users after search filter
            HashMap<String, String> foundUsers = new HashMap<>();
            //if viewing friends
            if(tabType.equals("View"))
            {
                //loop through all friends and check if user input is contained in any display names
                for(String friendId : friends.keySet())
                {
                    String displayName = friends.get(friendId);
                    if(displayName.toLowerCase().contains(query))
                    {
                        foundUsers.put(friendId, displayName);
                    }
                }
                //populate the recycler view with the found users
                PopulateRV(foundUsers);
            }
            //if viewing requests sent
            else if(tabType.equals("Requests Sent"))
            {
                //loop through requests sent and check if user input is contained in any display names
                for(String userId : requestsSent.keySet())
                {
                    if(requestsSent.get(userId).toLowerCase().contains(query))
                    {
                        foundUsers.put(userId, requestsSent.get(userId));
                    }
                }
                //populate the recycler view with the found users
                PopulateRV(foundUsers);
            }
            //if viewing user's to be added
            else if(tabType.equals("Add"))
            {
                //loop through list of users
                for(String userId : users.keySet())
                {
                    if(users.get(userId).toLowerCase().contains(query))
                    {
                        foundUsers.put(userId, users.get(userId));
                    }
                }
                //populate the recycler view with list of found users
                PopulateRV(foundUsers);
            }
            //if viewing requests received
            else if(tabType.equals("Requests Received"))
            {
                //loop through requests received and check if user input is contained in any display names
                for(String requestingUserId : requestsReceived.keySet())
                {
                    String displayName = requestsReceived.get(requestingUserId);
                    if(displayName.toLowerCase().contains(query))
                    {
                        foundUsers.put(requestingUserId, displayName);
                    }
                }
                //populate the recycler view with the found users
                PopulateRV(foundUsers);
            }
        }
        else
        {
            //otherwise set search property to null
            search = null;
            //if viewing friends list
            if(tabType.equals("View"))
            {
                //popualte recycler view with friends list
                PopulateRV(friends);
            }
            //if viewing users to add
            else if(tabType.equals("Add"))
            {
                //populate the recycler view with users to add
                PopulateRV(users);
            }
            //if viewing requests sent
            else if(tabType.equals("Requests Sent"))
            {
                //populate rv with requests sent
                PopulateRV(requestsSent);
            }
            //if viewing requests received
            else if(tabType.equals("Requests Received"))
            {
                //populate rv with the requests received
                PopulateRV(requestsReceived);
            }
        }
    }

    //method to retrieve all users from database that are not in friends list, requests sent or requests received
    private void GetUsers()
    {
        //clear all previous users from users list
        users.clear();
        //set new listener to find users
        usersQuery = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                //get snapshot key as user id
                String userID = snapshot.getKey();
                //check if id matches current user id
                if(!userID.equals(currentUser.getUid()))
                {
                    //check if any other list contains user id
                    if(!UserListContains(friends, userID) && !UserListContains(requestsReceived, userID) && !UserListContains(requestsSent, userID))
                    {
                        //find the display name user user id
                        displayUserNameQuery = new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                //get snapshot value as display name
                                String displayName = snapshot.getValue().toString();
                                //add user to list of users to display and call search method in case user has entered search input
                                users.put(userID, displayName);
                                Search(search);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        };
                        userRef.child(userID).child("DisplayName").addListenerForSingleValueEvent(displayUserNameQuery);
                    }
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
        userRef.addChildEventListener(usersQuery);
    }

    //method to attach display name listener to user reference in the database
    private void AttachDNListener(String userId, ValueEventListener dNListener)
    {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("ascentusers").child(userId);
        userRef.child("DisplayName").addListenerForSingleValueEvent(dNListener);
    }

    //method to retrieve the list of friends from the database
    public void RetrieveFriends()
    {
        ChildEventListener friendsQuery = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String friendsId = snapshot.getKey();
                //get friends display name
                ValueEventListener dNListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String displayName = snapshot.getValue().toString();
                        //add to friends list
                        friends.put(friendsId, displayName);
                        //remove from users list if contained in there already
                        users.remove(friendsId);
                        Search(search);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                };
                AttachDNListener(friendsId, dNListener);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                //listen for friends removed
                String userId = snapshot.getKey();
                String displayName = friends.get(userId);
                users.put(userId, displayName);
                friends.remove(userId);
                Search(search);
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        userRef.child(currentUser.getUid()).child("Friends").addChildEventListener(friendsQuery);
    }

    //method to retrieve list of requests sent from the database
    public void RetrieveRequestsSent()
    {
        ChildEventListener reqSentQuery = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String userId = snapshot.getKey();
                //get request sent display name
                ValueEventListener dNListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String displayName = snapshot.getValue().toString();
                        //add request sent to related hashmap
                        requestsSent.put(userId, displayName);
                        //remove from all users hashmap and call search method in case user has entered search input
                        users.remove(userId);
                        Search(search);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                };
                AttachDNListener(userId, dNListener);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                //listen for requests sent removed
                String userId = snapshot.getKey();
                String displayName = requestsSent.get(userId);
                requestsSent.remove(userId);
                users.put(userId, displayName);
                Search(search);
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        userRef.child(currentUser.getUid()).child("Friend Requests Sent").addChildEventListener(reqSentQuery);
    }


    //method to retrieve list of requests received from the database
    public void RetrieveRequestsReceived()
    {
        ChildEventListener reqReceivedQuery = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String userId = snapshot.getKey();
                ValueEventListener dNlistener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String displayName = snapshot.getValue().toString();
                        requestsReceived.put(userId, displayName);
                        users.remove(userId);
                        Search(search);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                };
                AttachDNListener(userId, dNlistener);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String userId = snapshot.getKey();
                String displayName = requestsReceived.get(userId);
                requestsReceived.remove(userId);
                users.put(userId, displayName);
                Search(search);
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        userRef.child(currentUser.getUid()).child("Friend Requests").addChildEventListener(reqReceivedQuery);
    }

    //method to check if a hashmap contains a user id
    /* Parameters
    * - userlist: to hashmap to check through
    * - checkId: the id to find in the hashmap
     */
    private boolean UserListContains(HashMap<String, String> userList, String checkId)
    {
        boolean found = false;
        for(String userId : userList.keySet())
        {
            if(checkId.equals(userId))
            {
                found = true;
                break;
            }
        }
        return found;
    }
}