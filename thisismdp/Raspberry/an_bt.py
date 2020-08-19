
from bluetooth import *

class An_Bt(object):
    def __init__(self):
        self.client_sock = None
    def connect(self):
        try:
        # Procedure and initialisation needed for connection
            self.bt_sock = BluetoothSocket(RFCOMM)
            self.bt_sock.bind(("",3))
            self.bt_sock.listen(1) 
            self.port = self.bt_sock.getsockname()[1]
            uuid = "00001101-0000-1000-8000-00805F9B34FB" # UUID for our app
            advertise_service(self.bt_sock, "Server18",
			 	    service_id = uuid,
				    service_classes = [uuid, SERIAL_PORT_CLASS],
				    profiles = [SERIAL_PORT_PROFILE])
            print ("Waiting for connection ... %d" % self.port)
            self.client_sock, client_info = self.bt_sock.accept() # Wait for a connection
            print ("Connection Established with ", client_info)
        except Exception as e:
            print ("Bluetooth Connection Exception: %s" %str(e)) # Print the error if needed
    
    # For disconnecting
    def disconnect(self):
        try:
            print ("Disconnecting")
            if self.client_sock is not None:
                self.client_sock.close()
                self.bt_sock.close()
        except Exception as e:
            print ("Bluetooth Disconnection Exception %s" %str(e))
    
    # Method to be used in the thread in m.py
    def android_read(self):
        try:
            data = self.client_sock.recv(2048).decode("utf-8") # Read from the client sock
            return data
        except Exception as e:
            print ("Android Read Exception: %s" %str(e)) # If an error occur print the error
            print ("Attempting to reconnect BT...")
            self.connect() # Try to reconnect using the connect method
    
    # Write method
    def android_write(self, msg):
        try:
            self.client_sock.send(str.encode(msg)) # Write the msg to the client_sock
        except Exception as e:
            print ("Android Write Exception: %s" %str(e)) #Print error if any
            print ("Attempting to reconnect BT...")
            self.connect() # Try to reconnect using the connect method