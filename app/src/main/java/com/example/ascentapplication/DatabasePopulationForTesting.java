package com.example.ascentapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

//class for populating the realtime database with more boulder problems to test grading algorithms on
/* mainly used as a testing class */
public class DatabasePopulationForTesting
{
    /* Private Properties */
    private FirebaseAuth mAuth;                                                 //firebase authentication object
    private FirebaseUser currentUser;                                           //firebase user object to hold currently signed in user
    private ArrayList<Hold> setupHolds = new ArrayList<>();                     //arraylist of holds for storing the setup holds
    private ArrayList<BoulderProblem> testBPs = new ArrayList<>();              //Arraylist of boulder problems for storing the boulder problems in the database used for testing
    private List<String> columns = Arrays.asList("a","b","c","d","e","f","g","h","i","j","k");      //list of strings for each column on the bouldering wall
    private HashMap<String, Integer> lettertoValue = new HashMap<String, Integer>();                              //a hashmap to contain values the will represent each column as an integer value

    //Constructor
    public DatabasePopulationForTesting()
    {
        //getting the current authentication instance and currently signed in user
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        GetSetup();
        AddTestBPListener();
    }

    //method to retrieve a setup from the realtime database to create testing boulder problems
    public void GetSetup()
    {
        //creating a child event listener for the setup in realtime database
        ChildEventListener setupEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                //get the snapshot value as a hold object and add it to list of setup holds
                Hold hold = snapshot.getValue(Hold.class);
                //checking the hold is not an empty hold
                if(!hold.GetType().equals("0"))
                {
                    setupHolds.add(hold);
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
        };
        //creating the reference for the realtime database in which the setup is stored
        DatabaseReference setupRef = FirebaseDatabase.getInstance().getReference().child("WallSetups").child("Algorithm Testing");
        //attaching the child event listener
        setupRef.addChildEventListener(setupEventListener);
    }

    //method to remove test problems from the realtime database
    public void RemoveTestProblems()
    {
        DatabaseReference testBPRef = FirebaseDatabase.getInstance().getReference().child("BoulderProblems").child("TestProblems");
        for(int i = 0; i < 1000; i++)
        {
            //set value of each test boulder problem to null to delete them
            testBPRef.child("BPTest" + i).setValue(null);
        }
    }

    //method to add a listener to realtime database to retrieve test boulder problems
    public void AddTestBPListener()
    {
        //child event listener to retrieve the boulder problems from the realtime database
        ChildEventListener testListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                //getting the snapshot value as a boulder problem object and adding it to the list of test boulder problems
                BoulderProblem testBP = snapshot.getValue(BoulderProblem.class);
                testBPs.add(testBP);
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
        };
        //creating the reference for the realtime database in which the test boulder problems are stored
        DatabaseReference testBpRef = FirebaseDatabase.getInstance().getReference().child("BoulderProblems").child("TestProblems");
        //attaching the child event listener
        testBpRef.addChildEventListener(testListener);
    }

    //method to check if a test boulder problem has already been created (i.e. the route is a duplicate and shouldn't be stored in the realtime database)
    /* Parameters
    * - testRoute : the route to check if it is a duplicate
     */
    public boolean TestContains(ArrayList<Hold> testRoute)
    {
        //creating boolean object to be returned with a default value of false
        boolean found = false;
        //loop through all the test boulder problems
        for(BoulderProblem bp : testBPs)
        {
            //if the size of a test route matches the route size of the argument object
            if(bp.GetRoute().size() == testRoute.size())
            {
                //creating a boolean object to indicate whether the routes match, default value of true
                boolean match = true;
                //loop through each hold in both routes and check if they match
                for(int i = 0; i < bp.GetRoute().size(); i++)
                {
                    if(!bp.GetRoute().get(i).GetHold().equals(testRoute.get(i).GetHold()))
                    {
                        //if a hold doesn't match then the routes don't match so set the boolean match to false and break
                        match = false;
                        break;
                    }
                }
                //checking if match is still true
                if(match)
                {
                    //if so then set found to true and break
                    found = true;
                    break;
                }
            }
        }
        //return the boolean value found
        return found;
    }

    //method to get a random integer between a specified min and max value (inclusive)
    /* Parameters
    * - min : the min value in the range
    * - max : the max value in the range
     */
    private int GetRandInt(int min, int max)
    {
        //creating a random object
        Random r = new Random();
        //get a random int below (the max - the min + 1) and add min to it, then return it
        return r.nextInt((max - min) + 1) + min;
    }

    //method to get a hold from the setup by a column and row
    /* Parameters
    * - col: the column to check for
    * - row : the row to check for
     */
    private Hold GetHoldByColAndRow(String col, String row)
    {
        //creating a null hold object
        Hold foundHold = null;
        //loop through all the setup holds
        for(Hold hold : setupHolds)
        {
            //checking if the hold column and row matches the arguments col and row
            if(hold.GetColumn().equals(col) && hold.GetRow().equals(row))
            {
                //if so then set found hold to be that hold and break
                foundHold = hold;
                break;
            }
        }
        //return the found hold
        return foundHold;
    }

    //method to set each column as an integer value in the hashmap lettertoValue
    public void SetColValues()
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

    //method to populate the realtime database with test boulder problems
    public void PopulateBPs()
    {
        //call method to set the column values
        SetColValues();

        //creating a reference for storing the test boulder problems in the realtime database
        DatabaseReference testBPRef = FirebaseDatabase.getInstance().getReference().child("BoulderProblems").child("TestProblems");

        //loop a specific number of times to create a specific number of test boulder problems
        for(int i = 0; i < 100; i++)
        {
            //create the starting holds
            ArrayList<Hold> startHolds = ProduceStartingHolds();

            //Create the middle holds
            ArrayList<Hold> bpTestRoute = ProduceMidHolds(startHolds);

            //create the final holds
            bpTestRoute.add(ProduceFinalHold(bpTestRoute));

            //check if the created route is a duplicate
            if(!TestContains(bpTestRoute))
            {
                //if not then grade the route
                String testGrade = BenchmarkGradingAlgorithm(bpTestRoute);

                //check if this grade has already been created a certain number of times, so that test boulder problems are more spread out among the grades
                if(testBpsGradesMax(testGrade) < 10)
                {
                    //create a test boulder problem object and uploaded it to the realtime database
                    BoulderProblem bpTest = new BoulderProblem("BPTest" + i, currentUser.getUid(), testGrade, bpTestRoute, "No", "Algorithm Testing");
                    testBPRef.child(bpTest.GetName()).setValue(bpTest);
                    testBPs.add(bpTest);
                }
                else
                {
                    i = i -1;
                }
            }
            else
            {
                i = i -1;
            }
        }
    }


    //method produces the number of created test boulder problems that have a specified grade
    private int testBpsGradesMax(String grade)
    {
        int gradeCount = 0;
        for(BoulderProblem bp : testBPs)
        {
            if(bp.GetGrade().equals(grade))
            {
                gradeCount += 1;
            }
        }
        return gradeCount;
    }

    //method to create the starting holds for a test route
    private ArrayList<Hold> ProduceStartingHolds()
    {
        //empty arraylist of holds
        ArrayList<Hold> startingHolds = new ArrayList<>();

        //creating a null first hold object
        Hold firstHold = null;
        //while the first hold is null
        while(firstHold == null)
        {
            //get a random column and row value
            int firstColInd = GetRandInt(0,10);
            String firstCol = columns.get(firstColInd);
            int firstRowInt = GetRandInt(0, 5);
            String firstRow = String.valueOf(firstRowInt);
            //get the hold using the column and row values
            //if no hold is found then first hold will still be null and the loop will continue
            firstHold = GetHoldByColAndRow(firstCol, firstRow);
        }
        //add first hold to the arraylist
        startingHolds.add(firstHold);

        //do same again for the second hold
        Hold secondHold = null;
        while(secondHold == null)
        {
            int secondColInd = GetRandInt(0, 10);
            String secondCol = columns.get(secondColInd);
            //second hold row must be on same row or higher than the first hold
            int secondRowInt = GetRandInt(Integer.parseInt(firstHold.GetRow()), 5);
            String secondRow = String.valueOf(secondRowInt);
            secondHold = GetHoldByColAndRow(secondCol, secondRow);
            //a check to make sure the second hold does not match the first hold
            if(secondHold != null)
            {
                if(firstHold.GetHold().equals(secondHold.GetHold()))
                {
                    secondHold = null;
                }
            }
        }
        startingHolds.add(secondHold);

        //return the starting holds
        return startingHolds;
    }

    //method to create the middle holds
    /* Parameters
    * - currentHolds : the arraylist containing holds already made for the test route
     */
    private ArrayList<Hold> ProduceMidHolds(ArrayList<Hold> currentHolds)
    {
        //get the index of the last created hold. To ensure that the next hold created on a row higher than the last hold
        int prevRowInd = Integer.parseInt(currentHolds.get(1).GetRow());
        //loop through to each of the next rows until the second to last row
        for(int i = prevRowInd+1; i < 18; i++)
        {
            //creating a row distance value by getting a random integer between 0 and 3
            //this is to add variability between test routes by making distances between holds varied by column and row
            int rowDist = GetRandInt(0, 3);
            //checking i + row distance is not greater than or equal to 18 (the last row index)
            if(i + rowDist >= 18)
            {
                //if so then -1 off row distance
                rowDist = (18-i)-1;
            }
            //set i to be i + the row distance
            i = i + rowDist;

            //creating a null middle hold
            Hold midHold = null;
            //while the middle hold is null
            while (midHold == null)
            {
                //get the column value
                int colInd = GetRandInt(0, 10);
                String col = columns.get(colInd);
                //get a hold by the column value and the value of i (which will act as the row index)
                midHold = GetHoldByColAndRow(col, String.valueOf(i));
                //if middle hold is no longer null
                if(midHold != null)
                {
                    //check the middle hold has not already been added to the test route
                    if(currentHolds.get(currentHolds.size()-1).GetHold().equals(midHold.GetHold()))
                    {
                        //if it has then set middle hold to null
                        midHold = null;
                    }
                }
            }
            //add the middle hold to the list of current holds in the test route
            currentHolds.add(midHold);
        }
        //return the list of current holds in the test route
        return currentHolds;
    }

    //method to produce the final hold in a test route
    /* Parameters
    * - currentHolds : the current holds already in the test route
     */
    private Hold ProduceFinalHold(ArrayList<Hold> currentHolds)
    {
        //create a null final hold object
        Hold finalHold = null;
        //while final hold is null
        while(finalHold == null)
        {
            //get a random column value
            int colInd = GetRandInt(0, 10);
            String col = columns.get(colInd);
            //find a hold using the column value and the row 18 (last row)
            finalHold = GetHoldByColAndRow(col, "18");
            //if a hold is found the final hold will no longer be null and will break the loop
        }
        //returning the final hold
        return finalHold;
    }

    //method to grade route
    //Was used to grade benchmarks previously hence the method name
    /* Parameters
    * - routeA : the arraylist of holds that represents the test route
     */
    public String BenchmarkGradingAlgorithm(ArrayList<Hold> routeA)
    {
        //set a default grade value of 0 (invalid grade)
        String grade = "0";

        //call method to get the average hold distance of the test route
        double avgHoldDistance = GetAverageHoldDistance(routeA);
        //call method to get the average hold difficulty of the rest route
        double avgHoldDifficulty = GetAverageHoldDifficulty(routeA);
        //creating a difficulty value for the test route using the average hold distance plus the average hold difficulty
        double difficultyValue = avgHoldDistance + avgHoldDifficulty;
        //using preset values get the grade for the test route based on the difficulty value
        if(difficultyValue < 3.8)
        {
            grade = "1";
        }
        else if(difficultyValue <4.2)
        {
            grade = "2";
        }
        else if(difficultyValue < 4.5)
        {
            grade = "3";
        }
        else if(difficultyValue < 4.7)
        {
            grade = "4";
        }
        else if(difficultyValue < 5.0)
        {
            grade = "4+";
        }
        else if(difficultyValue < 5.3)
        {
            grade = "5";
        }
        else if(difficultyValue < 5.6)
        {
            grade ="5+";
        }
        else if(difficultyValue < 6.0)
        {
            grade = "6A";
        }
        else if(difficultyValue < 6.3)
        {
            grade = "6A+";
        }
        else if(difficultyValue < 6.6)
        {
            grade = "6B";
        }
        else if(difficultyValue < 6.9)
        {
            grade = "6B+";
        }
        else if(difficultyValue < 7.2)
        {
            grade = "6C";
        }
        else if(difficultyValue < 7.5)
        {
            grade = "6C+";
        }
        else if(difficultyValue < 7.9)
        {
            grade = "7A";
        }
        else if(difficultyValue < 8.2)
        {
            grade = "7A+";
        }
        else if(difficultyValue < 8.5)
        {
            grade = "7B";
        }
        else if(difficultyValue < 8.8)
        {
            grade = "7B+";
        }
        else if(difficultyValue < 9.1)
        {
            grade = "7C";
        }
        else if(difficultyValue < 9.3)
        {
            grade = "7C+";
        }
        else if(difficultyValue < 9.5)
        {
            grade = "8A";
        }
        else if(difficultyValue < 9.8)
        {
            grade = "8A+";
        }
        else if(difficultyValue < 10.1)
        {
            grade = "8B";
        }
        else if(difficultyValue < 10.4)
        {
            grade = "8B+";
        }
        else if(difficultyValue < 10.7)
        {
            grade = "8C";
        }
        else if(difficultyValue < 11)
        {
            grade = "8C+";
        }
        else if(difficultyValue >= 11)
        {
            grade = "9A";
        }
        //returning the grade the value
        return grade;
    }

    //Method to find the average distance between holds (distance between holds: minimum of the distances between the next hold and each of the 3 holds before) in a route and return as difficulty factor value
    /* Parameters
    * - route : the arraylist of holds representing the test route
     */
    public Double GetAverageHoldDistance(ArrayList<Hold> route)
    {
        ArrayList<Double> distances = new ArrayList<>();                                            //arraylist to hold all the distances between holds
        double totalDistance = 0;                                                                   //holds the total distance value
        //loop through holds in the route
        for(int i = 0; i < route.size()-1; i++)
        {
            Hold nextHold = route.get(i+1);
            Hold currentHold = route.get(i);

            //getting the column value of the current hold and the next hold, then finding the difference between the 2
            int nextCol = lettertoValue.get(nextHold.GetColumn());
            int currentCol = lettertoValue.get(currentHold.GetColumn());
            int nextCurrColDist = nextCol-currentCol;
            //getting the row value of the current hold and the next hold, then finding the difference between the 2
            int nextRow = Integer.parseInt(nextHold.GetRow());
            int currentRow = Integer.parseInt(currentHold.GetRow());
            int nextCurrRowDist = nextRow-currentRow;
            //using pythagoras theorem to calculate the actual distance between the next hold and current hold
            double nextCurrHoldDistance = Math.sqrt(Math.pow(nextCurrRowDist, 2) + Math.pow(nextCurrColDist, 2));

            //if holdDistance is less than 2, then the difficulty of using them should be harder as they are very close together.
            //Therefore check for this instance and if found set the distance to 4 instead so that hold distance will show are harder difficulty
            if(nextCurrHoldDistance < 2)
            {
                nextCurrHoldDistance = 4;
            }
            //checking if the holds are on the same column. If not then this would be a sideways/diagonal movement
            //show this is harder by increasing the distance value by 1
            if(currentCol != nextCol)
            {
                nextCurrHoldDistance +=1;
            }

            double holdDistance = nextCurrHoldDistance;
            //checking that there is a previous hold (i.e. current hold is not the first hold)
            if(i > 0)
            {
                //get the previous hold and find the distance between it and the next hold
                Hold prevHold = route.get(i-1);
                int prevCol = lettertoValue.get(prevHold.GetColumn());
                int prevRow = Integer.parseInt(prevHold.GetRow());
                int nextPrevColDist = nextCol - prevCol;
                int nextPrevRowDist = nextRow - prevRow;
                double nextPrevHoldDistance = Math.sqrt(Math.pow(nextPrevRowDist, 2) + Math.pow(nextPrevColDist, 2));
                //if the distance between the prev and next hold is less than 2 then they are very close together
                //represent this as more difficult by setting the distance value to 4 instead
                if(nextPrevHoldDistance < 2)
                {
                    nextPrevHoldDistance = 4;
                }
                //checking if the holds are on the same column. If not then this would be a sideways/diagonal movement
                //show this is harder by increasing the distance value by 1
                if(prevCol != nextCol)
                {
                    nextPrevHoldDistance +=1;
                }

                //checking if the distance between prev hold and next hold is smaller than current hold and next hold
                //if so set the hold distance to the distance between prev hold and next hold, to show that is the more likely move to be made
                if(nextPrevHoldDistance < nextCurrHoldDistance)
                {
                    holdDistance = nextPrevHoldDistance;
                }

                //checking if there is a 2nd previous hold to get distance from
                if(i > 1)
                {
                    //if so get the 2nd prev hold and find distance between it and next hold
                    Hold prevHoldTwo = route.get(i-2);
                    int prevTwoCol = lettertoValue.get(prevHoldTwo.GetColumn());
                    int prevTwoRow = Integer.parseInt(prevHoldTwo.GetRow());
                    int nextTwoHoldColDist = nextCol - prevTwoCol;
                    int nextTwoHoldRowDist = nextRow - prevTwoRow;
                    double nextTwoHoldDistance = Math.sqrt(Math.pow(nextTwoHoldRowDist, 2) + Math.pow(nextTwoHoldColDist, 2));
                    //if the distance is less than 2 then they are very close together
                    //therefore indicate this move is harder by setting the distance to 4
                    if(nextTwoHoldDistance < 2)
                    {
                        nextTwoHoldDistance = 4;
                    }
                    //checking if the holds are on the same column. If not then this would be a sideways/diagonal movement
                    //show this is harder by increasing the distance value by 1
                    if(prevTwoCol != nextCol)
                    {
                        nextTwoHoldDistance +=1;
                    }

                    //if the distance is the new min value then set the hold distance to that value
                    if(nextTwoHoldDistance < holdDistance)
                    {
                        holdDistance = nextTwoHoldDistance;
                    }
                }
            }

            //add the distance to the hold list and the total distance
            distances.add(holdDistance);
            totalDistance += holdDistance;
        }
        double avgDistance = (double) totalDistance/distances.size();                               //dividing total distance by number spaces between holds to get the average hold distance
        return avgDistance;
    }

    //method to get the average hold difficulty for a route
    /* Parameters
    * - routeA : the arraylist of holds representing the test route
     */
    public double GetAverageHoldDifficulty(ArrayList<Hold> routeA)
    {
        //setting an initial total hold difficulty value of 0
        double totalHoldValue = 0;
        //loop through each hold in the route
        for(Hold hold : routeA)
        {
            //if in first 3 rows, it's a starting hold so reduce difficulty
            if(Integer.parseInt(hold.GetRow()) <= 3)
            {
                totalHoldValue += 1;
            }
            // if the hold type is 1, then it is a large jug. increase total hold value by 1
            else if(hold.GetType().equals("1"))
            {
                totalHoldValue += 1;
            }
            //elese if the type is 2 (mini-jug) or 3 (edge) then increase total hold value by 2
            else if(hold.GetType().equals("2") || hold.GetType().equals("3"))
            {
                totalHoldValue += 2;
            }
            //else if the type is 4 (sloper) then increase total hold value by 3
            else if(hold.GetType().equals("4"))
            {
                totalHoldValue += 3;
            }
            //else if the type is 5 (pocket) then increase the total hold value by 4
            else if(hold.GetType().equals("5"))
            {
                totalHoldValue += 4;
            }
            //else if the type is 6 (pinch) then increase the total hold value by 5
            else if(hold.GetType().equals("6"))
            {
                totalHoldValue += 5;
            }
            //else if the type is 7 (crimp) difficulty value is 6
            else if(hold.GetType().equals("7"))
            {
                totalHoldValue += 6;
            }
        }
        //get average hold difficulty by dividing total hold difficulty value by the number holds in the test route
        double avgHoldDifficulty = (double) totalHoldValue/routeA.size();
        //returning average hold difficulty
        return avgHoldDifficulty;
    }

    //method to get and output in the console the number of boulder problems for each grade
    public void GetGradeNums()
    {
        ArrayList<Integer> gradeCounts = new ArrayList<>();
        for(int i = 0; i < 26; i++)
        {
            gradeCounts.add(0);
        }
        for(BoulderProblem testBP : testBPs)
        {
            if(testBP.GetGrade().equals("1"))
            {
                gradeCounts.set(0, gradeCounts.get(0)+1);
            }
            else if(testBP.GetGrade().equals("2"))
            {
                gradeCounts.set(1, gradeCounts.get(1)+1);
            }
            else if(testBP.GetGrade().equals("3"))
            {
                gradeCounts.set(2, gradeCounts.get(2)+1);
            }
            else if (testBP.GetGrade().equals("4"))
            {
                gradeCounts.set(3, gradeCounts.get(3)+1);
            }
            else if(testBP.GetGrade().equals("4+"))
            {
                gradeCounts.set(4, gradeCounts.get(4)+1);
            }
            else if (testBP.GetGrade().equals("5"))
            {
                gradeCounts.set(5, gradeCounts.get(5)+1);
            }
            else if(testBP.GetGrade().equals("5+"))
            {
                gradeCounts.set(6, gradeCounts.get(6)+1);
            }
            else if(testBP.GetGrade().equals("6A"))
            {
                gradeCounts.set(7, gradeCounts.get(7)+1);
            }
            else if(testBP.GetGrade().equals("6A+"))
            {
                gradeCounts.set(8, gradeCounts.get(8)+1);
            }
            else if(testBP.GetGrade().equals("6B"))
            {
                gradeCounts.set(9, gradeCounts.get(9)+1);
            }
            else if(testBP.GetGrade().equals("6B+"))
            {
                gradeCounts.set(10, gradeCounts.get(10)+1);
            }
            else if(testBP.GetGrade().equals("6C"))
            {
                gradeCounts.set(11, gradeCounts.get(11)+1);
            }
            else if(testBP.GetGrade().equals("6C+"))
            {
                gradeCounts.set(12, gradeCounts.get(12)+1);
            }
            else if(testBP.GetGrade().equals("7A"))
            {
                gradeCounts.set(13, gradeCounts.get(13)+1);
            }
            else if(testBP.GetGrade().equals("7A+"))
            {
                gradeCounts.set(14, gradeCounts.get(14)+1);
            }
            else if(testBP.GetGrade().equals("7B"))
            {
                gradeCounts.set(15, gradeCounts.get(15)+1);
            }
            else if(testBP.GetGrade().equals("7B+"))
            {
                gradeCounts.set(16, gradeCounts.get(16)+1);
            }
            else if(testBP.GetGrade().equals("7C"))
            {
                gradeCounts.set(17, gradeCounts.get(17)+1);
            }
            else if(testBP.GetGrade().equals("7C+"))
            {
                gradeCounts.set(18, gradeCounts.get(18)+1);
            }
            else if(testBP.GetGrade().equals("8A"))
            {
                gradeCounts.set(19, gradeCounts.get(19)+1);
            }
            else if(testBP.GetGrade().equals("8A+"))
            {
                gradeCounts.set(20, gradeCounts.get(20)+1);
            }
            else if(testBP.GetGrade().equals("8B"))
            {
                gradeCounts.set(21, gradeCounts.get(21)+1);
            }
            else if(testBP.GetGrade().equals("8B+"))
            {
                gradeCounts.set(22, gradeCounts.get(22)+1);
            }
            else if(testBP.GetGrade().equals("8C"))
            {
                gradeCounts.set(23, gradeCounts.get(23)+1);
            }
            else if(testBP.GetGrade().equals("8C+"))
            {
                gradeCounts.set(24, gradeCounts.get(24)+1);
            }
            else if(testBP.GetGrade().equals("9A"))
            {
                gradeCounts.set(25, gradeCounts.get(25)+1);
            }
        }

        for(int i = 0; i < gradeCounts.size(); i++)
        {
            System.out.println("Grade " + i + ": " + gradeCounts.get(i));
        }
    }
}
