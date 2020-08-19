/**
 * 
 */
package Map;

import java.io.*;

/**
 * @author 
 *
 */
public class MapDescriptor {

    private static String binToHex(String binStr) {
        int dec = Integer.parseInt(binStr, 2);
        return Integer.toHexString(dec);
    }
    
    private static String hexToBin(String hexStr) {
    	String bin = "", tempStr="", tempBin;
    	int temp;
    	for(int i=0;i<hexStr.length(); i++)
    	{
    		tempStr += hexStr.charAt(i);
    		temp = Integer.parseInt(tempStr,16);
    		tempBin = Integer.toBinaryString(temp);
    		if(tempBin.length()!=4)
    			while(tempBin.length()!=4)
    				tempBin = "0"+tempBin;
    			
    		bin += tempBin;
    		tempStr ="";
    	}
    	return bin;
    }
    
    //Explore MDF String
    public static String genMDS1(Map map) {

		String mapString = "";
		String temp ="11";// First two bits set to 11
		for (int row = 0; row < Map.MAP_HEIGHT; row++) {
			for (int col = 0; col < Map.MAP_WIDTH; col++) {
				temp += map.getSquare(row, col).isExplored() ? "1" : "0";
				if(temp.length()==4){
					mapString += binToHex(temp);
					temp = "";
				}
			}
		}
		temp+= "11";
		mapString += binToHex(temp);
		// Last two bits set to 11
		// return mapString;
		return mapString;
	}
    
    //Obstacle MDF String
	public static String genMDS2(Map map) {

		String mapString = "";
		String temp = "";
		for (int row = 0; row < Map.MAP_HEIGHT; row++) {
			for (int col = 0; col < Map.MAP_WIDTH; col++) {
				if (map.getSquare(row, col).isExplored()) {
					temp += map.getSquare(row, col).isObstacle() ? "1" : "0";
					if(temp.length()==4){
						mapString += binToHex(temp);
						temp = "";
					}
				}
			}
		}
		
		// Pad with '0' to make the length a multiple of 8
		int tempLength = temp.length();
		int paddingLength = tempLength % 8;

		if (paddingLength != 0) {
			// Find the number of required bits
			paddingLength = 8 - paddingLength;

			for (int i = 0; i < paddingLength; i++) {
				temp += "0";
			}
			mapString += binToHex(temp);
		}
		return mapString;
	}
	
	// Specifically Used to Load Maps for the Simulator
	// 1st line explored and obstacle for exploredMap
	// 2nd Line for obstacle for the map
	public static void loadMapFromFile(Map map, String filename) {
		try {
			FileReader file = new FileReader(filename);
			BufferedReader buf = new BufferedReader(file);
			
			String hexStr1 = buf.readLine();
			String hexStr2 = buf.readLine();
			String binStr = hexToBin(hexStr1);
			
			//Part 1 Explored
			int strIndex = 2;
			for(int row=0; row < Map.MAP_HEIGHT; row++) {
				for(int col=0; col< Map.MAP_WIDTH; col++) {
					if(binStr.charAt(strIndex)=='1')
						map.getSquare(row, col).setExplored(true);
					strIndex++;
				}
			}
			
			//Part 2 Obstacles in the explored Area
			binStr = hexToBin(hexStr2);
			strIndex = 0;
			for(int row=0; row < Map.MAP_HEIGHT; row++) {
				for(int col=0; col< Map.MAP_WIDTH; col++) {
					if(map.getSquare(row, col).isExplored()) {
						if(binStr.charAt(strIndex)=='1') {
							map.getSquare(row, col).setObstacle(true);
							// Set the virtual wall around the obstacle
							for (int r = row - 1; r <= row + 1; r++)
								for (int c = col - 1; c <= col + 1; c++)
									if (map.checkValidSquare(r, c))
										map.getSquare(r, c).setvirtualBarrier(true);;
						}
						strIndex++;
					}
				}
			}
			buf.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//Save the Map Descriptor  to a map to a file
	public static void saveMapToFile(Map map, String filename) {
		try {
			
			FileWriter file = new FileWriter(filename);
			
			BufferedWriter buf = new BufferedWriter(file);
			String mapDes = genMDS1(map);
			System.out.println(mapDes);
			buf.write(mapDes);
			buf.newLine();
			
			mapDes = genMDS2(map);
			System.out.println(mapDes);
			buf.write(mapDes);
			buf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
    
}