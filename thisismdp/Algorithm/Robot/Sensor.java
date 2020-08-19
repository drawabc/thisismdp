package Robot;

import java.awt.Point;
import Robot.RobotConstants.Direction;
import Map.*;

public class Sensor {
	
	//Ranges of the Sensors
	private String id;
	private int minRange;
    private int maxRange;
    private double prevData;
    private double prevRawData;

    // Sensor's position on the map
    private Point sensorPos;

    private Direction sensorDir;

    public Sensor(String id, int minRange, int maxRange, int sensorPosRow, int sensorPosCol, Direction sensorDirection) {
        this.id = id;
    	this.minRange = minRange;
        this.maxRange = maxRange;
        this.sensorPos = new Point(sensorPosCol, sensorPosRow);
        this.sensorDir = sensorDirection;
        this.prevData = 9;
        this.prevRawData = 99;
    }

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getMinRange() {
		return minRange;
	}

	public void setMinRange(int minRange) {
		this.minRange = minRange;
	}

	public int getMaxRange() {
		return maxRange;
	}

	public void setMaxRange(int maxRange) {
		this.maxRange = maxRange;
	}

	public Point getPos() {
		return sensorPos;
	}
	
	public int getRow() {
		return sensorPos.y;
	}
	
	public int getCol() {
		return sensorPos.x;
	}

	public void setPos(int col, int row) {
		this.sensorPos.setLocation(col, row);
	}

	public Direction getSensorDir() {
		return sensorDir;
	}

	public void setSensorDir(Direction sensorDir) {
		this.sensorDir = sensorDir;
	}
    
	//Detect method for simulator
	//Senses where the obstacle is of the sensor if none detected return -1
	//Detected within the min and max range of sensor
	public int detect(Map map) {
		// Checking the range
		for (int cur = minRange; cur <= maxRange; cur++) {
			switch (sensorDir) {
				case UP:
					if (sensorPos.y + cur > Map.MAP_HEIGHT - 1)
						return -1;
					else if (map.getSquare(sensorPos.y + cur, sensorPos.x).isObstacle())
						return cur;
					break;
				case RIGHT:
					if (sensorPos.x + cur > Map.MAP_WIDTH - 1)
						return -1;
					else if (map.getSquare(sensorPos.y, sensorPos.x + cur).isObstacle())
						return cur;
					break;
				case DOWN:
					if (sensorPos.y - cur < 0)
						return -1;
					else if (map.getSquare(sensorPos.y - cur, sensorPos.x).isObstacle())
						return cur;
					break;
				case LEFT:
					if (sensorPos.x - cur < 0)
						return -1;
					else if (map.getSquare(sensorPos.y, sensorPos.x - cur).isObstacle())
						return cur;
					break;
			}
		}
		return -1;
	}

	public double getPrevData() {
		return prevData;
	}

	public void setPrevData(double prevData) {
		this.prevData = prevData;
	}

	public double getPrevRawData() {
		return prevRawData;
	}

	public void setPrevRawData(double prevRawData) {
		this.prevRawData = prevRawData;
	}
}
