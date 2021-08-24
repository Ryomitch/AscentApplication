package com.example.ascentapplication;

//Hold class for representing holds in boulder problems
public class Hold
{
    /*Properties*/
    //not private to enable them to be stored in firebase database
    public String column;                                      //the hold column
    public String row;                                          //the hold row
    public String type;                                         //the hold type

    //empty constructor
    public Hold()
    {

    }

    //full constructor
    public Hold(String Column, String Row, String Type)
    {

        column = Column;
        row = Row;
        type = Type;
    }


    /* Getters and Setters */
    public String GetHold()
    {
        String fullHold = column + row + type;
        return fullHold;
    }

    public String GetColumn()
    {
        return column;
    }
    public String GetRow()
    {
        return row;
    }
    public String GetType()
    {
        return type;
    }
    public void SetColumn(String Column)
    {
        column = Column;
    }
    public void SetType(String Type)
    {
        type = Type;
    }
    public void SetRow(String Row)
    {
        row = row;
    }
}
