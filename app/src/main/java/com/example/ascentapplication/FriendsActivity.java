package com.example.ascentapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.SearchView;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

//FriendsActivity class for containing fragments that display friends lists, requests sent and requests received and passing searches to them
public class FriendsActivity extends AppCompatActivity {

    /*private properties */
    private ViewPager2 viewPager;                                                                       //the viewpager for switching between tabs
    private final String[] tabNames = new String[]{"Friends", "Requests Sent", "Requests Received"};    //array holding the names of each tab

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        //calling method to display the users avatar on screen
        PopulateUserDetails();

        //creating tab adapter object to create fragment tabs for friends list, requests sent and requests received
        FriendsTabsAdapter friendsTabsAdapter = new FriendsTabsAdapter(this);
        friendsTabsAdapter.createFragment(0);
        friendsTabsAdapter.createFragment(1);
        friendsTabsAdapter.createFragment(2);

        //get the viewpager from the layout and set it's adapter
        viewPager = findViewById(R.id.FriendsViewPager);
        viewPager.setAdapter(friendsTabsAdapter);

        //attach the viewpager to the tabs
        TabLayout tabLayout = findViewById(R.id.FriendsTabLayout);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) ->
                tab.setText(tabNames[position])).attach();

        //call method to attach search listener to the search view
        AddSearchListener();
    }

    //method to populate the user avatar on the screen
    public void PopulateUserDetails()
    {
        //get the imageview from the layout and set it's bitmap using the user object from the Home Activity
        ImageView avatarIV = findViewById(R.id.FriendsAvatarIV);
        avatarIV.setImageBitmap(HomeActivity.appUser.GetAvatar());
    }

    //On click method to open the user's profile
    public void ProfileOnClick(View view)
    {
        //opening the profile screen
        Intent intent = new Intent(FriendsActivity.this, ProfileActivity.class);
        startActivity(intent);
    }

    //method to attach search listener to the search view
    public void AddSearchListener()
    {
        //get the search view from the layout
        SearchView searchView = findViewById(R.id.FriendsSearchView);
        //attach on query text listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                //get the currently viewed tab
                UsersListFragment currentTab = (UsersListFragment) getSupportFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());

                //if the user's has entered no search text
                if(s.equals(null) || s.equals("  "))
                {
                    //then just pass null to the current tab's search method
                    currentTab.Search(null);
                }
                else
                {
                    //otherwise pass the search text to the current tab's search method
                    currentTab.Search(s.toLowerCase());
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                //on text change listener

                //when the user deletes all text entered the pass null to current tab's search method
                if(s.equals(null) || s.equals(""))
                {
                    onQueryTextSubmit("  ");
                }
                return false;
            }
        });
    }

    //On click method to return to the home screen, clears all previous activities as the home screen on back pressed method returns to the sign in screen
    public void HomeOnClick(View view)
    {
        Intent intent = new Intent(FriendsActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    //on click method for the find users button
    public void FindUsersOnClick(View view)
    {
        //create a find users dialog fragment and set it to be closeable
        FindUsersDialogFragment findUsersDialogFragment = new FindUsersDialogFragment();
        findUsersDialogFragment.setCancelable(true);
        //set the style of the dialog fragment so that it appears from the right
        findUsersDialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.MaterialRightDialogSheet);
        //pass tag to indicate that fragment displays list of all other users
        findUsersDialogFragment.show(getSupportFragmentManager(), "findUserDialogFragment");
    }

}