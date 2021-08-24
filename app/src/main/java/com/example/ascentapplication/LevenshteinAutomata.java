package com.example.ascentapplication;

import android.nfc.tech.NfcA;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.automatalib.automata.Automaton;
import net.automatalib.automata.base.fast.AbstractFastMutable;
import net.automatalib.automata.base.fast.AbstractFastMutableNondet;
import net.automatalib.automata.base.fast.AbstractFastState;
import net.automatalib.automata.fsa.NFA;
import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.FastNFAState;
import net.automatalib.words.Alphabet;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class LevenshteinAutomata
{
    /* private class attributes */
    private ArrayList<Hold> routeA;                         //Arraylist to for the first route
    private ArrayList<Hold> routeB;                         //Arraylist for the second route
    private Alphabet<String> alphabet;                      //Arraylist to hold the alphabet of allowed holds. i.e. the holds that are on the setup
    private int maxDistance = 5;                            //The max number of errors allowed for an accepting state in the automaton
    private State acceptingState;                           //State object to hold the max accepting state of the automaton
    private State acceptedState = null;                     //State object to hold the first accepted state of the automaton

    /*Arraylist of Arraylist of States to hold all states in automaton.
    Each arraylist of states represents a row in the automaton with a relevant error value.
    i.e. the first row of states will all have error of 0, second row all states have error of 1 etc..
     */
    private ArrayList<ArrayList<State>> allStates;

    private List<String> columns = Arrays.asList("a","b","c","d","e","f","g","h","i","j","k");      //list holding all the possible columns for a hold
    private HashMap<String, Integer> lettertoValue = new HashMap<String, Integer>();                              //Hashmap to hold the integer values for each column

    //Class constructor
    /* Parameters */
    // - RouteA: the arraylist representing the first route
    // - RouteB: the arraylist representing the second route
    // - setup: the arraylist representing the holds in the setup
    public LevenshteinAutomata(ArrayList<Hold> RouteA, ArrayList<Hold> RouteB, ArrayList<Hold> setup)
    {
        /* setting the routeA and routeB attributes*/
        routeA = RouteA;
        routeB = RouteB;

        ColumnsToValues();                                              //Calling method to define the integer value of each column
        //CreateAlphabet(setup);                                          //calling method to create the automaton alphabet
        acceptingState = new State(routeA.size(), maxDistance);         //setting the max accepting state, with the last position in the first route and distance equal to the max error value
        CreateStatePath();                                              //calling the method to create the automaton
    }

    //Method to set integer values for each column in a hashmap
    private void ColumnsToValues()
    {
        lettertoValue.put("a", 1);
        lettertoValue.put("b", 2);
        lettertoValue.put("c", 3);
        lettertoValue.put("d", 4);
        lettertoValue.put("e", 5);
        lettertoValue.put("f", 6);
        lettertoValue.put("g", 7);
        lettertoValue.put("h", 8);
        lettertoValue.put("i", 9);
        lettertoValue.put("j", 10);
        lettertoValue.put("k", 11);
    }

    //Method to check if the second route was accepted
    public boolean RouteAccepted()
    {
        boolean accepted = false;
        if(acceptedState != null)
        {
            accepted = true;
        }
        return accepted;
    }

    //Method to return the first accepted state
    public State GetAcceptedState()
    {
        return acceptedState;
    }

    /*Method to create the path of states till the second route is accepted or rejected.
    Produces all states up to first/minimum accepting state,
    or if there is no accepting state then all states till route is determined as not accepted.
     */
    private void CreateStatePath()
    {
        allStates = new ArrayList<>();                                              //setting the empty list of all states
        State startState = new State();                                             //creating the start state with position 0, distance 0
        ArrayList<State> distanceZero = new ArrayList<>();                          //Creating the first state row for all states of distance 0
        distanceZero.add(startState);                                               //adding start state to the first row
        allStates.add(distanceZero);                                                //adding the first row to list of all states
        ArrayList<State> currentStates = new ArrayList<>();                         //creating a list to hold all current/recently created states
        currentStates.add(startState);                                              //adding the start state to list of current states
        for(Hold nextHold : routeB)                                                 //loop through each hold in routeB
        {
            ArrayList<State> newStates = new ArrayList<>();                         //create an arraylist for new states created by transitioning from the current state
            for(State currentState : currentStates)                                 //loop through the current states
            {
                currentState.SetNextStates(Transition(currentState, nextHold));     //call the transition method to get the next states produced from the current state
                if(RouteAccepted())                                                 //call method to check whether a state produced is an accepting state
                {
                    break;                                                          //if so break
                }
                newStates.addAll(currentState.GetNextStates());                     //add all the next states produced from the current state to the new states list
            }
            if(RouteAccepted())                                                     //again check if a state is accepted in order to break from checking the whole route when an accepting state is found
            {
                break;
            }
            currentStates = newStates;                                              //set the current states to be the list of new states
        }
    }

    /*method to output the states row by row as in a state graph
    * needs to be configured to output states in correct order and from top to bottom
    * */
    public void OutputStateGraph()
    {
        System.out.println("State Graph:");
        for(ArrayList<State> statesRow : allStates)
        {
            for(State state : statesRow)
            {
                System.out.print(state.GetPosition() + " : " + state.GetCurrentDistance() + " . ");
            }
            System.out.println(" Row End.");
        }
    }

    //method to carry out all possible transitions from a state and get the resulting states
    /* Parameters
    *   - currentState: the state to transition from
    *   - nextHold: the current hold being checked in the automata
     */
    private ArrayList<State> Transition(State currentState, Hold nextHold)
    {
        ArrayList<State> nextStates = new ArrayList<>();                    //creating the arraylist of resulting states to be returned
        int pos = currentState.GetPosition();                               //getting the current state position
        Hold routeAHold = routeA.get(pos);                                  //getting the hold in the route at that position
        String nextCheck = nextHold.GetHold();                              //getting the string form of the hold from routeB being checked
        String nextSymbol = routeAHold.GetHold();                           //getting the string form of the hold a the current position
        if(nextSymbol.equals(nextCheck))                                    //checking if the holds match
        {
            /* if the holds match then state achieved through identity as creating as resulting state
            * the state achieved through identity will have position increased by 1 but distance/error will remain the same
            * */
            State nextStateHorizontal = new State(pos+1, currentState.GetCurrentDistance());
            /* Call method to check whether the state is a new state and if so add it to list of all states*/
            if(AddToStatesRow(nextStateHorizontal))
            {
                /* checking if the resulting state is an accepting state */
                /* if so set the first accepting state to this state*/
                if(IsAcceptingState(nextStateHorizontal))
                {
                    acceptedState = nextStateHorizontal;
                    nextStates.add(nextStateHorizontal);          //add the new state to the list of resulting states outputted by this method
                    return nextStates;                            //skip other transitions and return the list of resulting states
                }
                nextStates.add(nextStateHorizontal);          //add the new state to the list of resulting states outputted by this method
            }

        }
        //calculate the error of insertion, deletion or substitution and add it to the current distance
        //To standardize the error value divide by 10 and round up
        int newDistance = currentState.GetCurrentDistance() + (int) Math.ceil(CalcHoldDiff(routeAHold, nextHold) / 10);
        if(newDistance<=maxDistance)                                                //checking the new distance is not above the max error/distance
        {
            //create the state achieved through deletion or substitution using the new distance and position increased by 1
            State nextStateDiagonal = new State(pos+1, newDistance);
            //check if the resulting state is a new state and if so add it to the list of all states
            if(AddToStatesRow(nextStateDiagonal))
            {
                //checking if this resulting state is accepting
                if(IsAcceptingState(nextStateDiagonal))
                {
                    acceptedState = nextStateDiagonal;
                    nextStates.add(nextStateDiagonal);      //add the new state to the list of resulting states outputted by this method
                    return nextStates;                      //skip other transitions and return the list of resulting states
                }
                nextStates.add(nextStateDiagonal);      //add the new state to the list of resulting states outputted by this method
            }
            //state achieved through insertion
            State nextStateVertical = new State(pos, newDistance);
            //check if the resulting state is a new state and if so add it to the list of all states
            if(AddToStatesRow(nextStateVertical))
            {
                nextStates.add(nextStateVertical);      //add the new state to the list of resulting states outputted by this method
            }
        }
        return nextStates;                              //return list of resulting states
    }

    /*Method to calculate the difference between 2 holds
    * hold difference is based on the actual distance between the holds plus the difference in the hold type/difficulty
    * Parameters:
    *  - holdA: the first hold
    *  - holdB: the second hold
     */
    private double CalcHoldDiff(Hold holdA, Hold holdB)
    {
        double holdDiff = 0.00;                                                                 //setting default hold difference
        /* getting the rows as integer values for both holds */
        int aRow = Integer.parseInt(holdA.GetRow());
        int bRow = Integer.parseInt(holdB.GetRow());

        /* getting the types as integer values for both holds */
        int aType = GetHoldTypeDifficulty(holdA);
        int bType = GetHoldTypeDifficulty(holdB);

        /* getting the columns as integer values for both holds */
        int aCol = lettertoValue.get(holdA.GetColumn());
        int bCol = lettertoValue.get(holdB.GetColumn());

        /* checking rows don't match */
        if(aRow != bRow)
        {
            double diffRow = Math.abs(aRow - bRow);                     //calculate the difference between the rows
            if(aCol != bCol)                                            //if the columns don't match
            {
                double diffCol = Math.abs(aCol - bCol);                                                 //calculate the difference between the columns
                double holdDistance = Math.sqrt(Math.pow(diffRow, 2) + Math.pow(diffCol, 2));           //use pythagoras theorem to calculate the distance between the holds
                if(aType != bType)                                                                      //if the hold types don't match
                {
                    double diffType = Math.abs(aType - bType);                                          //calculating the difference in hold type/difficulty
                    holdDiff = holdDistance + diffType;                                                 //calculating hold difference using distance and type difference values
                }
                else{
                    holdDiff = holdDistance;                                                            //otherwise hold difference is just the distance between the 2 holds
                }
            }
            else if(aType != bType)
            {
                double diffType = Math.abs(aType - bType);
                holdDiff = diffRow + diffType;                           //hold difference is distance between rows plus the difference in type/difficulty
            }
            else
            {
                holdDiff = diffRow;                                      //otherwise hold difference is simply the distance between the 2 rows
            }
        }
        else if(aCol != bCol)                      //checking if the columns don't match
        {
            double diffCol = Math.abs(aCol - bCol);                          //calculating distance between the columns
            if(aType != bType)                                               //if types don't match
            {
                double diffType = Math.abs(aType - bType);
                holdDiff = diffCol + diffType;                               //hold difference is the distance between the cols plus the difference in type/difficulty
            }
            else
            {
                holdDiff = diffCol;                                          //otherwise hold difference is the distance between the columns
            }
        }
        else
        {
            holdDiff = Math.abs(aType - bType);                              //otherwise the hold difference is just the difference between the hold types/difficulties
        }
        return holdDiff;                        //return hold difference
    }

    //Method to get the difficulty value of a Hold type
    /* Parameters
    * - hold : the hold to find the difficulty value of it's hold type
     */
    private int GetHoldTypeDifficulty(Hold hold)
    {
        int typeDifficulty = 0;
        if(Integer.parseInt(hold.GetRow()) <= 3)                            //if the hold is in one of the first 3 rows then the it is starting foothold so value is 1
        {
            typeDifficulty = 1;
        }
        else if(hold.GetType().equals("1"))                                 //if hold is type 1 (Large Jug) then value is 1
        {
            typeDifficulty = 1;
        }
        else if(hold.GetType().equals("2") || hold.GetType().equals("3"))   //if hold is type 2 or 3 (mini-jug or edge) then value is 2
        {
            typeDifficulty = 2;
        }
        else if(hold.GetType().equals("4"))                                 //if hold is type 4 (Sloper) then value is 3
        {
            typeDifficulty = 3;
        }
        else if(hold.GetType().equals("5"))                                 //if hold is type 5 (Pocket) then value is 4
        {
            typeDifficulty = 4;
        }
        else if(hold.GetType().equals("6"))                                 //if hold is type 6 (Pinch) then value is 5
        {
            typeDifficulty = 5;
        }
        else if(hold.GetType().equals("7"))                                 //if hold is type 7 (Crimp) then value is 6
        {
            typeDifficulty = 6;
        }
        return typeDifficulty;
    }

    //method to add states to list of all states if they are new states. returns boolean indicating whether the state was added or not
    /* Parameters
    * - newState: the state to be checked if it is new
     */
    private boolean AddToStatesRow(State newState)
    {
        boolean found = false;                                  //default boolean indicating state has not been found yet
        //checking if the size of allstates is equal less than or equal to the error value of newState
        //if so then the newState is a new state and rows must be addded to allstates as well
        if(allStates.size() <= newState.GetCurrentDistance())
        {
            //loop through and add empty rows to allStates except till it's size is equal to the error value of newState
            for(int i = allStates.size()-1; i < newState.GetCurrentDistance()-1; i++)
            {
                ArrayList<State> newDistanceRow = new ArrayList<>();
                allStates.add(newDistanceRow);
            }
            //now add a final row with newState included in it
            ArrayList<State> newDistanceRow = new ArrayList<>();
            newDistanceRow.add(newState);
            allStates.add(newDistanceRow);
            //this means that allstates now has a row for each error value up to (including) the error value of the new state
        }
        //otherwise no new rows need to be added
        else
        {
            //loop through all states and check if newState is indeed a new state
            for(State state: allStates.get(newState.GetCurrentDistance()))
            {
                if(state.GetPosition() == newState.GetPosition())
                {
                    found = true;                                   //indicate state has already been found
                    break;
                }
            }
            if(!found)                                               //if the state was not found then add it to the correct row
            {
                allStates.get(newState.GetCurrentDistance()).add(newState);
            }
        }
        //if the state was found then it was not added, if it was not found then it was added
        //therefore return the opposite of the found boolean value
        return !found;
    }

    //method to determine whether a state is an accepting state
    private boolean IsAcceptingState(State state)
    {
        //create a boolean with initial value false
        boolean accepting = false;
        //check if the position of the state matches the accepting state position and the distance is less than or equal to the distance of the accepting state
        if(state.GetPosition() == acceptingState.GetPosition() && state.GetCurrentDistance() <= acceptingState.GetCurrentDistance())
        {
            accepting = true;
        }
        return accepting;
    }

    //method to create an alphabet of all the possible holds in the automaton using the setup
    //this method is not needed as automaton as the array list setup already works as a the alphabet
    private void CreateAlphabet(ArrayList<Hold> setup)
    {
         alphabet = new Alphabet<String>()
         {
            ArrayList<Hold> holdsInAlphabet = new ArrayList<>();
            ArrayList<String> symbolAlphabet = new ArrayList<>();
            @Override
            public String getSymbol(int index) {
                return symbolAlphabet.get(index);
            }

            @Override
            public int getSymbolIndex(String symbol) {
                int index = -1;
                for(int i = 0; i < symbolAlphabet.size(); i++)
                {
                    if(symbolAlphabet.get(i).equals(symbol))
                    {
                        index = i;
                    }
                }
                return index;
            }

            @Override
            public boolean isEmpty() {
                if(symbolAlphabet.isEmpty())
                {
                    return true;
                }
                else
                {
                    return false;
                }
            }

            @Override
            public boolean contains(@Nullable Object o) {
                if(o.getClass().equals(Hold.class))
                {
                    String hold = ((Hold) o).GetHold();
                    for(String s : symbolAlphabet)
                    {
                        if(hold.equals(s))
                        {
                            return true;
                        }
                    }
                }
                else if(o.getClass().equals(String.class))
                {
                    for(String s : symbolAlphabet)
                    {
                        if(((String) o).equals(s))
                        {
                            return true;
                        }
                    }
                }
                return false;
            }

            @NonNull
            @Override
            public Iterator<String> iterator() {
                return symbolAlphabet.iterator();
            }

            @NonNull
            @Override
            public Object[] toArray() {
                return new Object[0];
            }

            @NonNull
            @Override
            public <T> T[] toArray(@NonNull T[] ts) {
                return null;
            }

            @Override
            public boolean add(String s) {
                if(contains(s))
                {
                    return false;
                }
                else
                {
                    Hold newHold = new Hold(s.substring(0,0), s.substring(1,1), s.substring(2,2));
                    if(newHold != null)
                    {
                        holdsInAlphabet.add(newHold);
                        symbolAlphabet.add(s);
                        return true;
                    }
                    else
                    {
                        return false;
                    }
                }
            }

            @Override
            public boolean remove(@Nullable Object o) {
                if(o.getClass().equals(Hold.class))
                {
                    if(contains(((Hold) o).GetHold()))
                    {
                        holdsInAlphabet.remove((Hold) o);
                        symbolAlphabet.remove(((Hold) o).GetHold());
                        return true;
                    }
                }
                else if(o.getClass().equals(String.class))
                {
                    String s = (String) o;
                    Hold newHold = new Hold(s.substring(0,0), s.substring(1,1), s.substring(2,2));
                    if(contains(s))
                    {
                        holdsInAlphabet.remove(newHold);
                        symbolAlphabet.remove(s);
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean containsAll(@NonNull Collection<?> collection) {
                if(collection.getClass().equals(Hold.class))
                {
                    Collection<Hold> holds = (Collection<Hold>) collection;
                    for(Hold hold : holds)
                    {
                        String holdAsString =  hold.GetHold();
                        if(!contains(holdAsString))
                        {
                            return false;
                        }
                    }
                    return true;
                }
                else if(collection.getClass().equals(String.class))
                {
                    Collection<String> symbols = (Collection<String>) collection;
                    for(String s : symbols)
                    {
                        if(!contains(s))
                        {
                            return false;
                        }
                    }
                    return true;
                }
                return false;
            }

            @Override
            public boolean addAll(@NonNull Collection<? extends String> collection) {
                for(String s : collection)
                {
                    Hold newHold = new Hold(s.substring(0,0), s.substring(1,1), s.substring(2,2));
                    if(newHold != null)
                    {
                        return false;
                    }
                    holdsInAlphabet.add(newHold);
                    symbolAlphabet.add(s);
                }
                return true;
            }

            @Override
            public boolean removeAll(@NonNull Collection<?> collection) {
                if(collection.getClass().equals(Hold.class))
                {
                    Collection<Hold> holds = (Collection<Hold>) collection;
                    for(Hold hold : holds)
                    {
                        if(contains(hold.GetHold()))
                        {
                            if(!remove(hold))
                            {
                                return false;
                            }
                        }
                    }
                    return true;
                }
                else if(collection.getClass().equals(String.class))
                {
                    Collection<String> symbols = (Collection<String>) collection;
                    for(String s : symbols)
                    {
                        if(contains(s))
                        {
                            if(!remove(s))
                            {
                                return false;
                            }
                        }
                    }
                    return true;
                }
                return false;
            }

            @Override
            public boolean retainAll(@NonNull Collection<?> collection) {
                if(collection.getClass().equals(Hold.class))
                {
                    Collection<Hold> holds = (Collection<Hold>) collection;
                    holdsInAlphabet.clear();
                    symbolAlphabet.clear();
                    for(Hold hold : holds)
                    {
                        holdsInAlphabet.add(hold);
                        symbolAlphabet.add(hold.GetHold());
                    }
                    return true;
                }
                else if(collection.getClass().equals(String.class))
                {
                    Collection<String> symbols = (Collection<String>) collection;
                    holdsInAlphabet.clear();
                    symbolAlphabet.clear();
                    for(String s : symbols)
                    {
                        Hold newHold = new Hold(s.substring(0,0), s.substring(1,1), s.substring(2,2));
                        holdsInAlphabet.add(newHold);
                        symbolAlphabet.add(s);
                    }
                    return true;
                }
                return false;
            }

            @Override
            public void clear() {
                removeAll(holdsInAlphabet);
            }

            @Override
            public int size() {
                return symbolAlphabet.size();
            }

            public void HoldToSymbol()
            {
                symbolAlphabet.clear();
                for(Hold hold : holdsInAlphabet)
                {
                    symbolAlphabet.add(hold.GetHold());
                }
            }
        };
    }
}
