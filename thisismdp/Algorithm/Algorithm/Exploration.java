package Algorithm;

import java.awt.Point;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import Map.*;
import Robot.*;
import Robot.RobotConstants.Command;
import Robot.RobotConstants.Direction;

public class Exploration {

    private double coverageLimit;
    private int timeLimit;

    private double areaExplored;
    private long startTime;
    private long endTime;

    private int stepPerSecond;
    private Point start;
    private boolean simulator;
    private Map map;
    private Robot robot;
    private Map exploredMap;

    public Exploration(Map exploredMap, Map map, Robot robot, double coverageLimit, int timeLimit, int stepPerSecond,
            boolean simulator) {
        this.exploredMap = exploredMap;
        this.map = map;
        this.robot = robot;
        this.coverageLimit = coverageLimit;
        this.timeLimit = timeLimit;
        this.stepPerSecond = stepPerSecond;
        this.simulator = simulator;
    }

    // getter setter
    public double getCoverageLimit() {
        return coverageLimit;
    }

    public void setCoverageLimit(double coverageLimit) {
        this.coverageLimit = coverageLimit;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
    }

    public Map getExploredMap() {
        return exploredMap;
    }

    public void setExploredMap(Map exploredMap) {
        this.exploredMap = exploredMap;
    }

    public Map getMap() {
        return map;
    }

    public void setMap(Map map) {
        this.map = map;
    }

    //////////////////////////////////////////////////////////////

    // EXPLORATION
    public void exploration(Point start) {
        // init
        this.start = start;
        // repeated move counter , for efficiency checking 4 steps atm
        // XYZCHNG change moves to repeatingMovesCnt, checkingStep to
        // repeatingMovesLimit
        int repeatingMovesCnt = 1;
        int repeatingMovesLimit = 4;

        // time
        startTime = System.currentTimeMillis();
        endTime = startTime + timeLimit;

        // map
        double prevArea = 0;
        areaExplored = exploredMap.exploredPercentage();
        
        boolean exploreComplete = false;
        System.out.println("Start exploration");
        int x = 0;
        // outerLoop
        outer: do {
            // update prevArea with previous loop areaExplored
            prevArea = areaExplored;
            x++;
            // try to move using right hug algo
            try {
                getMove();
            } catch (Exception e) {
                System.out.println("Error @ exploration getMove()");
                e.printStackTrace();
            }

            // renew areaExplored with newest values
            areaExplored = exploredMap.exploredPercentage();

            /*
            System.out.println("Current: " + System.currentTimeMillis());
            System.out.println("End: " + endTime);
            System.out.println(System.currentTimeMillis() >= endTime);
            
            */

            // if robot did not explore new areas, cnt++
            if (prevArea == areaExplored) {
                repeatingMovesCnt++;
            } else {
                repeatingMovesCnt = 1;
            }

            if(areaExplored>=100)
            {
            	exploreComplete = true;
            	break;
            }
            if(coverageLimit!=0)
            {
            	if(areaExplored>=coverageLimit)
            	{
            		exploreComplete = true;
            		break;
            	}
            }
            if(timeLimit!=0)
            {
            	if(System.currentTimeMillis() >= endTime)
            	{
            		exploreComplete = true;
            		break;
            	}
            }
            
            if (repeatingMovesCnt % repeatingMovesLimit == 0 || robot.getPosition().distance(start) == 0) {
                while (prevArea == areaExplored) {
                    if (!goToUnexplored()) {
                        break outer;
                    }
                    areaExplored = exploredMap.exploredPercentage();
                }
                repeatingMovesCnt = 1;
                repeatingMovesLimit = 3;
            }
        } while (!exploreComplete);

        // go back to Start
        System.out.println("ending exploration....");
        goToPoint(start);
        endTime = System.currentTimeMillis();
        double seconds = ((endTime - startTime) / 1000);
        System.out.println("Total Time: " + seconds);
    }

    // HUG ME TO THE RIGHT
    // throws interupted exception(?)
    public void getMove() throws InterruptedException {
        // XYZCHNG dir to curDir
        Direction curDir = robot.getDirection();

        // Check right is free then move right
        // XYZCHNG movable with directionIsFree
        if (directionIsFree(Direction.rotateCW(curDir))) {
            sleepSimTime(simulator);
            robot.move(Command.TURN_RIGHT, RobotConstants.MOVE_STEPS, exploredMap);
            robot.sense(exploredMap, map);
            // if after turn right can go forward, go forward
            if (directionIsFree(robot.getDirection())) {
                sleepSimTime(simulator);
                robot.move(Command.FORWARD, RobotConstants.MOVE_STEPS, exploredMap);
                robot.sense(exploredMap, map);
            }
            // else if can go forward, go forward
        } else if (directionIsFree(curDir)) {
            sleepSimTime(simulator);
            robot.move(Command.FORWARD, RobotConstants.MOVE_STEPS, exploredMap);
            robot.sense(exploredMap, map);
        }

        else {
            // XYZCHNG just turn left no need backwards
            sleepSimTime(simulator);
            robot.move(Command.TURN_LEFT, RobotConstants.MOVE_STEPS, exploredMap);
            robot.sense(exploredMap, map);

            if (directionIsFree(robot.getDirection())) {
                sleepSimTime(simulator);
                robot.move(Command.FORWARD, RobotConstants.MOVE_STEPS, exploredMap);
                robot.sense(exploredMap, map);
            }
        }
    }

    // you know what it does
    public boolean goToUnexplored() {
        System.out.println("Go to unexplored");
        // XYZCHNG unSquare and Square -> unExpSquare, expSquare
        Square unExpSquare = exploredMap.nearestUnexploredSquare(robot.getPosition());
        // XYCHNG look up
        Square expSquare = exploredMap.nearestExploredSquare(unExpSquare.getPos(), robot.getPosition());
        System.out.println(expSquare);
        if (expSquare == null) {
            return false;
        }

        System.out.println("Explored: " + expSquare.toString());

        return goToPoint(expSquare.getPos());
    }

    public boolean goToPoint(Point loc) {
        System.out.println("go to a Point");

        if (robot.getPosition().equals(start) && loc.equals(start)) {
            // fix direction to up
            while (robot.getDirection() != Direction.UP) {
                sleepSimTime(simulator);
                robot.sense(exploredMap, map);
                robot.move(Command.TURN_RIGHT, RobotConstants.MOVE_STEPS, exploredMap);

            }
            return false;
        }
        // init arrays
        ArrayList<Command> commands = new ArrayList<Command>();
        ArrayList<Square> path = new ArrayList<Square>();

        // generate fastest path to a point with FastestPath algo
        FastestPath fp = new FastestPath(exploredMap, robot, simulator);
        path = fp.run(robot.getPosition(), loc, robot.getDirection());
        // exit if no path found
        if (path == null)
            return false;

        fp.displayFastestPath(path, true);
        commands = fp.getPathCommands(path);

        System.out.println("Exploration Fastest Path Commands" + commands);

        // if go to a point other than start
        if (!loc.equals(start)) {
            for (Command c : commands) {
                System.out.println("Command: " + c);
                if ((c == Command.FORWARD) && !directionIsFree(robot.getDirection())) {
                    System.out.println("Cannot execute go forward");
                    break;
                } else {
                    // XYZCHNG remove step to check last move turn
                    robot.move(c, RobotConstants.MOVE_STEPS, exploredMap);
                    robot.sense(exploredMap, map);
                }
                sleepSimTime(simulator);
            }

            // if robot gets lost during exp
            if (!loc.equals(start) && exploredMap.exploredPercentage() < 100
                    && directionIsFree(Direction.rotateCW(robot.getDirection()))) {
                // XYZCHNG refactor
                fixRobotLost();
            }
        }

        // else if goes to start
        else {
            goToStart(commands, loc);
        }
        return true;

    }

    public void fixRobotLost() {
        Direction dir = nearestVirtualBarrier(robot.getPosition());

        // if not yet at WALL
        if (directionIsFree(dir)) {

            // align robot to face WALL
            while (dir != robot.getDirection()) {
                // XYZCHNG remove if branching turn right
                robot.move(Command.TURN_LEFT, RobotConstants.MOVE_STEPS, exploredMap);
            }

            // Move towards wall
            while (directionIsFree(robot.getDirection())) {
                robot.move(Command.FORWARD, RobotConstants.MOVE_STEPS, exploredMap);
                sleepSimTime(simulator);
                robot.sense(exploredMap, map);
            }

            // Align robot for right side to hug wall
            while (Direction.rotateACW(dir) != robot.getDirection()) {
                robot.move(Command.TURN_LEFT, RobotConstants.MOVE_STEPS, exploredMap);
                sleepSimTime(simulator);
                robot.sense(exploredMap, map);
            }
        }

    }

    // refactor else in goToPoint
    public void goToStart(ArrayList<Command> commands, Point loc) {
        int moveForwardCnt = 0;
        Command c = null;
        for (int i = 0; i < commands.size(); i++) {
            c = commands.get(i);
            sleepSimTime(simulator);
            if ((c == Command.FORWARD) && !directionIsFree(robot.getDirection())) {
                System.out.println("cannot command move forward");
                break;
            } else {
                if (c == Command.FORWARD) {
                    // increment forward counter to unleash them later
                    moveForwardCnt++;

                    // if last move alr, just go ahead
                    if (i == (commands.size() - 1)) {
                        robot.move(c, moveForwardCnt, exploredMap);
                        robot.sense(exploredMap, map);
                    }
                } else {
                    if (moveForwardCnt > 0) {
                        // the point robot start giving command to turn, unleash all forward
                        robot.move(Command.FORWARD, moveForwardCnt, exploredMap);
                        robot.sense(exploredMap, map);
                    }
                    robot.move(c, RobotConstants.MOVE_STEPS, exploredMap);
                    robot.sense(exploredMap, map);

                    // reset move forward counter
                    moveForwardCnt = 0;
                }
            }
        }

        // after reaching start, make robot face UP
        if (loc.equals(start)) {
            while (robot.getDirection() != Direction.UP) {
                robot.move(Command.TURN_RIGHT, RobotConstants.MOVE_STEPS, exploredMap);
                sleepSimTime(simulator);
                System.out.println("Robot move to direction" + robot.getDirection());
                robot.sense(exploredMap, map);
                if(!simulator && !directionIsFree(robot.getDirection())) {
                  robot.robotMessageTransmit("Ard", Command.ALIGN_FRONT, 0);
                  robot.robotMessageBeepBeep();
                  if(!directionIsFree(Direction.rotateCW(robot.getDirection()))) {
                      robot.robotMessageTransmit("Ard", Command.ALIGN_RIGHT, 0);
                      robot.robotMessageBeepBeep();
                  }
            }
        }
    }
        }
    

    // refactor for if(simulator)
    public void sleepSimTime(boolean simulator) {
        if (simulator)
            try {
                TimeUnit.MILLISECONDS.sleep(RobotConstants.MOVE_SPEED / stepPerSecond);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
    }

    // returns nearest dir to WALL
    public Direction nearestVirtualBarrier(Point pos) {
        int rowInc, colInc, lowest = 1000, lowestIter = 0, curDist = 0;
        Direction dir = Direction.RIGHT;
        System.out.println("Finding Nearest Wall");

        // find straight distance nearest wall
        for (int i = 0; i < 4; i++) {
            rowInc = (int) Math.sin(Math.PI / 2 * i);
            colInc = (int) Math.cos(Math.PI / 2 * i);
            curDist = 0;
            int j = 1;
            // XYZCHNG change for to while
            while (j < Map.MAP_HEIGHT) {
                if (exploredMap.checkValidSquare(pos.y + rowInc * j, pos.x + colInc * j)) {
                    // Keep Looping till reached a virtual wall
                    if (exploredMap.clearForRobot(pos.y + rowInc * j, pos.x + colInc * j))
                        curDist++;
                    else
                        break;
                }
                // search lead to end of grid
                else
                    break;
                j++;
            }
            System.out.println("Direction: " + i + " " + curDist);

            // Evaluate 'lowest' from curDist
            if (curDist < lowest) {
                // lowest horizontal distance to wall
                lowest = curDist;
                // direction with the lowest distance to wall, from RIGHT
                lowestIter = i;
            }
        }

        // use lowestiter to get direction
        for (int c = 0; c < lowestIter; c++) {
            dir = Direction.rotateACW(dir);
        }

        return dir;
    }

    // Returns true if a direction is free
    public boolean directionIsFree(Direction dir) {
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

        case RIGHT:
            rowInc = 0;
            colInc = 1;
            break;

        case DOWN:
            rowInc = -1;
            colInc = 0;
            break;
        }
        return exploredMap.checkValidAction(robot.getPosition().y + rowInc, robot.getPosition().x + colInc);

    }
}