import java.util.Scanner;

import Network.*;
import Robot.*;
import Map.*;
import Robot.RobotConstants.Direction;
import Robot.RobotConstants.Command;

public class TestBed {
	private static NetMgr net;
	private static final NetMgr netMgr = NetMgr.getInstance();

	public static void main(String[] args) {
		// Auto-generated method stub
		Robot robot = new Robot(false, Direction.UP, 1, 1);
		System.out.println(Command.FORWARD.ordinal());
		char[] moves = {'W','A','D','S'};
		
		netMgr.startConn();
		robot.robotMessageTransmit("Ard", Command.FORWARD, 1);	
		String msg = robot.robotMessageBeepBeep();
		System.out.println(msg);
		
//		robot.robotMessageTransmit("Ard", Command.TURN_LEFT, 1);

	//	robot.move(Command.TURN_RIGHT, RobotConstants.MOVE_STEPS, exploredMap);
		System.out.println(moves[Command.FORWARD.ordinal()]);
			
			
			
//		net.send("Alg|Ard|0|1\n");
		//Keep trying to connect if fail to connec
		
	}
}