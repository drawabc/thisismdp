
from bluetooth import *

class An_BT(object):

	def __init__(self):
		self.client_sock = None

	def connect(self):
		try:
			self.bt_sock = BluetoothSocket(RFCOMM) #creating socket for BT RFCOMM com
			self.bt_sock.bind(("",4))
			self.bt_sock.listen(1) #listens to accept one conn at a time
			self.port = self.bt_sock.getsockname()[1]
			uuid = "00001101-0000-1000-8000-00805F9B34FB" #need uuid for android app
			advertise_service(self.bt_sock, "Server18",
						service_id = uuid,
						service_classes = [uuid, SERIAL_PORT_CLASS],
						profiles = [SERIAL_PORT_PROFILE])
			print ("Waiting for connection on RFCOMM %d" % self.port)
			self.client_sock, client_info = self.bt_sock.accept()
			print ("Accepted connection from ", client_info)

		except Exception as e:
			print ("----------------------------------------")
			print ("Bluetooth Connection Exception: %s" %str(e))


	def disconnect(self):
		try:
			print ("Server going down")
			if self.client_sock is not None:
				self.client_sock.close()
			self.bt_sock.close()

		except Exception as e:
			print ("----------------------------------------")
			print ("Bluetooth Disconnection Exception %s" %str(e))


	def android_read(self):
		try:
			data = self.client_sock.recv(2048).decode("utf-8")
			return data

		except Exception as e:
			print ("----------------------------------------")
			print ("Android Read Exception: %s" %str(e))


	def android_write(self, msg):
		try:
			self.client_sock.send(str.encode(msg))

		except Exception as e:
			print ("----------------------------------------")
			print ("Android Write Exception: %s" %str(e))



if __name__ == "__main__":

	print ("\n----------------------------------------------------")
	print ("....................running anbt....................")
	print ("----------------------------------------------------\n")
	an_bt = An_BT()
	an_bt.connect()

	try:
		while 1:

			input = int(raw_input("1 - write, 0 - read"))
			if input == 0:
				print(an_bt.android_read())
				print("read")
			elif input == 1:
				an_bt.android_write(raw_input("enter msg:"))
			else:
				break

	except KeyboardInterrupt:
		an_bt.disconnect()

	an_bt.disconnect()

