package com.example.ascentapplication;

import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

//FindUsersDialogFragment class for containing the user list fragment which displays all users currently not a friend of current user, or has had requests sent to or received from for the current user
public class FindUsersDialogFragment extends DialogFragment {

    /* Private Properties */
    private UsersListFragment findUserFragment;                         //the contained fragment which displays a list of all users
    private SearchView findUsersSV;                                     //holds the search view for search users in the list

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        //inflate the layout for the dialog fragment
        View view = inflater.inflate(R.layout.find_users_dialog, container);

        //get the searchview from the layout
        findUsersSV = view.findViewById(R.id.FindUsersDialogSearchView);

        //initialise a new user list fragment
        findUserFragment = new UsersListFragment();

        //attach the fragment to this dialog fragment, pass the tag f3 to indicate that it must display all other users (not friends or requests sent or received)
        getChildFragmentManager().beginTransaction().setReorderingAllowed(true).add(R.id.FindUsersDialogFragmentCL, findUserFragment, "f3").commit();

        //call method to attach a search listener to the search view
        AddSearchListener();

        //get the constraint layout from the dialog fragment
        ConstraintLayout dialogFragmentCL = view.findViewById(R.id.FindUsersDialogFragmentCL);

        //adding on touch listener to the constraint layout and the containing layout of the dialog fragment,
        // allows user to close the dialog fragment by swiping left or right on it
        dialogFragmentCL.setOnTouchListener(new OnSwipeTouchListener(getContext())
        {
            @Override
            public void onSwipeRight() {
                super.onSwipeRight();
                dismiss();
            }

            @Override
            public void onSwipeLeft() {
                super.onSwipeLeft();
                dismiss();
            }
        });
        view.setOnTouchListener(new OnSwipeTouchListener(getContext()){
            @Override
            public void onSwipeRight() {
                super.onSwipeRight();
                dismiss();
            }

            @Override
            public void onSwipeLeft() {
                super.onSwipeLeft();
                dismiss();
            }
        });
        return view;
    }

    //method to add search listener to the search view
    private void AddSearchListener()
    {
        //attach on query text listener to search view
        findUsersSV.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                //when user clicks to submit a search

                //check if the user has entered anything
                if(s.equals(null) || s.equals("  "))
                {
                    //otherwise pass a null to the user list fragment
                    //ensures the fragment will display all users in the list instead of nothing
                    findUserFragment.Search(null);
                }
                else
                {
                    //if they have pass the search text to the user list fragment
                    findUserFragment.Search(s.toLowerCase());
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                //listens for the text change

                //so that the list updates with all users when they delete search text
                if(s.equals(null) || s.equals(""))
                {
                    onQueryTextSubmit("  ");
                }
                return false;
            }
        });
    }
}
