from pc_wf import *
from ar_ser import *
from an_bt import *
from cam import *
import time
import threading
#import queue

class componentThreads(threading.Thread):
    def __init__(self):
    	# Initialisation of the Cam, PC, Arduino, BT communication objects into the main class attributes
        threading.Thread.__init__(self)
        self.cam_thread = Cam()
        self.pc_thread = Pc_Wf()
        self.ar_thread = Ar_Usb()
        self.an_thread = An_Bt()
    # Method to be fired for the camera threading
    def startDetection(self):
        self.cam_thread.startDetecting()
    # THe method to be fired for the pc thread. Constantly tries to read something from pc
    def read_pc(self):
        self.pc_thread.connect() #Tries to establish connection with PC
        while True:
            try:
                data = self.pc_thread.pc_read() #Repeatedly try to read data
                original_data = data #Save the original data before processing it
                #Due to the PC sometimes sending 2-3 messages at once, we will need to split the messages ourselves before sending it
                splitted_data = data.split("\r\n")  #Split the data by newlines
                print("received from PC: " + original_data) #Log on console the data received
                if data is None: #Skip if is nothing
                    continue
                for each_data in splitted_data: #For each data splitted by newlines
                    splitted_each_data = each_data.split("|") #Split it by our established delimiter
                    if splitted_each_data[1] == "Ard": #If the 2nd part of the msg which indicates receiver is Arduino
                        self.ar_thread.ar_write(each_data + "\n") #Write to Arduino
                        print("Sent to ARD: " + each_data) #Log it down
                    elif splitted_each_data[1] == "And": #If it is Android
                        self.an_thread.android_write(each_data + "\n") #Send to android
                        print("Sent to AND: " + each_data) #log the message
              
            except:
                continue #If anything goes wrong in the while loop, just continue
        
    # Method to be fired for the Arduino thread. Constantly tries to read from Arduino
    def read_ar(self):
        while True:
            try:
                original_data = self.ar_thread.ar_read() #Constantyl read from arduino
                if original_data is None: # Continue if the data is nothing
                    continue
                if len(original_data) < 2: # If the length of the data is less than 2, it might be rubbish data. Skip if it is
                    continue
                if self.cam_thread.sendfound: # Meanwhile, if the boolean sendfound is True in cam_thread, indicating a signal to send image to PC
                    print("\n\n\nSENDING IMAGE FOUND\n\n\n") #Log that we are sending image
                    self.pc_thread.pc_write("Rpi|Alg|I\r\n") #Send image message
                    self.cam_thread.sendfound = False # Switch off the sendfound signal
                print("Received from Arduino: " + original_data) #Log data received from arduino
                self.pc_thread.pc_write(original_data + "\r\n") # Arduino always send to PC only, so no checking of receiver is needed
                print("Sent to PC: " + original_data) # Log the message
            except: 
                continue #Continue if anything goes wrong
    
    # Method to be fired for the Android thread. Constantly tries to read from Android
    def read_an(self):
        self.an_thread.connect() # Establish connection with Android
        while True:
            try:
                data = self.an_thread.android_read() #Repeatedly try to read data from android
                original_data = data # Save the original data before processing
                print("received from And: " + original_data) # Log the receiving data
                if data is None: # Skip if its nothing
                    continue
                data = data.split("\n") 
                data = data[0] 
                data = data.split("|") #Split the meaningful data by it's delimiter
                if data[1] == "Ard": # If the second segment of the data indicating the receiver says Arduino
                    self.ar_thread.ar_write(original_data + "\r\n") #Send to arduino
                    print("Sent to ARD: " + original_data) # Log the sent data
                if data[1] == "Alg": # If it is Alg
                    self.pc_thread.pc_write(original_data + "\r\n") # Send to PC
                    print("Sent to ALG: " + original_data) # Log the sent data
            except:
                continue # Skip for any errors that errors
    
    # Method to start the threads
    def initialise_threads(self):
        ar_read_thread = threading.Thread(target = self.read_ar, name = "Arduino Thread") # Create a thread that runs the read_ar method
        pc_read_thread = threading.Thread(target = self.read_pc, name = "PC Thread") # Create a thread that runs the read_pc method
        an_read_thread = threading.Thread(target = self.read_an, name = "Android Thread") #Create a thread that runs the read_an method
        cam_detecting_thread = threading.Thread(target = self.startDetection, name = "Cam Thread") # create a thread that runs the startDetection method
        ar_read_thread.daemon = True 
        pc_read_thread.daemon = True
        an_read_thread.daemon = True
        cam_detecting_thread.daemon = True
        # Start all the threads
        ar_read_thread.start() 
        pc_read_thread.start()
        an_read_thread.start()
        cam_detecting_thread.start()
    
    # Method to disconnect the connections for those with connections
    def disconnect_threads(self):
        self.pc_thread.disconnect()
        self.an_thread.disconnect()
    
    # Method to keep main alive.
    def keep_main_alive(self):
        while True:
            time.sleep(0.5)
    
if __name__ == "__main__":
    try:
        r = componentThreads()
        r.initialise_threads()
        r.keep_main_alive()
        r.disconnect_threads()
    except KeyboardInterrupt:
        r.disconnect_threads()
        