from time import sleep
import serial

class Ar_Usb(object):
    def __init__(self):
        self.ser = serial.Serial('/dev/ttyACM0', 115200) #Establish serial connection at the usb port stated and baud rate
    
    # Method to be used in m.py thread
    def ar_read(self):
        try:
            data = self.ser.readline().decode('utf-8').split("\r\n") # Read data from the serial port
            return data[0]
        except:
            return None
            print("Error reading from Arduino") # Indicate error from reading if any
    
    # Write method
    def ar_write(self, msg):
        self.ser.write((msg + "\n").encode('utf-8')) # Write to the serial port
    
    # Disconnect method
    def close_serial(self):
        ser.close()
