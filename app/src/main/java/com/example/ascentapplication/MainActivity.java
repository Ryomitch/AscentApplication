package com.example.ascentapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

// MainActivity class is for the sign-in activity
public class MainActivity extends AppCompatActivity
{
    /* private properties */
    private FirebaseAuth mAuth;                                     //declaring instance of Firebase Authentication

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();                         //initializing the Firebase Authentication Instance
        setContentView(R.layout.activity_main);
    }

    //activity start method
    /* used to check user sign in for easy access when already signed in */
    public void onStart()
    {
        super.onStart();

        mAuth = FirebaseAuth.getInstance();                         //initializing the Firebase Authentication Instance
        FirebaseUser currentUser = mAuth.getCurrentUser();          //adding check to see if user is signed in (otherwise getCurrrentUser will be null)

        //if user is signed in then go straight to the login activity
        if(currentUser != null)
        {
            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
            startActivity(intent);
        }
    }

    //On click method for the sign in button
    public void SignInOnClick(View btn)
    {
        //getting the user sign in inputs
        TextInputEditText emailET = (TextInputEditText) findViewById(R.id.EmailET);
        TextInputEditText passwordET = (TextInputEditText) findViewById(R.id.PasswordET);
        String email = emailET.getText().toString().trim();
        String password = passwordET.getText().toString().trim();

        //creating a validation object using the InputValidation class
        InputValidation validator = new InputValidation();
        if(validator.ValidateEmailInput(email) && validator.ValidatePasswordInput(password))                                                           //using validator object to validate the user input, returns true if they are valid
        {
            mAuth = FirebaseAuth.getInstance();
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {                //using firebase to authenticate user sign in
                @Override
                public void onComplete(@NonNull Task<AuthResult> task)
                {
                    if(task.isSuccessful())                                                                                                             //if user is signed in successfully
                    {
                        FirebaseUser user = mAuth.getCurrentUser();                                                                                     //getting the signed in user
                        Toast.makeText(MainActivity.this, "User: " + user.getDisplayName() + " Signed In", Toast.LENGTH_SHORT).show();     //output toast notification to tell user they have signed in successfully

                        //Clearing user input from activity after sign in
                        emailET.setText("");
                        passwordET.setText("");

                        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                        startActivity(intent);                                                                                                          //starting the Home Activity
                    }
                    else                                                                                                                                //otherwise user authentication failed
                    {
                        //Display failure message when firebase authentication is unsuccessful
                        Toast.makeText(MainActivity.this, "Sign In Failed. Try again.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        else
        {
            Toast.makeText(getApplicationContext(), "Your email or password input is invalid", Toast.LENGTH_SHORT).show();                      //tell user input is invalid
        }
    }

    //on click method for the sign up link
    public void SignUpOnClick(View btn)
    {
        //starting the Create Account activity;
        Intent intent = new Intent(MainActivity.this, CreateAccountActivity.class);
        startActivity(intent);
    }

    //On click method for sending a password reset link
    public void ResetPasswordOnClick(View btn)
    {
        //getting the user email input
        TextInputEditText emailET = (TextInputEditText) findViewById(R.id.EmailET);
        String email = emailET.getText().toString().trim();
        InputValidation validator = new InputValidation();

        //checking the email input is valid
        if(validator.ValidateEmailInput(email))
        {
            //sending a password reset email
            mAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful())
                    {
                        //if successful then notify user to check email
                        Toast.makeText(MainActivity.this, "Reset password link sent. Check your email", Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        //otherwise notify user to check their email is correct
                        Toast.makeText(MainActivity.this, "Failed to send reset link. Email not recognised", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
        else
        {
            //notify user that the email input is invalid
            Toast.makeText(MainActivity.this, "Must have valid email address to reset password for!", Toast.LENGTH_LONG).show();
        }
    }
}