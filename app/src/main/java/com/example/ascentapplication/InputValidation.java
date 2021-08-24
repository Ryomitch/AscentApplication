package com.example.ascentapplication;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

//Input Validation class contains methods for validating user input throughout the app
public class InputValidation
{
    //empty constructor for the class
    public InputValidation()
    {

    }

    //validation method for emails using Regular expressions
    /*
    Input:
    String email: the email address to be validated
     */
    public Boolean ValidateEmailInput(String email)
    {
        //String emailPattern = "^[a-zA-Z0-9_#$%&’*+/=?^.-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";

        String emailPattern = "^[a-zA-Z0-9_#$%&’*+/=?^.-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";  //defining the pattern for how email addresses must be formed using regular expression
        /*
        -  ^[a-zA-Z0-9_#$%&’*+/=?^.-]+    -    ^ is used to indicate the starting character of the email. Characters can be anywhere between a-z, A-Z, 0-9, and also some special symbols like "_, #, $, %" etc. + indicates 1 or more of these
        -  (?:\\.[a-zA-Z0-9_+&*-]+)*      -    this is the same as above, just stating again any of these characters can be additionally used (? indicates zero or more).
        -  @(?:[a-zA-Z0-9-]+\\.)+         -    name part is followed by an @ symbol. then again
        -  [a-zA-Z]{2,7}                  -    [] indicates the character set, {2, 7} matches the preceded at least 2 times but no more than 7 times i.e. the length of the top level domain
        -  $                              -    indicates end of string
         */

        Pattern pat = Pattern.compile(emailPattern);        //using a Pattern object to compile the string pattern as a regular expression

        if(email == null)                                   //checking if the email is empty
        {
            return false;
        }

        Matcher mat = pat.matcher(email);                   //using a Matcher object to compare the email to the pattern
        return mat.matches();                               //return the result of the comparison
    }

    //validation method for passwords using Regular expressions
    public Boolean ValidatePasswordInput(String password)
    {
        String passwordPattern = "^(?=.*[0-9])"             //at least one digit
                + "(?=.*[a-z])(?=.*[A-Z])"                  //at least one lower case and at least one upper case
                + "(?=.*['£!_?@#$%^&+=*.])"                  //at least one of any of these special characters
                + "(?=\\S+$).{8,20}$";                      //no white spaces and between 8 and 20 characters

        Pattern pat = Pattern.compile(passwordPattern);     //using a Pattern object to compile the string pattern as a regular expression

        if(password == null)                                //checking if the password is empty/null
        {
            return false;
        }

        Matcher mat = pat.matcher(password);                //using a Matcher object to compare the password to the pattern
        return mat.matches();                               //return the result of the comparison
    }

    public boolean PasswordDigitValidation(String password)
    {
        String passwordPattern =  "^(?=.*[0-9].*)";

        Pattern pat = Pattern.compile(passwordPattern);

        return pat.matcher(password).find();
    }

    public boolean PasswordLowerUpperCaseValidation(String password)
    {
        String passwordPattern = "(?=.*[a-z])(?=.*[A-Z])";

        Pattern pat = Pattern.compile(passwordPattern);

        return pat.matcher(password).find();
    }

    public boolean PasswordSpecialCharValidation(String password)
    {
        String passwordPattern = "(?=.*['£!_?@#$%^&+=*.])";

        Pattern pat = Pattern.compile(passwordPattern);

        return pat.matcher(password).find();
    }

    public boolean PasswordSpaceAndLengthValidation(String password)
    {
        String passwordPattern = "(?=\\S+$).{8,20}$";

        Pattern pat = Pattern.compile(passwordPattern);

        Matcher mat = pat.matcher(password);

        return mat.matches();
    }

    //Input validation method for User display name input
    public boolean ValidateDisplayNameInput(String displayName)
    {
        //checking if the display is between 5 and 20 characters long
        if(displayName.length() < 3 || displayName.length()>20)
        {
            return false;
        }
        return true;
    }

    //validating Boulder problem name input
    public boolean ValidateProblemNameInput(String problemName)
    {
        //checking problem name is between 3 and 20 characters long
        if(problemName.length() < 3 || problemName.length() > 20)
        {
            return false;
        }
        return true;
    }

}
