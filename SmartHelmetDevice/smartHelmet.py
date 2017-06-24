# Lab 9 XOR
import tensorflow as tf
import numpy as np
import time

#!/usr/bin/python
import smbus
import math

#mqtt
import paho.mqtt.client as mqtt
import random

#gps
import serial
import pynmea2

#------------------------------------------------ gps -----------------------------------------
def parseGPS(str):
	if str.find('GGA') > 0:
		msg = pynmea2.parse(str)
		return msg
	else :
		return None
serialPort = serial.Serial("/dev/ttyS0", 9600, timeout=0.5)

#------------------------------------------------ mqtt --------------------------------------
'''
def getMsg():
	msg = str(random.randrange(20, 36))
	return msg

def on_connect(client, userdata, flags, rc):
	print("Connected with result code " + str(rc))

def on_publish(client, userdata, mid):
	msg_id = mid
	print("message published")
'''
mqttc = mqtt.Client()
#mqttc.on_connect = on_connect
#mqttc.on_publish = on_publish

# YOU NEED TO CHANGE THE IP ADDRESS OR HOST NAME
mqttc.connect("192.168.43.95")
#mqttc.connect("localhost")

#-------------------------------- gyro&accel ----------------------------------------
# Power management registers
power_mgmt_1 = 0x6b
power_mgmt_2 = 0x6c

def read_byte(adr):
	return bus.read_byte_data(address, adr)

def read_word(adr):
	high = bus.read_byte_data(address, adr)
	low = bus.read_byte_data(address, adr+1)
	val = (high << 8) + low
	return val

def read_word_2c(adr):
	val = read_word(adr)
	if (val >= 0x8000):
		return -((65535 - val) + 1)
	else:
		return val
def dist(a,b):
	return math.sqrt((a*a)+(b*b))

def get_y_rotation(x,y,z):
	radians = math.atan2(x, dist(y,z))
	return -math.degrees(radians)

def get_x_rotation(x,y,z):
	radians = math.atan2(y, dist(x,z))
	return math.degrees(radians)

bus = smbus.SMBus(1) # or bus = smbus.SMBus(1) for Revision 2 boards
address = 0x68       # This is the address value read via the i2cdetect command

# Now wake the 6050 up as it starts in sleep mode
bus.write_byte_data(address, power_mgmt_1, 0)

#----------------------------------- deeplearning 1----------------------------------
tf.set_random_seed(777)  # for reproducibility
learning_rate = 0.1

X = tf.placeholder(tf.float32, [None, 2])
Y = tf.placeholder(tf.float32, [None, 1])

W1 = tf.Variable(tf.random_normal([2, 2]), name='weight1')
b1 = tf.Variable(tf.random_normal([2]), name='bias1')
layer1 = tf.sigmoid(tf.matmul(X, W1) + b1)

W2 = tf.Variable(tf.random_normal([2, 1]), name='weight2')
b2 = tf.Variable(tf.random_normal([1]), name='bias2')
hypothesis = tf.sigmoid(tf.matmul(layer1, W2) + b2)

# cost/loss function
cost = -tf.reduce_mean(Y * tf.log(hypothesis) + (1 - Y) *
			tf.log(1 - hypothesis))

train = tf.train.GradientDescentOptimizer(learning_rate=learning_rate).minimize(cost)

# Accuracy computation
# True if hypothesis>0.5 else False
predicted = tf.cast(hypothesis > 0.5, dtype=tf.float32)
accuracy = tf.reduce_mean(tf.cast(tf.equal(predicted, Y), dtype=tf.float32))

#---------------------------------- main -------------------------------------------
count = 0

gyro_xout_prev = 0.
gyro_yout_prev = 0.
gyro_zout_prev = 0.

accel_xout_prev = 0.
accel_yout_prev = 0.
accel_zout_prev = 0.

accel_xout_scaled_prev = 0. 
accel_yout_scaled_prev = 0.
accel_zout_scaled_prev = 0.

while True:
	#step1 sleep	
	print "step1"
	time.sleep(0.3)

	#step2 get gyro&aceel sensor value 
	print "step2"
	gyro_xout = read_word_2c(0x43)
	gyro_yout = read_word_2c(0x45)
	gyro_zout = read_word_2c(0x47)

	print "gyro_xout: ", gyro_xout, " scaled: ", (gyro_xout / 131)
	print "gyro_yout: ", gyro_yout, " scaled: ", (gyro_yout / 131)
	print "gyro_zout: ", gyro_zout, " scaled: ", (gyro_zout / 131)

	accel_xout = read_word_2c(0x3b)
	accel_yout = read_word_2c(0x3d)
	accel_zout = read_word_2c(0x3f)

	accel_xout_scaled = accel_xout / 16384.0
	accel_yout_scaled = accel_yout / 16384.0
	accel_zout_scaled = accel_zout / 16384.0

	#skip first step	
	if count == 0:
		gyro_xout_prev = gyro_xout
		gyro_yout_prev = gyro_yout
		gyro_zout_prev = gyro_zout

		accel_xout_prev = accel_xout
		accel_yout_prev = accel_yout
		accel_zout_prev = accel_zout

		accel_xout_scaled_prev = accel_xout_scaled 
		accel_yout_scaled_prev = accel_yout_scaled 
		accel_zout_scaled_prev = accel_zout_scaled
		count += 1
		continue

	#step3 check if value is validate
	print "step3"
	if math.sqrt(pow(accel_xout_prev - accel_xout,2)+pow(accel_yout_prev - accel_yout,2)+pow(accel_zout_prev - accel_zout,2)) < 1000 :
		continue
	
	#step4 put the value into deeplearning1 to find out accident
	print "step4"
	#can use variables in x_data2
	x_data2 = [[120, 121]]
	x_data2 = np.array(x_data2, dtype=np.float32)

#	y_data2 = [[0],[1],[0],[0],[1]]
#	y_data2 = np.array(y_data2, dtype=np.float32)

	saver = tf.train.Saver()
	# Launch graph
	with tf.Session() as sess:
		# Initialize TensorFlow variables
		sess.run(tf.global_variables_initializer())
		#new_saver = tf.train.import_meta_graph('my.ckpt.meta')
		#new_saver.restore(sess, tf.train.latest_checkpoint('./'))
		saver.restore(sess, 'my.ckpt')
	
		# Accuracy report
		# accident = 1 / non accident = 0
		h, c= sess.run([hypothesis, predicted],
			feed_dict={X: x_data2})
		print("\nHypothesis: ", h, "\nCorrect: ", c)
		if c == 0 : 
			continue
		#step5 wait unitle gap between two variables is small enough
		print "step5"
		waitCount = 0
		skipCount = 0
		gyro_xout_prev2 = 0.
		gyro_yout_prev2 = 0.
		gyro_zout_prev2 = 0.
	
		accel_xout_prev2 = 0.
		accel_yout_prev2 = 0.
		accel_zout_prev2 = 0.

		accel_xout_scaled_prev2 = 0. 
		accel_yout_scaled_prev2 = 0.
		accel_zout_scaled_prev2 = 0.

		while True :
			time.sleep(0.1)

			gyro_xout2 = read_word_2c(0x43)
			gyro_yout2 = read_word_2c(0x45)
			gyro_zout2 = read_word_2c(0x47)

			accel_xout2 = read_word_2c(0x3b)
			accel_yout2 = read_word_2c(0x3d)
			accel_zout2 = read_word_2c(0x3f)

			accel_xout_scaled2 = accel_xout / 16384.0
			accel_yout_scaled2 = accel_yout / 16384.0
			accel_zout_scaled2 = accel_zout / 16384.0

			if math.sqrt(pow(accel_xout_scaled_prev2 - accel_xout_scaled2,2)+pow(accel_yout_scaled_prev2 - accel_yout_scaled2,2)+pow(accel_zout_scaled_prev2 - accel_zout_scaled2,2)) < 1000 :
				waitCount += 1
			else :
				skipCount += 1
				waitCount = 0
			#count 30seconds		
			if skipCount > 300 :
				break	
			#count 10secons
			if waitCount > 10 :
				testa =1
				#step6 put the value into deeplearning2 to find out accident
				print "step6"
				saver = tf.train.Saver()
				# Launch graph
				with tf.Session() as sess:
					# Initialize TensorFlow variables
					sess.run(tf.global_variables_initializer())
					#new_saver = tf.train.import_meta_graph('my.ckpt.meta')
					#new_saver.restore(sess, tf.train.latest_checkpoint('./'))
					saver.restore(sess, 'my.ckpt')
	
					# Accuracy report
					# accident = 1 / non accident = 0
					h, c= sess.run([hypothesis, predicted],
						feed_dict={X: x_data2})
					print("\nHypothesis: ", h, "\nCorrect: ", c)
					if c == 0 : 
						break	
					#step7 get gps value
					print "step7"
					countGps = 0
					while True:
						time.sleep(0.1)
						str = serialPort.readline()
						msg = parseGPS(str)
						if countGps > 10000 :
							break
						else :
							countGps += 1
						if msg :
							if msg.lat and msg.lon :
								#step8 publis gps value 
								print "step8"

								print msg.lon+'----------'+msg.lat
								b = float(msg.lat)/100
								c = float(msg.lon)/100
								msg.lat = "%f" % (b)
								msg.lon = "%f" % (c)

								positionLat = msg.lat.find('.')
								positionLon = msg.lon.find('.')

								latDegree = int(msg.lat[:positionLat])
								lonDegree = int(msg.lon[:positionLon])
								latMinute = float(msg.lat[positionLat:])/60*100
								lonMinute = float(msg.lon[positionLon:])/60*100
								
								latMerge = latDegree + latMinute
								msg.lat = "%f" % (latMerge)
								lonMerge = lonDegree + lonMinute
								msg.lon = "%f" % (lonMerge)

								mqttc.loop_start()
								sendMsg = 'c'+msg.lon+'uuuu,'+msg.lon+','+msg.lat
								print sendMsg
								mqttc.publish("accident",sendMsg)	
								mqttc.loop_stop()				
								break
						else :
							continue
				
#print "accel_xout: ", accel_xout, " scaled: ", accel_xout_scaled
#print "accel_yout: ", accel_yout, " scaled: ", accel_yout_scaled
#print "accel_zout: ", accel_zout, " scaled: ", accel_zout_scaled
#print "x rotation: " , get_x_rotation(accel_xout_scaled, accel_yout_scaled, accel_zout_scaled)
#print "y rotation: " , get_y_rotation(accel_xout_scaled, accel_yout_scaled, accel_zout_scaled)
	gyro_xout_prev = gyro_xout
	gyro_yout_prev = gyro_yout
	gyro_zout_prev = gyro_zout
	
	accel_xout_prev = accel_xout
	accel_yout_prev = accel_yout
	accel_zout_prev = accel_zout

	accel_xout_scaled_prev = accel_xout_prev / 16384.0
	accel_yout_scaled_prev = accel_yout_prev / 16384.0
	accel_zout_scaled_prev = accel_zout_prev / 16384.0


