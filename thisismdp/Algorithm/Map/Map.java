package Map;

import java.util.ArrayList;
import java.util.HashMap;

import Robot.RobotConstants.Direction;
import javafx.scene.paint.Color;

import java.awt.Point;

public class Map {
	private final Square[][] mapGrid;
	private ArrayList<Point> imagePosition;
	private HashMap<Point,Direction> imageDirection;
	
	//MAP CONSTANTS
	//Public Variables 
	public static final short Square_CM = 10;
	public static final short MAP_HEIGHT = 20;
	public static final short MAP_WIDTH = 15;
	public static final short GOALZONE_ROW = 18;
	public static final short GOALZONE_COL = 13;
	public static final Point GOALZONE = new Point(GOALZONE_COL,GOALZONE_ROW);
	public static final short STARTZONE_ROW = 1;
	public static final short STARTZONE_COL = 1;
	
	//Graphic Constants
	public static final Color SZ_COLOR = Color.YELLOW;	//Start Zone Color
	public static final Color GZ_COLOR = Color.GREEN;	//Goal Zone Color
	public static final Color UE_COLOR = Color.LIGHTGRAY;	//Unexplored Color
	public static final Color EX_COLOR = Color.WHITE;	//Explored Color
	public static final Color OB_COLOR = Color.BLACK;	//Obstacle Color
	public static final Color CW_COLOR = Color.WHITESMOKE;	//Square Border Color
	public static final Color WP_COLOR = Color.LIGHTSKYBLUE;	// WayPoint Color
	public static final Color THRU_COLOR = Color.LIGHTBLUE;
	public static final Color PH_COLOR  = Color.rgb(0, 250, 0, 1); //Path Color
		
	public static final int MAP_Square_SZ = 25;			//Size of the Squares on the Map (Pixels)
	public static final int MAP_OFFSET = 10;
	
	public Map() {
		this.mapGrid = new Square[MAP_HEIGHT][MAP_WIDTH];
		this.imagePosition = new ArrayList<Point>();
		this.imageDirection=new HashMap<Point,Direction>();
		initMap();
	}
	
	private void initMap() {
		//Initialise Square on Grid
		for (int row=0;row < MAP_HEIGHT;row++) {
			for(int col = 0; col < MAP_WIDTH;col++) {
				this.mapGrid[row][col] = new Square(new Point(col, row));
				
				//Initialise virtual wall
				if (row == 0 || col == 0 || row == MAP_HEIGHT - 1 || col == MAP_WIDTH - 1) {
					mapGrid[row][col].setvirtualBarrier(true);
				}
			}
		}
	}
	
	//Add Detected Image to Map's image collection for image tracking (location & direction)
	public boolean imagePosition(Point position, Direction direction) {
		boolean result = false;
		if(checkValidSquare(position.y, position.x) && mapGrid[position.y][position.x].isObstacle() && imagePosition.indexOf(position) == -1)
		{
			imagePosition.add(position);
			imageDirection.put(position, direction);
		}
		return result;
	}
	
	//Check if the image has already been detected
	public boolean isImageDetected(Point position, Direction direction) {
		boolean result = imageDirection.containsKey(position);
		return result;
	}
	
	//Convert into String the Image Location & Direction
	public String imagePositionToString()
	{
		String detected = null;
		
		if(!imagePosition.isEmpty())
		{
			for(Point position: imagePosition) {
				detected += "(x="+position.x+" y="+position.y+" "+imageDirection.get(position).name().charAt(0)+")";
			}
		}
		
		return detected;
	}
	
	//Return Square
	public Square getSquare(int row, int col)
	{
		return mapGrid[row][col];
	}
	
	public ArrayList<Square> getNeighbours(Square c){
		ArrayList<Square> neighbours = new ArrayList<Square>();
		
		// UP
		Square squareUp = getSquare(c.getPos().y + 1, c.getPos().x);
		if(checkValidAction(squareUp.getPos().y, squareUp.getPos().x))
		{
			neighbours.add(squareUp);
		}
		
		//DOWN
		Square squareDown = getSquare(c.getPos().y - 1, c.getPos().x);
		if(checkValidAction(squareDown.getPos().y, squareDown.getPos().x))
		{
			neighbours.add(squareDown);
		}
		
		//LEFT
		Square squareLeft = getSquare(c.getPos().y, c.getPos().x - 1);
		if(checkValidAction(squareLeft.getPos().y, squareLeft.getPos().x))
		{
			neighbours.add(squareLeft);
		}
		
		//RIGHT
		Square squareRight = getSquare(c.getPos().y, c.getPos().x + 1);
		if(checkValidAction(squareRight.getPos().y, squareRight.getPos().x))
		{
			neighbours.add(squareRight);
		}
		
		return neighbours;
	}
	
	//Return Sqaure based on Point
	public Square getSquare(Point position) {
		return mapGrid[position.y][position.x];
	}
	
	//Remove existing Square with path
	public void removePaths(){
		for(int r=0; r < MAP_HEIGHT; r++)
		{
			for(int c=0; c < MAP_WIDTH; c++)
			{
				mapGrid[r][c].setPath(false);
			}
		}
	}
	
	//Returns the nearest unexplored Square from the current robot location
	public Square nearestUnexploredSquare(Point botLocation) {
		Square nearest = null;
		double distance = 1000;
		
		//Find nearest unexplored
		for (int r = 0; r < MAP_HEIGHT; r++)
		{
			for(int c = 0; c < MAP_WIDTH; c++)
			{
				Square currentSquare = mapGrid[r][c];
				
				if(!currentSquare.isExplored() && distance > botLocation.distance(currentSquare.getPos()))
				{
					nearest = currentSquare;
					distance = botLocation.distance(currentSquare.getPos());
				}
			}
		}
		
		return nearest;
	}
	
	//Returns the nearest explored Square from the current robot location
		public Square nearestExploredSquare(Point unexpLoc, Point botLocation) {
			Square nearest = null;
			double distance = 1000;
			
			//Find nearest unexplored
			for (int r = 0; r < MAP_HEIGHT; r++)
			{
				for(int c = 0; c < MAP_WIDTH; c++)
				{
					Square currentSquare = mapGrid[r][c];
					
					if(checkValidAction(r,c) && clearForRobot(r, c) && areaCovered(r, c))
					{
						if((distance > unexpLoc.distance(currentSquare.getPos())) && (currentSquare.getPos().distance(botLocation) > 0))
						{
							nearest = currentSquare;
							distance = unexpLoc.distance(currentSquare.getPos());
						}
					}
				}
			}
			
			return nearest;
		}
	
	//Ensure robot can move to the specified location by clearing it
	public boolean clearForRobot(int row, int col) {
		for (int r=row-1; r <= row + 1; r++)
		{
			for(int c = col-1; c <= col + 1; c++)
			{
				if(!checkValidSquare(r, c) || !mapGrid[r][c].isExplored() || mapGrid[r][c].isObstacle())
				{
					return false;
				}
			}
		}
		return true;
	}
	
	//Check if the row and column is within the map's limits
	public boolean checkValidSquare(int row, int col) {
		return row >= 0 && col >= 0 && row < MAP_HEIGHT && col < MAP_WIDTH;
	}
	
	//Check if not a virtual wall and valid to move there
	public boolean wayPointClear(int row, int col)
	{
		boolean result = checkValidSquare(row, col) && !(getSquare(row, col).isvirtualBarrier()) && !(getSquare(row, col).isObstacle());
		return result;
	}
	
	//Check if valid to move there and already explored
	public boolean checkValidAction(int row, int col)
	{
		boolean result = wayPointClear(row, col) && getSquare(row,col).isExplored();
		return result;
	}
	
	//Restart virtual walls around obstacle
	public void reinitVirtualBarrier()
	{
		for (int row = 0; row < MAP_HEIGHT; row++) {
			for (int col = 0; col < MAP_WIDTH; col++) {
				// Init Virtual wall
				if (row == 0 || col == 0 || row == MAP_HEIGHT - 1 || col == MAP_WIDTH - 1) {
					mapGrid[row][col].setvirtualBarrier(true);
				}
				if (mapGrid[row][col].isObstacle()) {
					for (int r = row - 1; r <= row + 1; r++)
						for (int c = col - 1; c <= col + 1; c++)
							if (checkValidSquare(r, c))
								mapGrid[row][col].setvirtualBarrier(true);
				}
			}
		}
		
	}
	
	//Mark area under the robot to be covered{
	public void passedOver(int row, int column)
	{
		for(int r = row - 1; r <= row + 1; r++)
		{
			for (int c = column - 1; c <= column +1; c++)
			{
				mapGrid[r][c].setCovered(true);
			}
		}
	}
	
	//Check if the entire area is covered
	public boolean areaCovered(int row, int column)
	{
		for(int r = row - 1; r <= row+1; r++)
		{
			for(int c = column - 1; c <= column + 1; c++)
			{
				if(! (mapGrid[r][c].isCovered()))
				{
					return true;
				}
			}
		}
		return false;
	}
	
	//Check if Square has unmoved place that is not completely passed over
	public boolean covered(int row, int column) {
		for(int r = row - 1; r <= row + 1; r++)
		{
			for(int c = column - 1; c <= column + 1; c++)
			{
				if(!mapGrid[r][c].isCovered())
				{
					return true;					
				}
			}
		}
		return false;
	}
	
	//Set all Square as explored based on boolean
	public void setAllExplored(boolean explored) {
		for (int r = 0 ; r < MAP_HEIGHT; r++)
		{
			for(int c = 0; c < MAP_WIDTH; c++)
			{
				mapGrid[r][c].setExplored(explored);
			}
		}
	}

	//Set all Square as covered based on boolean
	public void setAllCovered(boolean explored)
	{
		for (int r = 0; r < MAP_HEIGHT; r++)
		{
			for(int c = 0; c < MAP_WIDTH; c++)
			{
				mapGrid[r][c].setCovered(explored);
			}
		}
	}
	
	//Return percentage explroed
	public double exploredPercentage()
	{
		
		double total = MAP_HEIGHT * MAP_WIDTH;
		double explored = 0;
		
		for (int r = 0; r < MAP_HEIGHT; r++)
		{
			for (int c = 0; c < MAP_WIDTH; c++)
			{
				if(mapGrid[r][c].isExplored())
				{
					explored++;
				}
			}
		}
		
		double exploredPercentage = (explored / total) * 100;
		
		return exploredPercentage;
	}
	
	//Restart Map
	public void restartMap()
	{
		initMap();
	}
	
	
}