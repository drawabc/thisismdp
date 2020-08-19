from android_bluetooth import *

from arduino_serial import *

from pc_wifi import *

from cam import *


import queue

import threading

import time

class 

mThreads(threading.Thread):
    
	def __init__(self):
        
		threading.Thread.__init__(self)
        
		self.android_thread = An_BT()
        
		self.arduino_thread = Ar_Sr()
        
		self.pc_thread = Pc_Wf()
        
		self.cam_thread = Cam()



        
		self.android_thread.connect()
        
		self.arduino_thread.connect()
        
		self.pc_thread.connect()

        

		self.pc_queue = queue.Queue(maxsize=0)

        

		#time.sleep(1)

    


	def read_an(self):
        
		while 1:
            
			try:
                
				data = self.android_thread.android_read()

            
		except BlueToothError:
                
			print ("Bluetooth Connection Lost")
                
			self.android_thread.connect()
                
				continue

            
		
		if data is None:
                
			print ("NULL from ANDROID")

            

		elif (data[4:7] == 'Ard'):
                
			print ("[AN to AR]: %s" %data)
                
			self.arduino_thread.arduino_write('[' + data + ']')

            

		elif (data[4:7] == 'Alg'):
                
			print ("[AN to PC]: %s" %data)
                
			self.pc_thread.pc_write(data + '\n')

            
	
		else:
                
			print ("incorrect header from ANDROID - %s" %data)


    

	
	def read_ar(self):
        
		while 1:
            
			data = self.arduino_thread.arduino_read()

            

		if data is None:
                
			print ("NULL from ARDUINO")

            

		elif (data[4:7] == 'And'):
                
			print ("[AR to AN]: %s" %data)
                
			self.android_thread.android_write(data)

            

		elif (data[4:7] == 'Alg'):
                
			print ("[AR to PC]: %s" %data)
                
			self.pc_thread.pc_write(data)

            

		else:
                
			print ("incorrect header from ARDUINO - %s" %data)


    


	def read_pc(self, pc_queue):

        
		while 1:

            
			data = self.pc_thread.pc_read()
            
			data = data.split('\r\n')

            

			for i in range(0, len(data)):
                
				self.pc_queue.put_nowait(data[i])

            

			while not pc_queue.empty():
                
				data = self.pc_queue.get_nowait()

                

				if data is None:
                    
					print ("NULL from PC")

                

				elif (data[4:7] == 'Ard'):
                    
					print ("[PC to AR]: %s" %data)
                    
					self.arduino_thread.arduino_write('[' + data + ']')

                

				elif (data[4:7] == 'And'):
                    
					print ("[PC to AN]: %s" %data)
                    
					self.android_thread.android_write(data)

                

				else:
                    
					pass
                    
					#print "incorrect header from PC - %s" %data

    


	def read_cam(self):
           
			self.cam_thread.ready()

    


	def arrow_head(self):
        
			while 1:
            
				if self.cam_thread.arrow_seen():
                
				print (">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
                
				#print (">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
                
				print ("[RP to AN]")
                
				#print (">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
                
				#print (">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
                
				self.android_thread.android_write("RPi|And|A|")
                
				time.sleep(3)


    


	def initialize_threads(self):
                
			android_read_thread = threading.Thread(target = self.read_an, name = "Android Thread")
                
			arduino_read_thread = threading.Thread(target = self.read_ar, name = "Arduino Thread")
                
			pc_read_thread = threading.Thread(target = self.read_pc, args = (self.pc_queue,), name = "PC Thread")
                
			cam_read_thread = threading.Thread(target = self.read_cam, name = "Camera Thread")
                
			arrow_thread = threading.Thread(target = self.arrow_head, name = "Misc")

                

			android_read_thread.daemon = True
                
			arduino_read_thread.daemon = True
                
			pc_read_thread.daemon = True
                
			cam_read_thread.daemon = True
                
			arrow_thread.daemon = True

                
			android_read_thread.start()
                

			arduino_read_thread.start()
                
			pc_read_thread.start()
                
			cam_read_thread.start()
                
			arrow_thread.start()

                

			print ("All threads initialized!")


    

	
	def disconnect_all(self):
        
			self.android_thread.disconnect()
        
			self.arduino_thread.disconnect()
        
			self.pc_thread.disconnect()


    


	def keep_main_alive(self):
        
		while 1:           
			time.sleep(0.5)





if __name__ == "__main__":
    
	try:
            
		r = mThreads()
            
		r.initialize_threads()
            
		r.keep_main_alive()
            
		r.disconnect_all()

    

	except KeyboardInterrupt:
            
		r.disconnect_all()


