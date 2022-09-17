# BLE Gateway Manager for Hubitat

This app allows Hubitat users to create BLE beacons along with a BLE gateway.  The app adds your beacons to yout Hubitat hub
using Virtual Presence device drivers.  This allows you to build automations (e.g., using the Rule Machine) that trigger
events to occur based upon the presence (or absence) of a beacon.

# How Does it Work?

This solution consists of three parts:
- **BLE Gateway Manager**: This app is responsible for receiving beacon data from the gateway, creating new devices for each beacon, and
sending the "arrived" or "departed" commands to the beacon devices.  It uses the BLE Gateway Driver to interpret the incoming data from
the gateway.
- **BLE Gateway Driver**: Each gateway has its own driver, and is responsible for parsing the data received by the gateway app.  The driver
must parse the incoming data and format it into a contruct that allows the BLE gateway to process the incoming data.  This allows different
people to use the gateway of their choice.
- **Beacons/Virtual Presence Drivers**: The gateway allows you to create virtual presence devices for each detected beacon.  

# What BLE Gateways are supported?

As of today, only the April Brother BLE Gateway (v4) is supported.  You can purchase one here:

Additional gateway drivers can be built as long as the follow the gateway driver specifications (see below).

# What Beacon Types are supported?

As of now, only iBeacons are supported.  You can set any UUID, Major, and Minor number you want to use for each beacon.  If desired, you can
use a single UUID for all beacons in your home and then use different Major/Minor numbers to identify each beacon.  Or you can make all of the
values unique.  Each beacon must have its own unique combination in order to be detected as a separate device.

Additional beacon types can be added.

# How Do I Install This App?




