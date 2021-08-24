package com.example.ascentapplication;

//Class for creating a record for individual boulder problems for the user and uploading to their record in the database
public class BPRecord {

    //public properties to be uploaded and accessed by firebase
    public String attempts;
    public String complete;

    //empty cosntructor required to use firebase
    public BPRecord()
    {

    }

    //full cosntructor
    public BPRecord(String Attempts, String Complete)
    {
        attempts = Attempts;
        complete = Complete;
    }

    //getters
    public String GetAttempts()
    {
        return attempts;
    }

    public String GetComplete()
    {
        return complete;
    }


}
