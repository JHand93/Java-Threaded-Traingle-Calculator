/*
 * @(1)Triangles.java 1.00 9/17/14
 *
 * This program takes in a collection of integer
 * points on a 2D Cartesian Plane. It then finds
 * out how many unique right triangles can be found.
 * 
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * The driver class to run on the main thread
 *
 * @version 1.00 17 Sep 2014
 * @author John Handley
 */
public class Triangles {
  /**
   * ...method main: runs the program
   */
  public static void main(String[] args) throws InterruptedException{
	  
	/***************** VERIFY INPUTS *******************/  
    if (args.length != 2) {//Make sure correct number of variables
      System.err.println("Parameters: <File Path> <Thread Count>");
      System.exit(1);
    }
    if(Integer.parseInt(args[1]) < 1){//Is there good number of threads?
      System.err.println("Program needs at least 1 thread to execute.");
      System.exit(1);
    }
    try{//Try to read the file if it exists
      // Read the number of points
      Scanner fileScanner = new Scanner(new File(args[0]));
      int size = 0;
      if(fileScanner.hasNextInt()){
        size = fileScanner.nextInt();
      }
      else{//Look for the size at the top of the file
        System.err.println("The file is Empty.");
        fileScanner.close();
        System.exit(1);
      }
      if(size < 0){//Impossible to have negative number of points
        System.err.println("Size must be at least zero.");
        fileScanner.close();
        System.exit(1);
      }
      if(size < 3){//There have to be 3+ to make a triangle
        System.out.println(0);
        fileScanner.close();
        System.exit(0);
      }
      
      /************** FILL THE ARRAY WITH THE COORDINATES **************/
      int read = 0;//Coordinate Counter
      Cord arr[] = new Cord[size];// Array of Coordinates
      for(; read < size && fileScanner.hasNextInt(); read++){
        arr[read] = new Cord();
        arr[read].x = fileScanner.nextInt();
        if(!fileScanner.hasNextInt()){//There was an incomplete Coordinate
          System.err.println("There was an error reading the file.");
          fileScanner.close();
          System.exit(1);
        }
        arr[read].y = fileScanner.nextInt();
      }
      if(read != size){//Not everything was read
        System.err.println("There was an error reading the file.");
        fileScanner.close();
        System.exit(1);
      }
      fileScanner.close();//Done with file

      /*****************PREPARE ADJACENCY MATRIX ****************/
      // Fill the adjacency matrix with the lengths of the sides squared
      int[][] adj = new int[size][size];
      for(int i = 0; i < size; i++){
        for(int j = 0; j < size; j++){
          adj[i][j] = ((arr[j].x - arr[i].x) 
                      * (arr[j].x - arr[i].x))
                      + ((arr[j].y - arr[i].y)
                      * (arr[j].y - arr[i].y));
        }
      }
      
      /***************** START DIVIDING WORK *********************/
      int threadCount = Integer.parseInt(args[1]);// Number of threads
      int[] begin = new int[threadCount];// Begin Index
      int[] end = new int[threadCount];// End Index
      int perThread = (int) size / threadCount;// Work per thread
      int workSoFar = 0;// What's been done
      for(int i = 0; i < size % threadCount; i++){
        begin[i] = workSoFar;
        workSoFar += perThread + 1;
        end[i] = workSoFar - 1;
      }
      for(int i = size % threadCount; i < threadCount; i++){
        begin[i] = workSoFar;
        workSoFar += perThread;
        end[i] = workSoFar - 1;
      }
      /***************** DONE DIVIDING WORK *********************/

      /***************** START THREAD CREATION *****************/
      Thread threadList[] = new Thread[threadCount-1];
      Finder finderList[] = new Finder[threadCount-1];
      for(int i = 0; i < threadCount-1; i++) {
    	finderList[i] = new Finder(size, adj, begin[i+1], end[i+1]);
        threadList[i] = new Thread(finderList[i]);
        threadList[i].start();
      }
      
      int total = 0;
      SolveIt s = new SolveIt();
      total = s.run(size, adj, begin[0], end[0]);
      
      /************* FINISHED PROCESSING PREPARE EXIT *************/
      
      // Wait for threads to complete
      for(int i = 0; i < threadCount-1; i++) {
        threadList[i].join();
        total+=finderList[i].getTotal();
      }
      
      System.out.println(total);
    }
    catch(FileNotFoundException e){
      System.err.println("No file found");
      System.exit(1);
    }
    return;// Program finished successfully
  }

}

/**
 * The Thread class to call the SolveIt object
 *
 * @version 1.00 17 Sep 2014
 * @author John Handley
 */
class Finder implements Runnable {
  private int total = 0;// Threads Total
  private int size;//Number of Vertices
  private int begin;//begin index
  private int end;//end index
  private int[][] arr;//adjacency matrix
  /**
   * ...Constructor Finder: sets up the thread
   */
  public Finder(int theSize, int[][] theArr, int theBegin, int theEnd) {
    size = theSize;
    begin = theBegin;
    end = theEnd;
  
    arr = new int[size][];
    for(int i = 0; i < theSize; i++){
      arr[i] = new int[size];
      for(int j = 0; j < theSize; j++){
        arr[i][j] = theArr[i][j];
      }
    }
    return;
  }
  /**
   * ...Method Run: runs the thread by calling SolveIt
   */
  public void run() {
    SolveIt s = new SolveIt();
    total = (s.run(size, arr, begin, end));
    return;
  }
  /**
   * ...Method getTotal: gives the total to the parent thread
   */
  public int getTotal() {
    return total;
  }
}

/**
 * A class to store a coordinate
 *
 * @version 1.00 17 Sep 2014
 * @author John Handley
 */
class Cord {
  public int x;//X-axis
  public int y;//Y-axis
  /**
   * ...Constructor Cord: lets the program create an array from this
   */
  public Cord(){
    return;
  }
}

/**
 * Class that is used to solve the triangles problem
 *
 * @version 1.00 17 Sep 2014
 * @author John Handley
 */
class SolveIt{
  /**
   * ...Method Run: Calculates all the right triangles in the collection
   *				from all the coordinates.
   */
  public int run(int size, int[][] adj, int beg, int end){
      int total = 0;
      
      // Calculate the right triangles
      for(int i = beg; i <= end; i++){
        for(int j = 0; j < size-1; j++){
          if(j != i){
            for(int k = j+1; k < size; k++){
              if(k != i && k != j){
                if(((adj[i][j] + adj[i][k]) - adj[j][k]) == 0){
                  total++;
                }
              }
            }
          }
        }
      }
	  return total;
  }
}