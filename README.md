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
- **BLE Beacons Driver**: The gateway uses this driver to create devices for each detected beacon.  

# What BLE Gateways are Supported?

As of today, only the April Brother BLE Gateway (v4) is supported.  You can purchase one here: https://store.aprbrother.com/product/ab-ble-gateway-4-0

Additional gateway drivers can be built as long as the follow the gateway driver specifications (see below).

# What Beacon Types are Supported?

iBeacons, AltBeacons, and Eddystone-UID beacons are supported.  You can set any UUID, Major, and Minor number you want to use for each iBeacon/AltBeacon,
or any Namespace/Instance for Eddystone-UID.  Each beacon must have its own unique combination in order to be detected as a separate device and to avoid
confusion with distance calculation.

# How is Distance Calculated?

Distance is calculated (in feet) using the following formula: 10 ^ ((Measured Power - RSSI)/(10 * N)) * 3.28084
- N is set to a value of "2" for low-strength beacons
- If the beacon is not present (or the distance cannot be calculated), the distance value is set to 999999999 (this is also true for the rssi and power values).  The reason for this
is to make home automation rules easier to create (i.e., "if my beacon present and less than 100 feet away" will still work as expected if distance cannot be calculated).  This is by design.
- **NOTE: While distance is calculated, please note it is an estimate.  The detection of presence of a beacon will be much more reliable than the distance calculation.
If you decide to use distance in your home automations, it is recommended you use a range (e.g. <= 35 feet away).  You will need to test with your beacon in your environment
and see what distance values are calculated before creating home automations using the distance (custom) attribute.**
- More information about this calculation can be found here: https://medium.com/beingcoders/convert-rssi-value-of-the-ble-bluetooth-low-energy-beacons-to-meters-63259f307283

*Also note that RSSI and power values are typically represented as a negative number.  For home automation purposes and to simplify the creation of rules, these values
are represented as positive numbers instead.*

# How Do I Install and Configure this App?

First, install the app and device driver code in your Hubitat Hub.
1. Make sure the Hubitat Package Manager is installed.  If not, please see instructions here: https://hubitatpackagemanager.hubitatcommunity.com/installing.html
2. Go to the "Apps" section of your Hubitat Hub and click "Hubitat Package Manager".
3. Click "Install".
4. Choose the option to "Browse by Tags".
5. Under tags(s), choose "Presence & Location".
6. The corresponding applications will appear.  Select "BLE Gateway Manager".
7. Under the drivers to install, choose the "April Brother BLE Gateway" (to use this gateway/driver).  Click "Next".
8. Click "Next" again on the "Ready to Install" page to install.
9. Once the installation is complete, click "Next". 

Second, setup your gateway device in your Hubitat Hub:
1. Go to the "Devices" section and click the "Add Device" button.
2. Click the "Virtual" button, and then enter a name.
3. In the Type dropdown, choose your driver (e.g. "April Brother BLE Gateway"), then click "Save Device".

Third, setup the BLE Gateway Manager app in your Hubitat Hub:
1. Go to the "Apps" section and click the "Add User App" button.
2. Choose the "BLE Gateway Manager".
3. Select your BLE gateway device you added previously and then click Update.
4. Click Done.
5. Click the "BLE Gateway Manager" app again.
6. Note the link created to configure your gateway.  You will configure your gateway using this info. You can highlight and copy (Ctrl+C) if you wish.  In chrome, you can right-click and choose "Copy link address".

Fourth, setup your BLE Gateway.  These steps are specific to the April Brother BLE Gateway, and assumes it has all the factory default settings:
1. Power up your new BLE gateway and plug it into your ethernet network.
2. Download and install the gateway configuration tool, which can be found here: https://wiki.aprbrother.com/en/Software_AB_BLE_Gateway_V4.html
3. Once installed, launch the gateway configuration tool.
4. Click the button to Scan for your gateway.
5. Once the gateway appears, click to select.
6. Click the Network tab along the top.  Enter in your WiFi SSID and Security Key, then click Save.  Make sure the popup appears that confirms the changes have been saved.
7. Click the Application tab along the top.  Change the following settings:
   - Connection Type: HTTP Client
   - Host: The IP address of your Hubitat Hub.  You can find the IP address in the link displayed in the BLE Gateway Manager app.
   - Port: 80
   - URI: The path portion of the link displayed in the BLE Gateway Manager app, beginning with the word "apps" (e.g., "apps/api/###/gateway?access-token=xxxxxxxx").
     DO NOT include the "http://xx.xx.xx.xx/" part of the link in the URI field.
   - Request Interval: 5
   - Request Format: JSON
   - Duplicate Filter: Enable
   Click Save.  Make sure the popup appears that confirms the changes have been saved.
   
Fifth, setup your Beacons:
1. Configure your beacons using the software provided with your beacon.
1. IMPORTANT: Make sure your beacon is on and actively broadcasting before you attempt to add.  Only beacons that have been detected by the gateway and data transmitted to your Hubitat Hub will appear.
2. Go to the "Apps" section and click to open the "BLE Gateway Manager" app.
3. Click the "Add New Beacon" button.
4. The newly detected beacons will appear.  Select the one you want to add and give it a name, then click Next.
5. The beacon should be added successfully.  If not, an error message will appear.
6. Repeat these steps for each beacon.

NOTE: DO NOT add beacon devices manually.  Always add them through the app.  The app will not be able to work with beacon devices unless they are added by the app.

You can now configure your Hubitat Hub to automate events based upon the presence (or absence) of your beacons!

# BLE Gateway Driver Specification

This information is for developers who wish to build additional gateway drivers to work with the BLE Gateway Manager.

The device driver you create must implement the following:
- Add the capability "presenceSensor" (no need to implement arrived/departed methods)
- Add a command called "parsePayload" which takes 1 parameter named payload (type STRING).  This command is called each time the gateway receives a payload from the gateway.
- The driver must have the "singleThreaded" option set to "true"

The parsePayload method must parse the incoming payload (sent from the gateway to the BLE Gateway Manager).  The payload is the body of the incoming POST request sent to the gateway.

The parsePayload method must set a data value called "parsed" with a JSON string that matches the following structure:

```
{
	"beacons": [
		{	"type": "auto", "data": "1EFF060001092002EDE0328D74592AE19846B8A7ACF69B0EDD4F79CA2E5C0A", "rssi": -50 },
		{	"type": "auto", "data": "1EFF060001092002EDE0328D74592AE19846B8A7ACF69B0EDD4F79CA2E5C0A", "rssi": -67 },
		{	"type": "auto", "data": "1EFF060001092002EDE0328D74592AE19846B8A7ACF69B0EDD4F79CA2E5C0A", "rssi": -91 }
	]
}
```

As you can see, the JSON object contains an array of beacons, each having a type, uuid, major, and minor value.
- "type": This is the beacon type.  You can use "auto" to allow the gateway to automatically parse the incomming beacon data.
- "rssi": This is the RSSI value detected by the gateway.  It must be a negative integer.
- "data": This is the data advertized by the beacon (a hexidecimal string).

The JSON object should only contain beacons that are currently detected by the gateway.

For an example device driver, look here: https://github.com/ajardolino3/hubitat-ble-gateway/blob/main/april-brother-ble-gateway.groovy

The parsePayload method is called by the BLE Gateway Manager.  Once the method completes, the BLE Gateway Manager does the following:
1. Parses the advertised data and detects the beacon type.
2. Assigns each beacon a unique Network ID, using either the parsed UUID/Major/Minor values (iBeacon/AltBeacon) or Namespace/Instance (Eddystone-UID).
3. Checks to see if the beacon has been added as a device.  If so, it will set the value as arrived/present if its not already present.
4. Finds any other devices that were previously added as devices and sets the value to departed/not present if its currently present (if not provided in the parsed
payload, the BLE Gateway Manager assumes that the beacon is no longer present.
5. Updates the app state with all beacon information.  This is used to track beacon state and to identify newly detected beacons that have not yet been added.  The
rssi from the gateway and the measured power from the advertised beacon data is used to estimate distance (in feet).

