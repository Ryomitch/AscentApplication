package com.example.ascentapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.opengl.GLES10;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.renderscript.ScriptGroup;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.microedition.khronos.opengles.GL10;

//ProfileActivity class for retrieving and displaying user details, as well editing details and accessing user profile features
public class ProfileActivity extends AppCompatActivity {

    /* Private Properties */
    private Toolbar toolbar;                                    //toolbar object for the activity toolbar

    private FirebaseAuth mAuth;                                 //Firebase Authentication object for retrieving user data from firebase
    private FirebaseUser currentUser;                           //Firebase user object to hold the current user

    private int currentEdit;                                    //Integer indicating what state the edit profile is in

    private String cameraImagePath;                             //to contain path of image taken in camera
    private ImageView updateAvatarIV;                           //to hold the avatar imageview when displaying a new avatar image
    private Bitmap newAvatarBmp;                                //hold the bitmap when the user selects and uploads a new avatar
    private boolean avatarUpdated = false;                      //a boolean to indicate whether the user has selected and uploaded a new avatar

    private String viewUser;                                    //string holding the user id of a user who's profile is being viewed
    private String viewUserDisplayName;                         //holds the display name of a user who's profile is being viewed
    private Button friendButton;
    private ChildEventListener bpListener;                      //listener for bps when viewing another user's profile

    private String previousActivity;                            //holds the name of the previous activity for reference

    //reference for the realtime database location in which the user profiles are stored
    private DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("ascentusers");

    private ArrayList<BoulderProblem> viewBps = new ArrayList<>();                                  //arraylist holds the boulder problems created by the other user who's profile is being viewed

    //for each of the following arraylists the strings inside are all user ids
    private ArrayList<String> friends = new ArrayList<>();                  //arraylist for the current user's friends
    private ArrayList<String> requestsSent = new ArrayList<>();            //arraylist for the other users who the current user sent friend requests to
    private ArrayList<String> requestsReceived = new ArrayList<>();        //arraylist for the other users who the current user has received requests from

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        //setting the toolbar object using the toolbar view from the activity layout
        toolbar = findViewById(R.id.ProfileToolbar);
        toolbar.setTitle("");
        //calling method to set the action bar for the activity to the toolbar object for enabling the options menu to appear
        setSupportActionBar(toolbar);

        //getting the current user
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        //checking if the previous activity passed any extras
        if(getIntent().getExtras() != null)
        {
            //if so then the this activity is now for viewing another user's profile

            //get the user id of the user to viewed
            viewUser = getIntent().getStringExtra("View User");
            //get the previous activity name
            previousActivity = getIntent().getStringExtra("Previous Activity");

            //getting the friend button from the activity and set it's text as default to send friend request
            friendButton = findViewById(R.id.ProfileAddBtn);
            friendButton.setText(R.string.send_friend_request_button_text);
            friendButton.setVisibility(View.VISIBLE);

            //call methods to retrieve the current user's friends, requests sent and requests received
            RetrieveFriends();
            RetrieveRequestsReceived();
            RetrieveRequestsSent();

            //getting the layouts that are used a buttons for the friends screen, problems screen, favorites screen and record screen and setting them to be invisible
            ConstraintLayout friendsCL = findViewById(R.id.ProfileFriendsBtnCL);
            friendsCL.setVisibility(View.INVISIBLE);
            friendsCL.setOnClickListener(null);
            ConstraintLayout problemCL = findViewById(R.id.ProfileProblemsBtnCL);
            problemCL.setOnClickListener(null);
            problemCL.setVisibility(View.INVISIBLE);
            ConstraintLayout favCL = findViewById(R.id.ProfileFavBtnCL);
            favCL.setOnClickListener(null);
            favCL.setVisibility(View.INVISIBLE);
            ConstraintLayout recordCL = findViewById(R.id.ProfileRecordBtnCL);
            recordCL.setOnClickListener(null);
            recordCL.setVisibility(View.INVISIBLE);
            //changing the title of the screen
            TextView titleTV = findViewById(R.id.ProfileTitleTV);
            titleTV.setText("View Profile");
            //retrieving the display of the user being viewed from the realtime database
            FirebaseDatabase.getInstance().getReference().child("ascentusers").child(viewUser).child("DisplayName").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    //get the snapshot value as a string, which is the display name
                    viewUserDisplayName = snapshot.getValue().toString();

                    //call method to populate the screen with the retrieved display name
                    PopulateUserDetails(viewUserDisplayName, null);

                    //getting the view problems layout from the activity and setting it to visible
                    ConstraintLayout viewProblemsCL = findViewById(R.id.ProfileViewProblemsCL);
                    viewProblemsCL.setVisibility(View.VISIBLE);

                    //calling method to retrieve the viewed user's boulder problems and display them
                    ViewUserProblemsSearch();

                    //updating the toolbar avatar image view with the current user's avatar (retrieved from the Home Activity's static app user object)
                    RelativeLayout avatarRL = findViewById(R.id.ProfileToolbarAvatarRL);
                    ImageView avatarIV = findViewById(R.id.ProfileToolbarAvatarIV);
                    avatarIV.setImageBitmap(HomeActivity.appUser.GetAvatar());
                    avatarRL.setOnClickListener(ProfileActivity.this::ProfileOnClick);
                    avatarRL.setVisibility(View.VISIBLE);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
        else
        {
            //currentEdit set to 0 indicating user is only viewing their profile
            currentEdit = 0;

            //getting the current user details
            String displayName = currentUser.getDisplayName();
            String email = currentUser.getEmail();

            //calling method to display the current user profile details
            PopulateUserDetails(displayName, email);
        }

    }

    //method that display an options menu when the user clicks the options button
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        //only display the options menu if the user is viewing their own profile
        if(viewUser == null)
        {
            getMenuInflater().inflate(R.menu.edit_profile_menu, menu);
        }
        return true;
    }

    //listener for when an option is selected from the options menu
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem)
    {
        Toast.makeText(this, "Selected: " + menuItem.getTitle(), Toast.LENGTH_SHORT);       //message indicating selected option

        //switch case to determine outcome of selecting a menu option
        switch(menuItem.getItemId())
        {
            //user selects to edit name
            case R.id.edit_name_option:
                if(currentEdit == 0)                                                                //if the int is 0 then activity is not in profile edit mode and should activate edit mode
                {
                    //create a new dialog for changing the display name with bottom popup dialog style
                    Dialog changeDNDialog = new Dialog(this, R.style.MaterialBottomDialogSheet);
                    changeDNDialog.setCancelable(true);
                    changeDNDialog.setContentView(R.layout.change_display_name_dialog);
                    changeDNDialog.getWindow().setLayout(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
                    changeDNDialog.getWindow().setGravity(Gravity.CENTER);

                    //getting the display name edit text from the dialog
                    TextInputEditText displayNameET = changeDNDialog.findViewById(R.id.ChangeDisplayNameET);
                    //add on text change listener to display name input
                    AddDisplayNameChangeListener(displayNameET);

                    currentEdit = 1;                                                                //set integer to 1 to indicate activity is in profile edit display name mode

                    //get the cancel and submit button from the dialog
                    Button cancelDNBtn = changeDNDialog.findViewById(R.id.ChangeDNCancelBtn);
                    Button submitBtn = changeDNDialog.findViewById(R.id.ChangeDNSubmitBtn);

                    //add on click listener to the cancel button
                    cancelDNBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //set the profile out of edit mode
                            changeDNDialog.dismiss();
                        }
                    });

                    //add new on click listener for the submit button
                    submitBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String displayName = displayNameET.getText().toString();
                            InputValidation inputValidation = new InputValidation();
                            //using validator object to check the input is valid
                            if(inputValidation.ValidateDisplayNameInput(displayName))
                            {
                                if(!currentUser.getDisplayName().equals(displayName))
                                {
                                    //update the user's profile with the new display name, attach on success listener
                                    UserProfileChangeRequest displayChange = new UserProfileChangeRequest.Builder().setDisplayName(displayName).build();
                                    currentUser.updateProfile(displayChange).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            //update the user display name in the realtime database
                                            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("ascentusers").child(currentUser.getUid());
                                            userRef.child("DisplayName").setValue(displayName).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    //call method to update the profile display with the new display name
                                                    PopulateUserDetails(displayName, currentUser.getEmail());

                                                    //set the profile out of edit mode
                                                    currentEdit = 0;
                                                    //notify user display name has been successfully changed
                                                    Toast.makeText(getApplicationContext(), "Display Name Changed Successfully", Toast.LENGTH_SHORT).show();
                                                    changeDNDialog.dismiss();
                                                }
                                            });
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {                                   //attach on failure listener to update user's display name
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            //notify user display name could not be changed and show the error message
                                            Toast.makeText(ProfileActivity.this, "Display name change failed! Error message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                                else {
                                    Toast.makeText(ProfileActivity.this, "Enter a NEW display name!", Toast.LENGTH_SHORT).show();
                                }
                            }
                            else
                            {
                                //otherwise notify user that the display name input is invalid
                                Toast.makeText(getApplicationContext(), "Invalid Input. Must be between 3 and 20 characters", Toast.LENGTH_LONG).show();
                            }
                        }
                    });

                    //add on dismiss listener to change display name dialog
                    changeDNDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            if(currentEdit != 0)
                            {
                                Toast.makeText(ProfileActivity.this, "Display name change cancelled", Toast.LENGTH_SHORT).show();
                                currentEdit = 0;
                            }
                        }
                    });

                    changeDNDialog.show();
                }
                return true;
            case R.id.edit_email_option:                        //user selects to change email
                if(currentEdit == 0)                                                                //checking user is not already editing their profile
                {
                    //create a change email dialog with bottom popup dialog style
                    Dialog changeEmailDialog = new Dialog(this, R.style.MaterialBottomDialogSheet);
                    changeEmailDialog.setCancelable(true);
                    changeEmailDialog.setContentView(R.layout.change_email_dialog);
                    changeEmailDialog.getWindow().setLayout(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
                    changeEmailDialog.getWindow().setGravity(Gravity.CENTER);

                    //get the email input edit text
                    TextInputEditText emailET = changeEmailDialog.findViewById(R.id.ChangeEmailET);
                    //add on text changed listener to email input
                    AddEmailChangeListener(emailET);

                    currentEdit = 2;                                                                   //set integer to 1 to indicate activity is in profile edit email mode

                    //get the cancel and submit button from the dialog
                    Button cancelEmailBtn = changeEmailDialog.findViewById(R.id.ChangeEmailCancelBtn);
                    Button submitEmailBtn = changeEmailDialog.findViewById(R.id.ChangeEmailSubmitBtn);

                    //set the cancel button on click
                    cancelEmailBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            changeEmailDialog.dismiss();
                        }
                    });

                    //add new on click listener to submit button
                    submitEmailBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //create validation object
                            InputValidation inputValidation = new InputValidation();
                            //get user email input
                            String email = emailET.getText().toString();

                            //checking email input is valid
                            if(inputValidation.ValidateEmailInput(email))
                            {
                                if(!currentUser.getEmail().equals(email))
                                {
                                    //update the user profile with new email, attach on success listener
                                    currentUser.updateEmail(email).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            //send a new verification email
                                            currentUser.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    //set profile out of edit mode
                                                    currentEdit = 0;
                                                    changeEmailDialog.dismiss();
                                                    //call method to update profile screen with new email
                                                    PopulateUserDetails(currentUser.getDisplayName(), email);
                                                    //notify user email changed successfully
                                                    Toast.makeText(getApplicationContext(), "Email Changed Successfully", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {                                   //attach on failure listener to email change
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            //notify user that email could not be changed and show the error message
                                            Toast.makeText(ProfileActivity.this, "Email change failed! Error message: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }
                                else
                                {
                                    Toast.makeText(ProfileActivity.this, "Enter a NEW email address", Toast.LENGTH_SHORT).show();
                                }
                            }
                            else
                            {
                                //otherwise notify user the email input is invalid
                                Toast.makeText(getApplicationContext(), "Invalid Input", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                    changeEmailDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            if(currentEdit != 0)
                            {
                                Toast.makeText(ProfileActivity.this, "Email change cancelled", Toast.LENGTH_SHORT).show();
                                currentEdit = 0;
                            }
                        }
                    });

                    changeEmailDialog.show();
                }
                return true;
            case R.id.edit_password_option:                     //user selects to change password
                if(currentEdit == 0)                                                                //checking user is not already editing their profile
                {
                    currentEdit = 3;                                                                    //set integer to 1 to indicate activity is in profile edit display name mode

                    //creating an input validation object
                    InputValidation inputValidation = new InputValidation();

                    //creating a password dialog, with a bottom pop up dialog style (created in the style resource file)
                    Dialog passwordDialog = new Dialog(this, R.style.MaterialBottomDialogSheet);
                    //set the dialog to be closable
                    passwordDialog.setCancelable(true);
                    //set the content view of the dialog using the change password dialog layout from resources
                    passwordDialog.setContentView(R.layout.change_password_dialog);
                    //setting the height and width values and gravity of the dialog
                    passwordDialog.getWindow().setLayout(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
                    passwordDialog.getWindow().setGravity(Gravity.BOTTOM);

                    //getting new password input boxes
                    TextInputEditText newPasswordTIET = passwordDialog.findViewById(R.id.NewPasswordTIET);
                    TextInputEditText confirmPasswordTIET = passwordDialog.findViewById(R.id.NewConfirmPasswordTIET);

                    //adding text change listeners to the new pass word inputs
                    AddPasswordInputListener(newPasswordTIET);
                    AddPasswordInputListener(confirmPasswordTIET);

                    //getting the cancel password change button
                    Button cancelPasswordBtn = passwordDialog.findViewById(R.id.CancelPasswordEditBtn);
                    //getting the save password change button
                    Button savePasswordBtn = passwordDialog.findViewById(R.id.SubmitPasswordEditBtn);
                    //add on click listener to the cancel password change button
                    cancelPasswordBtn.setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            //dismiss the dialog when the button is clicked
                            passwordDialog.dismiss();
                        }
                    });
                    //adding on click listener to save password change button
                    savePasswordBtn.setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            //getting the user password inputs
                            TextInputEditText currentPasswordTIET = passwordDialog.findViewById(R.id.CurrentPasswordTIET);
                            String currentPasswordInput = currentPasswordTIET.getText().toString();
                            String newPasswordInput = newPasswordTIET.getText().toString();
                            String confirmPasswordInput = confirmPasswordTIET.getText().toString();

                            //using validation object to check the inputs are valid password inputs
                            if(inputValidation.ValidatePasswordInput(currentPasswordInput) && inputValidation.ValidatePasswordInput(newPasswordInput) && inputValidation.ValidatePasswordInput(confirmPasswordInput))
                            {
                                //checking the new password does not match the old password
                                if(!newPasswordInput.equals(currentPasswordInput))
                                {
                                    //checking the new password matches confirm password
                                    if(newPasswordInput.equals(confirmPasswordInput))
                                    {
                                        //reauthenticate the user with firebase before changing their password
                                        AuthCredential authCredential = EmailAuthProvider.getCredential(currentUser.getEmail(), currentPasswordInput);
                                        currentUser.reauthenticate(authCredential).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                //updating the user's password
                                                currentUser.updatePassword(newPasswordInput).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        //dismiss the dialog
                                                        passwordDialog.dismiss();
                                                        //notify the user the password has been changed
                                                        Toast.makeText(getApplicationContext(), "Password Changed Successfully", Toast.LENGTH_LONG).show();
                                                        //set current edit to 0 to indicate the activity is no longer in profile edit mode
                                                        currentEdit = 0;
                                                    }
                                                }).addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        //notify user of error message
                                                        Toast.makeText(getApplicationContext(), "Failed to change password. Error message: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                    }
                                                });
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                //Notify user Authentication failed
                                                Toast.makeText(getApplicationContext(), "Failed to authenticate. Check you entered the correct password. Error message: " + e.getMessage(), Toast.LENGTH_LONG).show();

                                            }
                                        });

                                    }
                                    else {
                                        //otherwise notify the user the password and confirm password must match
                                        Toast.makeText(getApplicationContext(), "New password and confirm password must match!", Toast.LENGTH_LONG).show();
                                    }
                                }
                                else {
                                    //otherwise notify the user the new password must not be the same as the old password
                                    Toast.makeText(getApplicationContext(), "New password must be different from old password!", Toast.LENGTH_LONG).show();
                                }
                            }
                            else {
                                //otherwise notify the user the password is invalid
                                Toast.makeText(getApplicationContext(),"Invalid input", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    //attaching listener to dialog for when it is dismissed
                    passwordDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            //checking the profile is no longer in edit mode
                            if(currentEdit != 0)
                            {
                                //if it is then notify user that password change has been cancelled and set profile out of edit mode
                                Toast.makeText(getApplicationContext(), "Password Change Cancelled", Toast.LENGTH_SHORT).show();
                                currentEdit = 0;
                            }
                        }
                    });
                    //show the password change dialog
                    passwordDialog.show();
                }
                return true;
            case R.id.edit_profile_picture_option:                                                  //menu option to change the user's profile picture
                if(currentEdit == 0)                                                                //checking the profile is not in edit mode
                {
                    currentEdit = 4;                                                                //set the currentedit to 4 to indicate profile is in edit avatar mode
                    Dialog avatarDialog = new Dialog(this);                                 //create a new dialog for changing avatars
                    avatarDialog.setContentView(R.layout.change_avatar_dialog);                     //attaching a layout to the dialog
                    updateAvatarIV = avatarDialog.findViewById(R.id.ChangeAvatarIV);                //getting the imageview for avatar on the dialog
                    updateAvatarIV.setImageBitmap(HomeActivity.appUser.GetAvatar());                //initially setting it to be the user's current avatar

                    Button cameraBtn = avatarDialog.findViewById(R.id.TakePhotoAvatarBtn);          //getting the camera button
                    Button browseBtn = avatarDialog.findViewById(R.id.BrowseAvatarBtn);             //getting the image browse button from the dialog
                    Button cancelAvatarBtn = avatarDialog.findViewById(R.id.ChangeAvatarCancelBtn); //getting the cancel change button
                    Button saveAvatarBtn = avatarDialog.findViewById(R.id.ChangeAvatarSaveBtn);     //getting the save change button

                    //attach on click listener to the camera button from the dialog
                    cameraBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //create intent to open camera screen
                            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            // Ensure that there's a camera activity to handle the intent
                            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                                // Create the File where the photo should go
                                File photoFile = null;
                                try {
                                    //call method to create temporary image file
                                    photoFile = CreateImageFile();
                                } catch (Exception e)
                                {
                                    //notify if error while creating file
                                    Toast.makeText(ProfileActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                                // Continue only if the File was successfully created
                                if (photoFile != null) {
                                    //create uri for new photo file
                                    Uri photoURI = FileProvider.getUriForFile(ProfileActivity.this,
                                            "com.example.ascentapplication.fileprovider",
                                            photoFile);
                                    //pass uri as extra to camera intent
                                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                                    //open camera pass 1 as request code
                                    startActivityForResult(takePictureIntent, 1);
                                }
                            }
                        }
                    });

                    browseBtn.setOnClickListener(new View.OnClickListener()                         //attaching an on click listener to the browse button
                    {
                        @Override
                        public void onClick(View v)
                        {
                            //opening the user's phone gallery to allow them to select an image
                            Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                            //using activity for result method to find out when user has selected an image and retrieve it later on
                            startActivityForResult(gallery, 100);
                        }
                    });

                    saveAvatarBtn.setOnClickListener(new View.OnClickListener()                     //attaching an on click listener to the save change button
                    {
                        @Override
                        public void onClick(View v)
                        {
                            //getting the image on the update avatar image view
                            BitmapDrawable avatarDrawable = (BitmapDrawable) updateAvatarIV.getDrawable();
                            Bitmap avatarBitmap = avatarDrawable.getBitmap();
                            if(avatarBitmap != HomeActivity.appUser.GetAvatar())                    //check the user has selected or taken a new image
                            {
                                //creating a reference for the location to put the new image in Firebase Storage
                                FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
                                StorageReference storageReference = firebaseStorage.getReference().child("User Avatars/" + currentUser.getUid());

                                //compressing the new image in preparation for upload
                                ByteArrayOutputStream out = new ByteArrayOutputStream();
                                BitmapDrawable bmpDrawable = (BitmapDrawable) updateAvatarIV.getDrawable();
                                newAvatarBmp = bmpDrawable.getBitmap();
                                newAvatarBmp.compress(Bitmap.CompressFormat.JPEG, 100, out);

                                //uploading the new new image to the storage reference and attaching an on success listener
                                UploadTask uploadTask = storageReference.putBytes(out.toByteArray());
                                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                        //notify user image has been uploaded
                                        Toast.makeText(ProfileActivity.this, "Avatar uploaded successfully", Toast.LENGTH_SHORT).show();
                                        try {
                                            //retrieve the url of the image that has been uploaded
                                            storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>()
                                            {
                                                @Override
                                                public void onSuccess(Uri uri)
                                                {
                                                    //set the user's avatar uri to the retrieved uri, attach on success listener
                                                    UserProfileChangeRequest updateAvatar = new UserProfileChangeRequest.Builder().setPhotoUri(uri).build();
                                                    currentUser.updateProfile(updateAvatar).addOnSuccessListener(new OnSuccessListener<Void>()
                                                    {
                                                        @Override
                                                        public void onSuccess(Void aVoid)
                                                        {
                                                            //notify user avatar changed successfully
                                                            Toast.makeText(getApplicationContext(), "Avatar Changed Successfully", Toast.LENGTH_SHORT).show();
                                                            //change avatar updated to indicate user has changed their avatar
                                                            avatarUpdated = true;
                                                            //set current edit to 0 to indicate profile no longer in edit mode
                                                            currentEdit = 0;
                                                            //dismiss the dialog
                                                            avatarDialog.dismiss();
                                                            //set the bitmap of the profile IV to the new avatar bitmap
                                                            ImageView avatarIV = findViewById(R.id.ProfilePicIV);
                                                            avatarIV.setImageBitmap(newAvatarBmp);
                                                            //also change the bitmap of the appuser object from the Home activity to the new avatar bitmap
                                                            //allows the new image to be used throughout the app without constantly re-downloading the same image
                                                            HomeActivity.appUser.SetAvatar(newAvatarBmp);
                                                        }
                                                    }).addOnFailureListener(new OnFailureListener()                         //attach on failure listner
                                                    {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e)
                                                        {
                                                            //notify user that the profile avatar uri could not be changed
                                                            Toast.makeText(getApplicationContext(), "Network Error Changing Avatar", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                                }
                                            });

                                        }
                                        catch(Exception e)                  //catch error when trying to retrieve the new avatar uri
                                        {
                                            e.printStackTrace();
                                        }

                                    }
                                });
                                uploadTask.addOnFailureListener(new OnFailureListener() {       //on failure listener for uploading the new avatar image to Firebase Storage
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        //notify user that the image could not be uploaded
                                        Toast.makeText(ProfileActivity.this, "Failure to upload image", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                            else{
                                //otherwise notify user that they still need to select a new image
                                Toast.makeText(getApplicationContext(), "You haven't selected a new image", Toast.LENGTH_LONG).show();
                            }
                        }
                    });

                    //attach on click listner to the cancel change button
                    cancelAvatarBtn.setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            //dismiss the dialog
                            avatarDialog.dismiss();
                        }
                    });

                    //attach on dismiss listener to the avatar change dialog
                    avatarDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            if(currentEdit != 0)                                                    //checking the profile is not already out of edit mode yet
                            {
                                currentEdit = 0;                                                    //if not then set the current edit to 0 and notify user avatar change is cancelled
                                Toast.makeText(getApplicationContext(), "Avatar Change Cancelled", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    //show the avatar change dialog
                    avatarDialog.show();
                }
                return true;
            case R.id.sign_out_option:                                                              //check for sign out option clicked
                if(currentEdit == 0)
                {
                    //create sign out dialog and set to bottom popup dialog style
                    Dialog signOutDialog = new Dialog(this, R.style.MaterialBottomDialogSheet);
                    signOutDialog.setCancelable(true);
                    signOutDialog.setContentView(R.layout.sign_out_dialog);
                    signOutDialog.getWindow().setLayout(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
                    signOutDialog.getWindow().setGravity(Gravity.BOTTOM);

                    //get the buttons from the dialog and set their on click listeners
                    Button signOutCancelBtn = signOutDialog.findViewById(R.id.SignOutCancelBtn);
                    Button signOutConfirmBtn = signOutDialog.findViewById(R.id.SignOutConfirmBtn);
                    signOutCancelBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            signOutDialog.dismiss();
                        }
                    });

                    signOutConfirmBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //sign out user and return to sign in screen
                            mAuth.signOut();
                            HomeActivity.appUser.SetAvatar(null);
                            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }
                    });

                    signOutDialog.show();
                }

                return true;
            case R.id.delete_account_option:
                if(currentEdit == 0)
                {
                    //create a new delete account dialog
                    Dialog deleteAccountDialog = new Dialog(this, R.style.MaterialBottomDialogSheet);
                    deleteAccountDialog.setCancelable(true);
                    deleteAccountDialog.setContentView(R.layout.delete_account_dialog);
                    deleteAccountDialog.getWindow().setLayout(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
                    deleteAccountDialog.getWindow().setGravity(Gravity.TOP);

                    //get controls from the dialog
                    TextInputEditText passwordET = deleteAccountDialog.findViewById(R.id.DeleteAccountPasswordTIET);
                    Button cancelBtn = deleteAccountDialog.findViewById(R.id.CancelDeleteAccountBtn);
                    Button submitBtn = deleteAccountDialog.findViewById(R.id.SubmitDeleteAccountBtn);

                    //add cancel button on click listener
                    cancelBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //dismiss the dialog
                            deleteAccountDialog.dismiss();
                        }
                    });

                    //add submit button on click listener
                    submitBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //get the user password input
                            String passwordInput = passwordET.getText().toString();
                            if(passwordInput != null && !passwordInput.isEmpty() && !passwordInput.equals(""))
                            {
                                //reauthenticate the user with firebase before deleting their account
                                AuthCredential authCredential = EmailAuthProvider.getCredential(currentUser.getEmail(), passwordInput);
                                currentUser.reauthenticate(authCredential).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        String currentUserUid = currentUser.getUid();
                                        currentUser.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                StorageReference userImageRef = FirebaseStorage.getInstance().getReference().child("User Avatars/" + currentUserUid);
                                                userImageRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        //create reference for location of all user created boulder problems in the database
                                                        DatabaseReference bpRef = FirebaseDatabase.getInstance().getReference().child("BoulderProblems").child("UserCreated");
                                                        //add listener to remove all the current user's boulder problems from the database
                                                        bpRef.addChildEventListener(new ChildEventListener() {
                                                            @Override
                                                            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                                                                if(snapshot.exists())
                                                                {
                                                                    BoulderProblem bp = snapshot.getValue(BoulderProblem.class);
                                                                    if(bp.GetSetterId().equals(currentUserUid))
                                                                    {
                                                                        //remove the users boulder problem from the database
                                                                        bpRef.child(bp.GetName()).setValue(null);
                                                                        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("ascentusers");
                                                                        userRef.addChildEventListener(new ChildEventListener() {
                                                                            @Override
                                                                            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                                                                                if(snapshot.exists())
                                                                                {
                                                                                    userRef.child(snapshot.getKey()).child("Favorites").child(bp.GetName()).setValue(null);
                                                                                    userRef.child(snapshot.getKey()).child("record").child(bp.GetName()).setValue(null);
                                                                                }
                                                                            }

                                                                            @Override
                                                                            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                                                                            }

                                                                            @Override
                                                                            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

                                                                            }

                                                                            @Override
                                                                            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                                                                            }

                                                                            @Override
                                                                            public void onCancelled(@NonNull DatabaseError error) {

                                                                            }
                                                                        });
                                                                    }
                                                                }
                                                            }

                                                            @Override
                                                            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                                                            }

                                                            @Override
                                                            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

                                                            }

                                                            @Override
                                                            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {

                                                            }
                                                        });
                                                        //create reference of location for all users of ascent app in database
                                                        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference().child("ascentusers");
                                                        //remove the current user's profile data from the database
                                                        usersRef.child(currentUserUid).setValue(null).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                //add listeners to find and remove values relating to current user on other user's profiles from the database
                                                                usersRef.addChildEventListener(new ChildEventListener() {
                                                                    @Override
                                                                    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                                                                        if(snapshot.exists())
                                                                        {
                                                                            String userId = snapshot.getKey();
                                                                            //delete the current user from another user's friends, requests sent and requests received
                                                                            usersRef.child(userId).child("Friends").child(currentUserUid).setValue(null);
                                                                            usersRef.child(userId).child("Friend Requests Sent").child(currentUserUid).setValue(null);
                                                                            usersRef.child(userId).child("Friend Requests").child(currentUserUid).setValue(null);
                                                                        }
                                                                    }

                                                                    @Override
                                                                    public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                                                                    }

                                                                    @Override
                                                                    public void onChildRemoved(@NonNull DataSnapshot snapshot) {

                                                                    }

                                                                    @Override
                                                                    public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                                                                    }

                                                                    @Override
                                                                    public void onCancelled(@NonNull DatabaseError error) {

                                                                    }
                                                                });
                                                            }
                                                        });
                                                        Toast.makeText(ProfileActivity.this, "User deleted successfully!", Toast.LENGTH_LONG).show();

                                                    }
                                                }).addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Toast.makeText(ProfileActivity.this, "Failed to delete user profile image. Error message: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                    }
                                                });
                                                Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                                                // set the new task and clear flags
                                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(intent);
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Toast.makeText(ProfileActivity.this, "User delete failed!. Error message: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(ProfileActivity.this, "Error Message: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                            else
                            {
                                Toast.makeText(ProfileActivity.this, "Please re-enter you password to delete your account!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    deleteAccountDialog.show();
                }
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    //method to add an on type listener to the display name input textview
    private void AddDisplayNameChangeListener(TextInputEditText displayNameInput)
    {
        //add listener to display name input text view for on text changes
        displayNameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                InputValidation validator = new InputValidation();
                //get the display name input by user
                String displayName = displayNameInput.getText().toString();
                //use validation object to check if it's valid
                if(validator.ValidateDisplayNameInput(displayName))
                {
                    //otherwise change input back to black color indicating input is valid
                    displayNameInput.setTextColor(getColor(R.color.white));
                    displayNameInput.setTooltipText("Enter your name to be displayed to other users in the app");
                }
                else
                {
                    //otherwise change color to red to notify user display name invalid and set a tooltip also
                    displayNameInput.setTextColor(getColor(R.color.red));
                    displayNameInput.setTooltipText("Display name must be between 3 and 20 characters long");
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    //method to add on-type listener to the email input textview
    private void AddEmailChangeListener(TextInputEditText emailInput)
    {
        //add listener to email input text view for text changes
        emailInput.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                //when text is changed
                InputValidation validator = new InputValidation();
                //using validator to check if email input is valid
                String email = emailInput.getText().toString().trim();
                if(validator.ValidateEmailInput(email))                                             //if valid set value to true and change text colour of email input to green to indicate it's valid
                {
                    emailInput.setTextColor(getColor(R.color.white));
                    //set first element of input checks to true
                }
                else                                                                                //otherwise set it's value to false and change text colour of email input to red to indicate it's invalid
                {
                    emailInput.setTextColor(getColor(R.color.red));
                    //set the first element of input checks to false
                }
            }
            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    //method to text change listener to the password input boxes
    private void AddPasswordInputListener(TextInputEditText passwordET)
    {
        passwordET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String password = passwordET.getText().toString();
                //call method to check the password for different rules and set tooltip acccordingly
                PasswordCheckTips(passwordET, password);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    //method checks passwords for incorrect input and set tooltip accordingly
    /* Parameters:
    * - passwordET : the input box for the password
    * - password : the password to check
     */
    private void PasswordCheckTips(TextInputEditText passwordET, String password)
    {
        //create validation object
        InputValidation inputValidation = new InputValidation();
        //check for at least one digit
        if(!inputValidation.PasswordDigitValidation(password))
        {
            passwordET.setTooltipText("Need at least one digit.");
            passwordET.setTextColor(getColor(R.color.red));
        }
        //check for at least one lower case and upper case char
        else if(!inputValidation.PasswordLowerUpperCaseValidation(password))
        {
            passwordET.setTooltipText("Need at least one upper and one lower case character.");
            passwordET.setTextColor(getColor(R.color.red));
        }
        //check for at least one special char
        else if(!inputValidation.PasswordSpecialCharValidation(password))
        {
            passwordET.setTooltipText("Need at least one of these special characters: '£!_?@#$%^&+=*.");
            passwordET.setTextColor(getColor(R.color.red));
        }
        //check no white spaces and length between 8 and 20 chars
        else if(!inputValidation.PasswordSpaceAndLengthValidation(password))
        {
            passwordET.setTooltipText("No white spaces allowed and must between 8 and 20 characters long.");
            passwordET.setTextColor(getColor(R.color.red));
        }
        else
        {
            //otherwise password valid
            passwordET.setTooltipText(null);
            passwordET.setTextColor(getColor(R.color.white));
        }
    }

    //method to create an image file for images taken in camera
    private File CreateImageFile() throws Exception
    {
        // Create an image file name
        String imageTimeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFName = "JPEG_" + imageTimeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        cameraImagePath = image.getAbsolutePath();
        return image;
    }


    //method activated when user has selected a new avatar from their gallery or taken a photo
    /* Parameters
    * - requestCode : holds the code that originally was passed when accessing the gallery or using the camera. used to ensure that code is run when user has selected image from gallery or taken a new photo
    * - resultCode : indicates whether activity was successful or there was an error
    * - data : contains the selected avatar uri
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        //checking user taken photo with camera
        if(resultCode==RESULT_OK && requestCode == 1)
        {
            //get the photo taken in camrea from saved file path
            Bitmap imageBitmp = BitmapFactory.decodeFile(cameraImagePath);
            //if image to big for display and upload then scale down otherwise set image to image view
            if(imageBitmp.getWidth() > 2048 || imageBitmp.getHeight() > 2048)
            {
                Bitmap bmp2 = Bitmap.createScaledBitmap(imageBitmp, 2048, 2048, true);
                updateAvatarIV.setImageBitmap(bmp2);
            }
            else
            {
                updateAvatarIV.setImageBitmap(imageBitmp);
            }
        }
        else if(resultCode==RESULT_OK && requestCode == 100)                                             //checking user selected avatar from gallery
        {
            Uri newAvatarUri = data.getData();                                                          //getting the selected avatar local uri
            DisplayProfileImage(updateAvatarIV, newAvatarUri);                                      //update the imageview on the update avatar dialog with the selected avatar
        }
    }

    //method to populate the profile screen with the relevant display name and email address
    /* Parameters
    * - displayName : the display name to show on screen
    * - email : the email to show on screen
     */
    public void PopulateUserDetails(String displayName, String email)
    {
        //getting the display and email text views and the profile avatar image view
        TextView displayNameTV = (TextView) findViewById(R.id.ProfileDisplayNameTV);
        TextView emailTV = (TextView) findViewById(R.id.ProfileEmailTV);
        ImageView avatarIV = findViewById(R.id.ProfilePicIV);
        displayNameTV.setVisibility(View.VISIBLE);

        //checking the another user profile is not being viewed
        if(viewUser == null)
        {
            //if not then show the user display name, email and avatar
            displayNameTV.setText("Display Name: " + displayName);
            emailTV.setText("Email: " + email);
            avatarIV.setImageBitmap(HomeActivity.appUser.GetAvatar());
        }
        else
        {
            //if another user's profile is being viewed, then show display name but not email
            displayNameTV.setText(displayName);
            emailTV.setVisibility(View.INVISIBLE);

            //check previous activities for the user's avatar ( to stop image from being re-downloaded unnecessarily
            if(previousActivity.equals("Virtual Wall Activity"))
            {
                avatarIV.setImageBitmap(VirtualWallActivity.viewAvatar);
            }
            else if(previousActivity.equals("Friends List"))
            {
                avatarIV.setImageBitmap(UsersRVAdapter.viewAvatar);
            }
            else if(previousActivity.equals("Requests List"))
            {
                avatarIV.setImageBitmap(RequestsRVAdapter.viewAvatar);
            }
            else
            {
                //if cannot find user avatar on previous activities, then set the imageview to the profile icon from resources
                Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.profile_icon);
                avatarIV.setImageBitmap(bmp);
            }
        }
    }

    //method to display the an profile picture on a specified image view using a specified local uri
    //only works with local uri's!
    /*
    * - imageView : the image view to show the profile picture on
    * - uri : the uri to retrieve the profile picture from
     */
    public void DisplayProfileImage(ImageView imageView, Uri uri)
    {
        //create a null bitmap object
        Bitmap bmpFinal = null;
        try {
            //try obtaining image bitmap from the local gallery
            Bitmap bmp = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            //if image is too big scale it down to save space on the app before displaying it
            if(bmp.getWidth() > 2048 || bmp.getHeight() > 2048)
            {
                Bitmap bmp2 = Bitmap.createScaledBitmap(bmp, 2048, 2048, true);
                imageView.setImageBitmap(bmp2);
                bmpFinal = bmp2;
            }
            //otherwise just display the image immediately
            else
            {
                imageView.setImageBitmap(bmp);
                bmpFinal = bmp;
            }
        }catch(IOException error)
        {
            //otherwise catch and show the error message
            Toast.makeText(ProfileActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    //on click method to access my problems tab of the search problems/routes screen
    public void MyProblemsOnClick(View view)
    {
        //creating intent for the search problems tab
        Intent intent = new Intent(ProfileActivity.this, ProblemSearchActivity.class);
        //passing extra to indicate activity should open on the my problems tab
        intent.putExtra("Selected Tab", 2);
        startActivity(intent);
    }

    //on click method to access the favorites tab of the search problems/routes screen
    public void FavoritesOnClick(View view)
    {
        //creating intent for the search problems tab
        Intent intent = new Intent(ProfileActivity.this, ProblemSearchActivity.class);
        //passing extra to indicate activity should open on the favorites tab
        intent.putExtra("Selected Tab", 1);
        startActivity(intent);
    }

    //on click method to access the record screen
    public void RecordOnClick(View view)
    {
        Intent intent = new Intent(ProfileActivity.this, RecordActivity.class);
        startActivity(intent);
    }

    //on click method to return to the home screen. clears all previous activities as pressing back on home screen returns sign in screen
    public void HomeOnClick(View view)
    {
        Intent intent = new Intent(ProfileActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    //on click method to open the friends screen
    public void FriendsOnClick(View view)
    {
        Intent intent = new Intent(ProfileActivity.this, FriendsActivity.class);
        startActivity(intent);
    }

    //overriding the on back pressed method
    @Override
    public void onBackPressed()
    {
        //if the user updates their avatar return to the home activity screen. ensures that all screens after use the new avatar image and prevents errors occurring
        if(avatarUpdated)
        {
            avatarUpdated = false;
            Intent intent = new Intent(ProfileActivity.this, HomeActivity.class);
            startActivity(intent);
        }
        //otherwise just return to the previous screen
        else
        {
            super.onBackPressed();
        }
    }

    //method to populate the viewed user's boulder problems on the view boulder problems recycler view
    public void PopulateRV()
    {
        RecyclerView rv = (RecyclerView) findViewById(R.id.ProfileViewProblemsRV);
        rv.setHasFixedSize(true);

        rv.setLayoutManager(new LinearLayoutManager(rv.getContext()));
        rv.setAdapter(new GeneralRVAdapter(viewBps));
    }

    //method to find the viewed user's boulder problems
    public void ViewUserProblemsSearch()
    {
        //creating a reference of the location where to fin boulder problems in the realtime database
        DatabaseReference bpRef = FirebaseDatabase.getInstance().getReference().child("BoulderProblems");
        //checking if a listener for the user's boulder problems has already been added.
        if(bpListener != null)
        {
            //remove the listener and clear the list to prevent boulder problems being duplicated on the screen
            viewBps.clear();
            bpRef.child("UsersCreated").removeEventListener(bpListener);
        }
        //creating boulder problem listener for the realtime database
        bpListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                //listens for each boulder problem/child node in the database
                //also listens for when new nodes are added

                //getting the boulder problem from the snapshot value
                BoulderProblem bp = snapshot.getValue(BoulderProblem.class);

                //checking the boulder problem is one of the viewed user's problems
                if(bp.GetSetterId().equals(viewUser))
                {
                    //set the display name of each boulder problem to be the viewed user's display name
                    bp.SetSetter(viewUserDisplayName);

                    //add the boulder problem to the array list
                    viewBps.add(bp);

                    //if the arraylist size is between 1 and 99 populate the screen with the boulder problems. prevents too many from being displayed
                    if(viewBps.size() > 0 && viewBps.size() < 100)
                    {
                        PopulateRV();
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                //listener checks for when boulder problems are removed from the database
                BoulderProblem bp = snapshot.getValue(BoulderProblem.class);

                //check boulder problem is one of viewed user's boulder problems
                if(bp.GetSetterId().equals(viewUser))
                {
                    //find the viewed user's boulder problem in the array list and remove it
                    for(int i = 0; i < viewBps.size(); i++)
                    {
                        if(viewBps.get(i).GetName().equals(bp.GetName()))
                        {
                            viewBps.remove(i);
                            PopulateRV();
                            break;
                        }
                    }
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        //attach the boulder problem listener to the boulder problems location in the realtime database. Order the query by boulder problem names
        bpRef.child("UserCreated").orderByChild("name").addChildEventListener(bpListener);
    }

    //method attach listener to retrieve all of current user's friends from the database
    public void RetrieveFriends()
    {
        //create child event listener for the friends list
        ChildEventListener friendsQuery = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                //for every friend node added get the key as the user id and add it list of friend ids
                String friendsId = snapshot.getKey();
                friends.add(friendsId);

                //check if user id is same as viewed user's id
                if(friendsId.equals(viewUser))
                {
                    //if so then set the friend button text to be remove friend option
                    friendButton.setText(R.string.remove_friend_button_text);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                //listens for when a friend is removed

                //get the user id of removed friend and remove it from the arraylist of friend ids
                String userId = snapshot.getKey();
                for(int i = 0; i < friends.size(); i++)
                {
                    if(friends.get(i).equals(userId))
                    {
                        friends.remove(i);
                    }
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        //attach listener to reference of friends list location in realtime database
        userRef.child(currentUser.getUid()).child("Friends").addChildEventListener(friendsQuery);
    }

    //method to retrieve all the requests sent by the currently signed in user
    public void RetrieveRequestsSent()
    {
        //creating child event listener to check for all requests sent in realtime database
        ChildEventListener reqSentQuery = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                //listens for existing or added child nodes

                //get the key as the user id and add to list of requests sent user ids
                String userId = snapshot.getKey();
                requestsSent.add(userId);

                //checking if the user id matches the currently viewed user's id
                if(userId.equals(viewUser))
                {
                    //if so set the friend button text to cancel friend request option
                    friendButton.setText(R.string.cancel_friend_request_button_text);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                //listens for requests sent being removed from the database

                //get user id of request sent removed and remove it from the list of request sent ids
                String userId = snapshot.getKey();
                for(int i = 0; i < requestsSent.size(); i++)
                {
                    if(requestsSent.get(i).equals(userId))
                    {
                        requestsSent.remove(i);
                    }
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        //attach listener to the reference of location for requests sent in the database
        userRef.child(currentUser.getUid()).child("Friend Requests Sent").addChildEventListener(reqSentQuery);
    }

    //method to retrieve all the requests received by the currently signed in user
    public void RetrieveRequestsReceived()
    {
        //create a child event listener to get all requests received
        ChildEventListener reqReceivedQuery = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                //listens for existing or newly added requests received

                //get the key as the user id and add to list of requests received ids
                String userId = snapshot.getKey();
                requestsReceived.add(userId);

                //checking if the currently viewed user's id matches retrieved user id
                if(userId.equals(viewUser))
                {
                    //if so set the friend button to the accept request option
                    friendButton.setText(R.string.accept_request_button_text);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                //listens for requests received being removed from the database

                //get the key as the user id and remove it from the requests received list
                String userId = snapshot.getKey();
                for(int i = 0; i < requestsReceived.size(); i++)
                {
                    if(requestsReceived.get(i).equals(userId))
                    {
                        requestsReceived.remove(i);
                    }
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        //attach listener to the reference of location for requests received
        userRef.child(currentUser.getUid()).child("Friend Requests").addChildEventListener(reqReceivedQuery);
    }

    //on click method for the friend button (only clickable when viewing another user's profile)
    public void FriendRequestOnClick(View view)
    {
        //check the user is viewing another user's profile
        if(viewUser != null)
        {
            //get the view clicked as a button, set it to be temporarily invisible
            Button requestBtn = (Button) view;
            requestBtn.setVisibility(View.INVISIBLE);

            //check if button is set as send friend request option
            if(requestBtn.getText().toString().equals(getString(R.string.send_friend_request_button_text)))
            {
                //create a reference for the current user's friend requests sent list in the database
                DatabaseReference friendRef = FirebaseDatabase.getInstance().getReference().child("ascentusers").child(currentUser.getUid()).child("Friend Requests Sent").child(viewUser);
                //add a new node with the viewed user's id in it as the key
                friendRef.setValue("Sent").addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //create a reference for the viewed user's requests received list in the database
                        DatabaseReference viewUserRef = FirebaseDatabase.getInstance().getReference().child("ascentusers").child(viewUser).child("Friend Requests").child(currentUser.getUid());
                        //add a new node in their requests received list with current user's id as key
                        viewUserRef.setValue("Received").addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                //notify user request has been sent successfully
                                Toast.makeText(ProfileActivity.this, "Friend Request Sent", Toast.LENGTH_LONG).show();
                                //set the button to the cancel friend request option and make it visible again
                                requestBtn.setText(R.string.cancel_friend_request_button_text);
                                requestBtn.setVisibility(View.VISIBLE);

                            }
                        });
                    }
                });
            }
            //check if button is set as cancel friend request option
            else if(requestBtn.getText().equals(getString(R.string.cancel_friend_request_button_text)))
            {
                //create a reference for the viewed user's friend requests received list
                DatabaseReference viewUserRef = FirebaseDatabase.getInstance().getReference().child("ascentusers").child(viewUser).child("Friend Requests").child(currentUser.getUid()).getRef();
                //remove the node for the current user from that location in the database
                viewUserRef.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //create a reference for the current user's friend requests sent list
                        DatabaseReference currentUserRef = FirebaseDatabase.getInstance().getReference().child("ascentusers").child(currentUser.getUid()).child("Friend Requests Sent").child(viewUser).getRef();
                        //remove the node for the viewed user from the that location in the database
                        currentUserRef.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                //notify user that request sent has been cancelled
                                Toast.makeText(ProfileActivity.this, "Friend Request Cancelled", Toast.LENGTH_LONG).show();
                                //set the button to the send friend request option and make it visible again
                                requestBtn.setText(R.string.send_friend_request_button_text);
                                requestBtn.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                });
            }
            //check if button is set a remove friend option
            else if(requestBtn.getText().equals(getString(R.string.remove_friend_button_text)))
            {
                //create a reference for the viewed user's friends list
                DatabaseReference viewUserRef = FirebaseDatabase.getInstance().getReference().child("ascentusers").child(viewUser).child("Friends").child(currentUser.getUid()).getRef();
                //remove the node for the current user from that location in the database
                viewUserRef.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //create a reference for the current user's friends list in the database
                        DatabaseReference currentUserRef = FirebaseDatabase.getInstance().getReference().child("ascentusers").child(currentUser.getUid()).child("Friends").child(viewUser).getRef();
                        //remove the node for the viewed user from that location in the database
                        currentUserRef.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                //notify the current user that the viewed user has been removed as a friend
                                Toast.makeText(ProfileActivity.this, "User Removed from Friends", Toast.LENGTH_LONG).show();
                                //set the button to the send friend request option and make it visible again
                                requestBtn.setText(R.string.send_friend_request_button_text);
                                requestBtn.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                });
            }
            //check if button is set a accept friend request button
            else if(requestBtn.getText().equals(getString(R.string.accept_request_button_text)))
            {
                //create reference for the current user's friend requests received list
                DatabaseReference reqRecRef = FirebaseDatabase.getInstance().getReference().child("ascentusers").child(currentUser.getUid()).child("Friend Requests").child(viewUser).getRef();
                //remove the node for the viewed user from that location in the database
                reqRecRef.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //create a reference for the viewed user's friend requests sent list
                        DatabaseReference reqSentRef = FirebaseDatabase.getInstance().getReference().child("ascentusers").child(viewUser).child("Friend Requests Sent").child(currentUser.getUid()).getRef();
                        //remove the node for the current user from that location in the database
                        reqSentRef.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                //create a reference for the viewed user's friends list
                                DatabaseReference friendRef = FirebaseDatabase.getInstance().getReference().child("ascentusers").child(viewUser).child("Friends").child(currentUser.getUid());
                                //add a new node for the current user in the viewed user's friends list
                                friendRef.setValue("Friend").addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        //create a reference for the current user's friends list
                                        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("ascentusers").child(currentUser.getUid()).child("Friends").child(viewUser);
                                        //add a new node for the viewed user in the current user's friends list
                                        userRef.setValue("Friend").addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                //notify the user they successfully accepted the request
                                                Toast.makeText(ProfileActivity.this, viewUserDisplayName + " Added as a Friend!", Toast.LENGTH_LONG).show();
                                                //set the button to the remove option and make it visible again
                                                requestBtn.setText(R.string.remove_friend_button_text);
                                                requestBtn.setVisibility(View.VISIBLE);
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                });
            }
        }
    }

    //on click method for the current user's profile
    public void ProfileOnClick(View view)
    {
        Intent intent = new Intent(ProfileActivity.this, ProfileActivity.class);
        startActivity(intent);
    }
}