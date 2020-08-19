package Map;

import java.awt.Point;

public class Square {
	private Point position;
	
	//Booleans for Exploration
	private boolean explored;
	private boolean obstacle;
	private boolean virtualBarrier;
	private boolean isWaypoint;
	private boolean covered;
	private boolean path;
	
	public Square(Point position) {
		this.position = position;
		this.explored = false;
	}
	
	//Getters & Setters
	
	public boolean isPath() {
		return this.path;
	}
	
	public void setPath(boolean path) {
		this.path = path;
	}
	
	public boolean isWaypoint() {
		return isWaypoint;
	}
	
	public boolean setWaypoint(boolean isWaypoint) {
		if(!obstacle && explored && !virtualBarrier) {
			this.isWaypoint = isWaypoint;
			return true;
		}
		else
		{
			return false;
		}
	}
	public boolean isExplored() {
		return this.explored;
	}
	public void setExplored(boolean explored) {
		this.explored = explored;
	}
	public boolean isObstacle() {
		return this.obstacle;
	}
	public void setObstacle(boolean obstacle) {
		this.obstacle = obstacle;
	}
	public boolean isvirtualBarrier() {
		return this.virtualBarrier;
	}
	public void setvirtualBarrier(boolean virtualBarrier) {
		this.virtualBarrier = virtualBarrier;
	}
	public Point getPos() {
		return this.position;
	}
	public void setPos(Point position) {
		this.position = position;
	}
	
	public boolean isCovered() {
		return covered;
	}

	public void setCovered(boolean covered) {
		this.covered = covered;
	}
	
	//Check if robot can move to the square
	public boolean movableSquare() {
		return explored && !obstacle && !virtualBarrier;
	}
	
	@Override
	public String toString() {
		return "Square [pos=" + position + ", explored=" + explored + ", obstacle=" + obstacle + ", virtualBarrier=" + virtualBarrier+ ", isWayPoint=" + isWaypoint + ", covered=" + covered + ", path=" + path + "]";
	}
}
