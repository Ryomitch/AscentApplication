package com.example.ascentapplication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

//State class to represent states in the levenshtein automaton
public class State
{
    /*private properties */
    private int position;
    private int currentDistance;
    private boolean expanded;
    private ArrayList<State> nextStates;

    //empty constructor for the state class
    public State()
    {
        position = 0;
        currentDistance = 0;
    }

    //full constructor
    public State(int Position, int newDistance)
    {
        currentDistance = newDistance;
        position = Position;
    }

    //getters and setters for the state class
    public int GetPosition()
    {
        return position;
    }

    public int GetCurrentDistance()
    {
        return currentDistance;
    }
    public boolean GetExpanded() { return expanded; }
    public ArrayList<State> GetNextStates() { return nextStates; }
    public void SetPosition(int Position)
    {
        position = Position;
    }
    public void SetDistance(int newDistance)
    {
        currentDistance = newDistance;
    }
    public void SetExpanded(boolean Expanded) { expanded = Expanded; }
    public void SetNextStates(ArrayList<State> NextStates) { nextStates = NextStates; }
    public void AddToNextStates(State nextState)
    {
        if(!NextStatesContains(nextState))
        {
            nextStates.add(nextState);
        }
    }
    private boolean NextStatesContains(State nextState)
    {
        boolean found = false;
        for(State state : nextStates)
        {
            if(state.GetCurrentDistance() ==  nextState.GetCurrentDistance() && state.GetPosition() == nextState.GetPosition())
            {
                found = true;
                break;
            }
        }
        return found;
    }
}
