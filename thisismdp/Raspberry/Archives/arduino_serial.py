import serial
import time
import os

class Ar_Sr(object):

	def __init__(self):
		self.port = '/dev/ttyACM0'
		self.baud_rate = 9600


	def connect(self):
		try:
			self.serial_sock = serial.Serial(self.port, self.baud_rate)
			print ("Connection Established! %s" %self.port)

		except Exception as e:
			print ("Serial Connection Exception: %s" %str(e))


	def disconnect(self):
		try:
			self.serial_sock.close()
			print ("Closing serial socket")

		except Exception as e:
			print ("Serial Disconnection Exception: %s" %str(e))


	def arduino_read(self):
		try:
			data = self.serial_sock.readline().decode("utf-8")
			return data

		except Exception as e:
			print ("Arduino Read Exception: %s" %str(e))


	def arduino_write(self, data):
		try:
			#x = os.system("ls /dev/ttyACM0")
			#if x != 0:
			#	print("disconnected")
			self.serial_sock.write(str.encode(data))

		except Exception as e:
			print ("Arduino Write Exception: %s" %str(e))


if __name__ == "__main__":
	print ("ruuuuuunnnn")
	sr = Ar_Sr()
	sr.connect()
	print ("Serial Connection Established 2")

	try:
		while 1:
			#print "read"
			#print "data received: %s" % sr.arduino_read()
			x = raw_input()
			sr.arduino_write(x)
			print("sent")
			print(sr.arduino_read())

	except KeyboardInterrupt:
		sr.disconnect()

