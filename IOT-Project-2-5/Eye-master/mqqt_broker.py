# Imports for MQTT
import time
import datetime
from random import random
import threading

import blink_counter

import paho.mqtt.client as mqtt
import paho.mqtt.publish as publish

from tellcore.constants import TELLSTICK_HUMIDITY, TELLSTICK_TEMPERATURE
from tellcore.telldus import TelldusCore

#
core = TelldusCore()
#
sensors = core.sensors()
devices = core.devices()

room_humidity_threshold_low = 30
room_humidity_threshold_high = 50
eye_blinking_rate_threshold_low = 15
eye_blinking_rate_threshold_high = 20

for device in devices:
    if device.id == 1:
        actuator = device

# Set MQTT broker and topic
broker = "test.mosquitto.org."  # Broker

pub_topic_humidity = "group/sensors/humidity"
pub_topic_eye_blink_rate = "group/eye_blink/counter"
pub_topic_humiditifyer_state = "group/humiditifyer/state"


############### MQTT section ##################

isActuatorTurnOn = 0
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
    global humidity
    for sensor in sensors:
        if sensor.id == 135:
            humidity = int(sensor.value(TELLSTICK_HUMIDITY).value)
    return humidity


def get_blink_rate():
    return blink_counter.BlinkCounter.blinkRate


def controlHumiditifier(humidity):
    global isActuatorTurnOn
    if (humidity < room_humidity_threshold_low) or (humidity > room_humidity_threshold_high):
        actuator.turn_on()
        isActuatorTurnOn = 1
    else:
        isActuatorTurnOn = 0

def getHumiditifierState():
    return isActuatorTurnOn

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
        controlHumiditifier(hud)
        blink_rate = get_blink_rate()
        print("Blink rate: ====> " + str(blink_rate))
        client.publish(pub_topic_eye_blink_rate, blink_rate)
        client.publish(pub_topic_humiditifyer_state, getHumiditifierState())
        time.sleep(2.0)


first_thread = threading.Thread(target=thread_1)
first_thread.start()
#
second_thread = threading.Thread(target=blink_counter.BlinkCounter.thread_2)
second_thread.start()
