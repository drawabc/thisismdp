import time
import socket


class Pc_Wf(object):

	# Initialise the needed information
    def __init__(self):
        self.tcp_ip = "192.168.16.1"
        self.port = 8080 # Establish 8080 as the port

    # Method for connectionb
    def connect(self):
        try:
        	# Procedure for connection
            self.ip_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.ip_sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.ip_sock.bind((self.tcp_ip,self.port))
            self.ip_sock.listen(1)
            self.pc_sock, self.address = self.ip_sock.accept()
            print ("Connection Established!", self.address)
        except Exception as e:
        	# Print error if any
            print ("Connection Exception: %s" %str(e))

    # Disconnect method
    def disconnect(self):
        try:
            self.ip_sock.close()
        except Exception as e:
            print ("Disconnection Exception: %s" %str(e))

    # Read method, to be used in a loop in m.py thread
    def pc_read(self):
        try:
            data = self.pc_sock.recv(2048).decode("utf-8") # Read from  pc_sock
            return data
        except Exception as e:
            print ("PC Read Error: %s" %str(e)) # Print error if any
            print("Attempting to reconnect... ") 
            self.connect() # Attempt to reconnect

    # Write method
    def pc_write(self, data):
        try:
            self.pc_sock.sendall(str.encode(data)) #Write to pc_sock
        except socket.error as e:
            print (e) # Print error 
            print("Attempting to reconnect... ") 
            self.connect() # Try to reconnect
        # Same as above
        except IOError as e:
            if e.errno == errno.EPIPE:
                print ("EPIPE error")
                print("Attempting to reconnect... ")
                self.connect()
            else:
                pass

