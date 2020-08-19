package Robot;

// Changes
// reachGoal --> goalHit
// right/frontDistAlign --> " Align
// prevAction --> prevAction
// pos --> roboPos
// rotateSensors --> rotateSensorsACW
// rotateSensors params left --> acw
// RobotConstants sHORT/LONG_MIN/MAX -> SMALL/BIG_"
// RobotConstants RightLIMIT
// RobotConstants RIGHT_NEAR_LIMIT, RIGHT_FAR_LIMIT
// robotMessageTransmit function added
// alignment method added

//java packages

import java.util.concurrent.TimeUnit;
import java.awt.Point;
import java.util.ArrayList;

//project classes

import Map.*;
import Network.NetMgr;
import Robot.RobotConstants.Direction;
import Robot.RobotConstants.Command;

public class Robot {
	private boolean simulate;
	private boolean goalHit;
	private Point roboPos;
	private Direction dir;
	private Command prevAction;
	
	private boolean rightAlign = false;
	private boolean frontAlign = false;
	private int senseCount = 0;
	private int alignCount = 0;
	private boolean fastSense = false;
	
	private ArrayList<Sensor> sensorList;
	
	public boolean isSimulate() {
		return simulate;
	}

	public void setSimulate(boolean simulate) {
		this.simulate = simulate;
	}
	
	public boolean isFastSense() {
		return fastSense;
		}
		
	public void setFastSense(boolean fastSense){
		this.fastSense = fastSense;
	}
	
	public boolean isGoalHit() {
		return goalHit;
	}
	
	public void setGoalHit(boolean goalHit){
		this.goalHit = goalHit;
	}
	

	public Robot(boolean simulate, Direction dir, int row, int col) {
		this.setSimulate(simulate);
		this.dir = dir;
		this.goalHit = false;
		this.roboPos = new Point(col,row);
		sensorList = new ArrayList<Sensor>();
		
		// Init sensors
		
		// 1 Left sensor
		Sensor LL1 = new Sensor("LL1", RobotConstants.BIG_MIN, RobotConstants.BIG_MAX, row + 1, col - 1,
				Direction.LEFT);
				
		// 2 Right sensors
		Sensor SR1 = new Sensor("SR1", RobotConstants.SMALL_MIN, RobotConstants.SMALL_MAX, row - 1, col + 1,
				Direction.RIGHT);
		Sensor SR2 = new Sensor("SR2", RobotConstants.SMALL_MIN, RobotConstants.SMALL_MAX, row + 1, col + 1,
				Direction.RIGHT);
		
		// 3 Front sensors
		Sensor SF1 = new Sensor("SF1", RobotConstants.SMALL_MIN, RobotConstants.SMALL_MAX, row + 1, col + 1,
				Direction.UP);
		Sensor SF2 = new Sensor("SF2", RobotConstants.SMALL_MIN, RobotConstants.SMALL_MAX, row + 1, col, Direction.UP);
		Sensor SF3 = new Sensor("SF3", RobotConstants.SMALL_MIN, RobotConstants.SMALL_MAX, row + 1, col - 1,
				Direction.UP);

		// Add sensors to robot sensorList
		
		sensorList.add(SF1);
		sensorList.add(SF2);
		sensorList.add(SF3);
		sensorList.add(SR1);
		sensorList.add(SR2);
		sensorList.add(LL1);
		
		// Sync robot and sensor direction
		
		switch(dir) {
			case LEFT:
				rotateSensorsACW(true);
				break;
			
			case RIGHT:
				rotateSensorsACW(false);
				break;
			
			case DOWN:
				rotateSensorsACW(false);
				rotateSensorsACW(false);
				break;
			default:
				break;
		}

	}
	
	// Get Set methods for Robot instance attributes;
	
	public ArrayList<Sensor> getSensorList() {
		return sensorList;
	}

	public void setSensorList(ArrayList<Sensor> sensorList) {
		this.sensorList = sensorList;
	}

	public Direction getDirection() {
		return dir;
	}

	public void setDirection(Direction dir) {
		this.dir = dir;
	}


	public Sensor getSensor(String id) {
		for (Sensor s : sensorList)
			if (s.getId().equals(id))
				return s;

		return null;
	}
	
	
	public void rotateSensorsACW(boolean acw) {
		double angle = 0;
		int newCol, newRow;

		if (acw)
			angle = Math.PI / 2;
		else
			angle = -Math.PI / 2;

		// Rotation Formula used: x = cos(a) * (x1 - x0) - sin(a) * (y1 - y0) + x0
		// y = sin(a) * (x1 - x0) + cos(a) * (y1 - y0) + y0: 
		// Explained in this link https://math.stackexchange.com/questions/1601600/calculate-position-of-object-rotating-around-an-axis
		for (Sensor s : sensorList) {
			if (acw)
				s.setSensorDir(Direction.rotateACW(s.getSensorDir()));
			else
				s.setSensorDir(Direction.rotateCW(s.getSensorDir()));

			newCol = (int) Math
					.round((Math.cos(angle) * (s.getCol() - roboPos.x) - Math.sin(angle) * (s.getRow() - roboPos.y) + roboPos.x));
			newRow = (int) Math
					.round((Math.sin(angle) * (s.getCol() - roboPos.x) - Math.cos(angle) * (s.getRow() - roboPos.y) + roboPos.y));
			s.setPos(newCol, newRow);
		}
	}
	
	public void setPosition(int col, int row) {
		int colDiff = col - roboPos.x;
		int rowDiff = row - roboPos.y;
		roboPos.setLocation(col, row);
		for (Sensor s : sensorList) {
			s.setPos(s.getCol() + colDiff, s.getRow() + rowDiff);
		}
	}
	
	public Point getPosition() {
		return roboPos;
	}
	
	public void setStartPos(int col, int row, Map exploredMap) {
		setPosition(col, row);
		exploredMap.setAllExplored(false);
		exploredMap.setAllCovered(false);
		for (int r = row - 1; r <= row + 1; r++) {
			for (int c = col - 1; c <= col + 1; c++) {
				exploredMap.getSquare(r, c).setExplored(true);
				exploredMap.getSquare(r, c).setCovered(true);
			}

		}
	}
	
	// Moving method for robot and sensors
	
	public void move(Direction dir, boolean forward, int steps, Map exploredMap) {
		int rowInc = 0, colInc = 0;
		switch (dir) {
		case UP:
			rowInc = 1;
			colInc = 0;
			break;
		case LEFT:
			rowInc = 0;
			colInc = -1;
			break;
		case DOWN:
			rowInc = -1;
			colInc = 0;
			break;
		case RIGHT:
			rowInc = 0;
			colInc = 1;
			break;
		}

		if (!forward) {
			rowInc *= -1;
			colInc *= -1;
		}

		if (exploredMap.checkValidAction(roboPos.y + rowInc * steps,roboPos.x + colInc * steps)) {
			setPosition(roboPos.x + colInc * steps, roboPos.y + rowInc * steps);
			if (!fastSense) {
				for (int i = 0; i < steps; i++) {
					exploredMap.passedOver(roboPos.y - rowInc * i, roboPos.x - colInc * i);
				}
			}
		}
	}
	
	// Moving using RobotConstants Commands
	
	public void move(Command m, int steps, Map exploredMap) {
		//Array of Commands for faster movement
		char[] moves = {'W','A','D','X'};
		if (!simulate) {
			System.out.println("Alg|Ard|"+m+"|"+steps);
			if(!fastSense)
				NetMgr.getInstance().send("Alg|Ard|" + m.ordinal() + "|" + steps + "|");
			else
				NetMgr.getInstance().send("Alg|Ard|" + m.ordinal() + "|" + steps + "|");
			
			NetMgr.getInstance().send("Alg|And|"+ m.ordinal() + "|" + steps + "|");
		}
		switch (m) {
			// true is turn left, false is turn right
		case FORWARD:
			move(dir, true, steps, exploredMap);
			break;
		case BACKWARD:
			move(dir, false, steps, exploredMap);
			break;
		case TURN_LEFT:
			dir = Direction.rotateACW(dir);
			rotateSensorsACW(true);
			break;
		case TURN_RIGHT:
			dir = Direction.rotateCW(dir);
			rotateSensorsACW(false);
			break;
		default:
			System.out.println("Invalid Move Received");
			break;
		}
		prevAction = m;
	}
	
	// Robot command messages to Arduino/Android
	// data for additional information

	public void robotMessageTransmit(String receiver, Command c, int data){
		NetMgr.getInstance().send("Alg|"+receiver+"|"+c.ordinal()+"|"+data+"|");
	/*	try{
			TimeUnit.MILLISECONDS.sleep(20);
		} catch (Exception e){
			e.printStackTrace();
		} */
	} 

	public String robotMessageBeepBeep(){
		return NetMgr.getInstance().receive();
	}

	// Alignment commands sent to Arduino
	
	public boolean alignment(double sensorData[][], Map exploredMap, Map map){
		alignCount++;
		
		// Checking front alignment too close/far from location
		String msg = null;
		System.out.println("Previous action:" + prevAction);
		// Check for front alignment for movements requiring moving forward
		if (prevAction == Command.FORWARD || prevAction == Command.BACKWARD) {
			frontAlign = false;
			for (int i = 0; i < 3; i++) {
				System.out.println(
						"Front " + i + ": " + sensorData[i][1] + " PrevData: " + sensorList.get(i).getPrevData()
								+ " SensorDiff: " + Math.abs(sensorData[i][1] - sensorList.get(i).getPrevData()));
				if (sensorList.get(i).getPrevData() < 9
						&& Math.abs(sensorData[i][1] - sensorList.get(i).getPrevData()) != 1) {
					System.out.println("Initial Front Cal Condition Passed!");
					senseCount++;
					// Sense again to ensure correctness
					if (senseCount < 2) {
						NetMgr.getInstance().send("Alg|Ard|S|0");
						return true;
					} else {
						senseCount = 0;
						break;
					}
				}
			}
			boolean cal = false;
			// Check whether there is a need to calibrate
			for (int i = 0; i < 3; i++) {
				if (sensorData[i][1] == 1 && (sensorData[i][0] < RobotConstants.RIGHT_NEAR_LIMIT
						|| sensorData[i][0] > RobotConstants.RIGHT_FAR_LIMIT)) {
					cal = true;
					break;
				}

			}

			// Discrepancy detected among the sensor data received
			if (cal) {
				//robotMessageTransmit("Ard", Command.TURN_LEFT, 1);
				//robotMessageBeepBeep();
				robotMessageTransmit("Ard", Command.ALIGN_FRONT, 1);
			//	robotMessageTransmit("And", Command.ALIGN_FRONT, 1);
				frontAlign=true;
				robotMessageBeepBeep();
			}

		}

		// ALIGNMENT ALGORITHM
		// Compare average distance between both right sensors prev and current: 
		// double prevRightAvg = (sensorList.get(3).getPrevRawData() +
		// sensorList.get(4).getPrevRawData())/2;
		// double curRightAvg = (sensorData[3][0] + sensorData[4][0])/2;
		// System.out.println("cur: "+curRightAvg+" prev:"+prevRightAvg);
		// if too close/ far from right wall...prevRightAvg and curRightAvg 
		// have too large a difference, and obstacle detected at sensor (sensorData[][i])
		// if(!rightAlign && Math.abs(curRightAvg-prevRightAvg) >=
		// RobotConstants.RIGHT_LIMIT && sensorData[3][1]==1 &&
		// sensorData[4][1]==1){
			
		boolean distAlign = true;
		for (int i = 3; i < 5; i++)
			if ((sensorData[i][0] < RobotConstants.RIGHT_NEAR_LIMIT
					|| sensorData[i][0] > RobotConstants.RIGHT_FAR_LIMIT) && sensorData[i][1] == 1)
				distAlign = false;

		// Actions to be done

		System.out.println("distAlign " + !distAlign);
		System.out.println("rightDistAlign " + !rightAlign);
		System.out.println("R1 or R2 " + (sensorData[3][1] == 1 || sensorData[4][1] == 1));
		if (!rightAlign && !distAlign && (sensorData[3][1] == 1 || sensorData[4][1] == 1) && ((prevAction!=Command.TURN_RIGHT && prevAction!=Command.TURN_LEFT)|| !frontAlign)) {
			System.out.println("Right Distance Alignment-------------------------------");
			//robotMessageTransmit("And", Command.ALIGN_FRONT, 1);
			//robotMessageTransmit("Ard", Command.TURN_RIGHT, 1);
			//msg = robotMessageBeepBeep();
			robotMessageTransmit("Ard", Command.ALIGN_RIGHT, 1);
			System.out.println("LINE 377")
			msg = robotMessageBeepBeep();
			//robotMessageTransmit("Ard", Command.TURN_LEFT, 1);

			if (sensorData[3][1] == 1 && sensorData[4][1] == 1) {
				
				robotMessageTransmit("Ard", Command.ALIGN_RIGHT, 1);
				msg = robotMessageBeepBeep();
			}
			rightAlign = true;
			

			return true;
		}
		
		rightAlign = false;
		
		// Check Right Alignment
		if (Math.abs(sensorData[3][0] - sensorData[4][0]) > RobotConstants.RIGHT_LIMIT && sensorData[3][1] <= 1
				&& sensorData[4][1] <= 1) {
			System.out.println("Right Alignment------------------------");
			//robotMessageTransmit("And", Command.ALIGN_RIGHT, 0);
			robotMessageTransmit("Ard", Command.ALIGN_RIGHT, 0);
			
			return true;
		}
		return false;
	}
	// Robot Sensing for Image pos, dir & Obstacles
	
	public void sense(Map exploredMap, Map map) {
		double obsBlock;
		double[][] sensorData = new double[6][2];
		int rowInc = 1, colInc = 1;
		String msg = null;
		if (!simulate) {
			String[] msgArr;
			
			// Keep sensing surroundings and do appropriate actions
			
			do {
				System.out.println("Sense Loop");
				msg = robotMessageBeepBeep();
				System.out.println(msg);
				System.out.println("After receiving message from Bob's brain");
				msgArr = msg.split("\\|");
				System.out.println("method"+msgArr[2]);
				
				// 1st, Check if image detected from RPI (case !S in msgArr[2])
				
				if(msgArr[2].compareToIgnoreCase("S")!=0) {
					boolean obsDetected =false;
					int row=0, col=0;
					switch (dir) {
					case LEFT:
						row = 2;
						break; 

					case DOWN:
						col = -2;
						break; 

					case UP:
						col = 2;
						break; 

					case RIGHT:
						row = -2;
						break;
					}
					if(exploredMap.getSquare(roboPos.y+row, roboPos.x+col).isObstacle()) {
						obsDetected = true;
						}
					// Image exists only if obstacle also exists there
					System.out.println("Image Detected at ("+roboPos.x+col+","+roboPos.y+row+")");
					if(obsDetected) {
						//Create new point for img location
						Point imgPos = new Point(roboPos.x+col, roboPos.y+row);
						//Save Image location and Direction
						exploredMap.imagePosition(imgPos, Direction.rotateACW(dir));
						char c = Direction.rotateACW(dir).name().charAt(0);
						//Send Coordinates of image to Android
						NetMgr.getInstance().send("Alg|And|A|"+imgPos.x+","+imgPos.y+","+c+"|");
					}
				}
				// Sensor data received from Arduino
				// Checked after checking for image first. If obstacle not yet initialised at location, image position not registered
				// A whole length of string representing all the data from all sensors
				else {
					
					// List of strings representing each sensor's data
					String[] sensorStr = msgArr[3].split("\\,");
					System.out.println("Received " + sensorStr.length + " sensor data");
		
					// Translate string to integer for each sensor
					for (int i = 0; i < sensorStr.length; i++) {
						String[] sensorStrArr = sensorStr[i].split("\\:");
						sensorData[i][0] = Double.parseDouble(sensorStrArr[1]);
						sensorData[i][1] = Double.parseDouble(sensorStrArr[2]);
					}
				}
				// If image detected but not obstacle, this does not work, but most likely to detect obstacle before image.
			}while(msgArr[2].compareToIgnoreCase("S")!=0);
		}

		// not !simulate
		for (int i = 0; i < sensorList.size(); i++) {
			// check if sensor detects any obstacle
			if (!simulate) {
				obsBlock = sensorData[i][1];
			} else 
				obsBlock = sensorList.get(i).detect(map); // for simulator
			// Assign the rowInc and colInc based on sensor Direction
			switch (sensorList.get(i).getSensorDir()) {
			case UP:
				rowInc = 1;
				colInc = 0;
				break;

			case LEFT:
				rowInc = 0;
				colInc = -1;
				break;

			case RIGHT:
				rowInc = 0;
				colInc = 1;
				break;

			case DOWN:
				rowInc = -1;
				colInc = 0;
				break;
			}
			// Detecting phantom blocks
			int existingObsBlock = -1;
			if (!simulate) {
				// Check Map for existing obstacle location at minRange
				// 1st condition: Valid square but unexplored, no existing obstacle yet, 2nd condition: there is an existing obstacle
				for (int j = sensorList.get(i).getMinRange(); j <= sensorList.get(i).getMaxRange(); j++) {
					if (exploredMap.checkValidSquare(sensorList.get(i).getRow() + rowInc * j,
							sensorList.get(i).getCol() + colInc * j)
							&& !exploredMap.getSquare(sensorList.get(i).getRow() + rowInc * j,
									sensorList.get(i).getCol() + colInc * j).isExplored()) {
						existingObsBlock = 0;
						if (exploredMap.getSquare(sensorList.get(i).getRow() + rowInc * j,
								sensorList.get(i).getCol() + colInc * j).isObstacle())
							existingObsBlock = j;
						break;
					} else
						break;
				}

				System.out.println(
						sensorList.get(i).getId() + " existing:" + existingObsBlock + " obsBlock:" + obsBlock);
				// Discrepancy between explored map and sensor reading request sensor reading
				// again
				// Obstacle detected / obstacle already exists on exploredMap
				// When obsBlock = 9, obstacle does not exist
				if (existingObsBlock != -1 && existingObsBlock != obsBlock && obsBlock != 9 && existingObsBlock != 0) {
					System.out.println(
							"Possible Phantom block conflict with existing block---------------------------------");
					System.out.println("SenseCount: " + senseCount);
					// Second reading removes existing block if discrepancy still exists
					if (senseCount > 1) {
						senseCount = 0;
						System.out.println("Discarding existing block for sensor "+sensorList.get(i).getId());
						exploredMap.getSquare(sensorList.get(i).getRow() + rowInc * existingObsBlock,
								sensorList.get(i).getCol() + colInc * existingObsBlock).setObstacle(false);
					} else {
						System.out.println("Error Possible Phantom Block Detected! Resensing");
						NetMgr.getInstance().send("Alg|Ard|S|0"); //sense again
						senseCount++;
						sense(exploredMap, map);
						return;
					}
				}
				sensorList.get(i).setPrevData(obsBlock);
				sensorList.get(i).setPrevRawData(sensorData[i][0]);
			}
			// After checking for phantom blocks
			// Discover each of the blocks infront of the sensor if possible
			for (int j = sensorList.get(i).getMinRange(); j <= sensorList.get(i).getMaxRange(); j++) {

				// Check if the block is valid otherwise exit (Edge of Map)
				if (exploredMap.checkValidSquare(sensorList.get(i).getRow() + rowInc * j,
						sensorList.get(i).getCol() + colInc * j)) {
					// Change the Square to explored first
					exploredMap
							.getSquare(sensorList.get(i).getRow() + rowInc * j, sensorList.get(i).getCol() + colInc * j)
							.setExplored(true);
					// if obstacle within min-max range, j will be == obsBlock
					// && obstacle not discovered before
					if (j == obsBlock && !exploredMap
							.getSquare(sensorList.get(i).getRow() + rowInc * j, sensorList.get(i).getCol() + colInc * j)
							.isCovered()) {
					//	exploredMap.getSquare(sensorList.get(i).getRow() + rowInc * j,
					//			sensorList.get(i).getCol() + colInc * j).setExplored(true);
						exploredMap.getSquare(sensorList.get(i).getRow() + rowInc * j,
								sensorList.get(i).getCol() + colInc * j).setObstacle(true);

						// Virtual Wall Initialized: Set walls that form 1 square boundary around obstacle blocks
						for (int r = sensorList.get(i).getRow() + rowInc * j - 1; r <= sensorList.get(i).getRow()
								+ rowInc * j + 1; r++)
							for (int c = sensorList.get(i).getCol() + colInc * j - 1; c <= sensorList.get(i).getCol()
									+ colInc * j + 1; c++)
								if (exploredMap.checkValidSquare(r, c))
									exploredMap.getSquare(r, c).setvirtualBarrier(true);

						break; //exit loop since sensor has done its job
					} 
					// if existing obstacle is shown but not detected now, remove from map
					else if (exploredMap
							.getSquare(sensorList.get(i).getRow() + rowInc * j, sensorList.get(i).getCol() + colInc * j)
							.isObstacle()) {
					//	exploredMap.getSquare(sensorList.get(i).getRow() + rowInc * j,
					//			sensorList.get(i).getCol() + colInc * j).setExplored(true);
						exploredMap.getSquare(sensorList.get(i).getRow() + rowInc * j,
								sensorList.get(i).getCol() + colInc * j).setObstacle(false);
						// Set Virtual Wall off
						for (int r = sensorList.get(i).getRow() + rowInc * j - 1; r <= sensorList.get(i).getRow()
								+ rowInc * j + 1; r++)
							for (int c = sensorList.get(i).getCol() + colInc * j - 1; c <= sensorList.get(i).getCol()
									+ colInc * j + 1; c++)
								if (exploredMap.checkValidSquare(r, c))
									exploredMap.getSquare(r, c).setvirtualBarrier(false);

						exploredMap.reinitVirtualBarrier();
					}
				} 
				else
					break;
			}
		}
		if(!simulate){
			System.out.println("Realigning");
			boolean realign = alignment(sensorData, exploredMap, map);
			if (realign) {
				sense(exploredMap, map);
			}
			if (!fastSense)
				sendMapDescriptor(exploredMap);
		}
	
	}

	public void sendMapDescriptor(Map exploredMap) {
		String data = MapDescriptor.genMDS1(exploredMap);
		NetMgr.getInstance().send("Alg|And|MD1|" + data + "|");
		data = MapDescriptor.genMDS2(exploredMap);
		NetMgr.getInstance().send("Alg|And|MD2|" + data + "|");
	}
}
