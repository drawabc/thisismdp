package Algorithm;

import java.awt.Point;

import Robot.*;
import Robot.RobotConstants.Direction;
import Robot.RobotConstants.Command;
import Map.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.smartcardio.CommandAPDU;

public class FastestPath {
    private Robot robot;
    private Map exploredMap;

    private HashMap<Square, Square> prevSquare;

    // A star stuffs
    private ArrayList<Square> toVisit;
    private ArrayList<Square> visited;

    // g(u)
    private HashMap<Point, Double> costG;

    // generated path
    private ArrayList<Square> path;
    private boolean simulator = true;

    public FastestPath(Map exploredMap, Robot robot, boolean simulator) {
        // init
        this.exploredMap = exploredMap;
        this.robot = robot;
        this.simulator = simulator;
        prevSquare = new HashMap<Square, Square>();

        costG = new HashMap<Point, Double>();

        // Init costG array for explored Square
        for (int row = 0; row < Map.MAP_HEIGHT; row++) {
            for (int col = 0; col < Map.MAP_WIDTH; col++) {
                // if the Square is obstacle/ unexplored/ unexplored CostG is high
                if (!exploredMap.getSquare(row, col).movableSquare())
                    costG.put(new Point(col, row), RobotConstants.INFINITE_COST);

                // Else set costG for the Square to 0 first
                else
                    costG.put(new Point(col, row), 0.0);
            }
        }

    }

    private double getCostG(Point SquareA, Point SquareB, Direction dir) {
        double moveCost = RobotConstants.MOVE_COST;
        double turnCost = getTurnCost(dir, getSquareDirection(SquareA, SquareB));
        return moveCost + turnCost;
    }

    // heuristics destination to goal
    private double getCostH(Point Square, Point goal) {
        return Square.distance(goal);
    }

    // Get the direction of SquareB from SquareA
    private Direction getSquareDirection(Point SquareA, Point SquareB) {
        if (SquareA.y - SquareB.y > 0)
            return Direction.DOWN;
        else if (SquareA.y - SquareB.y < 0)
            return Direction.UP;
        else if (SquareA.x - SquareB.x > 0)
            return Direction.LEFT;
        else
            return Direction.RIGHT;
    }

    private double getTurnCost(Direction a, Direction b) {
        // Max of 2 turns in either direction same direction will get 0
        int turns = Math.abs(a.ordinal() - b.ordinal());
        if (turns > 2)
            turns %= 2;

        return turns * RobotConstants.TURN_COST;
    }

    private Square minCostSquare(Point goal) {
        double min = 10999;
        double cost;
        Point p;
        Square Square = null;

        for (int i = 0; i < toVisit.size(); i++) {
            p = toVisit.get(i).getPos();

            cost = costG.get(p) + getCostH(p, goal);
            if (cost < min) {
                min = cost;
                Square = toVisit.get(i);
            }
        }
        return Square;
    }

    // the real A STAR
    public ArrayList<Square> run(Point start, Point goal, Direction dir) {
        System.out.println("A* running");
        double gCost;

        path = new ArrayList<Square>();
        toVisit = new ArrayList<Square>();
        visited = new ArrayList<Square>();
        Square cur = exploredMap.getSquare(start);
        toVisit.add(cur);
        ArrayList<Square> neighbours;

        // Set curDir to starting dir
        Direction curDir = dir;

        while (!toVisit.isEmpty()) {
            cur = minCostSquare(goal);

            // get direction of next square from previous Square
            if (prevSquare.containsKey(cur))
                curDir = getSquareDirection(prevSquare.get(cur).getPos(), cur.getPos());

            visited.add(cur);
            toVisit.remove(cur);

            // check if goal reached
            if (visited.contains(exploredMap.getSquare(goal))) {
                path = getPath(start, goal);
                return path;
            }

            neighbours = exploredMap.getNeighbours(cur);
            // XYZCHNG refactor for loop
            for (Square neighbor : neighbours) {
                if(visited.contains(neighbor))
					continue;
				
				gCost = costG.get(cur.getPos())+ getCostG(cur.getPos(),neighbor.getPos(),curDir);
                
                
                // if neighbor not in to visit update prevSquare(links) and costG(cost)
                if (!toVisit.contains(neighbor)) {
                    prevSquare.put(neighbor, cur);

                    costG.put(neighbor.getPos(), gCost);
                    toVisit.add(neighbor);
                } else {
                    // XYZCHNG change curGCost to oldGCost
                    double oldGCost = costG.get(neighbor.getPos());
                    // if old path cost to neighbor is more expensive update
                    if (gCost < oldGCost) {
                        costG.replace(neighbor.getPos(), gCost);
                        prevSquare.replace(neighbor, cur);
                    }
                }
            }
        }
        // if cannot find path
        System.out.println("Failed ASTAR return null");
        return null;
    }

    // returns Squares forming a path from start to goal from the links in
    // prevSquare
    public ArrayList<Square> getPath(Point start, Point goal) {
        Square cur = exploredMap.getSquare(goal);
        Square startSquare = exploredMap.getSquare(start);
        ArrayList<Square> path = new ArrayList<Square>();

        while (cur != startSquare) {
            path.add(cur);
            cur = prevSquare.get(cur);
        }
        Collections.reverse(path);
        return path;
    }

    // display fastest path on simulator
    public void displayFastestPath(ArrayList<Square> path, boolean display) {
        System.out.println("Number of steps = " + (path.size() - 1) + "\n");

        Square temp;
        System.out.println("Path: ");
        for (int i = 0; i < path.size(); i++) {
            temp = path.get(i);

            // Set the path Squre to display as path on simulator
            exploredMap.getSquare(temp.getPos()).setPath(display);
            System.out.println(exploredMap.getSquare(temp.getPos()).toString());

            // Output Path on console
            if (i != (path.size() - 1))
                System.out.print("(" + temp.getPos().y + ", " + temp.getPos().x + ") --> ");
            else
                System.out.print("(" + temp.getPos().y + ", " + temp.getPos().x + ")");

        }
    }

    public ArrayList<Command> getPathCommands(ArrayList<Square> path) {
        // init
        Robot tempRobot = new Robot(true, robot.getDirection(), robot.getPosition().y, robot.getPosition().x);
        tempRobot.setFastSense(true);
        // XYZCHNG moves to commandList
        ArrayList<Command> commandList = new ArrayList<Command>();

        Command move;

        // XYZCHNG change Square to curSquare
        Square curSquare = exploredMap.getSquare(tempRobot.getPosition());
        Square newSquare;

        // XYZCHNG change SquareDir to nextDir
        Direction nextDir;

        // iterate through path
        for (int i = 0; i < path.size(); i++) {
            move = Command.ERROR;
            newSquare = path.get(i);

            // XYZCHNG SquareDir to nextDir
            nextDir = getSquareDirection(curSquare.getPos(), newSquare.getPos());

            // if next Square is in different direction with robot
            if (tempRobot.getDirection() != nextDir) {
                if (Direction.reverse(tempRobot.getDirection()) == nextDir) {
                    // turn left twice or turnaround
                    move = Command.TURN_LEFT;
                    tempRobot.move(move, RobotConstants.MOVE_STEPS, exploredMap);
                    commandList.add(move);
                    tempRobot.move(move, RobotConstants.MOVE_STEPS, exploredMap);
                    commandList.add(move);
                } else {
                    move = getTurnMovement(tempRobot.getDirection(), nextDir);
                    tempRobot.move(move, RobotConstants.MOVE_STEPS, exploredMap);
                    commandList.add(move);
                }
            }
            // afterwards move Forward, moving from one block to another is FORWARD move
            move = Command.FORWARD;
            tempRobot.move(move, RobotConstants.MOVE_STEPS, exploredMap);
            commandList.add(move);
            curSquare = newSquare;
        }
        return commandList;
    }

    // Determine if a Square is a calibration point (all front and right Square are
    // virtual walls)
    public boolean calibratePoint(Square cur, Direction robotDir) {
        Square front1 = cur, front2 = cur, front3 = cur, right1 = cur, right2 = cur;
        switch (robotDir) {
        case UP:
            front1 = exploredMap.getSquare(cur.getPos().y + 1, cur.getPos().x - 1);
            front2 = exploredMap.getSquare(cur.getPos().y + 1, cur.getPos().x);
            front3 = exploredMap.getSquare(cur.getPos().y + 1, cur.getPos().x + 1);
            right1 = exploredMap.getSquare(cur.getPos().y + 1, cur.getPos().x + 1);
            right2 = exploredMap.getSquare(cur.getPos().y - 1, cur.getPos().x + 1);
            break;
        case DOWN:
            front1 = exploredMap.getSquare(cur.getPos().y - 1, cur.getPos().x - 1);
            front2 = exploredMap.getSquare(cur.getPos().y - 1, cur.getPos().x);
            front3 = exploredMap.getSquare(cur.getPos().y - 1, cur.getPos().x + 1);
            right1 = exploredMap.getSquare(cur.getPos().y - 1, cur.getPos().x - 1);
            right2 = exploredMap.getSquare(cur.getPos().y + 1, cur.getPos().x - 1);
            break;
        case RIGHT:
            front1 = exploredMap.getSquare(cur.getPos().y + 1, cur.getPos().x + 1);
            front2 = exploredMap.getSquare(cur.getPos().y, cur.getPos().x + 1);
            front3 = exploredMap.getSquare(cur.getPos().y - 1, cur.getPos().x + 1);
            right1 = exploredMap.getSquare(cur.getPos().y - 1, cur.getPos().x + 1);
            right2 = exploredMap.getSquare(cur.getPos().y - 1, cur.getPos().x - 1);
            break;
        case LEFT:
            front1 = exploredMap.getSquare(cur.getPos().y - 1, cur.getPos().x - 1);
            front2 = exploredMap.getSquare(cur.getPos().y, cur.getPos().x - 1);
            front3 = exploredMap.getSquare(cur.getPos().y + 1, cur.getPos().x - 1);
            right1 = exploredMap.getSquare(cur.getPos().y - 1, cur.getPos().x - 1);
            right2 = exploredMap.getSquare(cur.getPos().y + 1, cur.getPos().x + 1);
            break;
        }
        if (front1.isvirtualBarrier() && front2.isvirtualBarrier() && front3.isvirtualBarrier()
                && right1.isvirtualBarrier() && right2.isvirtualBarrier())
            return true;

        return false;
    }

    public Command getTurnMovement(Direction curDir, Direction nextDir) {
        Command cmd = Command.ERROR;
        if (curDir.ordinal() < nextDir.ordinal()) {
            cmd = Command.TURN_LEFT;
        } else {
            cmd = Command.TURN_RIGHT;
        }
        if (curDir == Direction.UP) {
            if (nextDir == Direction.RIGHT)
                cmd = Command.TURN_RIGHT;
        }
        if (curDir == Direction.RIGHT) {
            if (nextDir == Direction.UP)
                cmd = Command.TURN_LEFT;
        }
        return cmd;
    }

} // class boundary