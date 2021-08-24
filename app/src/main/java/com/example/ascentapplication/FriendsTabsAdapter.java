package com.example.ascentapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

//FriendsTabsAdapter class used to create users list fragments as tabs
public class FriendsTabsAdapter extends FragmentStateAdapter
{
    //public constructor
    // paramater parentActivity represents the parent activity
    public FriendsTabsAdapter(AppCompatActivity parentActivity)
    {
        super(parentActivity);
    }

    //override the create fragment method
    @NonNull
    @Override
    public Fragment createFragment(int position)
    {
        //in any tab position create a user list fragment.
        // the tag will automatically represent it's position so that the fragment will know to display the
        // friends list, or requests sent or requests received
        switch(position)
        {
            case 0:
                return new UsersListFragment();
            case 1:
                return new UsersListFragment();
            case 2:
                return new UsersListFragment();
            default:
                return new UsersListFragment();
        }
    }

    //override the get item count. there will be 3 tabs
    @Override
    public int getItemCount()
    {
        return 3;
    }
}
