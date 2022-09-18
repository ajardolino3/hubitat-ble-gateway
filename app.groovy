import groovy.json.*

def appVersion() { return "1.0.0" }

definition(
	name: "BLE Gateway Manager",
	namespace: "ajardolino3",
	author: "Art Ardolino",
	description: "Processes incoming bluetooth beacon data for home automation",
	category: "Bluetooth",
	iconUrl: "",
	iconX2Url: "",
	singleInstance: true,
	documentationLink: "https://github.com/ajardolino3/hubitat-ble-gateway/blob/main/README.md",
    importUrl: "",
    singleThreaded: true // important: required so gateway manager and gateway driver can communicate with each other
)

preferences() {
    page name: "mainPage"
    page name: "addBeaconPage"
    page name: "addBeaconStatusPage"
    page name: "listBeaconsPage"
}

def mainPage() {
    if (!state.accessToken) {   
        // Enable OAuth in the app settings or this call will fail
        createAccessToken() 
    }
    String uri = makeUri("/");
    return dynamicPage(name: "mainPage", title: "BLE Gateway Manager", uninstall: true, install: true) {
        section(){ 
            paragraph("The BLE Gateway Manager processes data from a Bluetooth (BLE) Gateway to detect the presence of beacons.")
			input "gateway", "capability.presenceSensor", title: "Select your BLE Gateway Device", multiple: false, required: true
            paragraph("Configure your gateway to use this URL: <a href='${uri}'>${uri}</a>")
			href "addBeaconPage", title: "<b>Add New Beacon</b>", description: "Adds new beacon device."
			href "listBeaconsPage", title: "<b>List Beacons</b>", description: "Lists added beacon devices."
			input "debugLog", "bool", title: "Enable debug logging", submitOnChange: true, defaultValue: false
        }
    }
}

def listBeaconsPage() {
    def devices = getChildDevices()
    def message = ""
    devices.each{ beacon ->
        message += "\t${beacon.label} (${beacon.name}, Network ID: ${beacon.getDeviceNetworkId()})\n"
    }
    return dynamicPage(name: "listBeaconsPage", title: "List Beacons", install: false, nextPage: mainPage) {
        section() {
            paragraph "The following beacon devices were added using the 'Add New Beacon' feature, and are setup to interface with the selected gateway."
            paragraph message
        }
    }
}

def addBeaconPage() {
	def newBeacons = [:]
	state.beacons.each { beacon ->
		def isChild = getChildDevice(beacon.value.dni)
		if (!isChild && beacon.value.present) {
			newBeacons["${beacon.value.dni}"] = "${beacon.value.dni}"
		}
	}
    
    return dynamicPage(name: "addBeaconPage", title: "Add New Beacon", install: false, nextPage: addBeaconStatusPage) {
        section(){
            paragraph "NOTE: Beacon must be actively broadcasting and detected by the gateway to appear in this list.  If the beacon does not appear and is actively broadcasting, wait a few seconds and then refresh this page and try again."
			input ("selectedAddBeacon", "enum",
				   required: true,
				   multiple: false,
				   title: "Select a beacon to add (${newBeacons.size() ?: 0} new beacons detected)",
				   description: "Use the dropdown to select beacon.",
                   options: newBeacons)
            input "selectedBeaconName", "string", required: true, title: "Give this Beacon a Name"
        }
    }
}

def addBeaconStatusPage() {
    def status = []
    logDebug("beacon: " + selectedAddBeacon)
    def isChild = getChildDevice(selectedAddBeacon)
    def success = false
    def err = ""
    if (!isChild) {
        try {
            addChildDevice("hubitat", "Virtual Presence", selectedAddBeacon, ["label": selectedBeaconName])
            success = true
        }
        catch(exception) {
            error = exception
        }
    }
    if(success) {
        message = "New beacon added successfully (" + selectedBeaconName + ")."
    } else {
        message = "Unable to add beacon: " + err;
    }
	app?.removeSetting("selectedAddBeacon")
	app?.removeSetting("selectedBeaconName")
    
	return dynamicPage(name:"addBeaconStatusPage",
					   title: "Add Beacon Status",
					   nextPage: mainPage,
					   install: false) {
	 	section() {
            paragraph message
		}
	}
}

private def makeUri(String extraPath) {
    return getFullLocalApiServerUrl() + extraPath + "gateway?access_token=${state.accessToken}"
}


def installed() {
    // called when app is installed
    state.beacons = [:]
}

def updated() {
    // called when settings are updated
}

def uninstalled() {
    // called when app is uninstalled
}

mappings {
    path("/gateway") { action: [POST: "postGateway"] }
}

// receives incoming post requests from the gateway, sends to the gateway driver for parsing, then updates state data from parsed request
def postGateway() {
    logDebug("POST received from Gateway: " + request.body)
    logDebug("Sending payload to device: " + gateway.name + ", payload: " + request.body)
    def result = null
    try
    {
        result = gateway.parsePayload(request.body)
    }
    catch(error)
    {
        logDebug("Unable to parse payload: " + error)
        return
    }
    def parsed = gateway.getDataValue("parsed")

    def slurper = new JsonSlurper()
    def obj = slurper.parseText(parsed)
    logDebug("Parsed payload from Gateway: " + obj)
    
    state.beacons.each { beacon ->
        beacon.value.present = false
    }
    
    def detected = String.format("%tF %<tH:%<tM", java.time.LocalDateTime.now())
    obj.beacons.each { beacon ->
        if(beacon.type)
        {
            switch(beacon.type) {
                case "iBeacon":
                    if(beacon.uuid instanceof String && beacon.major instanceof Integer && beacon.minor instanceof Integer) {
                        beacon.dni = "${beacon.uuid}:${beacon.major}:${beacon.minor}"
                        logDebug("iBeacon detected:" + beacon.dni)
                    } else {
                        logDebug("iBeacon type has invalid uuid, major, or minor types")
                    }
                    break
                default:
                    logDebug("unknown beacon type: " + beacon.type)
                    break
            }
        }
        if(beacon.dni)
        {
            def b = state.beacons["${beacon.dni}"]
            if(!b) {
                b = [:]
                b.dni = beacon.dni
            }
            b.present = true
            b.detected = detected
            state.beacons["${beacon.dni}"] = b
        }
    }
    
	state.beacons.each { beacon ->
        //if(beacon.value.detected) logDebug("last detected: " + Date.parse("yyyy-MM-dd HH:mm", beacon.value.detected))
        def b = beacon.value
		def isChild = getChildDevice(b.dni)
		if (isChild) {
            if(isChild.currentValue("presence") == "present" && !b.present) {
                logDebug("Beacon: " + b.dni + ", presence: not present")
                isChild.departed()
            }
            else if(isChild.currentValue("presence") == "not present" && b.present) {
                logDebug("Beacon: " + b.dni + ", presence: present")
                isChild.arrived()
            }
    	}
    }
}

def logDebug(msg) {
    if(debugLog) log.debug(msg)
}


