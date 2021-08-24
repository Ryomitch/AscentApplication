package com.example.ascentapplication;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

//class to display a list of boulder problems in a recycler view. attaches each boulder problem to their own item layout
public class GeneralRVAdapter extends RecyclerView.Adapter<GeneralRVAdapter.ViewHolder>
{
    /*Private Properties */
    private List<BoulderProblem> bps = new ArrayList<BoulderProblem>();                                         //list of boulder problems to be displayed

    //constructor for the RV adapter
    public GeneralRVAdapter(List<BoulderProblem> nBps)
    {
        bps = nBps;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View listItem = inflater.inflate(R.layout.bp_cl_with_tv, parent, false);
        ViewHolder viewHolder = new ViewHolder(listItem);
        return viewHolder;
    }

    //method to bind each boulder problem to their own layout and position in the recycler view
    @Override
    public void onBindViewHolder(ViewHolder holder, int position)
    {
        final BoulderProblem nBps = bps.get(position);
        holder.nameTv.setText(nBps.GetName());
        holder.setterTv.setText("(Setter: " + nBps.GetSetter() + ")");
        holder.gradeTv.setText("Grade: " + nBps.GetGrade());
        //set on click listener for the item view button
        holder.viewBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                //open the virtual wall activity and pass boulder problem details to it as extras
                Intent intent = new Intent(view.getContext(), VirtualWallActivity.class);
                intent.putExtra("Selected Problem Name", nBps.GetName());
                intent.putExtra("Selected Problem Setter", nBps.GetSetter());
                intent.putExtra("Selected Problem SetterId", nBps.GetSetterId());
                intent.putExtra("Selected Problem Grade", nBps.GetGrade());
                intent.putStringArrayListExtra("Selected Problem Route", nBps.GetRouteAsStringList());
                intent.putExtra("Selected Problem Algorithm", nBps.GetAlgorithm());
                intent.putExtra("Selected Problem Setup", nBps.GetSetup());
                view.getContext().startActivity(intent);
            }
        });
    }

    //returns the amount of items in recycler view
    @Override
    public int getItemCount()
    {
        return bps.size();
    }

    //custom view holder class that is used to get an item layout and the views contained in it
    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        /*item properties*/
        TextView nameTv;
        TextView setterTv;
        TextView gradeTv;
        Button viewBtn;
        Context context;
        public ViewHolder(View view)
        {
            super(view);
            context = view.getContext();
            nameTv = (TextView) view.findViewById(R.id.BP_Name);
            setterTv = (TextView) view.findViewById(R.id.BP_Setter);
            gradeTv = (TextView) view.findViewById(R.id.BP_Grade);
            viewBtn = (Button) view.findViewById(R.id.BPItemBtn);
        }
    }

}
