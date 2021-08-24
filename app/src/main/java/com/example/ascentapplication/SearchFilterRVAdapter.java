package com.example.ascentapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Filter;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

//recylcer view adapter for the hold type filters when search boulder problems
public class SearchFilterRVAdapter extends RecyclerView.Adapter<SearchFilterRVAdapter.FilterViewHolder>
{
    //creating an item check interface for the hold type check boxes
    //will listen to when item box is checked or unchecked
    interface OnItemCheckListener {
        void onItemCheck(String item);
        void onItemUncheck(String item);
    }

    /*Private Properties*/
    private List<String> holdTypes = new ArrayList<>();                                             //the arraylist holding all the possible hold type filters
    private OnItemCheckListener onItemClick;                                                        //contains the on item check listener interface

    public ArrayList<FilterViewHolder> viewHolders = new ArrayList<>();                             //arraylist containing the viewholders for each hold type item

    //constructor
    public SearchFilterRVAdapter(List<String> HoldTypes, OnItemCheckListener onItemCheckListener)
    {
        holdTypes = HoldTypes;
        onItemClick = onItemCheckListener;
    }

    //defines the viewholder for the items attaches them to the recycler view parent
    @Override
    public FilterViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View listItem = inflater.inflate(R.layout.search_filter_item, parent, false);
        FilterViewHolder viewHolder = new FilterViewHolder(listItem);
        viewHolders.add(viewHolder);
        return viewHolder;
    }

    //bind each hold type item to a view holder. sets the on check changed listener for the check boxes
    @Override
    public void onBindViewHolder(FilterViewHolder viewHolder, int position)
    {
        final String holdType = holdTypes.get(position);
        viewHolder.holdTypeCheckBox.setText(holdType);
        viewHolder.holdTypeCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b)
                {
                    onItemClick.onItemCheck(holdType);
                }
                else
                {
                    onItemClick.onItemUncheck(holdType);
                }
            }
        });
    }

    //returns number of items in the recycler view
    @Override
    public int getItemCount()
    {
        return holdTypes.size();
    }

    //customer view holder class for the hold type filter items
    public static class FilterViewHolder extends RecyclerView.ViewHolder
    {
        CheckBox holdTypeCheckBox;
        Context context;
        public FilterViewHolder(View view)
        {
            super(view);
            context = view.getContext();
            holdTypeCheckBox = view.findViewById(R.id.SearchFilterItemCB);
        }
    }

}
