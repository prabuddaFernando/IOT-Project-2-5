# Imports for MQTT
import time
import datetime
from random import random
import threading

import blink_counter

import paho.mqtt.client as mqtt
import paho.mqtt.publish as publish

# from tellcore.constants import TELLSTICK_HUMIDITY, TELLSTICK_TEMPERATURE
# from tellcore.telldus import TelldusCore
#
# core = TelldusCore()
#
# sensors = core.sensors()

# Set MQTT broker and topic
broker = "test.mosquitto.org."  # Broker

pub_topic_humidity = "group/sensors/humidity"
pub_topic_temerature = "group/sensors/temperature"
pub_topic_eye_blink_rate = "group/eye_blink/counter"


############### MQTT section ##################

# when connecting to mqtt do this;
def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("Connection established. Code: " + str(rc))
    else:
        print("Connection failed. Code: " + str(rc))


def on_publish(client, userdata, mid):
    print("Published: " + str(mid))


def on_disconnect(client, userdata, rc):
    if rc != 0:
        print("Unexpected disonnection. Code: ", str(rc))
    else:
        print("Disconnected. Code: " + str(rc))


def on_log(client, userdata, level, buf):  # Message is in buf
    print("MQTT Log: " + str(buf))


############### Sensor section ##################
def get_humidity():
    return 33
    # global humidity
    # for sensor in sensors:
    #     if sensor.id == 135:
    #         humidity = int(sensor.value(TELLSTICK_HUMIDITY).value)
    # #print(humidity)
    # return humidity


def get_temperature():
    return 34


#     global temperature
#     for sensor in sensors:
#         if sensor.id == 135:
#             temperature = int(sensor.value(TELLSTICK_TEMPERATURE).value)
#     return temperature

def get_blink_rate():
    return blink_counter.BlinkCounter.blinkCounter


# Connect functions for MQTT
client = mqtt.Client()
client.on_connect = on_connect
client.on_disconnect = on_disconnect
client.on_publish = on_publish
client.on_log = on_log

# Connect to MQTT
print("Attempting to connect to broker " + broker)
client.connect(broker)
client.loop_start()


# Loop that publishes message
def thread_1():
    while True:
        hud = get_humidity()
        client.publish(pub_topic_humidity, str(hud))
        temp = get_temperature()
        client.publish(pub_topic_temerature, str(temp))
        blink_rate = get_blink_rate()
        print("Blink rate: ====> " + str(blink_rate))
        client.publish(pub_topic_eye_blink_rate, blink_rate)
        time.sleep(2.0)


first_thread = threading.Thread(target=thread_1)
first_thread.start()
#
second_thread = threading.Thread(target=blink_counter.BlinkCounter.thread_2)
second_thread.start()