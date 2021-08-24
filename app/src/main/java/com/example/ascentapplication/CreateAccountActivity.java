package com.example.ascentapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

//CreateAccountActivity class for allowing users to create accounts on the app
public class CreateAccountActivity extends AppCompatActivity
{
    /* Private Properties */
    private FirebaseAuth mAuth;                                                                     //Firebase authentication object
    private InputValidation validator = new InputValidation();                                      //input validation object
    private Boolean[] inputChecks = new Boolean[4];                                                 //boolean array to hold results of input validation checks
    private TextInputEditText emailInput;                                                           //object for user email input view
    private TextInputEditText displayNameInput;                                                     //object for user display name input view
    private TextInputEditText createPasswordInput;                                                  //object for user password input view
    private TextInputEditText confirmPasswordInput;                                                 //object for user confirm password input view

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        mAuth = FirebaseAuth.getInstance();                                                         //getting the firebase authentication instance

        Arrays.fill(inputChecks, Boolean.FALSE);                                                    //setting default values for user input checks

        //finding the views for user input
        emailInput = (TextInputEditText) findViewById(R.id.CreateEmailET);
        displayNameInput = (TextInputEditText) findViewById(R.id.CreateDisplayNameET);
        createPasswordInput = (TextInputEditText) findViewById(R.id.CreatePasswordET);
        confirmPasswordInput = (TextInputEditText)findViewById(R.id.ConfirmPasswordET);

        //adding change listener to each of the input views
        AddEmailChangeListener();
        AddDisplayNameChangeListener();
        AddPasswordChangeListener();
        AddConfirmPasswordChangeListener();
    }

    //On click Method for create account button
    public void CreateAccount(View v)
    {
        //call method to check if user input is valid
        if(AllChecks())
        {
            //create dialog for privacy policy agreement
            Dialog ppDialog = new Dialog(this, R.style.MaterialBottomDialogSheet);
            ppDialog.setCancelable(true);
            ppDialog.setContentView(R.layout.privacy_policy_dialog);
            ppDialog.getWindow().setLayout(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
            ppDialog.getWindow().setGravity(Gravity.BOTTOM);

            //get the dialog accept checkbox and submit button
            CheckBox ppAcceptCB = ppDialog.findViewById(R.id.PrivacyPolicyCheckbox);
            Button ppSubmitBtn = ppDialog.findViewById(R.id.PrivacyPolicySubmitButton);
            //get the link textview
            TextView ppLinkTV = ppDialog.findViewById(R.id.PrivacyPolicyLinkTV);
            //set link movement method to open the link
            ppLinkTV.setMovementMethod(LinkMovementMethod.getInstance());
            //set new on click listener for submit button
            ppSubmitBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //check user has accepted the privacy policy
                    if(ppAcceptCB.isChecked())
                    {
                        //getting the user input
                        String email = emailInput.getText().toString().trim();
                        String password = createPasswordInput.getText().toString();

                        //getting user display name input
                        String displayName = displayNameInput.getText().toString();

                        //calling Firebase to create a new user with email and password, attaching an on completion listener
                        //create account method will also automatically sign user in
                        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(CreateAccountActivity.this, new OnCompleteListener<AuthResult>()
                        {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task)
                            {
                                if(task.isSuccessful())                                                                                                         //if user creation successful
                                {
                                    //notify user account created successfully
                                    Toast.makeText(CreateAccountActivity.this, "New User: " + displayName + " created successfully", Toast.LENGTH_SHORT).show();

                                    //get the current user and update their display name to the default display name created earlier
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    UserProfileChangeRequest displayChange = new UserProfileChangeRequest.Builder().setDisplayName(displayName).build();
                                    user.updateProfile(displayChange);

                                    /* setting a default user avatar using the profile icon stored in app resources */

                                    //creating a storage reference of where to store the user's default avatar using their user id
                                    FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
                                    StorageReference storageReference = firebaseStorage.getReference().child("User Avatars/" + user.getUid());

                                    //compressing the profile icon resource in  preparation for upload
                                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                                    Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.profile_icon);
                                    bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);

                                    //creating an upload task to upload the profile icon to the storage reference, attaching an on success listener
                                    UploadTask uploadTask = storageReference.putBytes(out.toByteArray());
                                    uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>()
                                    {
                                        @Override
                                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
                                        {
                                            //once image has been uploaded, get the url of the uploaded image resource from firebase storage
                                            storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                @Override
                                                public void onSuccess(Uri uri)
                                                {
                                                    //using the uri of the image from firebase storage set the user's photo uri
                                                    UserProfileChangeRequest newAvatar = new UserProfileChangeRequest.Builder().setPhotoUri(uri).build();
                                                    user.updateProfile(newAvatar);

                                                    //Clearing user inputs from activity
                                                    emailInput.setText("");
                                                    createPasswordInput.setText("");
                                                    confirmPasswordInput.setText("");

                                                    //setting the user display name in the realtime database also
                                                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("ascentusers").child(user.getUid());
                                                    userRef.child("DisplayName").setValue(user.getDisplayName()).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            //creating an empty favorites list location in the realtime database
                                                            userRef.child("Favorites").setValue("Favorites List").addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                @Override
                                                                public void onSuccess(Void aVoid) {
                                                                    //sending the user an verification email to their email address, attaching on complete listener
                                                                    user.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>()
                                                                    {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task)
                                                                        {
                                                                            if(task.isSuccessful())
                                                                            {
                                                                                //if verification email sent successfully notify the user
                                                                                Toast.makeText(CreateAccountActivity.this, "Verification Email Sent", Toast.LENGTH_SHORT).show();
                                                                                //starting the home activity
                                                                                Intent intent = new Intent(CreateAccountActivity.this, HomeActivity.class);
                                                                                startActivity(intent);
                                                                            }
                                                                            else {
                                                                                //otherwise notify the user the verification wasn't sent
                                                                                Log.e("sendEmailVerification", task.getException().toString());
                                                                                Toast.makeText(CreateAccountActivity.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                                                                            }
                                                                        }
                                                                    });
                                                                }
                                                            });

                                                        }
                                                    });
                                                }
                                            });

                                        }
                                    });
                                }
                                else
                                {
                                    //Account Creation failed
                                    Toast.makeText(CreateAccountActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                    else
                    {
                        //notify user they must accept the pp first
                        Toast.makeText(CreateAccountActivity.this, "You must accept the terms of our privacy policy before creating an account", Toast.LENGTH_LONG).show();
                    }
                }
            });
            ppDialog.show();
        }
    }

    //method to check if result of validating all input is successful or not
    private Boolean AllChecks()
    {
        //if first element of input checks is false then the email is invalid
        if(!inputChecks[0])
        {
            Toast.makeText(CreateAccountActivity.this, "Email is invalid", Toast.LENGTH_LONG).show();
            return false;
        }
        //else if the second element of input checks is false then display name is invalid
        else if(!inputChecks[1])
        {
            Toast.makeText(CreateAccountActivity.this, "Display name must between 3 and 20 characters long", Toast.LENGTH_LONG).show();
            return false;
        }
        //else if the third or fourth element is false then the password or confirm password is does not meet requirements
        else if(!inputChecks[2] || !inputChecks[3])
        {
            Toast.makeText(CreateAccountActivity.this, "Password must be between 8-20 characters long, with at least one lower case and at least one upper case. Must also have at least one number and one special character", Toast.LENGTH_LONG).show();
            return false;
        }
        else
        {
            //get the user password input
            String password = createPasswordInput.getText().toString();
            String confirm = confirmPasswordInput.getText().toString();
            if(!password.equals(confirm))                                                               //check new password and confirm password match
            {
                //if not notify user they don't match and return false
                Toast.makeText(CreateAccountActivity.this, "Passwords Not Matching", Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;                                                                                //otherwise all checks are fine so return true
        }
    }

    //method to add on-type listener to the email input textview
    private void AddEmailChangeListener()
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
                //using validator to check if email input is valid
                String email = emailInput.getText().toString().trim();
                if(validator.ValidateEmailInput(email))                                             //if valid set value to true and change text colour of email input to green to indicate it's valid
                {
                    emailInput.setTextColor(getColor(R.color.black));
                    //set first element of input checks to true
                    inputChecks[0] = true;
                }
                else                                                                                //otherwise set it's value to false and change text colour of email input to red to indicate it's invalid
                {
                    emailInput.setTextColor(getColor(R.color.red));
                    //set the first element of input checks to false
                    inputChecks[0] = false;
                }
            }
            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    //method to add an on type listener to the display name input textview
    private void AddDisplayNameChangeListener()
    {
        //add listener to display name input text view for on text changes
        displayNameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //get the display name input by user
                String displayName = displayNameInput.getText().toString();
                //use validation object to check if it's valid
                if(validator.ValidateDisplayNameInput(displayName))
                {
                    //otherwise change input back to black color indicating input is valid
                    displayNameInput.setTextColor(getColor(R.color.black));
                    displayNameInput.setTooltipText("Enter your name to be displayed to other users in the app");
                    inputChecks[1] = true;
                }
                else
                {
                    //otherwise change color to red to notify user display name invalid and set a tooltip also
                    displayNameInput.setTextColor(getColor(R.color.red));
                    displayNameInput.setTooltipText("Display name must be between 3 and 20 characters long");
                    inputChecks[1] = false;
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }


    //method to add an on type listener to the password input textview
    private void AddPasswordChangeListener()
    {
        //add listener to password input text view for text changes
        createPasswordInput.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                //when text changes
                //using validator to check if password input is valid
                String password = createPasswordInput.getText().toString();
                PasswordCheckTips(createPasswordInput, 2, password);
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    //method to add an on type listener to the confirm password input textview
    private void AddConfirmPasswordChangeListener()
    {
        //add listener to confirm password input text view
        confirmPasswordInput.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                //when text changes
                //using validator to check if confirm password input is valid
                String password = confirmPasswordInput.getText().toString();
                PasswordCheckTips(confirmPasswordInput, 3, password);
            }
            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    //method checks passwords for incorrect input and set tooltip accordingly
    /* Parameters:
     * - passwordET : the input box for the password
     * - password : the password to check
     */
    private void PasswordCheckTips(TextInputEditText passwordET, int boxNum, String password)
    {
        //check for at least one digit
        if(!validator.PasswordDigitValidation(password))
        {
            passwordET.setTooltipText("Need at least one digit. Press info button for more details");
            passwordET.setTextColor(getColor(R.color.red));
            inputChecks[boxNum] = false;
        }
        //check for at least one lower case and upper case char
        else if(!validator.PasswordLowerUpperCaseValidation(password))
        {
            passwordET.setTooltipText("Need at least one upper and one lower case character. Press info button for more details");
            passwordET.setTextColor(getColor(R.color.red));
            inputChecks[boxNum] = false;
        }
        //check for at least one special char
        else if(!validator.PasswordSpecialCharValidation(password))
        {
            passwordET.setTooltipText("Need at least one of these special characters: '£!_?@#$%^&+=*. \n Press info button for more details");
            passwordET.setTextColor(getColor(R.color.red));
            inputChecks[boxNum] = false;
        }
        //check no white spaces and length between 8 and 20 chars
        else if(!validator.PasswordSpaceAndLengthValidation(password))
        {
            passwordET.setTooltipText("No white spaces allowed and must between 8 and 20 characters long. \n Press info button for more details");
            passwordET.setTextColor(getColor(R.color.red));
            inputChecks[boxNum] = false;
        }
        else
        {
            //otherwise password valid
            passwordET.setTooltipText(null);
            passwordET.setTextColor(getColor(R.color.black));
            inputChecks[boxNum] = true;
        }
    }

    //On click method for displaying the password info dialog
    public void PasswordInfoOnClick(View view)
    {
        //creating the password info dialog with bottom pop up dialog style (from the resources)
        Dialog passwordInfoDialog = new Dialog(this, R.style.MaterialBottomDialogSheet);
        //set dialog to be closeable
        passwordInfoDialog.setCancelable(true);
        //set the content view of the dialog using the password info dialog layout
        passwordInfoDialog.setContentView(R.layout.password_info_dialog);
        //setting the height and width of the dialog
        passwordInfoDialog.getWindow().setLayout(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
        //set the gravity of the dialog to bottom so it appears from and stays on bottom of screen
        passwordInfoDialog.getWindow().setGravity(Gravity.BOTTOM);
        //get the close button from the dialog layout and set a new on click listener to dismiss the dialog
        Button closeInfoBtn = passwordInfoDialog.findViewById(R.id.PasswordInfoDialogCloseBtn);
        closeInfoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                passwordInfoDialog.dismiss();
            }
        });
        //show the dialog
        passwordInfoDialog.show();
    }
}