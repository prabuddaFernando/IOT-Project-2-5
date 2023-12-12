# Imports for MQTT
import time
import datetime
import paho.mqtt.client as mqtt
import paho.mqtt.publish as publish

from tellcore.constants import TELLSTICK_HUMIDITY, TELLSTICK_TEMPERATURE
from tellcore.telldus import TelldusCore

core = TelldusCore()

sensors = core.sensors()

# Set MQTT broker and topic
broker = "test.mosquitto.org."  # Broker

pub_topic = "group/sensors/humidity"

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
    global humidity
    for sensor in sensors:
        if sensor.id == 135:
            humidity = int(sensor.value(TELLSTICK_HUMIDITY).value)
    #print(humidity)
    return humidity


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
while True:
    data_to_send = get_humidity()
    client.publish(pub_topic, str(data_to_send))
    time.sleep(2.0)
