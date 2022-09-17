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

As of today, only the April Brother BLE Gateway (v4) is supported.  You can purchase one here: https://store.aprbrother.com/product/ab-ble-gateway-4-0

Additional gateway drivers can be built as long as the follow the gateway driver specifications (see below).

# What Beacon Types are supported?

As of now, only iBeacons are supported.  You can set any UUID, Major, and Minor number you want to use for each beacon.  If desired, you can
use a single UUID for all beacons in your home and then use different Major/Minor numbers to identify each beacon.  Or you can make all of the
values unique.  Each beacon must have its own unique combination in order to be detected as a separate device.

Additional beacon types can be added.

# How Do I Install and Configure this App?

First, install the app and device driver code in your Hubitat Hub.
1. Go to the "Apps Code" section of your Hubitat Hub and click the "New App" button.
2. Click the import button.  Enter this url for the app: https://raw.githubusercontent.com/ajardolino3/hubitat-ble-gateway/main/app.groovy
3. Click the save button to save the app.
4. Click the OAuth button, then click "Enable OAuth in App" when the popup window appears.  Click the Update button.  This step is necessary
to allow the BLE gateway to communicate with your Hubitat hub.
5. Go to the "Drivers Code" section of your Hubitat Hub and click the "New Driver" button.
6. Click the import button.  Enter this url for the app: https://raw.githubusercontent.com/ajardolino3/hubitat-ble-gateway/main/april-brother-ble-gateway.groovy
7. Click the save button to save the app.

Second, setup your gateway device in your Hubitat Hub:
1. Go to the "Devices" section and click the "Add Device" button.
2. Click the "Virtual" button, and then enter a name.
3. In the Type dropdown, choose your driver (e.g. "April Brother BLE Gateway"), then click Save.

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
   - Host: The IP address of your Hubitat Hub.  This is in the link displayed in the the BLE Gateway Manager on your hub.
   - Port: 80
   - URI: The url you copied, beginning with the word "apps" (e.g. apps/api/###/gateway?access-token=xxxxxxxx)
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

You can now configure your Hubitat Hub to automate events based upon the presence (or absence) of your beacons!

