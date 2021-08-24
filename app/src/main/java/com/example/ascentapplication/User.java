package com.example.ascentapplication;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

//User class object holds the current app user and their avatar bitmap to be used throughout the app
public class User
{
    /* Private Properties */
    private FirebaseUser user;                                                      //the currently signed in user
    private Bitmap userAvatar;                                                      //the user's avatar as a bitmap

    //class constructor
    /*Parameters
    * - User : the currently signed in user
    * - UserAvatar : the user's avatar as a bitmap
     */
    public User(FirebaseUser User, Bitmap UserAvatar)
    {
        user = User;
        userAvatar = UserAvatar;
    }

    //getters and setters
    public FirebaseUser GetUser()
    {
        return user;
    }
    public Bitmap GetAvatar()
    {
        return userAvatar;
    }
    public void SetUser(FirebaseUser User)
    {
        user = User;
    }
    public void SetAvatar(Bitmap UserAvatar)
    {
        userAvatar = UserAvatar;
    }
}
