from tellcore.constants import TELLSTICK_HUMIDITY, TELLSTICK_TEMPERATURE
from tellcore.telldus import TelldusCore

# Initialize Tellstick
core = TelldusCore()

# Get sensor data
sensors = core.sensors()
print(sensors)
for sensor_id in sensors:
    print(sensor_id.id)
    print(sensor_id.value(TELLSTICK_HUMIDITY).value)
    print(sensor_id.value(TELLSTICK_TEMPERATURE).value)