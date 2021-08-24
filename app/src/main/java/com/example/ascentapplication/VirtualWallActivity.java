package com.example.ascentapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.constraintlayout.widget.Guideline;
import androidx.core.content.ContextCompat;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PostProcessor;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

//VirtualWallActivity class for displaying the virtual bouldering wall. Allows boulder problem creation/ boulder problem viewing or setup creation if an admin
public class VirtualWallActivity extends AppCompatActivity {

    /*Private Properties*/
    private Toolbar toolbar;                                                                                    //contains the toolbar for the activity
    private FirebaseAuth mAuth;                                                                                 //contains the firebase authentication instance
    private FirebaseUser currentUser;                                                                           //contains the current user

    //for setting up the virtual wall display
    private ArrayList<String> allSetups = new ArrayList<>();                                                    //contains the list of all setups
    private final List<String> columns = Arrays.asList("a","b","c","d","e","f","g","h","i","j","k");            //contains the list of columns on the virtual wall
    private HashMap<String, Integer> lettertoValue = new HashMap<String, Integer>();                                          //contains the value of each column as an integer
    private String setup = "Ascent Beginner";                                                                   //contains the currently viewed setup
    private ArrayList<ImageView> allHoldIVs = new ArrayList<>();                                                //contains the list of all hold imageviews on the virtual wall
    private ArrayList<Hold> allHolds = new ArrayList<>();                                                       //contains the list of all holds on the virtual wall (all holds in the setup)
    private ChildEventListener populateWallEventListener;                                                       //listener for finding the setup in the database and populating the virtuall wall holds

    //specific to creating boulder problems
    private ArrayList<Hold> route = new ArrayList<>();                                                          //contains the route when being created by the user as a list of holds
    private boolean routeComplete = false;                                                                      //contains a boolean indicating whether the user ahs finished creating a valid boulder problem
    private String selectedGrade;                                                                               //contains the selected grade for a created boulder problem
    private String isAlgorithm = "Yes";                                                                         //contains a string indicator of whether the boulder problem was created with the recommended graded or not
    private int bpCount = 0;                                                                                    //contains the number of created Boulder problems in the database. for reference when doing grading algorithm
    private int bpForSetupCount = 0;                                                                            //contains the number of boulder problems created for the setup

    //specific to viewing boulder problems
    private BoulderProblem viewProblem;                                                                         //contains the currently viewed boulder problem
    public static Bitmap viewAvatar;                                                                            //contains the avatar of setter of the viewed boulder problem
    private ArrayList<String> userFavorites = new ArrayList<>();                                                //contains the list of the user's favorites boulder problems
    private Boolean favorited = false;                                                                          //boolean indicating whether the current boulder problem is favorited

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_virtual_wall);

        //get the toolbar from the layout, set it's default title to null as layout contains a textview for the title
        toolbar = findViewById(R.id.VirtualWallToolbar);
        toolbar.setTitle("");
        //call method to allow options menu on the toolbar
        setSupportActionBar(toolbar);

        //get the firebase authentication instance and the current user
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        //call methods to get list of all setups, user's favorite boulder problems, and the number of created boulder problems in the database
        GetAllSetups();
        GetUserFavs();
        GetNumCreatedBPs();

        //assign values to each column on the virtual wall
        lettertoValue.put("a", 1);
        lettertoValue.put("b", 2);
        lettertoValue.put("c", 3);
        lettertoValue.put("d", 4);
        lettertoValue.put("e", 5);
        lettertoValue.put("f", 6);
        lettertoValue.put("g", 7);
        lettertoValue.put("h", 8);
        lettertoValue.put("i", 9);
        lettertoValue.put("j", 10);
        lettertoValue.put("k", 11);

        //check if the previous activity passed any extras
        if(getIntent().getExtras() != null)
        {
            //if so then activity is for viewing a boulder problem
            //set the viewed boulder problem using the extras
            viewProblem = new BoulderProblem();
            viewProblem.SetName(getIntent().getSerializableExtra("Selected Problem Name").toString());
            viewProblem.SetSetterId(getIntent().getSerializableExtra("Selected Problem SetterId").toString());
            viewProblem.SetSetter(getIntent().getSerializableExtra("Selected Problem Setter").toString());
            viewProblem.SetGrade(getIntent().getSerializableExtra("Selected Problem Grade").toString());
            viewProblem.ConvertRouteStringList((ArrayList<String>) getIntent().getSerializableExtra("Selected Problem Route"));
            viewProblem.SetAlgorithm(getIntent().getSerializableExtra("Selected Problem Algorithm").toString());
            viewProblem.SetSetup(getIntent().getSerializableExtra("Selected Problem Setup").toString());
            //change the setup based on the viewed boulder problem
            setup = viewProblem.GetSetup();

            //populate the avatar of the setter of the boulder problem
            PopulateUserDetails();

            //call method to populate the setup on the wall and show the boulder problem on the setup
            PopulateVirtualWall();
        }
        else
        {
            //otherwise in creating boulder problem mode

            //create a reference for the user's last viewed setup name location in database
            //this so the app remembers and shows the last setup the user viewed
            DatabaseReference prevViewedSetupRef = FirebaseDatabase.getInstance().getReference().child("ascentusers").child(currentUser.getUid()).child("Last Viewed Setup");
            prevViewedSetupRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    //if snapshot exists then setup will be it's value
                    if(snapshot.exists())
                    {
                        setup = snapshot.getValue().toString();
                    }

                    //call method to display user avatar on screen
                    PopulateUserDetails();

                    //call method to populate the setup on the virtual wall
                    PopulateVirtualWall();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }

    //method to retrieve the number of bps in database that are created by users
    public void GetNumCreatedBPs()
    {
        //create a reference of the location of where to find boulder problems in the realtime database
        DatabaseReference bpRef = FirebaseDatabase.getInstance().getReference().child("BoulderProblems");
        //creating reference of where to find user created boulder problems (problems made by users in app, not for testing purposes)
        DatabaseReference createdBPRef = bpRef.child("UserCreated");

        //add listener to the reference
        createdBPRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //get the child node count to find out number of created boulder problems
                bpCount = (int) snapshot.getChildrenCount();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    //method that populates the menu options based on the user
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        //inflate the menu layout from resources
        getMenuInflater().inflate(R.menu.virtual_wall_menu, menu);
        /*
        //if the user has this user id (i.e. the admin id), can add more admin id's here
        if(currentUser.getUid().equals("I7EgF4HcrGbJGB9O2oauLRFkbjr2"))
        {
            //if the admin is not viewing a boulder problem then show set wall option
            if((viewProblem==null))
            {
                menu.findItem(R.id.set_wall_option).setVisible(true);
            }
            //otherwise don't show the set wall option
            else
            {
                menu.findItem(R.id.set_wall_option).setVisible(false);
            }
        }
        //otherwise hide the set wall option
        else
        {
            menu.findItem(R.id.set_wall_option).setVisible(false);
        }*/
        //if the user is viewing a boulder problem
        if(viewProblem!=null)
        {
            //hide the change setup option
            menu.findItem(R.id.change_setup_option).setVisible(false);
            menu.findItem(R.id.set_wall_option).setVisible(false);
            //if the user is viewing their own boulder problem then show the delete option
            if(viewProblem.GetSetterId().equals(currentUser.getUid()))
            {
                menu.findItem(R.id.delete_option).setVisible(true);
            }
            //otherwise hide delete option
            else
            {
                menu.findItem(R.id.delete_option).setVisible(false);
            }
        }
        //otherwise show the set wall option and hide the delete option
        else
        {
            menu.findItem(R.id.change_setup_option).setVisible(true);
            menu.findItem(R.id.delete_option).setVisible(false);
        }
        return true;
    }

    //method for menu option on click
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem)
    {
        //use switch case to determine which menu item was selected
        switch(menuItem.getItemId())
        {
            case R.id.set_wall_option:                                                              //if user selected to set a new wall
                TextView toolbarTV = findViewById(R.id.VirtualWallToolbarTV);                       //get the textview in the toolbar
                toolbarTV.setText("Create Setup");                                                  //change it's text to create setup
                TextView titleTV = findViewById(R.id.VirtualWallTitleTV);                           //get the screen title textview
                titleTV.setText("New Wall Setup");                                                  //change it's text to new wall setup
                RelativeLayout addButton = findViewById(R.id.VirtualWallAddBtnRL);                  //get the add button
                addButton.setVisibility(View.VISIBLE);                                              //set the add button to visible
                RelativeLayout resetBtn = findViewById(R.id.VirtualWallResetBtnRL);                 //get the reset button
                //change the reset button on click to now clear holds added to the wall
                resetBtn.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        //loop through all the holds image views on the wall and set them to empty images. reset hold types to 0, and reset their tag
                        for(int i = 0; i < allHolds.size(); i++)
                        {
                            ImageView holdIV = allHoldIVs.get(i);
                            Hold hold = allHolds.get(i);
                            holdIV.setImageDrawable(null);
                            hold.SetType("0");
                            holdIV.setTag(hold.GetColumn() + hold.GetRow() + hold.GetType());
                        }
                    }
                });
                ImageView cancelImage = findViewById(R.id.VirtualWallSearchIV);                     //get the search imageview
                cancelImage.setImageResource(R.drawable.cancel);                                    //change the search image to a cancel image
                RelativeLayout cancelBtn = findViewById(R.id.VirtualWallSearchBtnRL);               //get the search button
                //change the search button on click to a cancel button on click
                cancelBtn.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        toolbarTV.setText(setup);                                                   //change the toolbar textview back to the name of the viewed setup
                        titleTV.setText("Create Boulder Problem");                                  //change the title textview back to create boulder problem
                        cancelImage.setImageResource(R.drawable.search_icon);                       //change the cancel image back to a search image
                        PopulateVirtualWall();                                                      //call method to repopulate the virtual wall
                        //change the reset button on click back to original on click functionality
                        resetBtn.setOnClickListener(new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View view)
                            {
                                ResetOnClick(view);
                            }
                        });
                        addButton.setVisibility(View.INVISIBLE);                                    //hide the add button
                        //reset the add button on click back to original functionality
                        addButton.setOnClickListener(new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View view)
                            {
                                AddProblemOnClick(view);
                            }
                        });
                        //reset the cancel button on click to original functionality
                        cancelBtn.setOnClickListener(new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View view)
                            {
                                SearchOnClick(view);
                            }
                        });
                    }
                });
                //change the add button on click to now allow new setup to be added the database
                addButton.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        Dialog addWallSetupDialog = new Dialog(view.getContext());                                      //create an add setup dialog
                        addWallSetupDialog.setContentView(R.layout.add_new_wallset_dialog);                             //inflate the dialog layout
                        TextInputEditText setupNameET = addWallSetupDialog.findViewById(R.id.AddWallSetTIET);           //get the setup name input edit text
                        Button addSetupBtn = addWallSetupDialog.findViewById(R.id.AddWallSetBtn);                       //get the dialog add setup button
                        //add new on click the add setup button
                        addSetupBtn.setOnClickListener(new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View view)
                            {
                                String setupName = setupNameET.getText().toString();                                    //get the user setup name input
                                //check user has entered a name for the setup
                                if(!setupName.isEmpty() && !setupName.equals("") && setupName != null)
                                {
                                    //create list to contain the holds selected in starting, middle or final rows
                                    ArrayList<Hold> finalRow = new ArrayList<>();
                                    ArrayList<Hold> startingRows = new ArrayList<>();
                                    ArrayList<Hold> middleRows = new ArrayList<>();
                                    //find all the holds the user has selected organising them into the correct list. If the user did not set a type then the hold is ignored
                                    for(int i = 0; i < allHolds.size(); i++)
                                    {
                                        Hold hold = allHolds.get(i);
                                        int holdRow = Integer.parseInt(hold.GetRow());
                                        if(holdRow <= 5)
                                        {
                                            if(!hold.GetType().equals("0"))
                                            {
                                                startingRows.add(hold);
                                            }
                                        }
                                        else if(holdRow == 18)
                                        {
                                            if(!hold.GetType().equals("0"))
                                            {
                                                finalRow.add(hold);
                                            }
                                        }
                                        else
                                        {
                                            if(!hold.GetType().equals("0"))
                                            {
                                                middleRows.add(hold);
                                            }
                                        }
                                    }
                                    //check the number of selected holds meets the requirements
                                    if(startingRows.size() >= 2 && middleRows.size() >= 2 && finalRow.size() >= 1)
                                    {
                                        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();                //create reference of realtime database
                                        //loop through all the holds on the wall and upload them to the database under the new setup
                                        for(int i = 0; i< allHolds.size(); i++)
                                        {
                                            Hold hold = allHolds.get(i);
                                            ImageView holdIV = allHoldIVs.get(i);
                                            Map<String, Object> setupDBEntry = new HashMap<>();
                                            setupDBEntry.put("WallSetups/" + setupName + "/" + hold.GetColumn() + "" + hold.GetRow(), hold);
                                            dbRef.updateChildren(setupDBEntry);
                                            //change the hold on click back to selecting a hold for creating boulder problems
                                            holdIV.setOnClickListener(new View.OnClickListener()
                                            {
                                                @Override
                                                public void onClick(View view) {
                                                    SelectHoldOnClick(holdIV, hold);
                                                }
                                            });
                                        }
                                        setup = setupName;                                                                      //change the current setup to the new setup name
                                        addWallSetupDialog.dismiss();                                                           //dismiss the add setup dialog
                                        toolbarTV.setText(setup);                                                               //change toolbar textview to name of new setup
                                        titleTV.setText("Create Boulder Problem");                                              //change the title textview to create boulder problem
                                        addButton.setVisibility(View.INVISIBLE);                                                //hide the add button
                                        //reset the add button on click to original functionality
                                        addButton.setOnClickListener(new View.OnClickListener()
                                        {
                                            @Override
                                            public void onClick(View view)
                                            {
                                                AddProblemOnClick(view);
                                            }
                                        });
                                        //change the reset button functionality back to original on click method
                                        resetBtn.setOnClickListener(new View.OnClickListener()
                                        {
                                            @Override
                                            public void onClick(View view)
                                            {
                                                ResetOnClick(view);
                                            }
                                        });
                                        //change the cancel image back to search image and reset the cancel button back to search functionality
                                        cancelImage.setImageResource(R.drawable.search_icon);
                                        cancelBtn.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                SearchOnClick(view);
                                            }
                                        });
                                        //notify user that the setup has been created successfully
                                        Toast.makeText(VirtualWallActivity.this, "Setup Created", Toast.LENGTH_SHORT).show();
                                    }
                                    else
                                    {
                                        //tell user they need to select more holds
                                        Toast.makeText(VirtualWallActivity.this, "You must select at least 2 holds from the starting rows (1-5), \n 2 from middle rows (6-17), \n and 1 from the last row (18).", Toast.LENGTH_LONG).show();
                                    }
                                }
                                else
                                {
                                    //tell user they need to enter a setup name
                                    Toast.makeText(VirtualWallActivity.this, "Please enter a setup name.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                        //show the add setup dialog
                        addWallSetupDialog.show();
                    }
                });
                //loop through all the holds and prepare for new setup mode
                for(int j = 0; j < allHoldIVs.size(); j++)
                {
                    //get the holds and setup all their types to empty holds
                    ImageView holdIV = allHoldIVs.get(j);
                    Hold hold = allHolds.get(j);
                    hold.SetType("0");
                    //change their tag to empty hold tag, clear their hold image and background
                    holdIV.setTag(hold.GetColumn() + hold.GetRow() + hold.GetType());
                    holdIV.setImageDrawable(null);
                    holdIV.setBackground(null);
                    //change the hold on click functionality for selecting new types of holds
                    holdIV.setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View view)
                        {
                            //create a set hold dialog
                            Dialog setHoldDialog = new Dialog(view.getContext());
                            setHoldDialog.setContentView(R.layout.set_holdtype_dialog);
                            //get the set hold type textview
                            TextView setHoldTypeTV = setHoldDialog.findViewById(R.id.SetHoldTypeTV);
                            //change the textview text to show which hold the user is setting the hold type for (i.e. hold column and row)
                            setHoldTypeTV.setText("Set Hold type for " + holdIV.getTag().toString().substring(0, holdIV.getTag().toString().length()-1) + ":");
                            //create a list with the different hold types
                            List<String> holdTypes = Arrays.asList("None", "Large Jug", "Jug", "Edge", "Sloper", "Pocket", "Pinch", "Crimp");
                            //create a new array adpater to show the list in the dropdown/spinner on the dialog
                            ArrayAdapter<String> typesAdapter = new ArrayAdapter<String>(setHoldDialog.getContext(), android.R.layout.simple_spinner_item, holdTypes);
                            typesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            Spinner holdTypeSpinner = (Spinner) setHoldDialog.findViewById(R.id.SetHoldTypeSpinner);
                            holdTypeSpinner.setAdapter(typesAdapter);

                            //add new on item selected listener to spinner
                            holdTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
                                {
                                    //when user selects a hold type change the hold tag and image to show the selected hold type
                                    String holdTag = holdIV.getTag().toString();
                                    Hold aHold = allHolds.get(0);
                                    for(Hold hold : allHolds)
                                    {
                                        if(hold.GetColumn().equals(holdTag.substring(0,1)) && hold.GetRow().equals(holdTag.substring(1, holdTag.length()-1)))
                                        {
                                            aHold = hold;
                                        }
                                    }
                                    aHold.SetType(String.valueOf(i));
                                    SetImageViewHoldType(holdIV, aHold);
                                }

                                @Override
                                public void onNothingSelected(AdapterView<?> adapterView)
                                {
                                }
                            });
                            //show the set hold dialog
                            setHoldDialog.show();
                        }
                    });
                }
                return true;
            case R.id.change_setup_option:                                                          //option to change the currently viewed setup when creating boulder problems
                Dialog changeSetupDialog = new Dialog(VirtualWallActivity.this);            //create a new change setup dialog
                changeSetupDialog.setContentView(R.layout.change_setup_dialog);                     //inflate dialog layout
                Spinner setupSpinner = changeSetupDialog.findViewById(R.id.ChangeSetupSpinner);     //get the dropdown/spinner from the dialog layout
                //create a new array adapter to show the possible setups to select from
                ArrayAdapter<String> setupAdapter = new ArrayAdapter<String>(changeSetupDialog.getContext(), android.R.layout.simple_spinner_item, allSetups);
                setupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                setupSpinner.setAdapter(setupAdapter);
                //set the currently select setup in the spinner to the currently viewed setup
                for(int i = 0; i < allSetups.size(); i++)
                {
                    if(setup.equals(allSetups.get(i)))
                    {
                        setupSpinner.setSelection(i);
                        break;
                    }
                }
                //add new on item selected listener to the spinner
                setupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        //change the name of currently viewed setup
                        setup = allSetups.get(i);
                        //change the user's last viewed setup in the database to remember which setup to show next time
                        DatabaseReference prevViewedSetupRef = FirebaseDatabase.getInstance().getReference().child("ascentusers").child(currentUser.getUid());
                        prevViewedSetupRef.child("Last Viewed Setup").setValue(setup);
                        //call method to repopulate wall with the newly selected setup
                        PopulateVirtualWall();
                    }
                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });
                //show the change setup dialog
                changeSetupDialog.show();
                return true;
            case R.id.help_option:                                                                  //option to get help with the virtual wall
                DisplayHelpDialog();                                                                //call method to display the help dialog
                return true;
            case R.id.delete_option:                                                                //option to delete a boulder problem
                DisplayDeleteDialog();                                                              //call method to display the delete dialog
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    //method to set the image of a hold on the virtual wall based on their hold type
    /* Parameters
    * - holdIV : the imageview for which determine the hold image
    * - hold : the hold object that corresponds to the hold image view
     */
    public void SetImageViewHoldType(ImageView holdIV, Hold hold)
    {
        //if type is 0 then don't show a hold image
        if(hold.GetType().equals("0"))
        {
            holdIV.setImageDrawable(null);
        }
        //if type is 1 then show the large jug image
        else if(hold.GetType().equals("1"))
        {
            holdIV.setImageResource(R.drawable.large_jugs_a);
        }
        //if type is 2 then show the mini jugs image
        else if(hold.GetType().equals("2"))
        {
            holdIV.setImageResource(R.drawable.mini_jugs_a);
        }
        //if type is 3 then show the edges image
        else if(hold.GetType().equals("3"))
        {
            holdIV.setImageResource(R.drawable.edges_a);
        }
        //if type is 4 then show sloper image
        else if(hold.GetType().equals("4"))
        {
            holdIV.setImageResource(R.drawable.slopers_a);
        }
        //if type is 5 then show the pocket image
        else if(hold.GetType().equals("5"))
        {
            holdIV.setImageResource(R.drawable.pockets_a);
        }
        //if type is 6 then show the pinches image
        else if(hold.GetType().equals("6"))
        {
            holdIV.setImageResource(R.drawable.pinches_a);
        }
        //if type is 7 then show the crimp image
        else if(hold.GetType().equals("7"))
        {
            holdIV.setImageResource(R.drawable.crimps_a);
        }
        //change the tag of the image view to correspond with the hold column, row and hold type
        holdIV.setTag(hold.GetColumn() + hold.GetRow() + hold.GetType());
    }

    //method to retrieve the names of all setups in the realtime database
    public void GetAllSetups()
    {
        //create reference for location where setups are stored
        DatabaseReference setupRef = FirebaseDatabase.getInstance().getReference().child("WallSetups");
        //add child listener to the reference
        setupRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if(snapshot.exists())
                {
                    //the snapshot key will be the name of the setup
                    String aSetup = snapshot.getKey();
                    //add the setup name to list of all setups
                    allSetups.add(aSetup);
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

    //method to populate the virtual wall on screen with the selected setup. creates image views for all the holds on the wall with correct hold type images
    public void PopulateVirtualWall()
    {
        //check for hold image views that were created previously
        if(allHoldIVs.size() > 0)
        {
            //if so loop through and set all of them to null images
            for(int i = 0; i<allHoldIVs.size(); i++)
            {
                ImageView holdIV = allHoldIVs.get(i);
                holdIV.setImageDrawable(null);
                holdIV.setBackground(null);
            }
        }
        //clear all previous hold image views and all previous corresponding hold objects
        allHoldIVs.clear();
        allHolds.clear();
        //creating any routes that were previously created or in process of being created
        route.clear();
        //set the route complete indicator to false
        routeComplete = false;
        //get the layout containing the virtual wall
        ConstraintLayout virtualWallCL = findViewById(R.id.VirtualWallCL);
        //create a new constraint set object. will be used to determine where to show each of the holds
        ConstraintSet constraintSet = new ConstraintSet();
        //get the textview from the toolbar and set it to show the name of the currently viewed setup
        TextView toolbarTV = findViewById(R.id.VirtualWallToolbarTV);
        toolbarTV.setText(setup);
        //create a reference for the location of setups in the realtime database
        DatabaseReference setupRef = FirebaseDatabase.getInstance().getReference().child("WallSetups");
        //define new event listener for getting all the holds from the database and showing them on the wall
        populateWallEventListener = new ChildEventListener()
        {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName)
            {
                //get the snapshot value as a hold object
                Hold hold = snapshot.getValue(Hold.class);
                //add the hold to list of all holds
                allHolds.add(hold);
                //get the hold column and row values
                int col = lettertoValue.get(hold.GetColumn());
                int row = Integer.parseInt(hold.GetRow());

                //determine the name of the guidelines which the hold image view will be contained in based on the column and row of the hold
                //so if a hold has column B (or column 2) and row 2 then the image view for that hold
                // must be between the column a and column b guideline (for left on right guidelines)
                // and also between row 1 and row 2 guidelines (bottom and top guidelines)
                //these guidelines are pre-defined in the virtual wall activity layout resource file
                String colGuideLeft = "WallCol" + columns.get(col-1).toUpperCase() + "Guideline";
                String colGuideRight ="";
                if(col < columns.size())
                {
                    colGuideRight = "WallCol" + columns.get(col).toUpperCase() + "Guideline";
                }
                else
                {
                    colGuideRight = "WallColEndGuideline";
                }
                String rowGuideBottom = "";
                String rowGuideTop = "WallRow" + row + "Guideline";
                if(row == 1)
                {
                    rowGuideBottom ="WallRowStartGuideline";
                }
                else
                {
                    rowGuideBottom = "WallRow" + (row-1) + "Guideline";
                }
                Guideline colGuidelineLeft = virtualWallCL.findViewWithTag(colGuideLeft);
                Guideline colGuidelineRight = virtualWallCL.findViewWithTag(colGuideRight);
                Guideline rowGuidelineTop = virtualWallCL.findViewWithTag(rowGuideTop);
                Guideline rowGuidelineBottom = virtualWallCL.findViewWithTag(rowGuideBottom);

                //create a new imageview to represent the hold and add to list of all hold image views
                ImageView holdIV = new ImageView(VirtualWallActivity.this);
                allHoldIVs.add(holdIV);
                //call method to display correct image based on the hold type
                SetImageViewHoldType(holdIV, hold);
                //set the scale type of image to center crop to show iamge properly
                holdIV.setScaleType(ImageView.ScaleType.CENTER_CROP);
                //generate a new view id for the imageview using the containing layout
                holdIV.setId(virtualWallCL.generateViewId());
                //add the imageview to the containing layout
                virtualWallCL.addView(holdIV);
                //define the height and width of the imageview
                holdIV.getLayoutParams().height = 60;
                holdIV.getLayoutParams().width = 60;
                //add a new on click to image view
                holdIV.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        //call method to determine the hold selection
                        SelectHoldOnClick(holdIV, hold);
                    }
                });
                //define the constraint set of the image view on the virtual wall using the guidelines
                constraintSet.clone(virtualWallCL);
                constraintSet.connect(holdIV.getId(), ConstraintSet.LEFT, colGuidelineLeft.getId(), ConstraintSet.LEFT);
                constraintSet.connect(holdIV.getId(), ConstraintSet.RIGHT, colGuidelineRight.getId(), ConstraintSet.RIGHT);
                constraintSet.connect(holdIV.getId(), ConstraintSet.TOP, rowGuidelineTop.getId(), ConstraintSet.TOP);
                constraintSet.connect(holdIV.getId(), ConstraintSet.BOTTOM, rowGuidelineBottom.getId(), ConstraintSet.BOTTOM);
                constraintSet.applyTo(virtualWallCL);

                //check if this is the last hold (the wall contains 198 holds)
                if(allHoldIVs.size() == 198)
                {
                    //check if the user is viewing a boulder problem
                    if(viewProblem != null)
                    {
                        //create a database reference
                        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
                        //attach child listener for the location of problem in the user's record.
                        //in other words check if user's record shows that user has completed the viewed boulder problem or not
                        dbRef.child("ascentusers/" + currentUser.getUid() + "/record/" + viewProblem.GetName() + "/complete").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                //if the location exists
                                if(snapshot.exists())
                                {
                                    //check if the value says yes (they have completed it)
                                    if(snapshot.getValue().toString().equals("Yes"))
                                    {
                                        //if so then call method to display the selected boulder problem and pass true to indicate that user has completed it and cannot add more attempts
                                       DisplaySelectedProblem();
                                    }
                                    //if not then call method to display the viewed boulder problem but pass false to indicate that user can still add more attempts and set it to complete
                                    else
                                    {
                                        DisplaySelectedProblem();
                                    }
                                }
                                //otherwise the user has not completed the boulder problem
                                else
                                {
                                    DisplaySelectedProblem();
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
        //add the popualte wall listener to the location of the currently viewed setup
        setupRef.child(setup).addChildEventListener(populateWallEventListener);
    }

    //method for selecting a hold when creating boulder problems
    /* Parameters
    * - holdIV : the image view that represents the hold the user selected
    * - hold : the hold object that corresponds to the hold the user selected
     */
    public void SelectHoldOnClick(ImageView holdIV, Hold hold)
    {
        //check if the hold is not an empty hold
        if(!hold.GetType().equals("0"))
        {
            //check the route being created already contains the selected hold (i.e. user is de-selecting the hold)
            if(route.contains(hold))
            {
                //check if the route is already finished (user has created a full route already)
                if(routeComplete)
                {
                    //check if the hold is on the last row
                    if(hold.GetRow().equals("18"))
                    {
                        //if so then remove hold from the route and set the route to uncomplete
                        route.remove(hold);
                        routeComplete = false;
                        //hide the add boulder problem button
                        RelativeLayout addProblemBtn = (RelativeLayout) findViewById(R.id.VirtualWallAddBtnRL);
                        addProblemBtn.setVisibility(View.INVISIBLE);
                        //clear circle indicating that the hold was selected
                        holdIV.setBackground(null);
                    }
                }
                //otherwise user can deselect hold if it is the previous hold
                else if(route.indexOf(hold) == route.size()-1)
                {
                    //else de-select the hold by removing it from the route and clearing the circle that indicates it is selected
                    route.remove(hold);
                    holdIV.setBackground(null);
                }
            }
            else if(!routeComplete)                                         //otherwise if the route is unfinished and hold not already selected
            {
                if(hold.GetRow().equals("18") && !routeComplete && route.size()>=4)                             //check if it is a finishing hold and the route has at least 4 holds in it already
                {
                    //if so the hold can set to selected

                    //set background to red circle to indicate finish hold selected
                    holdIV.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.finish_hold_clicked));
                    //change boolean to indicate the route is complete and and the hold to the route arraylist
                    routeComplete = true;
                    route.add(hold);
                    //show the add boulder problem button
                    RelativeLayout addProblemBtn = (RelativeLayout) findViewById(R.id.VirtualWallAddBtnRL);
                    addProblemBtn.setVisibility(View.VISIBLE);
                }
                //checking if the hold is a start hold (is it in rows 1-5 and does the route not already have 2 start holds)
                else if(route.size() < 2)
                {
                    if(hold.GetRow().equals("1") || hold.GetRow().equals("2") || hold.GetRow().equals("3") || hold.GetRow().equals("4") || hold.GetRow().equals("5"))
                    {
                        //set the backforund of the hold iamge view to a green circle to indicate it is a start hold
                        holdIV.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.start_hold_clicked));
                        //add the hold to the route
                        route.add(hold);
                    }
                    else
                    {
                        Toast.makeText(VirtualWallActivity.this, "Starting holds must be between rows 1 and 5 (inclusive).", Toast.LENGTH_SHORT).show();
                    }
                }
                //check if the route is too big ( check if equal to 20 as the user can still select finish hold when on 19 holds in route)
                else if(route.size() > 20)
                {
                    Toast.makeText(getApplicationContext(), "Too Many Holds Selected! Max 20 holds!", Toast.LENGTH_SHORT).show();
                }
                //check if the hold is a middle hold (not one of first 2 selected holds), not in last row and
                else if(route.size() >= 2)
                {
                    //check hold is not in last row
                    if(!hold.GetRow().equals("18"))
                    {
                        //check if hold is positioned higher or in same row as the previous hold)
                        if(Integer.parseInt(hold.GetRow()) >= Integer.parseInt(route.get(route.size()-1).GetRow()))
                        {
                            //create boolean which will indicate if the hold is on same row as 2 other holds
                            // (can only have 2 holds on the same row)
                            boolean sameRowHolds = false;
                            //loop through the route so far
                            for(int i = 0; i < route.size(); i++)
                            {
                                //if the hold row matches the selected hold row
                                if(route.get(i).GetRow().equals(hold.GetRow()))
                                {
                                    //loop through all the holds in the route again
                                    for(int j = 0; j < route.size(); j++)
                                    {
                                        //now check if hold row matches selected hold and the it is not the same hold as the one previously found
                                        if(route.get(j).GetRow().equals(hold.GetRow()) && j!=i)
                                        {
                                            //if so then the selected hold is on a row the same as 2 other holds and cannot be selected
                                            sameRowHolds = true;
                                            break;
                                        }
                                    }
                                }
                            }
                            //if not on row same as 2 other holds
                            if(!sameRowHolds)
                            {
                                //then set the background of the hold to a blue circle to indicate this is a middle hold and add the hold to the route
                                holdIV.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.middle_hold_clicked));
                                route.add(hold);
                            }
                        }
                        else
                        {
                            Toast.makeText(VirtualWallActivity.this, "Hold must be above or on the row of the last selected hold", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else
                    {
                        Toast.makeText(VirtualWallActivity.this, "Must select at least 2 middle holds before selecting a final hold", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    //method to populate the user avatar on the toolbar
    public void PopulateUserDetails()
    {
        //get the avatar image view
        ImageView avatarIV = findViewById(R.id.VirtualWallAvatarIV);
        //check if the user is not viewing a boulder problem, or if they're viewing a boulder problem they set
        if(viewProblem == null || currentUser.getUid().equals(viewProblem.GetSetterId()))
        {
            //if this is the case then shwo the user's own avatar
            //retrieved from the home activity to prevent image being downloaded multiple times
            avatarIV.setImageBitmap(HomeActivity.appUser.GetAvatar());
        }
        //otherwise the user is viewing a boulder problem not created by them, so screen must show the setter's avatar
        else
        {
            //create reference of location where setter's avatar is stored
            FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
            StorageReference storageReference = firebaseStorage.getReference().child("User Avatars/" + viewProblem.GetSetterId());

            //download the the avatar from the firebase storage
            final long ONE_MEGABYTE = 1024 * 1024;
            storageReference.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    //set the image view to show the downloaded image and set the viewAvatar property to the downloaded image
                    viewAvatar = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    avatarIV.setImageBitmap(viewAvatar);
                    /*temp code to upload user photo uri to database*/
                }}).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                }
            });
        }
    }

    //On click method to return to the home screen. clears all previous activities as home screen on back pressed method returns to sign in page
    public void HomeOnClick(View view)
    {
        Intent intent = new Intent(VirtualWallActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    //On click method to open the profile screen
    public void WallProfileOnClick(View view)
    {
        Intent myIntent = new Intent(VirtualWallActivity.this, ProfileActivity.class);
        //if the user is viewing a boulder problem not created by them, then pass extras to the profile screen indicating it should the setter's profile and not the current user's profile
        if(viewProblem != null && !viewProblem.GetSetterId().equals(currentUser.getUid()))
        {
            myIntent.putExtra("View User", viewProblem.GetSetterId());
            myIntent.putExtra("Previous Activity", "Virtual Wall Activity");
            //myIntent.putExtra("View Avatar", viewAvatar);
        }
        startActivity(myIntent);
    }

    //on click method for the reset button to reset when creating boulder problems
    public void ResetOnClick(View view)
    {
        //set route complete boolean to false and clear the route array list
        routeComplete = false;
        route.clear();
        //set all hold image view backgrounds to null to clear a selection circles
        for(ImageView holdIV : allHoldIVs)
        {
            holdIV.setBackground(null);
        }
        //hide the add button in case it was showing
        RelativeLayout addBtn = findViewById(R.id.VirtualWallAddBtnRL);
        addBtn.setVisibility(View.INVISIBLE);
    }

    //Method to display the help dialog
    private void DisplayHelpDialog()
    {
        //create a help dialog with a right popup dialog style
        Dialog helpDialog = new Dialog(this, R.style.MaterialRightDialogSheet);
        helpDialog.setCancelable(true);
        helpDialog.setContentView(R.layout.boulder_problem_help_dialog);
        helpDialog.getWindow().setLayout(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
        helpDialog.getWindow().setGravity(Gravity.RIGHT);
        //get the help dialog constrain layout
        ConstraintLayout helpDialogCL = helpDialog.findViewById(R.id.ProblemHelpDialogCL);
        //add on touch listener to the help dialog constraint layout to allow user to swipe right to dismiss the dialog
        helpDialogCL.setOnTouchListener(new OnSwipeTouchListener(helpDialog.getContext())
        {
            @Override
            public void onSwipeRight() {
                super.onSwipeRight();
                helpDialog.dismiss();
            }
        });

        //get the dialog close button
        Button closeBtn = helpDialog.findViewById(R.id.ProblemHelpCloseBtn);
        //set close button on click listener to close the dialog
        closeBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                helpDialog.dismiss();
            }
        });
        //show the help dialog
        helpDialog.show();
    }

    //method to display the Delete Dialog
    private void DisplayDeleteDialog()
    {
        Dialog delDialog = new Dialog(this, R.style.MaterialBottomDialogSheet);
        delDialog.setCancelable(true);
        delDialog.setContentView(R.layout.delete_problem_dialog);
        delDialog.getWindow().setLayout(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
        delDialog.getWindow().setGravity(Gravity.BOTTOM);
        Button delBtn = delDialog.findViewById(R.id.DeleteDialogProblemBtn);
        delBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatabaseReference bpRef = FirebaseDatabase.getInstance().getReference().child("BoulderProblems").child("UserCreated").child(viewProblem.GetName());
                bpRef.setValue(null).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("ascentusers");
                        userRef.addChildEventListener(new ChildEventListener() {
                            @Override
                            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                                if(snapshot.exists())
                                {
                                    userRef.child(snapshot.getKey()).child("Favorites").child(viewProblem.GetName()).setValue(null);
                                    userRef.child(snapshot.getKey()).child("record").child(viewProblem.GetName()).setValue(null);
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
                        Toast.makeText(VirtualWallActivity.this, "Problem Deleted!", Toast.LENGTH_SHORT).show();
                        delDialog.dismiss();
                        onBackPressed();
                    }
                });
            }
        });

        Button cancelBtn = delDialog.findViewById(R.id.DeleteDialogCancelBtn);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                delDialog.dismiss();
            }
        });
        delDialog.show();
    }

    //method for adding benchmark boulder problems. Only for admin use
    public void AddBenchmarkProblemOnClick(View view)
    {
        String recGrade = BenchmarkGradingAlgorithm(route);
        DisplayAddDialog(recGrade);
    }

    /*some private properties used for algorithm testing purposes */
    private long startTime;                                                                         //to hold the time the algorithm was started
    private int bpsCompared = 0;                                                                    //to hold the number of boulder problems the algorithm compared
    private boolean added = false;                                                                  //a boolean to indicate whether the boulder problem route has already been created and added to the database

    //On click method to compare the created boulder problem with existing ones, determine a recommended grade and display add boulder problem dialog
    //method will call upon the levenshtein automaton to determine recommended grades
    //also contains code to run distance metric algorithm instead which is currently commented out as levenshtein method was preferred
    public void AddProblemOnClick(View view)
    {
        //set number of compared boulder problems to 0
        bpsCompared = 0;
        //initially set the number of boulder problems for the current setup to the number of all boulder problems
        bpForSetupCount = bpCount;
        //get the current system time as start time
        startTime = System.nanoTime();

        //ArrayList<BoulderProblem> compareBPs = new ArrayList<>();                                 //commented out code used for distance metric algorithm

        //create hashmap to hold all the boulder problem accepted by the levenshtein automaton
        HashMap<BoulderProblem, Integer> acceptedBps = new HashMap<BoulderProblem, Integer>();
        //create reference for location of boulder problems in the realtime database
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("BoulderProblems");
        //use child event listener to get each boulder problem from the database
        ChildEventListener allCreatedProblems = new ChildEventListener()
        {
            //create a boolean to indicate whether the route was already in the database (i.e. a duplicate route)
            boolean routeMatch = false;
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName)
            {
                //check if route is a duplicate
                if(!routeMatch)
                {
                    //if not the get the snapshot value as boulder problem object
                    BoulderProblem bp = snapshot.getValue(BoulderProblem.class);
                    //compare the size of the boulder problem with the created route
                    if(bp.GetRoute().size() == route.size())
                    {
                        //if they are same size then compare the route holds individually
                        boolean match = true;
                        for(int i = 0; i < bp.GetRoute().size(); i++)
                        {
                            //if a pair of holds do not match then set match boolean to false and break
                            if(!bp.GetRoute().get(i).GetHold().equals(route.get(i).GetHold()))
                            {
                                match = false;
                                break;
                            }
                        }
                        //if the routes match then set the boolean to true
                        if(match)
                        {
                            routeMatch = true;
                        }

                    }
                    //again check if the routes match
                    if(!routeMatch)
                    {
                        //if not the check the boulder problem setup matches the current setup
                        if(bp.GetSetup().equals(setup))
                        {

                            //create a levenshtein automaton object for comparing the boulder problems
                            LevenshteinAutomata lA = new LevenshteinAutomata(route, bp.GetRoute(), allHolds);
                            //increase the number of boulder problems compared
                            bpsCompared++;
                            //call method to output the state graph of the automaton to the console (only shows in the console, not on screen)
                            lA.OutputStateGraph();
                            //check if the automaton has accepted the boulder problem compared
                            if(lA.RouteAccepted())
                            {
                                //if accepted get the accepted state and output it to the console
                                State accepted = lA.GetAcceptedState();
                                //add the accepted boulder problem to list of accepted boulder problems
                                acceptedBps.put(bp, accepted.GetCurrentDistance());
                                //outputting accepted state to console for testing purposes
                                System.out.println("Accepted State: " + accepted.GetPosition() + " : " + accepted.GetCurrentDistance());
                            }

                            //compareBPs.add(bp);                                                   //code for distance metric algorithm method
                        }
                        else
                        {
                            //otherwise decrease the number of boulder problems for that setup
                            bpForSetupCount -= 1;
                        }

                        /* code below for use in distance metric algorithm method */
                        /*
                        if(compareBPs.size() == bpForSetupCount)
                        {
                            String recGrade = GetRecommendedGrade(compareBPs);
                            long timeElapsed = System.nanoTime() - startTime;
                            double elapsedSeconds = timeElapsed * Math.pow(10, -9);
                            System.out.println("Compared: " + compareBPs.size());
                            System.out.println("Time taken: " + elapsedSeconds + " seconds");
                            DisplayAddDialog(recGrade);
                        }*/

                        //check the number of boulder problems compared equals the number of boulder problems for the current setup

                        if(bpsCompared == bpForSetupCount)
                        {
                            //check if any boulder problems were accepted
                            if(acceptedBps.size()>0)
                            {
                                //find the first boulder problem with the minimum distance from the created route
                                BoulderProblem minBp = null;
                                for(BoulderProblem aBp : acceptedBps.keySet())
                                {
                                    if(minBp == null)
                                    {
                                        minBp = aBp;
                                    }
                                    if(acceptedBps.get(aBp) < acceptedBps.get(minBp))
                                    {
                                        minBp = aBp;
                                    }
                                }

                                //set the recommended grade using the miminum distance boulder problem
                                String recGrade = minBp.GetGrade();

                                //get the current system time as the finish time
                                long timeElapsed = System.nanoTime() - startTime;
                                //determine the time passed in seconds from start to finish of the levenshtein automaton method
                                double elapsedSeconds = timeElapsed * Math.pow(10, -9);
                                //output to the console the number of boulder problems compared (for testing purposes and length of time taken)
                                System.out.println("Compared: " + bpsCompared);
                                System.out.println("Time taken: " + elapsedSeconds + " seconds");
                                System.out.println("Distance of closest BP: " + acceptedBps.get(minBp));

                                //call method to display the add boulder problem dialog passing the recommended grade
                                DisplayAddDialog(recGrade);
                            }
                            else
                            {
                                //otherwise no boulder problems were accepted, therefore use the benchmark grading algorithm to determine a recommended grade instead
                                String recGrade = BenchmarkGradingAlgorithm(route);
                                //get the time taken to determine a recommended grade
                                long timeElapsed = System.nanoTime() - startTime;
                                double elapsedSeconds = timeElapsed * Math.pow(10, -9);
                                //output number of problems compared and the time taken
                                System.out.println("Compared: " + bpsCompared);
                                System.out.println("Time taken: " + elapsedSeconds + " seconds");
                                //call method to display the add boulder problem dialog passing the recommended grade
                                DisplayAddDialog(recGrade);
                            }
                        }
                    }
                    else
                    {
                        //check if the route has just been created by the user now
                        if(!added)
                        {
                            //if not then the route created matches a route created already, therefore notify the user
                            //get the display name of the setter whos set the route that matches the created route
                            ValueEventListener dNListener = new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    //notify the user the route has already been created previously
                                    bp.SetSetter(snapshot.getValue().toString());
                                    Toast.makeText(VirtualWallActivity.this, "Route Already Created As: " + bp.GetName() + " by setter: " + bp.GetSetter(), Toast.LENGTH_LONG).show();
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            };
                            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("ascentusers").child(bp.GetSetterId());
                            userRef.child("DisplayName").addListenerForSingleValueEvent(dNListener);
                        }
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName)
            {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot)
            {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error)
            {

            }
        };
        // below is reference location for the test boulder problems.
        // Note will not work just by removing comments!
        //dbRef.child("TestProblems").orderByChild("name").addChildEventListener(allCreatedProblems);

        //reference location for user created boulder problems
        dbRef.child("UserCreated").orderByChild("name").addChildEventListener(allCreatedProblems);
    }

    //method to display the add boulder problem dialog
    /* Parameters
    * - recGrade : the recommended grade for the created boulder problem
     */
    public void DisplayAddDialog(String recGrade)
    {
        //create a new add boulder problem dialog with bottom popup dialog style
        Dialog addProblemDialog = new Dialog(this, R.style.MaterialBottomDialogSheet);
        addProblemDialog.setCancelable(true);
        addProblemDialog.setContentView(R.layout.add_problem_dialog);
        addProblemDialog.getWindow().setLayout(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
        addProblemDialog.getWindow().setGravity(Gravity.BOTTOM);

        //create an array list with all possible grades
        List<String> grades = Arrays.asList("1", "2", "3", "4", "4+", "5", "5+", "6A", "6A+", "6B", "6B+", "6C", "6C+", "7A", "7A+", "7B", "7B+", "7C", "7C+", "8A", "8A+", "8B", "8B+", "8C", "8C+", "9A");
        //create an array adapter for the list of grades and attach to the dropdown/spinner from the dialog
        ArrayAdapter<String> gradesAdapter = new ArrayAdapter<String>(addProblemDialog.getContext(), android.R.layout.simple_spinner_item, grades);
        gradesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner gradeSpinner = (Spinner) addProblemDialog.findViewById(R.id.AddProblemGradeSpinner);
        gradeSpinner.setAdapter(gradesAdapter);
        //set the dropdown to initially show the recommended grade
        gradeSpinner.setSelection(grades.indexOf(recGrade));
        //add an on item selected listener to the dropdown
        gradeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
            {
                //set the selected grade for the boulder problem
                selectedGrade = adapterView.getItemAtPosition(i).toString();
                //check if the selected grade is the index of the recommended grade
                if(i == grades.indexOf(recGrade))
                {
                    //if so then the boulder problem was graded by algorithm.
                    //attached to boulder problems as may be useful to know in long term how many boulder problems created using the algorithm grading
                    isAlgorithm = "Yes";
                }
                else
                {
                    //otherwise the boulder problem was not graded by algorithm
                    isAlgorithm = "No";
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView)
            {

            }
        });
        //create an array list with possible options for the attempts dropdown
        List<String> attempts = Arrays.asList("-", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "Over 10");
        //create an array adapter for the attempts list and attach it to the attempts dropdown/spinner view on the dialog
        ArrayAdapter<String> attemptsAdapter = new ArrayAdapter<String>(addProblemDialog.getContext(), android.R.layout.simple_spinner_item, attempts);
        attemptsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //get the complete checkbox from the dialog
        CheckBox completeCB = addProblemDialog.findViewById(R.id.AddProblemCompleteCB);
        Spinner attemptSpinner = addProblemDialog.findViewById(R.id.AddProblemAttemptSpinner);
        attemptSpinner.setAdapter(attemptsAdapter);
        //set the initial selection for the spinner to 0
        attemptSpinner.setSelection(0);
        //add on item select listener to the attempts spinner
        attemptSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
            {
                //if the user selects to add attempts (more than 0 attempts)
                if(i > 0)
                {
                    //set the complete checkbox option to visible
                    completeCB.setVisibility(View.VISIBLE);
                }
                else
                {
                    //otherwise keep the complete checkbox option invisible
                    completeCB.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        //get the add problem button
        Button addProblemBtn = (Button) addProblemDialog.findViewById(R.id.AddProblemBtn);
        //set a new on click for the add problem button
        addProblemBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                //get the user boulder problem name input
                EditText problemNameET = (EditText) addProblemDialog.findViewById(R.id.AddProblemNameTIET);
                String problemName = problemNameET.getText().toString();
                //using input validation object to check if it is a valid boulder problem name (i.e. between 3 and 20 character long
                InputValidation validator = new InputValidation();
                if(validator.ValidateProblemNameInput(problemName))
                {
                    //set the added boolean to true
                    added = true;
                    //create a new boulder problem object using the input name, the current user id, the selected grade, the created route, the algorithm indicator and the current setup name
                    BoulderProblem newProblem = new BoulderProblem(problemName, currentUser.getUid(), selectedGrade, route, isAlgorithm, setup);
                    //create a database reference
                    DatabaseReference dbref = FirebaseDatabase.getInstance().getReference();
                    //create a new map to pass the boulder problem into. the string will be the location to save boulder problem in the database
                    Map<String, Object> newBp = new HashMap<>();
                    newBp.put("BoulderProblems/UserCreated/" + newProblem.GetName(), newProblem);
                    //upload the boulder problem to the database
                    dbref.updateChildren(newBp).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            //set the view problem property to the newly created boulder problem
                            viewProblem = newProblem;
                            //check if the user added attempts for the problem
                            if(attemptSpinner.getSelectedItemPosition() > 0)
                            {
                                //check if the user set the boulder problem to be completed by them
                                if(completeCB.isChecked())
                                {
                                    //Create bprecord object with the number of attempts and complete indicator set to yes and upload to user's record under the bp name
                                    BPRecord bpRecord = new BPRecord(attemptSpinner.getSelectedItem().toString(), "Yes");
                                    dbref.child("ascentusers/" + currentUser.getUid() + "/record/" + newProblem.GetName()).setValue(bpRecord).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            DisplaySelectedProblem();
                                        }
                                    });
                                }
                                else
                                {
                                    //Create bprecord object with the number of attempts and complete indicator set to yes and upload to user's record under the bp name
                                    BPRecord bpRecord = new BPRecord(attemptSpinner.getSelectedItem().toString(), "No");
                                    dbref.child("ascentusers/" + currentUser.getUid() + "/record/" + newProblem.GetName()).setValue(bpRecord).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            DisplaySelectedProblem();
                                        }
                                    });
                                }
                            }
                            else
                            {
                                //call method to display the new boulder problem passing argument false to indicate the user can add more attempts
                                DisplaySelectedProblem();
                            }
                            //notify the user that the boulder problem has been created
                            Toast.makeText(VirtualWallActivity.this, "Boulder Problem created successfully", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //if failed to upload the boulder problem print the error message
                            Toast.makeText(VirtualWallActivity.this, "Creation Unsuccessful. Error Message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    //dismiss the add problem dialog
                    addProblemDialog.dismiss();
                }
                else
                {
                    //notify the user that they must have a valid boulder problem name
                    Toast.makeText(VirtualWallActivity.this,"Problem name must be between 3 and 20 characters!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        //show the add problem dialog
        addProblemDialog.show();
    }

    //method for getting the recommended grade of a created boulder problem by using the distance metric algorithm.
    // Currently not in use as levenshtein automaton method is preferred
    //also contains functionality to get recommended grade by comparing average hold distance and hold difficulty of boulder problems and the created route.
    // this is commented out for now
    public String GetRecommendedGrade(ArrayList<BoulderProblem> compareBPs)
    {
        //create a list to hold the distance for each boulder problem that when they have been compared
        ArrayList<Integer> comparisons = new ArrayList<>();
        for(BoulderProblem bp : compareBPs)
        {
            //call get the distance between the boulder problem and the created route and add to list of distances
            int distance = DistanceMetricAlgorithm(route, bp.GetRoute()).intValue();
            comparisons.add(distance);
        }

        //get the index of the boulder problem that has the minimum distance from the created route
        int min = comparisons.get(0);
        int minIndex = 0;
        for(int i = 1; i < comparisons.size(); i++)
        {
            if(comparisons.get(i) < min)
            {
                min = comparisons.get(i);
                minIndex = i;
            }
        }

        /*
        ArrayList<Double> comparisons = new ArrayList<>();
        for(BoulderProblem bp: compareBPs)
        {
            double difference = DifficultyGradingAlgorthim(route, bp.GetRoute());
            comparisons.add(difference);
        }

        double min = comparisons.get(0);
        int minIndex = 0;
        for(int i = 0; i < comparisons.size(); i++)
        {
            if(comparisons.get(i) < min)
            {
                min = comparisons.get(i);
                minIndex = i;
            }
        }*/

        System.out.println("Distance of closest BP: " + min);
        //get grade of the minimum distance boulder problem for the recommended grade
        String recGrade = compareBPs.get(minIndex).GetGrade();
        return recGrade;
    }

    //method to compare the difference between two boulder problem routes by using a modified edit distance algorithm
    public Double DistanceMetricAlgorithm(ArrayList<Hold> routeA, ArrayList<Hold> routeB)
    {
        /* initialising the empty distance matrix. we add 1 to row and column to matrix to hold the position inidicators for each route element */
        double[][] d = new double[routeA.size()+1][routeB.size()+1];
        /* set all values of first column to values between 1 and size of routeA + 1 */
        for(int i = 1; i < routeA.size()+1; i++)
        {
            d[i][0] = i;
        }
        /* set all values of first row to values between 1 and size of routeB + 1 */
        for(int j = 1; j < routeB.size()+1; j++)
        {
            d[0][j] = j;
        }

        /* loop through the length of route b*/
        for(int j = 1; j < routeB.size()+1; j++)
        {
            /* loop through the length of route a */
            for(int i = 1; i < routeA.size()+1; i++)
            {
                //create a double variable to hold value of the difference between the 2 holds in terms of their actual distance from each other on the virtual wall
                double subCost = 0;

                //get the holds to compare from each route
                Hold aHold = routeA.get(i-1);
                Hold bHold = routeB.get(j-1);

                //check if the hold from route a matches the hold from route b
                if(aHold.equals(bHold))
                {
                    subCost = 0;
                }
                else
                {
                    //otherwise get the rows of both holds
                    int aHoldRow = Integer.parseInt(aHold.GetRow());
                    int bHoldRow = Integer.parseInt(bHold.GetRow());
                    //check if the columns match
                    if(aHold.GetColumn().equals(bHold.GetColumn()))
                    {
                        //if so the actual distance between the two holds is the difference in the rows
                        subCost = Math.abs(aHoldRow-bHoldRow);
                    }
                    //check if the rows match
                    else if(aHoldRow == bHoldRow)
                    {
                        //if so the actual distance is the difference in the columns
                        int a = lettertoValue.get(aHold.GetColumn());
                        int b = lettertoValue.get(bHold.GetColumn());
                        subCost = Math.abs(a-b);
                    }
                    else
                    {
                        //otherwise the actual distance is calculated using pythagoras theorem
                        double dif1 = Math.abs(aHoldRow-bHoldRow);
                        int a = lettertoValue.get(aHold.GetColumn());
                        int b = lettertoValue.get(aHold.GetColumn());
                        double dif2 = Math.abs(a-b);
                        subCost = Math.sqrt((Math.pow(dif1, 2.00)+ Math.pow(dif2, 2.00)));
                    }
                }
                //set the deletion cost for this comparison
                double del = d[i-1][j]+1;
                //set the insertion cost for this comparison
                double ins = d[i][j-1]+1;
                //set the substitution cost for this comparison. add the actual distance between the 2 holds as a modifier for the substitution cost
                double sub = d[i-1][j-1]+subCost;
                //set the edit distance between the holds in the matrix using the min of the deletion, insertion and substitution costs
                double val = Math.min(Math.min(del, ins), sub);
                //add the edit distance to the matrix
                d[i][j]= val;
            }
        }

        //loop through print the edit distance matrix to the console
        for(int r = 0; r < d.length; r++)
        {
            for(int c = 0; c < d[r].length; c++)
            {
                System.out.print(d[r][c] + " ");
            }
            System.out.println();
        }

        //the edit distance between the two routes will be the bottom right element in matrix
        double distance = d[routeA.size()][routeB.size()];
        return distance;
    }

    //method to get the difficulty value of a route based on hold distances and hold type difficulties
    //used for checking the difficulty value of boulder problem being viewed. mainly for testing purposes
    /* Parameters
    * - viewRoute : the arraylist holds that represents the route
     */
    private void GetProblemDifficultyVal(ArrayList<Hold> viewRoute)
    {
        //get the average hold distance and average hold difficulty of the route
        double avgHoldDistance = GetAverageHoldDistance(viewRoute);
        double avgHoldDifficulty = GetAverageHoldDifficulty(viewRoute);
        //difficulty value is the both these values added
        double difficultyValue = avgHoldDistance + avgHoldDifficulty;
        //print out the difficulty value
        System.out.println("Route difficulty: " + difficultyValue);
    }

    //a custom method for grading benchmark boulder problems
    /* Parameters
    * - routeA : the route to be graded
     */
    public String BenchmarkGradingAlgorithm(ArrayList<Hold> routeA)
    {
        //set initial grade to 0
        String grade = "0";
        //get the average hold distance and average hold difficulty of the route
        double avgHoldDistance = GetAverageHoldDistance(routeA);
        double avgHoldDifficulty = GetAverageHoldDifficulty(routeA);
        //get the difficulty value by adding the 2 previous values
        double difficultyValue = avgHoldDistance + avgHoldDifficulty;
        System.out.println("Difficulty Value:" + difficultyValue);
        //using pre-defined grade boundaries determine the grade for the route
        if(difficultyValue < 3.8)
        {
            grade = "1";
        }
        else if(difficultyValue <4.2)
        {
            grade = "2";
        }
        else if(difficultyValue < 4.5)
        {
            grade = "3";
        }
        else if(difficultyValue < 4.7)
        {
            grade = "4";
        }
        else if(difficultyValue < 5.0)
        {
            grade = "4+";
        }
        else if(difficultyValue < 5.3)
        {
            grade = "5";
        }
        else if(difficultyValue < 5.6)
        {
            grade ="5+";
        }
        else if(difficultyValue < 6.0)
        {
            grade = "6A";
        }
        else if(difficultyValue < 6.3)
        {
            grade = "6A+";
        }
        else if(difficultyValue < 6.6)
        {
            grade = "6B";
        }
        else if(difficultyValue < 6.9)
        {
            grade = "6B+";
        }
        else if(difficultyValue < 7.2)
        {
            grade = "6C";
        }
        else if(difficultyValue < 7.5)
        {
            grade = "6C+";
        }
        else if(difficultyValue < 7.9)
        {
            grade = "7A";
        }
        else if(difficultyValue < 8.2)
        {
            grade = "7A+";
        }
        else if(difficultyValue < 8.5)
        {
            grade = "7B";
        }
        else if(difficultyValue < 8.8)
        {
            grade = "7B+";
        }
        else if(difficultyValue < 9.1)
        {
            grade = "7C";
        }
        else if(difficultyValue < 9.3)
        {
            grade = "7C+";
        }
        else if(difficultyValue < 9.5)
        {
            grade = "8A";
        }
        else if(difficultyValue < 9.8)
        {
            grade = "8A+";
        }
        else if(difficultyValue < 10.1)
        {
            grade = "8B";
        }
        else if(difficultyValue < 10.4)
        {
            grade = "8B+";
        }
        else if(difficultyValue < 10.7)
        {
            grade = "8C";
        }
        else if(difficultyValue < 11)
        {
            grade = "8C+";
        }
        else if(difficultyValue >= 11)
        {
            grade = "9A";
        }
        return grade;
    }

    //method to get the average hold difficulty of a route
    /* Parameters:
    * - routeA : the route to get average hold difficulty of
     */
    public double GetAverageHoldDifficulty(ArrayList<Hold> routeA)
    {
        //create initial total hold difficulty value
        double totalHoldValue = 0;
        //loop through each hold in the route
        for(Hold hold : routeA)
        {
            //for each hold are in the first 3 rows then the hold difficulty value should always be 1 as they are likely to be a starting foothold
            if(Integer.parseInt(hold.GetRow()) <= 3)
            {
                totalHoldValue += 1;
            }
            //if a hold is of type then it is a large jug, increase difficulty value by 1
            else if(hold.GetType().equals("1"))
            {
                totalHoldValue += 1;
            }
            //if the hold is of type 2 (mini-jug) or 3 (edge) then increase difficulty value by 2
            else if(hold.GetType().equals("2") || hold.GetType().equals("3"))
            {
                totalHoldValue += 2;
            }
            //if the hold is of type 4 (sloper) then increase total value by 3
            else if(hold.GetType().equals("4"))
            {
                totalHoldValue += 3;
            }
            // if the hold is of type 5 (pocket) then increase total value by 4
            else if(hold.GetType().equals("5"))
            {
                totalHoldValue += 4;
            }
            //if the hold is of type 6 (pinch) then increase total value by 5
            else if(hold.GetType().equals("6"))
            {
                totalHoldValue += 5;
            }
            //if the hold is of type 7 (crimp) then increase total value by 6
            else if(hold.GetType().equals("7"))
            {
                totalHoldValue += 6;
            }
        }
        //get the average hold difficulty value
        double avgHoldDifficulty = (double) totalHoldValue/routeA.size();
        return avgHoldDifficulty;
    }

    //method to calculate the difference in average hold distance and average hold difficulty between to routes
    //this method is an alternative grading option but not currently used
    public double DifficultyGradingAlgorthim(ArrayList<Hold> routeA, ArrayList<Hold> routeB)
    {
        double avgHoldDistA = GetAverageHoldDistance(routeA);
        double avgHoldDistB = GetAverageHoldDistance(routeB);
        double avgHoldDifA = GetAverageHoldDifficulty(routeA);
        double avgHoldDifB = GetAverageHoldDifficulty(routeB);
        double difficultyValA = avgHoldDistA + avgHoldDifA;
        double difficultyValB = avgHoldDistB + avgHoldDifB;
        double difference = Math.abs(difficultyValA - difficultyValB);
        return difference;
    }

    //Method to find the average distance between holds (distance between holds: minimum of the distances between the next hold and each of the 3 holds before) in a route and return as difficulty factor value
    public Double GetAverageHoldDistance(ArrayList<Hold> route)
    {
        ArrayList<Double> distances = new ArrayList<>();                                            //arraylist to hold all the distances between holds
        double totalDistance = 0;                                                                   //holds the total distance value
        //loop through holds in the route
        for(int i = 0; i < route.size()-1; i++)
        {
            Hold nextHold = route.get(i+1);
            Hold currentHold = route.get(i);

            //getting the column value of the current hold and the next hold, then finding the difference between the 2
            int nextCol = lettertoValue.get(nextHold.GetColumn());
            int currentCol = lettertoValue.get(currentHold.GetColumn());
            int nextCurrColDist = nextCol-currentCol;
            //getting the row value of the current hold and the next hold, then finding the difference between the 2
            int nextRow = Integer.parseInt(nextHold.GetRow());
            int currentRow = Integer.parseInt(currentHold.GetRow());
            int nextCurrRowDist = nextRow-currentRow;
            //using pythagoras theorem to calculate the actual distance between the next hold and current hold
            double nextCurrHoldDistance = Math.sqrt(Math.pow(nextCurrRowDist, 2) + Math.pow(nextCurrColDist, 2));

            //if holdDistance is less than 2, then the difficulty of using them should be harder as they are very close together.
            //Therefore check for this instance and if found set the distance to 4 instead so that hold distance will show are harder difficulty
            if(nextCurrHoldDistance < 2)
            {
                nextCurrHoldDistance = 4;
            }
            //checking if the holds are on the same column. If not then this would be a sideways/diagonal movement
            //show this is harder by increasing the distance value by 1
            if(currentCol != nextCol)
            {
                nextCurrHoldDistance +=1;
            }

            double holdDistance = nextCurrHoldDistance;
            //checking that there is a previous hold (i.e. current hold is not the first hold)
            if(i > 0)
            {
                //get the previous hold and find the distance between it and the next hold
                Hold prevHold = route.get(i-1);
                int prevCol = lettertoValue.get(prevHold.GetColumn());
                int prevRow = Integer.parseInt(prevHold.GetRow());
                int nextPrevColDist = nextCol - prevCol;
                int nextPrevRowDist = nextRow - prevRow;
                double nextPrevHoldDistance = Math.sqrt(Math.pow(nextPrevRowDist, 2) + Math.pow(nextPrevColDist, 2));
                //if the distance between the prev and next hold is less than 2 then they are very close together
                //represent this as more difficult by setting the distance value to 4 instead
                if(nextPrevHoldDistance < 2)
                {
                    nextPrevHoldDistance = 4;
                }
                //checking if the holds are on the same column. If not then this would be a sideways/diagonal movement
                //show this is harder by increasing the distance value by 1
                if(prevCol != nextCol)
                {
                    nextPrevHoldDistance +=1;
                }

                //checking if the distance between prev hold and next hold is smaller than current hold and next hold
                //if so set the hold distance to the distance between prev hold and next hold, to show that is the more likely move to be made
                if(nextPrevHoldDistance < nextCurrHoldDistance)
                {
                    holdDistance = nextPrevHoldDistance;
                }

                //checking if there is a 2nd previous hold to get distance from
                if(i > 1)
                {
                    //if so get the 2nd prev hold and find distance between it and next hold
                    Hold prevHoldTwo = route.get(i-2);
                    int prevTwoCol = lettertoValue.get(prevHoldTwo.GetColumn());
                    int prevTwoRow = Integer.parseInt(prevHoldTwo.GetRow());
                    int nextTwoHoldColDist = nextCol - prevTwoCol;
                    int nextTwoHoldRowDist = nextRow - prevTwoRow;
                    double nextTwoHoldDistance = Math.sqrt(Math.pow(nextTwoHoldRowDist, 2) + Math.pow(nextTwoHoldColDist, 2));
                    //if the distance is less than 2 then they are very close together
                    //therefore indicate this move is harder by setting the distance to 4
                    if(nextTwoHoldDistance < 2)
                    {
                        nextTwoHoldDistance = 4;
                    }
                    //checking if the holds are on the same column. If not then this would be a sideways/diagonal movement
                    //show this is harder by increasing the distance value by 1
                    if(prevTwoCol != nextCol)
                    {
                        nextTwoHoldDistance +=1;
                    }

                    //if the distance is the new min value then set the hold distance to that value
                    if(nextTwoHoldDistance < holdDistance)
                    {
                        holdDistance = nextTwoHoldDistance;
                    }
                }
            }

            //add the distance to the hold list and the total distance
            distances.add(holdDistance);
            totalDistance += holdDistance;
        }
        double avgDistance = (double) totalDistance/distances.size();                               //dividing total distance by number spaces between holds to get the average hold distance
        return avgDistance;
    }

    //On click method for the search button
    public void SearchOnClick(View view)
    {
        //open the search boulder problem screen
        Intent intent = new Intent(VirtualWallActivity.this, ProblemSearchActivity.class);
        startActivity(intent);
    }

    //method to display a boulder problem that the user has selected to view or just created
    public void DisplaySelectedProblem()
    {
        //get the layout that acts as an add button
        RelativeLayout addBtn = (RelativeLayout) findViewById(R.id.VirtualWallAddBtnRL);
        ImageView addBtnIV = findViewById(R.id.VirtualWallAddIV);
        addBtnIV.setBackgroundResource(R.drawable.profile_image_clickable);
        addBtnIV.setImageResource(R.drawable.log_attempts_icon);
        addBtnIV.setScaleType(ImageView.ScaleType.CENTER_CROP);
        //otherwise set the add button to visible and change the on click listener
        addBtn.setVisibility(View.VISIBLE);
        addBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {
                //create a new add result dialog with a bottom popup dialog style
                Dialog addResultDialog = new Dialog(view.getContext(), R.style.MaterialBottomDialogSheet);
                addResultDialog.setContentView(R.layout.add_result_dialog);
                addResultDialog.setCancelable(true);
                addResultDialog.getWindow().setLayout(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
                addResultDialog.getWindow().setGravity(Gravity.BOTTOM);

                //create a list containing the attempts options
                List<String> attempts = Arrays.asList("-", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "Over 10");
                //get the complete check box and the attempt spinner from the add result dialog
                CheckBox completeCB = addResultDialog.findViewById(R.id.AddResultCompleteCB);
                Spinner attemptSpinner = addResultDialog.findViewById(R.id.AddResultAttemptSpinner);
                //create an adapter for the attempts list and attach it to the attemps spinner
                ArrayAdapter<String> attemptsAdapter = new ArrayAdapter<String>(addResultDialog.getContext(), android.R.layout.simple_spinner_item, attempts);
                attemptSpinner.setAdapter(attemptsAdapter);
                //create a database reference
                DatabaseReference dbref = FirebaseDatabase.getInstance().getReference();
                //attach a listener to the location of the user's record for the current boulder problem in the database
                dbref.child("ascentusers/" + currentUser.getUid() +"/record/" + viewProblem.GetName() + "/attempts").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //check if the location exists (i.e. if the user has previously added attempts
                        if(snapshot.exists())
                        {
                            //based on the value in the database set the attempts spinner to match the value in the database
                            for(int i = 0; i < attempts.size(); i++)
                            {
                                if(snapshot.getValue().toString().equals(attempts.get(i)))
                                {
                                    attemptSpinner.setSelection(i);
                                    break;
                                }
                            }
                        }
                        //otherwise set the attempt spinner to none for default
                        else
                        {
                            attemptSpinner.setSelection(0);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
                //add listener for the boudler problem in user's record to determine if it is complete or not
                dbref.child("ascentusers/" + currentUser.getUid() +"/record/" + viewProblem.GetName() + "/complete").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //check if record location exists
                        if(snapshot.exists())
                        {
                            //get the complete string
                            String complete = snapshot.getValue().toString();
                            //if yes then set the checkbox to checked
                            if(complete.equals("Yes"))
                            {
                                completeCB.setChecked(true);
                            }
                            else
                            {
                                //otherwise set to unchecked
                                completeCB.setChecked(false);
                            }
                        }
                        else
                        {
                            //otherwise set to unchecked
                            completeCB.setChecked(false);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
                //add an on item selected listener to the spinner
                attemptSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
                    {
                        //if the user selects to add attempts
                        if(i > 0)
                        {
                            //set the complete check box to visible
                            completeCB.setVisibility(View.VISIBLE);
                        }
                        //otherwise keep the compelte checkbox invisible
                        else
                        {
                            completeCB.setVisibility(View.INVISIBLE);
                        }
                    }
                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });
                //get the add result button from the dialog
                Button addResultBtn = addResultDialog.findViewById(R.id.AddResultBtn);
                addResultBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //if the user has selected more than 0 attempts
                        if(attemptSpinner.getSelectedItemPosition() > 0)
                        {
                            //create a database reference
                            DatabaseReference dbref = FirebaseDatabase.getInstance().getReference();
                            if(completeCB.isChecked())
                            {
                                //create a bp record object to upload to the user's record in the database
                                BPRecord bpRecord = new BPRecord(attemptSpinner.getSelectedItem().toString(), "Yes");
                                dbref.child("ascentusers/" + currentUser.getUid() + "/record/" + viewProblem.GetName()).setValue(bpRecord).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        //notify user a complete result has been added and open the record screen
                                        Toast.makeText(addResultDialog.getContext(), "Completed Result Added", Toast.LENGTH_SHORT).show();
                                        //dismiss this dialog
                                        addResultDialog.dismiss();
                                        //open the record screen
                                        Intent intent =  new Intent(VirtualWallActivity.this, RecordActivity.class);
                                        startActivity(intent);
                                    }
                                });
                            }
                            else
                            {
                                //create a bp record object to upload to the user's record in the database
                                BPRecord bpRecord = new BPRecord(attemptSpinner.getSelectedItem().toString(), "No");
                                dbref.child("ascentusers/" + currentUser.getUid() + "/record/" + viewProblem.GetName()).setValue(bpRecord).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        //notify the user has added some attempts for the boulder problem and open the record screen
                                        Toast.makeText(addResultDialog.getContext(), "Attempts added", Toast.LENGTH_SHORT).show();
                                        //dismiss this dialog
                                        addResultDialog.dismiss();
                                        //open the record screen
                                        Intent intent =  new Intent(VirtualWallActivity.this, RecordActivity.class);
                                        startActivity(intent);
                                    }
                                });
                            }
                        }
                    }
                });
                //show the add result dialog
                addResultDialog.show();
            }
        });
        //get the title text view and set it to show the name of the viewed boulder problem
        TextView titleTv = (TextView) findViewById(R.id.VirtualWallTitleTV);
        titleTv.setText(viewProblem.GetName());

        //get the route of the viewed boulder problem
        ArrayList<Hold> viewRoute = viewProblem.GetRoute();
        //set all the on click listeners of all holds on the virtual wall to null so the user cannot select holds when viewing a boulder problem
        for(ImageView aHoldIV : allHoldIVs)
        {
            aHoldIV.setOnClickListener(null);
        }
        //loop through each hold in the route
        for(int i = 0; i< viewRoute.size(); i++)
        {
            Hold vHold = viewRoute.get(i);
            //get the layout containing the virtual wall
            ConstraintLayout virtualWallCL = findViewById(R.id.VirtualWallCL);
            //create the hold tag that would correspond to the current hold
            String holdTag = vHold.GetColumn() + vHold.GetRow() + vHold.GetType();
            //get the corresponding hold image view
            ImageView vHoldIV = (ImageView) virtualWallCL.findViewWithTag(holdTag);
            //get the row of the hold
            String viewRow = vHold.GetRow();
            //if the hold is one of the first 2 holds it is a starting hold
            if(i < 2 && (viewRow.equals("1") || viewRow.equals("2") || viewRow.equals("3") || viewRow.equals("4") || viewRow.equals("5")))
            {
                //set the background for the hold to a start hold
                vHoldIV.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.start_hold_clicked));
            }
            //if the hold is on row 18 then it is finish hold
            else if(viewRow.equals("18"))
            {
                //set the background to a red circle
                vHoldIV.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.finish_hold_clicked));
            }
            //otherwise the hold is a middle hold
            else
            {
                //set the background to a blue circle
                vHoldIV.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.middle_hold_clicked));
            }
        }
        //get the reset button imageview to change it into a favorite button
        ImageView favIV = findViewById(R.id.VirtualWallResetIV);
        //loop through the user's favorites and check if the currently viewed boulder problem is a favorite
        for(String s : userFavorites)
        {
            if(s.equals(viewProblem.GetName()))
            {
                //if so then set the favorite button to the full icon
                favIV.setImageResource(R.drawable.fav_icon_full);
                //set the favorited boolean to true to indicate this boulder problem is a favorite
                favorited = true;
                break;
            }
        }
        if(!favorited)
        {
            //otherwise set it to the unclicked favorite icon
            favIV.setImageResource(R.drawable.fav_icon);
        }
        //set the color filter to white to show the favorite images as white
        favIV.setColorFilter(Color.WHITE);
        //get the layout that acts as the reset button
        RelativeLayout favBtn = findViewById(R.id.VirtualWallResetBtnRL);
        //change it's on click to act as a favorite button
        favBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                //if the boulder problem is already a favorite
                if(favorited)
                {
                    for(int i  = 0; i < userFavorites.size(); i++)
                    {
                        if(userFavorites.get(i).equals(viewProblem.GetName()))
                        {
                            //remove the boulder problem from the user's favorites in the database
                            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
                            dbRef.child("ascentusers/" + currentUser.getUid() + "/Favorites/" + viewProblem.GetName()).setValue(null);
                            break;
                        }
                    }
                    //set the favorited boolean to false and set the favorite image view to the unclicked version
                    favorited = false;
                    favIV.setImageResource(R.drawable.fav_icon);
                    //notify user boulder problem removed from favorites
                    Toast.makeText(VirtualWallActivity.this, "Boulder Problem removed from Favorites", Toast.LENGTH_SHORT).show();
                }
                //otherwise not already a favorite
                else
                {
                    //add the boulder problem to the user's favorites in the database
                    DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
                    dbRef.child("ascentusers/" + currentUser.getUid() + "/Favorites/" + viewProblem.GetName()).setValue("Fav");
                    //set the favorited boolean to true and set the favorite image view to the clicked version
                    favorited = true;
                    favIV.setImageResource(R.drawable.fav_icon_full);
                    //notify user boulder problem is added to favorites
                    Toast.makeText(VirtualWallActivity.this, "Boulder Problem added to Favorites", Toast.LENGTH_SHORT).show();
                }
                //set the color filter to white to make sure image appears white color
                favIV.setColorFilter(Color.WHITE);
            }
        });

        //call method to get the difficulty value of the viewed boulder problem
        //just for testing purposes, only outputs to the console
        GetProblemDifficultyVal(viewProblem.GetRoute());
    }

    //method to get the user's list of favorite boulder problems
    public void GetUserFavs()
    {
        //create a database reference
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        //attach a child event listener to the location of the user's favorites list in the realtime database
        dbRef.child("ascentusers/" + currentUser.getUid() + "/Favorites").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                //listens for existing or newly added favorites

                //if exists
                if(snapshot.exists())
                {
                    //add the snapshot key (boulder problem name) to the list of user's favorites
                    userFavorites.add(snapshot.getKey());
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot)
            {
                //listens to favorites being removed
                if(snapshot.exists())
                {
                    //find and remove the the favorite the user's favorites list
                    for(int i = 0; i < userFavorites.size(); i++)
                    {
                        if(userFavorites.get(i).equals(snapshot.getKey()))
                        {
                            userFavorites.remove(i);
                            break;
                        }
                    }
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

}