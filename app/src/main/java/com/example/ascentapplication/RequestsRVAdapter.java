package com.example.ascentapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;

//custom rv adapter for the user requests received
public class RequestsRVAdapter extends RecyclerView.Adapter<RequestsRVAdapter.RequestsViewHolder> {
    private HashMap<String, String> users = new HashMap<>();
    private FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private ArrayList<String> userIds = new ArrayList<>();
    private ArrayList<String> displayNames = new ArrayList<>();
    private ViewGroup parent;
    public static Bitmap viewAvatar;

    //class constructor
    public RequestsRVAdapter(HashMap<String, String> Users)
    {
        users = Users;
        for(String userId : users.keySet())
        {
            userIds.add(userId);
            displayNames.add(users.get(userId));
        }
    }

    @Override
    public RequestsViewHolder onCreateViewHolder(ViewGroup Parent, int viewType)
    {
        parent = Parent;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View listItem = inflater.inflate(R.layout.request_list_item, parent, false);
        RequestsViewHolder viewHolder = new RequestsViewHolder(listItem);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RequestsViewHolder holder, int position)
    {
        final String userId = userIds.get(position);
        final String displayName = displayNames.get(position);
        holder.displayNameTV.setText(displayName);
        View.OnClickListener profileOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewAvatar = ((BitmapDrawable) holder.avatarIV.getDrawable()).getBitmap();
                Intent intent = new Intent(view.getContext(), ProfileActivity.class);
                intent.putExtra("View User", userId);
                intent.putExtra("Previous Activity", "Requests List");
                view.getContext().startActivity(intent);
            }
        };
        holder.avatarRL.setOnClickListener(profileOnClick);
        //on click listener for accepting friend requests
        View.OnClickListener acceptOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatabaseReference reqRecRef = FirebaseDatabase.getInstance().getReference().child("ascentusers").child(currentUser.getUid()).child("Friend Requests").child(userId).getRef();
                reqRecRef.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        DatabaseReference reqSentRef = FirebaseDatabase.getInstance().getReference().child("ascentusers").child(userId).child("Friend Requests Sent").child(currentUser.getUid()).getRef();
                        reqSentRef.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                DatabaseReference friendRef = FirebaseDatabase.getInstance().getReference().child("ascentusers").child(userId).child("Friends").child(currentUser.getUid());
                                friendRef.setValue("Friend").addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("ascentusers").child(currentUser.getUid()).child("Friends").child(userId);
                                        userRef.setValue("Friend").addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Toast.makeText(holder.acceptBtn.getContext(), displayName + " Added as a Friend!", Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                });
            }
        };
        holder.acceptBtn.setOnClickListener(acceptOnClick);
        //on click listener for rejecting friend requests
        View.OnClickListener rejectOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatabaseReference viewUserRef = FirebaseDatabase.getInstance().getReference().child("ascentusers").child(currentUser.getUid()).child("Friend Requests").child(userId).getRef();
                viewUserRef.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        DatabaseReference currentUserRef = FirebaseDatabase.getInstance().getReference().child("ascentusers").child(userId).child("Friend Requests Sent").child(currentUser.getUid()).getRef();
                        currentUserRef.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(holder.rejectBtn.getContext(), "Friend Request Declined", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }
        };
        holder.rejectBtn.setOnClickListener(rejectOnClick);
        FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
        StorageReference storageReference = firebaseStorage.getReference().child("User Avatars/" + userId);
        final long ONE_MEGABYTE = 1024 * 1024;
        storageReference.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                viewAvatar = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                holder.avatarIV.setImageBitmap(viewAvatar);
            }}).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Bitmap bmp = BitmapFactory.decodeResource(parent.getResources(), R.drawable.profile_icon);
                holder.avatarIV.setImageBitmap(bmp);
            }
        });
    }

    @Override
    public int getItemCount()
    {
        return users.size();
    }

    public static class RequestsViewHolder extends RecyclerView.ViewHolder
    {
        RelativeLayout avatarRL;
        ImageView avatarIV;
        TextView displayNameTV;
        Button acceptBtn;
        Button rejectBtn;
        public RequestsViewHolder(View view)
        {
            super(view);
            avatarRL = view.findViewById(R.id.RequestItemAvatarRL);
            avatarIV = view.findViewById(R.id.RequestItemAvatarIV);
            displayNameTV = view.findViewById(R.id.RequestItemDisplayNameTV);
            acceptBtn = view.findViewById(R.id.RequestItemAcceptBtn);
            rejectBtn = view.findViewById(R.id.RequestItemRejectBtn);
        }

    }
}
