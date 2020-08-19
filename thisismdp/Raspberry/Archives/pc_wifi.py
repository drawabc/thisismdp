import time
import socket


class Pc_Wf(object):

	def __init__(self):
		self.tcp_ip = "192.168.16.1"
		self.port = 22


	def connect(self):
		try:
			self.ip_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
			self.ip_sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
			self.ip_sock.bind((self.tcp_ip,self.port))
			self.ip_sock.listen(1)
			self.pc_sock, self.address = self.ip_sock.accept()
			print ("Connection Established!", self.address)
		except Exception as e:
			print ("Connection Exception: %s" %str(e))


	def disconnect(self):
		try:
			self.ip_sock.close()
		except Exception as e:
			print ("Disconnection Exception: %s" %str(e))


	def pc_read(self):
		try:
			data = self.pc_sock.recv(2048).decode("utf-8")
			#data = data.split('\r\n')
			return data
		except Exception as e:
			print ("PC Read Exception: %s" %str(e))


	def pc_write(self, data):
		try:
			#new_data = data + '\n'
			self.pc_sock.sendall(str.encode(data))
			#self.pc_sock.sendto(new_data, self.address)
		except socket.error as e:
			print ("Socket Error")
		except IOError as e:
			if e.errno == errno.EPIPE:
				print ("EPIPE error")
			else:
				pass
		#except Exception, e:
		#	print "PC Write Exception: %s" %str(e)


if __name__ == "__main__":
	print ("Running main")
	pc = Pc_Wf()
	pc.connect()

	try:
		#while 1:

			print ("read")
			print (pc.pc_read())
			print (pc.pc_read())

			#input = int(raw_input("choose 0 -read, 1 -write:"))

			#if input == 0:
			#	print(pc.pc_read())
			#	print("read")

			#elif input == 1:
			#	pc.pc_write(raw_input("enter msg:"))

			#else:
			#	break

	except KeyboardInterrupt:
		pc.disconnect()

	pc.disconnect()
