package Robot;

import javafx.scene.paint.Color;

public class RobotConstants {

	// G values used for A* algorithm
	public static final int MOVE_COST = 1;
	public static final int TURN_COST = 5;
	public static final double INFINITE_COST = 10000000;
//	public static final int CALIBRATE_AFTER = 3; //Calibrate After number of moves
	
	public static final int MOVE_STEPS = 1;
	public static final int MOVE_SPEED = 5000;	//Delays before movement (Lower = faster) in milliseconds
	public static final long WAIT_TIME = 5000;	//Time waiting before retransmitting in milliseconds
	public static final short CAMERA_RANGE = 4;

	// Sensors default range (In grids)
	public static final int SMALL_MIN = 1;
	public static final int SMALL_MAX = 3;

	public static final int BIG_MIN = 1;
	public static final int BIG_MAX = 5;
	
	public static final double RIGHT_LIMIT = 0.5; // distance limit where right sensor will do calibration once exceeded
	public static final double RIGHT_NEAR_LIMIT = 1.0;
	public static final double RIGHT_FAR_LIMIT = 3.8;
	//Constants to render Robot
	public static final Color ROBOT_BODY = Color.rgb(139, 0, 0, 0.8);
	public static final Color ROBOT_OUTLINE = Color.BLACK;
	public static final Color ROBOT_DIRECTION = Color.WHITESMOKE;
	
	// Direction enum based on compass
	public static enum Direction {
		UP, LEFT, DOWN, RIGHT;

		// Used to Get the new direction, when robot turns right
		public static Direction rotateACW(Direction currDirection) {
			return values()[(currDirection.ordinal() + 1) % values().length];
		}

		// Used to Get the new direction, when robot turns left
		public static Direction rotateCW(Direction currDirection) {
			return values()[(currDirection.ordinal() + values().length - 1) % values().length];
		}
		
		public static Direction reverse(Direction currDirection) {
			return values()[(currDirection.ordinal() + 2) % values().length];
		}

	};
	
	
	public static enum Command{
		FORWARD, TURN_LEFT, TURN_RIGHT, BACKWARD, ALIGN_FRONT, ALIGN_RIGHT, START_EXP, START_FP, END, SET_WAYPOINT, CURRENT_POS, SEND_SENSORS, ERROR;
	}
}
