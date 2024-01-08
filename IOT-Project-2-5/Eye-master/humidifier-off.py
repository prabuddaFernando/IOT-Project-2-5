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
for sensor in sensors:
    print(sensor.id)
    print(sensor.value(TELLSTICK_HUMIDITY).value)
    if sensor.id == 135:
        humidity = int(sensor.value(TELLSTICK_HUMIDITY).value)

actuator.turn_off()
