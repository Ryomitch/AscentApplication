package com.example.ascentapplication;

import android.content.Context;
import android.content.Intent;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

//public class for displaying a list of user's completed or unfinished boulder problems in a recycler view
public class RecordRVAdapter extends RecyclerView.Adapter<RecordRVAdapter.RecordViewHolder>{
    private List<BoulderProblem> bps = new ArrayList<BoulderProblem>();

    public RecordRVAdapter(List<BoulderProblem> nBps)
    {
        bps = nBps;
    }

    @Override
    public RecordViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View listItem = inflater.inflate(R.layout.record_list_item, parent, false);
        RecordViewHolder viewHolder = new RecordViewHolder(listItem);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecordViewHolder holder, int position)
    {
        final BoulderProblem nBps = bps.get(position);
        holder.nameTv.setText(nBps.GetName());
        holder.setterTv.setText("(Setter: " + nBps.GetSetter() +")");
        holder.gradeTv.setText("Grade: " + nBps.GetGrade());
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        dbRef.child("ascentusers/" + currentUser.getUid() + "/record/").child(nBps.GetName()).child("attempts").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists())
                {
                    holder.attemptsTv.setText("Attempts: " + snapshot.getValue().toString());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        holder.viewBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
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

    @Override
    public int getItemCount()
    {
        return bps.size();
    }

    public static class RecordViewHolder extends RecyclerView.ViewHolder
    {
        TextView nameTv;
        TextView setterTv;
        TextView gradeTv;
        TextView attemptsTv;
        Button viewBtn;
        Context context;
        public RecordViewHolder(View view)
        {
            super(view);
            context = view.getContext();
            nameTv = (TextView) view.findViewById(R.id.RecordBP_Name);
            setterTv = (TextView) view.findViewById(R.id.RecordBP_Setter);
            gradeTv = (TextView) view.findViewById(R.id.RecordBP_Grade);
            attemptsTv = (TextView) view.findViewById(R.id.RecordBP_Attempts);
            viewBtn = (Button) view.findViewById(R.id.RecordItemBtn);
        }
    }

}
