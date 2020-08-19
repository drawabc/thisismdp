import java.awt.Point;
import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

import Algorithm.*;
import Map.*;
import Map.Map;
import Network.NetMgr;
import Robot.*;
import Robot.RobotConstants.Command;
import Robot.RobotConstants.Direction;

//JavaFX Libraries
import javafx.application.Application;
import javafx.geometry.*;
import javafx.event.*;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import sun.misc.GC;
import javafx.concurrent.Task;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;

//Program Classes

public class Simulator extends Application {
	private static final NetMgr netMgr = NetMgr.getInstance();
	
	//Program Variables
	private Map map;	//Holds the simulator's loaded map
	private Map exploredMap;
	private Point wayPoint = new Point(Map.GOALZONE);
	private Robot robot;
	private boolean expMapDraw = true;
	private boolean sim = true;

	private boolean setObstacle = false;
	private boolean setWayPoint = false;
	private boolean setRobot = false;
	private boolean taskStart = false;
	
	//GUI Components
	private GraphicsContext gc;
	private Canvas mapGrid;
	
	//UI Componets
	private Label lTimeLimit, lCoverageLimit, lSteps;
	private TextField tTimeLimit, tCoverageLimit, tSteps;
	private ComboBox<String> cbMode;
	private Button btnConnect, btnStart, btnSetWayPoint, btnSetRobot, btnSetObstacle, btnRestartMap, btnLoadMap, btnSaveMap;
	private FileChooser fileChooser;
	private ScrollBar sbTimeLimit, sbCoverageLimit, sbSteps;
	
	//Simulator Mode
	private final String REAL_FAST = "Real Fastest Path";
	private final String REAL_EXP = "Real Exploration";
	private final String SIM_FAST = "Simulation Fastest Path";
	private final String SIM_EXP = "Simulation Exploration Path";

	// Threads for each of the tasks
	private Thread fastTask, expTask;
	
	public static void main(String[] args)
	{
		launch(args);
	}
	
	//Draw Map Graphics
	private void drawMap(boolean explored)
	{
		//Draw the Map Graphics
		gc.setStroke(Map.CW_COLOR);
		gc.setLineWidth(2);
		
		//Draw the Square on the Map Canvas
		for(int row = 0; row < Map.MAP_HEIGHT;row++)
		{
			for(int col = 0; col < Map.MAP_WIDTH; col++)
			{
				//If Start Zone
				if((row <= Map.STARTZONE_ROW + 1) && (col <= Map.STARTZONE_COL + 1))
				{
					gc.setFill(Map.SZ_COLOR);
				}
				//If Goal Zone
				else if( (row >= Map.GOALZONE_ROW - 1) && (col >=Map.GOALZONE_COL - 1))
				{
					gc.setFill(Map.GZ_COLOR);
				}
				//If other Squares on the map
				else
				{
					//If Square has been explored
					if(explored)
					{
						//If it is an obstacle
						if(exploredMap.getSquare(row, col).isObstacle())
						{
							gc.setFill(Map.OB_COLOR);							
						}
						//If it is a path
						else if(exploredMap.getSquare(row, col).isPath())
						{
							gc.setFill(Map.PH_COLOR);							
						}
						//If it is covered
						else if(exploredMap.getSquare(row, col).isCovered())
						{
							gc.setFill(Map.THRU_COLOR);							
						}
						//If it has been explroed
						else if (exploredMap.getSquare(row, col).isExplored())
						{
							gc.setFill(Map.EX_COLOR);							
						}
						else
						{
							gc.setFill(Map.UE_COLOR);							
						}
					}
					else
					{
						//If it is an obstacle
						if(map.getSquare(row, col).isObstacle())
						{
							gc.setFill(Map.OB_COLOR);
						}
						else
						{
							gc.setFill(Map.EX_COLOR);
						}
					}
				}
				
				//Draw Square on the Map based on indicated Position
				gc.strokeRect(
						col * Map.MAP_Square_SZ + Map.MAP_OFFSET / 2,
						(Map.MAP_Square_SZ - 1) * Map.MAP_HEIGHT - row * Map.MAP_Square_SZ
						+ Map.MAP_OFFSET / 2,
						Map.MAP_Square_SZ,
						Map.MAP_Square_SZ
				);
				gc.fillRect(
						col * Map.MAP_Square_SZ + Map.MAP_OFFSET / 2,
						(Map.MAP_Square_SZ - 1) * Map.MAP_HEIGHT - row * Map.MAP_Square_SZ
						+ Map.MAP_OFFSET / 2,
						Map.MAP_Square_SZ,
						Map.MAP_Square_SZ
				);
			}
			
			//Draw waypoint on the map
			if(wayPoint != null)
			{
				gc.setFill(Map.WP_COLOR);
				gc.fillRect(
						wayPoint.getX() * Map.MAP_Square_SZ + Map.MAP_OFFSET / 2,
						(Map.MAP_Square_SZ - 1) * Map.MAP_HEIGHT
						- wayPoint.getY() * Map.MAP_Square_SZ + Map.MAP_OFFSET / 2,
						Map.MAP_Square_SZ,
						Map.MAP_Square_SZ
				);
				gc.setFill(Color.BLACK);
				gc.fillText(
						"W",
						wayPoint.getX() * Map.MAP_Square_SZ + Map.MAP_OFFSET / 2
						+ Map.Square_CM / 2,
						(Map.MAP_Square_SZ - 1) * Map.MAP_HEIGHT
						- (wayPoint.getY() - 1) * Map.MAP_Square_SZ + Map.MAP_OFFSET / 2
						- Map.Square_CM / 2
				);
			}
		}
	}

	//Draw Robot
	public void drawRobot()
	{
		gc.setStroke(RobotConstants.ROBOT_OUTLINE);
		gc.setLineWidth(2);

		gc.setFill(RobotConstants.ROBOT_BODY);
		
		int col = robot.getPosition().x - 1;
		int row = robot.getPosition().y + 1;
		int dirCol = 0, dirRow = 0;
		
		gc.strokeOval(
				col * Map.MAP_Square_SZ + Map.MAP_OFFSET / 2,
				(Map.MAP_Square_SZ - 1) * Map.MAP_HEIGHT - row * Map.MAP_Square_SZ
				+ Map.MAP_OFFSET / 2,
				3 * Map.MAP_Square_SZ,
				3 * Map.MAP_Square_SZ
		);
		gc.fillOval(
				col * Map.MAP_Square_SZ + Map.MAP_OFFSET / 2,
				(Map.MAP_Square_SZ - 1) * Map.MAP_HEIGHT - row * Map.MAP_Square_SZ
						+ Map.MAP_OFFSET / 2,
				3 * Map.MAP_Square_SZ,
				3 * Map.MAP_Square_SZ
		);

		gc.setFill(RobotConstants.ROBOT_DIRECTION);
		switch (robot.getDirection()) {
		case UP:
			dirCol = robot.getPosition().x;
			dirRow = robot.getPosition().y + 1;
			break;
		case DOWN:
			dirCol = robot.getPosition().x;
			dirRow = robot.getPosition().y - 1;
			break;
		case LEFT:
			dirCol = robot.getPosition().x - 1;
			dirRow = robot.getPosition().y;
			break;
		case RIGHT:
			dirCol = robot.getPosition().x + 1;
			dirRow = robot.getPosition().y;
			break;
		}
		
		gc.fillOval(
				dirCol * Map.MAP_Square_SZ + Map.MAP_OFFSET / 2,
				(Map.MAP_Square_SZ - 1) * Map.MAP_HEIGHT - dirRow * Map.MAP_Square_SZ
						+ Map.MAP_OFFSET / 2,
				Map.MAP_Square_SZ,
				Map.MAP_Square_SZ
		);
		
		gc.setFill(Color.BLACK);
		
		for (Sensor s : robot.getSensorList()) {
			gc.fillText(
					s.getId(),
					s.getCol() * Map.MAP_Square_SZ + Map.MAP_OFFSET / 2,
					(Map.MAP_Square_SZ) * Map.MAP_HEIGHT - s.getRow() * Map.MAP_Square_SZ
							+ Map.MAP_OFFSET / 2);
		}
	}
	
	//Place Obstacle
	private boolean setObstacle(int row, int col)
	{
		boolean validity = false;
		//If Square is valid and not an existing obstacle
		if(map.checkValidSquare(row, col) && !map.getSquare(row, col).isObstacle())
		{
			map.getSquare(row, col).setObstacle(true);
			
			//Set virtual wall around obstacle
			for(int r = row - 1; r <= row + 1; r++)
			{
				for(int c = col - 1; c <= col + 1; c++)
				{
					if(map.checkValidSquare(r, c))
					{
						map.getSquare(r, c).setvirtualBarrier(true);
					}
				}
			}
			
			validity = true;
		}
		
		return validity;
		
	}

	//Remove Obstacle
	private boolean removeObstacle(int row, int col)
	{
		boolean validity = false;
		//If Square is valid and not an existing obstacle
		if(map.checkValidSquare(row, col) && map.getSquare(row, col).isObstacle())
		{
			map.getSquare(row, col).setObstacle(false);
			
			//Set virtual wall around obstacle
			for(int r = row - 1; r <= row + 1; r++)
			{
				for(int c = col - 1; c <= col + 1; c++)
				{
					if(map.checkValidSquare(r, c))
					{
						map.getSquare(r, c).setvirtualBarrier(false);
					}
				}
			}
			reinitVirtualBarrier();
			validity = true;
		}
		
		return validity;
		
	}
	
	//Reinitialise virtual walls around obstacles
	private void reinitVirtualBarrier()
	{
		for (int row = 0; row < Map.MAP_HEIGHT; row++) {
			for (int col = 0; col < Map.MAP_WIDTH; col++) {
				if (map.getSquare(row, col).isObstacle()) {
					for (int r = row - 1; r <= row + 1; r++)
						for (int c = col - 1; c <= col + 1; c++)
							if (map.checkValidSquare(r, c))
								map.getSquare(r, c).setvirtualBarrier(true);
				}
			}
		}		
	}
	
	//Set waypoint
	private boolean setWayPoint(int row, int col)
	{
		boolean validity = false;
		//Check if the waypoint is clear
		if(exploredMap.wayPointClear(row, col))
		{
			if(wayPoint!=null)
			{
				exploredMap.getSquare(wayPoint).setWaypoint(false);
			}
			wayPoint = new Point(col, row);
			if (!setObstacle)
			{
				expMapDraw = false;				
			}
			validity = true;
		}
		return validity;
	}

	//Set Robot Location & Rotate
	private boolean setRobotLocation(int row, int col)
	{
		boolean validity = false;
		
		//Check if it is a valid action at the location
		if(map.checkValidAction(row, col))
		{
			Point point = new Point (col, row);
			if(robot.getPosition().equals(point))
			{
				robot.move(Command.TURN_LEFT, RobotConstants.MOVE_STEPS, exploredMap);
				System.out.println("Robot Direction Changed to " + robot.getDirection().name());
			}
			else
			{
				robot.setStartPos(col, row, exploredMap);
				System.out.println("Robot moved to new position at row: " + row + " col:" + col);				
			}
			validity = true;
		}
		
		return validity;
	}

	
	//Simulator GUI
	public void start(Stage priStage)
	{
		map = new Map();
		//Set all explored for loading & saving map
		map.setAllExplored(true);
		exploredMap = new Map();
		
		//Default location at the startzone
		robot = new Robot(sim, Direction.UP, 1, 1);
		robot.setStartPos(robot.getPosition().x, robot.getPosition().y, exploredMap);
		
		//Setting Title & Values for the Window
		priStage.setTitle("Bob's Simulator");
		
		//Grid + Settings
		GridPane grid = new GridPane();
		GridPane controlGrid = new GridPane();
		grid.setAlignment(Pos.CENTER);
		grid.setHgap(5);
		grid.setVgap(5);
		grid.setPadding(new Insets(5, 5, 5, 5));

		controlGrid.setAlignment(Pos.TOP_CENTER);
		controlGrid.setHgap(10);
		controlGrid.setVgap(10);
		
		//Drawing
		mapGrid = new Canvas(
				Map.MAP_Square_SZ * Map.MAP_WIDTH + 1 + Map.MAP_OFFSET,
				Map.MAP_Square_SZ * Map.MAP_HEIGHT + 1 + Map.MAP_OFFSET
				);
		gc = mapGrid.getGraphicsContext2D();
		//expMapDraw = !setObstacle;
		expMapDraw = true;
		
		new Timer().scheduleAtFixedRate(new TimerTask() {
			public void run() {
				drawMap(expMapDraw);
				drawRobot();
			}
		},100,100);
		
		mapGrid.setOnMouseClicked(MapClick);
		
		//Labels
		lTimeLimit = new Label("Time Limit: ");
		lCoverageLimit = new Label("Coverage Limit:");
		lSteps = new Label("Steps: ");
		
		//Texts
		tTimeLimit = new TextField();
		tTimeLimit.setDisable(true);
		tTimeLimit.setMaxWidth(50);
		tCoverageLimit = new TextField();
		tCoverageLimit.setDisable(true);
		tCoverageLimit.setMaxWidth(50);
		tSteps = new TextField();
		tSteps.setDisable(true);
		tSteps.setMaxWidth(50);
		
		//Choice Box for Simulator Modes
		cbMode = new ComboBox<String>();
		cbMode.getItems().addAll(SIM_EXP, SIM_FAST, REAL_EXP, REAL_FAST);
		cbMode.getSelectionModel().select(SIM_EXP);
		
		//Buttons
		btnConnect = new Button("Connect");
		btnConnect.setMaxWidth(500);
		btnStart = new Button("Start Sim");
		btnStart.setMaxWidth(500);
		btnStart.setStyle("-fx-background-color: #77dd77; ");
		btnSetWayPoint = new Button("Set Waypoint");
		btnSetWayPoint.setMaxWidth(500);
		btnSetWayPoint.setStyle("-fx-background-color: #fdfd96; ");
		btnSetRobot = new Button("Set Robot Position");
		btnSetRobot.setMaxWidth(500);
		btnSetRobot.setStyle("-fx-background-color: #aec6cf; ");
		btnSetObstacle = new Button("Set Obstacles");
		btnSetObstacle.setStyle("-fx-background-color: black; -fx-text-fill: white;");
		btnSetObstacle.setMaxWidth(500);
		btnRestartMap = new Button("Reset Map");
		btnRestartMap.setMaxWidth(500);
		btnRestartMap.setStyle("-fx-background-color: #c23b22; ");
		btnLoadMap = new Button("Load Map");
		btnLoadMap.setMaxWidth(500);
		btnSaveMap = new Button("Save Map");
		btnSaveMap.setMaxWidth(500);

		// File Chooser
		fileChooser = new FileChooser();
		
		//Scroll Bars
		sbTimeLimit = new ScrollBar();
		sbTimeLimit.setMin(10);
		sbTimeLimit.setMax(240);
		sbCoverageLimit = new ScrollBar();
		sbCoverageLimit.setMin(10);
		sbCoverageLimit.setMax(100);
		sbSteps = new ScrollBar();
		sbSteps.setMin(1);
		sbSteps.setMax(100);
		
		//Button Action Listeners
		btnStart.setOnMouseClicked(bcStart);
		btnSetWayPoint.setOnMouseClicked(bcSetWayPoint);
		btnSetRobot.setOnMouseClicked(bcSetRobot);
		btnSetObstacle.setOnMouseClicked(bcSetObstacle);
		btnRestartMap.setOnMouseClicked(bcRestartMap);
		btnLoadMap.setOnMouseClicked(new EventHandler<MouseEvent>() {
			public void handle(MouseEvent e) {
				if (setObstacle) {
					fileChooser.setTitle("Choose file to load Map from");
					File file = fileChooser.showOpenDialog(priStage);
					if (file != null) {
						MapDescriptor.loadMapFromFile(map, file.getAbsolutePath());
					}
					expMapDraw = false;
				} else {
					fileChooser.setTitle("Choose file to load ExploredMap to");
					File file = fileChooser.showOpenDialog(priStage);
					if (file != null) {
						MapDescriptor.loadMapFromFile(exploredMap, file.getAbsolutePath());
					}
					expMapDraw = true;
				}

			}
		});
		btnSaveMap.setOnMouseClicked(new EventHandler<MouseEvent>() {
			public void handle(MouseEvent e) {
				if (setObstacle) {
					fileChooser.setTitle("Choose file to save Map to");
					File file = fileChooser.showOpenDialog(priStage);
					if (file != null) {
						MapDescriptor.saveMapToFile(map, file.getAbsolutePath());
					}
				} else {
					fileChooser.setTitle("Choose file to save ExploredMap to");
					File file = fileChooser.showOpenDialog(priStage);
					if (file != null) {
						MapDescriptor.saveMapToFile(exploredMap, file.getAbsolutePath());
					}
				}

			}
		});
		
		sbTimeLimit.valueProperty().addListener(change -> {
			tTimeLimit.setText("" + (int) sbTimeLimit.getValue() + " s");
		});

		sbCoverageLimit.valueProperty().addListener(change -> {
			tCoverageLimit.setText("" + (int) sbCoverageLimit.getValue() + "%");
		});

		sbSteps.valueProperty().addListener(change -> {
			tSteps.setText("" + (int) sbSteps.getValue());
		});
		
		controlGrid.add(lTimeLimit, 0, 2, 1, 1);
		controlGrid.add(sbTimeLimit, 1, 2, 4, 1);
		controlGrid.add(tTimeLimit, 5, 2, 1, 1);
		
		controlGrid.add(lCoverageLimit, 0, 3, 1, 1);
		controlGrid.add(sbCoverageLimit, 1, 3, 4, 1);
		controlGrid.add(tCoverageLimit, 5, 3, 1, 1);
		
		controlGrid.add(lSteps, 0, 4, 1, 1);
		controlGrid.add(sbSteps, 1, 4, 4, 1);
		controlGrid.add(tSteps, 5, 4, 1, 1);
		
		controlGrid.add(cbMode, 0, 5, 3, 1);
		controlGrid.add(btnStart, 3, 5, 3, 1);
		
		controlGrid.add(btnLoadMap, 0, 6, 3, 1);
		controlGrid.add(btnSaveMap, 3, 6, 3, 1);
		controlGrid.add(btnRestartMap, 0, 7, 6, 1);
		
		controlGrid.add(btnSetWayPoint, 0, 8, 6, 1);

		controlGrid.add(btnSetRobot, 0, 9, 2, 1);
		controlGrid.add(btnSetObstacle, 2, 9, 4, 1);

		GridPane.setFillWidth(cbMode, true);

		grid.add(controlGrid, 0, 0);
		grid.add(mapGrid, 1, 0);

		// Dimensions of the Window
		Scene scene = new Scene(grid, 800, 600);
		priStage.setScene(scene);
		scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
			public void handle(KeyEvent e) {
				sim = false;
				robot.setSimulate(sim);
				System.out.println("System movement");
				if (!netMgr.isConnected()) {
					netMgr.startConn();
					netMgr.send("Alg|Ard|S|0");
					robot.sense(exploredMap, map);
				}
				switch (e.getCode()) {
				case W:
					robot.move(Command.FORWARD, 1, exploredMap);
					robot.sense(exploredMap, map);
					break;
				case S:
					robot.move(Command.BACKWARD, 1, exploredMap);
					robot.sense(exploredMap, map);
					break;
				case A:
					robot.move(Command.TURN_RIGHT, 1, exploredMap);
					robot.sense(exploredMap, map);
					break;
				case D:
					robot.move(Command.TURN_LEFT, 1, exploredMap);
					robot.sense(exploredMap, map);
					break;
				default:
					break;
				}
				System.out.println("Robot Direction AFTER:" + robot.getDirection());
			}
		});

		priStage.show();
	}
	
	//Mouse Event Handler for Clicking & Detecting location on map
	private EventHandler<MouseEvent> MapClick = new EventHandler<MouseEvent>()
	{
		public void handle(MouseEvent event) {
			double mouseX = event.getX();
			double mouseY = event.getY();

			int selectedCol = (int) ((mouseX - Map.MAP_OFFSET / 2) / Map.MAP_Square_SZ);
			int selectedRow = (int) (Map.MAP_HEIGHT - (mouseY - Map.MAP_OFFSET / 2) / Map.MAP_Square_SZ);
			
			//For Debugging
			System.out.println(exploredMap.getSquare(selectedRow, selectedCol).toString() + " validMove:" + exploredMap.checkValidAction(selectedRow, selectedCol));


			if (setWayPoint) {
				System.out.println(setWayPoint(selectedRow, selectedCol)
						? "New WayPoint set at row: " + selectedRow + " col: " + selectedCol
						: "Unable to put waypoint at obstacle or virtual wall!");
			}
			if (setRobot)
				System.out.println(setRobotLocation(selectedRow, selectedCol) ? "Robot Position has changed"
						: "Unable to put Robot at obstacle or virtual wall!");

			if (setObstacle) {
				if (event.getButton() == MouseButton.PRIMARY)
					System.out.println(setObstacle(selectedRow, selectedCol)
							? "New Obstacle Added at row: " + selectedRow + " col: " + selectedCol
							: "Obstacle at location alredy exists!");
				else
					System.out.println(removeObstacle(selectedRow, selectedCol)
							? "Obstacle removed at row: " + selectedRow + " col: " + selectedCol
							: "Obstacle at location does not exists!");

			}
			if (setObstacle)
			{
				expMapDraw = false;				
			}
			else
			{
				expMapDraw = true;				
			}
		}
	};

	//Mouse Event Handler for Restarting Map
	private EventHandler<MouseEvent> bcRestartMap = new EventHandler<MouseEvent>() {
		public void handle(MouseEvent event)
		{
			if(setObstacle)
			{
				map.restartMap();
				map.setAllCovered(true);
			}
			else
			{
				exploredMap.restartMap();
				exploredMap.setAllExplored(false);
			}
			robot.setStartPos(robot.getPosition().x, robot.getPosition().y, exploredMap);
		}
	};
	
	//Mouse Event Handler for Start
	private EventHandler<MouseEvent> bcStart = new EventHandler<MouseEvent>() {
		public void handle(MouseEvent event)
		{
			String mode = cbMode.getSelectionModel().getSelectedItem();
			switch(mode) {
			case REAL_FAST:
				
				netMgr.startConn();
				sim = false;
				robot.setSimulate(false);
				System.out.println("SF Here");
				exploredMap.removePaths();
				expMapDraw = true;
				fastTask = new Thread(new FastTask());
				fastTask.start();
				
				break;
			case REAL_EXP:
				
				netMgr.startConn();
				sim = false;
				robot.setSimulate(false);
				System.out.println("FastSense"+robot.isFastSense());
				expTask = new Thread(new ExplorationTask());
				expTask.start();
				
				break;
			case SIM_FAST:
				
				sim = true;
				expMapDraw = true;
				robot.setFastSense(true);
				System.out.println("SF Here");
				exploredMap.removePaths();
				fastTask = new Thread(new FastTask());
				fastTask.start();
				
				break;
			case SIM_EXP:
				
				sim = true;
				System.out.println("SE Here");
				robot.sense(exploredMap, map);
				expMapDraw = true;
				expTask = new Thread(new ExplorationTask());
				expTask.start();
				
				break;
		}
		}
	};

	//Mouse Event Handler for Setting Robot
	private EventHandler<MouseEvent> bcSetRobot = new EventHandler<MouseEvent>() {
		public void handle(MouseEvent e) {
			setRobot = !setRobot;
			if (!setRobot)
			{
				btnSetRobot.setText("Set Robot Position");			
				btnSetRobot.setStyle("-fx-background-color: #aec6cf; ");
			}
			else
			{
				btnSetRobot.setText("Confirm Robot Position");	
				btnSetRobot.setStyle("-fx-background-color: #77dd77; ");	
			}

			setWayPoint = false;
			setObstacle = false;
		}
	};

	//Mouse Event Handler for Setting Way Point
	private EventHandler<MouseEvent> bcSetWayPoint = new EventHandler<MouseEvent>() {
		public void handle(MouseEvent e) {
			setWayPoint = !setWayPoint;
			if(setWayPoint)
			{
				btnSetWayPoint.setText("Confirm Waypoint");
				btnSetWayPoint.setStyle("-fx-background-color: #77dd77;");				
			}
			else
			{
				btnSetWayPoint.setText("Set Waypoint");
				btnSetWayPoint.setStyle("-fx-background-color: #fdfd96; ");				
			}
			setObstacle = false;
			setRobot = false;
		}
	};
	private EventHandler<MouseEvent> bcSetObstacle = new EventHandler<MouseEvent>() {
		public void handle(MouseEvent e) {
			setObstacle = !setObstacle;
			if (!setObstacle) {
				btnSetObstacle.setText("Set Obstacles");
				btnSetObstacle.setStyle("-fx-background-color: black; -fx-text-fill: white;");
				btnLoadMap.setText("Load Explored Map");
				btnSaveMap.setText("Save Explored Map");
			} else {
				btnSetObstacle.setText("Confirm Obstacles");
				btnSetObstacle.setStyle("-fx-background-color: #77dd77;");
				btnLoadMap.setText("Load Map");
				btnSaveMap.setText("Save Map");
			}
			setRobot = false;
			setWayPoint = false;
			expMapDraw = !setObstacle;
		}
	};
	
	class ExplorationTask extends Task<Integer> {
		@Override
		protected Integer call() throws Exception {
			String msg = null;
			Command c;
			// Wait for Start Command
			if (!sim) {
				do {
					robot.setFastSense(false);
					msg = netMgr.receive();
					String[] msgArr = msg.split("\\|");
					System.out.println("Command received: " + msgArr[2]);
					c = Command.ERROR;
					
					if (msgArr[2].compareToIgnoreCase("C") == 0) {
						System.out.println("Calibrating");
						for (int i = 0; i < 4; i++) {
							robot.move(Command.TURN_RIGHT, RobotConstants.MOVE_STEPS, exploredMap);
							senseAndAlign();
						}
						netMgr.send("Alg|Ard|" + Command.ALIGN_RIGHT.ordinal() + "|0");
						System.out.println("THIS IS SIM ALIGHRIGHT");
						msg = netMgr.receive();
						System.out.println("Done Calibrating");
					} else {
						c = Command.values()[Integer.parseInt(msgArr[2])];
					}

					if (c == Command.CURRENT_POS) {
						String[] data = msgArr[3].split("\\,");
						int col = Integer.parseInt(data[0]);
						int row = Integer.parseInt(data[1]);
						Direction dir = Direction.values()[Integer.parseInt(data[2])];
						int wayCol = Integer.parseInt(data[3]);
						int wayRow = Integer.parseInt(data[4]);
						robot.setStartPos(col, row, exploredMap);
						while(robot.getDirection()!=dir) {
							robot.rotateSensorsACW(true);
							robot.setDirection(Direction.rotateACW(robot.getDirection()));
						}
						
						wayPoint = new Point(wayCol, wayRow);
						System.out.println("Waypoint received" + wayPoint);

					} else if (c == Command.START_EXP) {
						netMgr.send("Alg|Ard|S|0");
					}
				} while (c != Command.START_EXP);
			}
			robot.sense(exploredMap, map);
			System.out.println("coverage: " + sbCoverageLimit.getValue());
			System.out.println("time: " + sbTimeLimit.getValue());
			double coverageLimit = (int) (sbCoverageLimit.getValue());
			int timeLimit = (int) (sbTimeLimit.getValue() * 1000);
			int steps = (int) (sbSteps.getValue());
			// Limits not set
			if (coverageLimit == 0)
				coverageLimit = 100;
			if (timeLimit == 0)
				timeLimit = 240000;
			if (steps == 0)
				steps = 5;

			Exploration explore = new Exploration(exploredMap, map, robot, coverageLimit, timeLimit, steps, sim);
			explore.exploration(new Point(Map.STARTZONE_COL, Map.STARTZONE_COL));
			if (!sim) {
				netMgr.send("Alg|And|DONE|"+exploredMap.imagePositionToString());
				netMgr.send("Alg|And|" + Command.END + "|");
				Command com = null;
				do {
					String[] msgArr = robot.robotMessageBeepBeep().split("\\|");
					com = Command.values()[Integer.parseInt(msgArr[2])];
					System.out.println("Fastest path msg :" + msgArr[2]);
					if (com == Command.START_FP) {
						sim = false;
						System.out.println("RF Here");
						fastTask = new Thread(new FastTask());
						fastTask.start();
						break;
					}
				} while (com != Command.START_FP);
			}

			return 1;
		}
	}

	class FastTask extends Task<Integer> {
		@Override
		protected Integer call() throws Exception {
			robot.setFastSense(true);
			double startT = System.currentTimeMillis();
			double endT = 0;
			FastestPath fp = new FastestPath(exploredMap, robot, sim);
			ArrayList<Square> path;
			path = fp.run(new Point(robot.getPosition().x, robot.getPosition().y), wayPoint, robot.getDirection());
			path.addAll(fp.run(wayPoint, Map.GOALZONE, robot.getDirection()));

			fp.displayFastestPath(path, true);
			ArrayList<Command> commands = fp.getPathCommands(path);

			int steps = (int) (sbSteps.getValue());
			// Limits not set
			if (steps == 0)
				steps = 5;

			int moves = 0;
			System.out.println(commands);
			Command c = null;
			for (int i = 0; i < commands.size(); i++) {
				c = commands.get(i);
				if (sim) {
					try {
						TimeUnit.MILLISECONDS.sleep(RobotConstants.MOVE_SPEED / steps);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

				}
				if (c == Command.FORWARD && moves<9) {
					moves++;
					// If last command
					if (i == (commands.size() - 1)) {
						robot.move(c, moves, exploredMap);
						if(!sim)
						netMgr.receive();
					}
				} else {
					
					if (moves > 0) {
						System.out.println("Moving Forwards "+moves+" steps.");
						robot.move(Command.FORWARD, moves, exploredMap);
						if(!sim)
						netMgr.receive();
					}
					robot.move(c, RobotConstants.MOVE_STEPS, exploredMap);
					if(!sim)
					netMgr.receive();
					moves = 0;
				}
			}
			
			if (!sim) {
				netMgr.send("Alg|Ard|"+RobotConstants.Command.ALIGN_FRONT.ordinal()+"|");
				netMgr.send("Alg|And|" + RobotConstants.Command.END+"|");
			}
			
			endT = System.currentTimeMillis();
			int seconds = (int)((endT - startT)/1000%60);
			int minutes = (int)((endT - startT)/1000/60);
			System.out.println("Total Time: "+minutes+"mins "+seconds+"seconds");
			return 1;
		}
	}

	//Normal
	public void senseAndAlign() {
		String msg = null;
		double[][] sensorData = new double[6][2];
		msg = robot.robotMessageBeepBeep();
		String[] msgArr = msg.split("\\|");
		String[] strSensor = msgArr[3].split("\\,");
		System.out.println("Received " + strSensor.length + " sensor data");
		// Translate string to integer
		for (int i = 0; i < strSensor.length; i++) {
			String[] arrSensorStr = strSensor[i].split("\\:");
			sensorData[i][0] = Double.parseDouble(arrSensorStr[1]);
			sensorData[i][1] = Double.parseDouble(arrSensorStr[2]);
		}

		// Discrepancy detected among the sensor data received
		if (sensorData[0][1] == 1 || sensorData[2][1] == 1) {
			netMgr.send("Alg|Ard|" + Command.ALIGN_FRONT.ordinal() + "|1");
			if(!sim)
			netMgr.receive();
		}
	}
}
