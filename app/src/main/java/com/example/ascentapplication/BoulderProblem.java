package com.example.ascentapplication;

import android.provider.ContactsContract;

import androidx.annotation.NonNull;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//Class for Boulder Problem objects to be used in the app and saved to the realtime database
public class BoulderProblem implements Serializable
{
    //properties of a Boulder Problem
    public String name;                                                                             //boulder problem name
    public String setter;                                                                           //setter display name
    public String setterId;                                                                         //setter user id
    public String grade;                                                                            //boulder problem grade
    public ArrayList<Hold> route;                                                                   //boulder problem route
    public String algorithm;                                                                        //string indicating if boulder problem created with recommended grade
    public String setup;                                                                            //the setup on which boulder problem was created

    //Empty constructor to allow Boulder problem objects to be saved to the realtime database
    public BoulderProblem()
    {
    }

    //full constructor
    public BoulderProblem(String Name, String SetterId, String Grade, ArrayList<Hold> Route, String Algorithm, String Setup)
    {
        setterId = SetterId;
        name = Name;
        grade = Grade;
        route = Route;
        algorithm = Algorithm;
        setup = Setup;
    }


    //method to get the list of holds in the route as list of strings instead
    public ArrayList<String> GetRouteAsStringList()
    {
        ArrayList<String> listRoute = new ArrayList<>();
        for(Hold hold : route)
        {
            listRoute.add(hold.GetColumn() + hold.GetRow() + hold.GetType());
        }
        return listRoute;
    }

    //method to convert a list of strings reprensenting holds in a route to a list of hold objects
    public void ConvertRouteStringList(ArrayList<String> listRoute)
    {
        route = new ArrayList<>();
        for(String s : listRoute)
        {
            route.add(new Hold(s.substring(0,1), s.substring(1, s.length()-1), s.substring(s.length()-1)));
        }
    }

    //getters and setters
    public String GetName()
    {
        return name;
    }

    public String GetSetter()
    {
        return setter;
    }

    public String GetSetterId() {return setterId;}

    public String GetGrade()
    {
        return grade;
    }

    public ArrayList<Hold> GetRoute()
    {
        return route;
    }

    public String GetAlgorithm() { return algorithm; }

    public String GetSetup() { return setup; }

    public void SetName(String Name)
    {
        name = Name;
    }

    public void SetSetter(String Setter)
    {
        setter = Setter;
    }

    public void SetSetterId(String SetterId) { setterId = SetterId; }

    public void SetGrade(String Grade)
    {
        grade = Grade;
    }

    public void SetRoute(ArrayList<Hold> Route)
    {
        route = Route;
    }

    public void SetAlgorithm(String Algorithm) { algorithm = Algorithm; }

    public void SetSetup(String Setup) { setup = Setup; }
}
