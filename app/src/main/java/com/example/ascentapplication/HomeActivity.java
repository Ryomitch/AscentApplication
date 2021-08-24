package com.example.ascentapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

//HomeActivity class for the app home screen and navigating to other app features
public class HomeActivity extends AppCompatActivity
{
    /* Private Properties */
    private FirebaseAuth mAuth;                                 //firebase authentication object
    private FirebaseUser currentUser;                           //firebase user object

    /* Public static property */
    public static User appUser;                                 //app user object. For easily accessing the user avatar bitmap across all other app screens

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        //getting the current user
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        //calling method to get and display the user avatar
        GetUserAvatar();
    }

    //method to retrieve the user avatar from Firebase Storage and display it
    public void GetUserAvatar()
    {
        //getting the layouts that act as button on the home screen
        ConstraintLayout profileCL = findViewById(R.id.ProfileBtnCL);
        ConstraintLayout cpCL = findViewById(R.id.ProblemCL);
        ConstraintLayout spCL = findViewById(R.id.SearchCL);
        //setting the layout on clicks to null so that user cannot use them till avatar is retrieved
        profileCL.setOnClickListener(null);
        cpCL.setOnClickListener(null);
        spCL.setOnClickListener(null);

        //creating a Firebase storage reference for retrieving the user avatar
        FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
        StorageReference storageReference = firebaseStorage.getReference().child("User Avatars/" + currentUser.getUid());

        //downloading the user avatar
        final long ONE_MEGABYTE = 1024 * 1024;
        storageReference.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                //if the avatar is successfully downloaded convert it to a bitmap
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                //set app user to a new User object using the Firebase user object and the avatar bitmap
                appUser = new User(currentUser, bmp);

                //call method to display the user avatar on screen
                PopulateUserDetails();

                //re-enabling the layout on clicks so that the user can access other features now their avatar is downloaded
                profileCL.setOnClickListener(HomeActivity.this::ProfileOnClick);
                cpCL.setOnClickListener(HomeActivity.this::CreateProblemsOnClick);
                spCL.setOnClickListener(HomeActivity.this::SearchProblemsOnClick);
            }}).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                //otherwise if avatar not successful downloaded then set the user avatar using the profile icon in the app resources
                Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.profile_icon);

                //set app user to a new User object using the Firebase user object and the avatar bitmap
                appUser = new User(currentUser, bmp);

                //re-enabling the layout on clicks so that the user can access other features now their avatar is downloaded
                profileCL.setOnClickListener(HomeActivity.this::ProfileOnClick);
                cpCL.setOnClickListener(HomeActivity.this::CreateProblemsOnClick);
                spCL.setOnClickListener(HomeActivity.this::SearchProblemsOnClick);
            }
        });
    }

    //Onclick method for the Profile button
    public void ProfileOnClick(View view)
    {
        //starting the profile screen
        Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
        startActivity(intent);
    }

    //Onclick method for the Create Problems Button
    public void CreateProblemsOnClick(View view)
    {
        //starting the virtual wall screen
        Intent intent = new Intent(HomeActivity.this, VirtualWallActivity.class);
        startActivity(intent);
    }

    //Onclick method for the Search button
    public void SearchProblemsOnClick(View view)
    {
        //starting the problem search activity
        Intent intent =  new Intent(HomeActivity.this, ProblemSearchActivity.class);
        startActivity(intent);
    }

    //Method to display the user avatar on the Home screen
    public void PopulateUserDetails()
    {
        //getting the avatar image view
        ImageView avatarIV = findViewById(R.id.ProfileIV);
        //setting the bitmap of avatar image view using the user avatar bitmap
        avatarIV.setImageBitmap(appUser.GetAvatar());
    }

    /*override on back pressed to return to sign in page and remove all activities from back stack*/
    @Override
    public void onBackPressed()
    {
        //signing the user out and setting the user avatar to null to delete the downloaded bitmap
        mAuth.signOut();
        appUser.SetAvatar(null);

        //starting the main activity and clearing all previous activities
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        // set the new task and clear flags
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}