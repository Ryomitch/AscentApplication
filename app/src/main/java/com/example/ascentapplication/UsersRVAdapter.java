package com.example.ascentapplication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
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

//Recylcer View Adapter class for displaying users in a list
public class UsersRVAdapter extends RecyclerView.Adapter<UsersRVAdapter.UserViewHolder>
{
    /* Private Properties */
    private HashMap<String, String> users = new HashMap<>();                                        //holds the list of users to be displayed (Id, Name)
    private ArrayList<String> userIds = new ArrayList<>();                                          //holds the lsit of user ids
    private ArrayList<String> displayNames = new ArrayList<>();                                     //holds the list of user display names
    private FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();                 //holds the currently signed in user
    private ViewGroup parent;                                                                       //holds the parent layout of the recycler view
    private String listType;                                                                        //holds the user list type (friends, requests sent or others)

    /* Public static Avatar property to reduce number of image downloads */
    public static Bitmap viewAvatar;

    //constructor for the adapter
    /*Parameters
    * - users: the list of users to display
    * - ListType: the user list type being displayed
     */
    public UsersRVAdapter(HashMap<String, String> Users, String ListType)
    {
        users = Users;
        listType = ListType;

        //loop through put each of the users' ids and display names into respective lists so they'll be indexed
        for(String userId : users.keySet())
        {
            userIds.add(userId);
            displayNames.add(users.get(userId));
        }
    }

    //method to create the holding layout for each item
    @Override
    public UserViewHolder onCreateViewHolder(ViewGroup Parent, int viewType)
    {
        parent = Parent;
        LayoutInflater inflater = LayoutInflater.from(Parent.getContext());
        View listItem = inflater.inflate(R.layout.user_list_item, Parent, false);
        UserViewHolder viewHolder = new UserViewHolder(listItem);
        return viewHolder;
    }

    //method to bind an item in the recycler view to an item layout
    @Override
    public void onBindViewHolder(UserViewHolder holder, int position)
    {
        //get the user based on item position, show display name on item view holder
        final String userId = userIds.get(position);
        final String displayName = displayNames.get(position);
        holder.displayNameTV.setText(displayName);

        //if the user is viewing their own friends
        if(listType.equals("View"))
        {
            //change button to show a remove friend icon and change on click
            holder.btn.setImageResource(R.drawable.remove_friend_icon);
            View.OnClickListener removeFriendOnClick = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //create reference for location user's friends list in database
                    DatabaseReference viewUserRef = FirebaseDatabase.getInstance().getReference().child("ascentusers").child(userId).child("Friends").child(currentUser.getUid()).getRef();
                    //remove current user from the friends list
                    viewUserRef.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            //create reference for current user's friends list
                            DatabaseReference currentUserRef = FirebaseDatabase.getInstance().getReference().child("ascentusers").child(currentUser.getUid()).child("Friends").child(userId).getRef();
                            //remove the user from current user's friends list
                            currentUserRef.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    //notify current user friend has been removed
                                    Toast.makeText(holder.btn.getContext(), "User Removed from Friends", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    });
                }
            };
            //set the new on click listener
            holder.btn.setOnClickListener(removeFriendOnClick);
        }
        //if user is viewing requests they've sent
        else if(listType.equals("Requests Sent"))
        {
            //change button image to show cancel request icon
            holder.btn.setImageResource(R.drawable.cancel_friend_request_icon);
            View.OnClickListener cancelRequestOnClick = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //create reference of location for user's requests received list
                    DatabaseReference viewUserRef = FirebaseDatabase.getInstance().getReference().child("ascentusers").child(userId).child("Friend Requests").child(currentUser.getUid()).getRef();
                    //remove current signed in user from that list
                    viewUserRef.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            //create reference for signed in user's requests sent list
                            DatabaseReference currentUserRef = FirebaseDatabase.getInstance().getReference().child("ascentusers").child(currentUser.getUid()).child("Friend Requests Sent").child(userId).getRef();
                            //remove user from that list
                            currentUserRef.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    //notify user request has been cancelled
                                    Toast.makeText(holder.btn.getContext(), "Friend Request Cancelled", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    });
                }
            };
            //set the new on click listener
            holder.btn.setOnClickListener(cancelRequestOnClick);
        }
        //if user other users
        else if(listType.equals("Add"))
        {
            //Allow user to swipe on an item left or right to temporarily remove it from the displayed list
            holder.itemContainer.setOnTouchListener(new OnSwipeTouchListener(holder.itemContainer.getContext()){
                @Override
                public void onSwipeRight() {
                    super.onSwipeRight();
                    //remove the user from the arraylists and call notfy removed to update the recycler view
                    users.remove(userId);
                    userIds.remove(position);
                    displayNames.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, users.size());
                }

                @Override
                public void onSwipeLeft() {
                    super.onSwipeLeft();
                    //remove the user from the arraylists and call notfy removed to update the recycler view
                    users.remove(userId);
                    userIds.remove(position);
                    displayNames.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, users.size());
                }
            });
            //change the image to show add friend icon
            holder.btn.setImageResource(R.drawable.add_friend_icon);
            //create new button on click listener
            View.OnClickListener sendRequestOnClick = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //create reference for the location of the signed in user's requests sent list
                    DatabaseReference friendRef = FirebaseDatabase.getInstance().getReference().child("ascentusers").child(currentUser.getUid()).child("Friend Requests Sent").child(userId);
                    //add the user to that list in the database
                    friendRef.setValue("Sent").addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            //create reference for the location of the user's requests received list
                            DatabaseReference viewUserRef = FirebaseDatabase.getInstance().getReference().child("ascentusers").child(userId).child("Friend Requests").child(currentUser.getUid());
                            //add that signed in user to that list
                            viewUserRef.setValue("Received").addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    //notify user that request has been sent
                                    Toast.makeText(holder.btn.getContext(), "Friend Request Sent", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    });
                }
            };
            //set the new on click listener
            holder.btn.setOnClickListener(sendRequestOnClick);
        }
        //create a new on click listener for the item profile image
        View.OnClickListener profileOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //open the profile activity. Pass extras indicating it should display profile of a different user to one signed in
                Intent intent = new Intent(view.getContext(), ProfileActivity.class);
                //set viewAvatar image to the image of the user who clicked on, so profile activity will get this image to display on the profile
                viewAvatar = ((BitmapDrawable) holder.avatarIV.getDrawable()).getBitmap();
                intent.putExtra("View User", userId);
                intent.putExtra("Previous Activity", "Friends List");
                view.getContext().startActivity(intent);
            }
        };
        //set the on click listener of item profile image
        holder.avatarRL.setOnClickListener(profileOnClick);
        //create the reference for the location of user avatar in Firebase Storage
        FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
        StorageReference storageReference = firebaseStorage.getReference().child("User Avatars/" + userId);
        //download the image from storage
        final long ONE_MEGABYTE = 1024 * 1024;
        storageReference.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                //set the viewAvatar property to the downloaded image
                viewAvatar = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                //set the item profile avatar image to the downloaded image
                holder.avatarIV.setImageBitmap(viewAvatar);
            }}).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                //otherwise get the profile icon from resources
                Bitmap bmp = BitmapFactory.decodeResource(parent.getResources(), R.drawable.profile_icon);
                holder.avatarIV.setImageBitmap(bmp);
            }
        });
    }

    //returns the number of users being displayed in the recycler view
    @Override
    public int getItemCount()
    {
        return users.size();
    }

    //custom viewholder class for the user items in the recycler view
    public static class UserViewHolder extends RecyclerView.ViewHolder
    {
        //item view properties
        View itemContainer;                                             //the highest level layout of the item view
        RelativeLayout avatarRL;                                        //the layout containing the avatar image, acts as profile button
        ImageView avatarIV;                                             //the image view containing user avatar
        TextView displayNameTV;                                         //the textview containing the user display name
        ImageButton btn;                                                //the button that acts as an add friend, remove friend or send request button based on the list type
        //constructor for the viewholder
        public UserViewHolder(View view)
        {
            super(view);
            //retrieve the item views from the layout
            itemContainer = view;
            avatarRL = view.findViewById(R.id.UserItemAvatarRL);
            avatarIV = view.findViewById(R.id.UserItemAvatarIV);
            displayNameTV = view.findViewById(R.id.UserItemDisplayNameTV);
            btn = view.findViewById(R.id.UserItemBtn);
        }
    }


}
