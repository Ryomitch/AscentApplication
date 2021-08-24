package com.example.ascentapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.SearchView;
import android.widget.Switch;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//activity class for boulder problem search screen
public class ProblemSearchActivity extends AppCompatActivity {

    /*Private Properties*/
    private final String[] tabNames = new String[]{"General", "Favorites", "My Routes"};            //contains the array of tab names
    private Boolean searchedByText = false;                                                         //boolean indicating if user has searched by text or not
    private ArrayList<Integer> filters = new ArrayList<>();                                         //arraylist to contain list of selected hold type filters
    private Dialog filterDialog;                                                                    //contains the hold type filter dialog
    private String sortOption = "name";                                                             //holds the user's selected sort option
    private ViewPager2 viewPager;                                                                   //holds the viewpager for the swithcing between tabs
    private boolean ascendingOption = true;                                                         //boolean indicating the order in which item are to be displayed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //inflate the layout
        setContentView(R.layout.activity_problem_search);

        //get the toolbar and set title to null as layout already contains title textview
        Toolbar toolbar = findViewById(R.id.ProblemSearchToolbar);
        toolbar.setTitle("");
        //call this method to enable to options menu on the custom toolbar
        setSupportActionBar(toolbar);

        //call method to populate the current user's avatar image on the toolbar
        PopulateUserDetails();

        //create a custom tab adapater and create each of the tabs for general, favorites and my routes
        ProblemSearchTabsAdapter tabsAdapter = new ProblemSearchTabsAdapter(this);
        tabsAdapter.createFragment(0);
        tabsAdapter.createFragment(1);
        tabsAdapter.createFragment(2);
        //get the viewpager from the layout, attach the tab adapter
        viewPager = findViewById(R.id.ProblemSearchViewPager);
        viewPager.setAdapter(tabsAdapter);
        //attach the viewpager to the tab layout to enable switching between tabs
        TabLayout tabLayout = findViewById(R.id.ProblemSearchTabLayout);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) ->
                tab.setText(tabNames[position])).attach();
        //all method to create listener for user search input
        AddQueryListener();

        //check if this screen is supposed to open a specific tab
        if(getIntent().getExtras() != null)
        {
            //get the specific tab index and set the viewpager to that index
            int selectedTab = getIntent().getIntExtra("Selected Tab", 0);
            viewPager.setCurrentItem(selectedTab);
        }

        //call method to prepare the hold type filter dialog
        SetFilterDialog();
    }

    //method to popualte the user avatar on screen
    public void PopulateUserDetails()
    {
        ImageView avatarIV = findViewById(R.id.ProblemSearchAvatarIV);
        avatarIV.setImageBitmap(HomeActivity.appUser.GetAvatar());
    }

    //on click method to open the profile screen
    public void ProfileOnClick(View view)
    {
        Intent intent = new Intent(ProblemSearchActivity.this, ProfileActivity.class);
        startActivity(intent);
    }

    //method creates the options menu on the toolbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.problem_search_menu, menu);
        //set the view benchmarks option to visible
        menu.findItem(R.id.view_benchmarks_option).setVisible(true);
        return true;
    }

    //method to listen for user selecting an option from the menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.view_benchmarks_option:
                BenchmarksDialogFragment benchmarksDialogFragment = new BenchmarksDialogFragment();
                benchmarksDialogFragment.setCancelable(true);
                benchmarksDialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.MaterialRightDialogSheet);
                benchmarksDialogFragment.show(getSupportFragmentManager(), "Benchmarks Dialog Fragment");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //method to add a search input listener to the search view
    public void AddQueryListener()
    {
        //get the search view from the layout and set a new on query listener
        SearchView searchView = findViewById(R.id.ProblemSearchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s)
            {
                //get the current tab
                GeneralFragment currentTab = (GeneralFragment) getSupportFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
                //if user has searched with no input
                if(s.equals(null) || s.equals("  "))
                {
                    //pass the filters empty search and sort option to the current tab search method
                    currentTab.Search(filters, null, sortOption, ascendingOption);
                }
                else
                {
                    //otherwise get the search input and pass it, along with filters, and sort option to the current tab search method
                    String search = s;
                    currentTab.Search(filters, search.toLowerCase(), sortOption, ascendingOption);
                    //set the searched by text boolean to true
                    searchedByText = true;
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                //detects when user deletes their search input and change displayed out accordingly
                if(s.equals(null) || s.equals(""))
                {
                    onQueryTextSubmit("  ");
                    searchedByText = false;
                }
                return false;
            }
        });
    }

    //method to prepare the hold type filter dialog
    public void SetFilterDialog()
    {
        //create a new filter dialog
        filterDialog = new Dialog(ProblemSearchActivity.this, R.style.MaterialRightDialogSheet);
        filterDialog.setContentView(R.layout.search_filter_dialog);
        //create list of hold types and pass them to a custom Recycler view adapter
        List<String> holdTypes = Arrays.asList(new String[] {"Jug", "Mini-jug", "Edge", "Sloper", "Pocket", "Pinch", "Crimp"});
        SearchFilterRVAdapter filterRVAdapter = new SearchFilterRVAdapter(holdTypes, new SearchFilterRVAdapter.OnItemCheckListener(){
            //add a new on click for selecting an item from the recycler view
            @Override
            public void onItemCheck(String item)
            {
                //get the index of the selected item in the hold types list
                int index = 0;
                for(int i = 0; i < holdTypes.size(); i++)
                {
                    if(holdTypes.get(i).equals(item))
                    {
                        index = i;
                        break;
                    }
                }
                //add one to the index as the number represents the hold type
                index+=1;
                //add the hold type number to list of selected filters
                filters.add(index);
                //get the search view and it's search input
                SearchView searchView = findViewById(R.id.ProblemSearchView);
                String query = searchView.getQuery().toString().toLowerCase();
                //check that the user has entered a search
                if (!searchedByText)
                {
                    query = null;
                }
                //get the currently viewed tab
                GeneralFragment currentTab = (GeneralFragment) getSupportFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
                //call the tab search method, passing in the list of selected filters, the search input, selected sort, and selected ascending or descending option
                currentTab.Search(filters, query, sortOption, ascendingOption);
            }
            //add on click for when user deselects item
            @Override
            public void onItemUncheck(String item)
            {
                //get the index of the item in the hold types list
                int index = 0;
                for(int i = 0; i < holdTypes.size(); i++)
                {
                    if(holdTypes.get(i).equals(item))
                    {
                        index = i;
                        break;
                    }
                }
                //add one to the index so that the numbr represents the hold type value (1 = jug, 2 = mini-jug, etc)
                index +=1;
                //remove the deselected value from selected filters
                //filters.remove(index);
                filters.remove(Integer.valueOf(index));
                //get the search view
                SearchView searchView = findViewById(R.id.ProblemSearchView);
                //get the search input
                String query = searchView.getQuery().toString().toLowerCase();
                //check user has actually entered search input
                if (!searchedByText)
                {
                    query = null;
                }
                //get the currently viewed tab
                GeneralFragment currentTab = (GeneralFragment) getSupportFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
                //pass the selected filters, search input, selected sort and selected ascending or descending option to the current tab search method
                currentTab.Search(filters, query, sortOption, ascendingOption);
            }
        });
        RecyclerView filterRV = filterDialog.findViewById(R.id.SearchFilterRV);
        filterRV.setHasFixedSize(true);
        filterRV.setLayoutManager(new LinearLayoutManager(ProblemSearchActivity.this));
        filterRV.setAdapter(filterRVAdapter);
        Button clearBtn = filterDialog.findViewById(R.id.SearchFilterClearBtn);
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for(SearchFilterRVAdapter.FilterViewHolder filterViewHolder : filterRVAdapter.viewHolders)
                {
                    filterViewHolder.holdTypeCheckBox.setChecked(false);
                }
            }
        });
    }

    //on click method for the sort options
    public void SortOnClick(View view)
    {
        //create a new dialog for the sort options with bottom popup dialog style
        final Dialog sortBottomSheetDialog = new Dialog(ProblemSearchActivity.this, R.style.MaterialBottomDialogSheet);
        sortBottomSheetDialog.setContentView(R.layout.sort_dialog); // your custom view.
        sortBottomSheetDialog.setCancelable(true);
        sortBottomSheetDialog.getWindow().setLayout(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
        sortBottomSheetDialog.getWindow().setGravity(Gravity.BOTTOM);
        //get the radio buttons and ascending/descending switches from the dialog
        RadioButton defaultRB = sortBottomSheetDialog.findViewById(R.id.SortDefaultRB);
        RadioButton gradeRB = sortBottomSheetDialog.findViewById(R.id.SortGradeRB);
        RadioButton setterRB = sortBottomSheetDialog.findViewById(R.id.SortSetterRB);
        Switch defaultSwitch = sortBottomSheetDialog.findViewById(R.id.SortDefaultDirectionSwitch);
        Switch gradeSwitch = sortBottomSheetDialog.findViewById(R.id.SortGradeDirectionSwitch);
        Switch setterSwitch = sortBottomSheetDialog.findViewById(R.id.SortSetterDirectionSwitch);

        //set the on click listener for each of the sort option radio buttons
        defaultRB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                defaultSwitch.setVisibility(View.VISIBLE);
                gradeSwitch.setVisibility(View.INVISIBLE);
                setterSwitch.setVisibility(View.INVISIBLE);
                sortOption = "name";
                SearchView searchView = findViewById(R.id.ProblemSearchView);
                String query = searchView.getQuery().toString().toLowerCase();
                if (!searchedByText)
                {
                    query = null;
                }
                GeneralFragment currentTab = (GeneralFragment) getSupportFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
                currentTab.Search(filters, query, sortOption, ascendingOption);
            }
        });
        gradeRB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                defaultSwitch.setVisibility(View.INVISIBLE);
                gradeSwitch.setVisibility(View.VISIBLE);
                setterSwitch.setVisibility(View.INVISIBLE);
                sortOption = "grade";
                SearchView searchView = findViewById(R.id.ProblemSearchView);
                String query = searchView.getQuery().toString().toLowerCase();
                if (!searchedByText)
                {
                    query = null;
                }
                GeneralFragment currentTab = (GeneralFragment) getSupportFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
                currentTab.Search(filters, query, sortOption, ascendingOption);
            }
        });
        setterRB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                defaultSwitch.setVisibility(View.INVISIBLE);
                gradeSwitch.setVisibility(View.INVISIBLE);
                setterSwitch.setVisibility(View.VISIBLE);
                sortOption = "setter";
                SearchView searchView = findViewById(R.id.ProblemSearchView);
                String query = searchView.getQuery().toString().toLowerCase();
                if (!searchedByText)
                {
                    query = null;
                }
                GeneralFragment currentTab = (GeneralFragment) getSupportFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
                currentTab.Search(filters, query, sortOption, ascendingOption);
            }
        });

        defaultSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(defaultSwitch.isChecked())
                {
                    ascendingOption = true;
                }
                else
                {
                    ascendingOption = false;
                }
                SearchView searchView = findViewById(R.id.ProblemSearchView);
                String query = searchView.getQuery().toString().toLowerCase();
                if (!searchedByText)
                {
                    query = null;
                }
                GeneralFragment currentTab = (GeneralFragment) getSupportFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
                currentTab.Search(filters, query, sortOption, ascendingOption);
            }
        });

        gradeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(gradeSwitch.isChecked())
                {
                    ascendingOption = true;
                }
                else
                {
                    ascendingOption = false;
                }
                SearchView searchView = findViewById(R.id.ProblemSearchView);
                String query = searchView.getQuery().toString().toLowerCase();
                if (!searchedByText)
                {
                    query = null;
                }
                GeneralFragment currentTab = (GeneralFragment) getSupportFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
                currentTab.Search(filters, query, sortOption, ascendingOption);
            }
        });

        setterSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(setterSwitch.isChecked())
                {
                    ascendingOption = true;
                }
                else
                {
                    ascendingOption = false;
                }
                SearchView searchView = findViewById(R.id.ProblemSearchView);
                String query = searchView.getQuery().toString().toLowerCase();
                if (!searchedByText)
                {
                    query = null;
                }
                GeneralFragment currentTab = (GeneralFragment) getSupportFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
                currentTab.Search(filters, query, sortOption, ascendingOption);
            }
        });
        //show the sort option dialog
        sortBottomSheetDialog.show();
    }

    //on click for the filter button
    public void FilterOnClick(View view)
    {
        if(filterDialog.isShowing())
        {
            filterDialog.dismiss();
        }
        else
        {
            filterDialog.show();
            filterDialog.getWindow().setGravity(Gravity.RIGHT);
        }
    }

    //on click to return to the home screen
    public void HomeOnClick(View view)
    {
        Intent intent = new Intent(ProblemSearchActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}