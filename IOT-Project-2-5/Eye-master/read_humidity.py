from tellcore.constants import TELLSTICK_HUMIDITY, TELLSTICK_TEMPERATURE
from tellcore.telldus import TelldusCore

# Initialize Tellstick
core = TelldusCore()

# Get sensor data
sensors = core.sensors()
devices = core.devices()
for device in devices:
    if device.id == 1:
        actuator = device
print(sensors)
for sensor in sensors:
    if sensor.id == 135:
        humidity = int(sensor.value(TELLSTICK_HUMIDITY).value)
        temperature = sensor.value(TELLSTICK_TEMPERATURE).value
print(humidity)