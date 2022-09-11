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
	documentationLink: "",
	importUrl: "",
    singleThreaded: true // important: required so gateway manager and gateway driver can communicate with each other
)

preferences() {
    page name: "mainPage"
    page name: "addBeaconsPage"
    page name: "addBeaconsStatusPage"
}

def mainPage() {
    if (!state.accessToken) {   
        // Enable OAuth in the app settings or this call will fail
        createAccessToken() 
    }
    String uri = makeUri("/");
    return dynamicPage(name: "mainPage", title: "BLE Gateway Manager", uninstall: true, install: true) {
        section(){ 
            paragraph("This app uses an <a href=\"https://store.aprbrother.com/product/ab-ble-gateway-4-0\" target=\"_blank\">April Brother BLE Gateway (v4)</a> to associate beacons with Virtual Presence devices to automate events.")
			input "gateway", "capability.presenceSensor", title: "BLE Gateway", multiple: false, required: true
			href "addBeaconsPage",
				title: "<b>Add New Beacon Devices</b>",
				description: "Add New Beacon Devices."

            app(name: "anyOpenApp", appName: "April Brother BLE Beacon", namespace: "ajardolino3", title: "<b>Add a new Bluetooth Beacon</b>", multiple: true)
            paragraph("Gateway URL: <a href='${uri}'>${uri}</a>")
			input "debugLog", "bool", title: "Enable debug logging", submitOnChange: true, defaultValue: false
        }
    }
}

def addBeaconsPage() {
	def newBeacons = [:]
	state.beacons.each { beacon ->
		def isChild = getChildDevice(beacon.value.uuid)
		if (!isChild) {
			newBeacons["${beacon.value.uuid}"] = "${beacon.value.uuid}"
		}
	}
    
    return dynamicPage(name: "addBeaconsPage", title: "Add Beacons", install: false, nextPage: addBeaconsStatusPage) {
        section(){
			input ("selectedAddBeacons", "enum",
				   required: false,
				   multiple: true,
				   title: "Select beacons to add (${newBeacons.size() ?: 0} new detected).\n\t" +
				   "Total Beacons: ${state.beacons.size()}",
				   description: "Use the dropdown to select beacons.  Then select 'Next'.",
                   options: newBeacons)
        }
    }
}

def addBeaconsStatusPage() {
    selectedAddBeacons.each{ beacon ->
        logDebug("beacon: " + beacon)
		def isChild = getChildDevice(beacon)
		if (!isChild) {
            addChildDevice("ajardolino3", "Bluetooth Beacon", beacon)
        }
    }
	app?.removeSetting("selectedAddDevices")
	return dynamicPage(name:"addBeaconsStatusPage",
					   title: "Installation Status",
					   nextPage: mainPage,
					   install: false) {
	 	section() {
			paragraph "Installed"
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
    // The root path - you can also map other paths or use parameters in paths and posted data
    path("/beacons") { action: [GET: "getBeacons"]}
    path("/gateway") { action: [POST: "postGateway"] }
}

// receives incoming post requests from the gateway, sends to the gateway driver for parsing, then updates state data from parsed request
def postGateway() {
    logDebug("POST received from Gateway: " + request.body)
    logDebug("Sending payload to device: " + gateway.name + ", payload: " + request.body)
    def result = gateway.parsePayload(request.body)
    def parsed = gateway.getDataValue("parsed")

    def slurper = new JsonSlurper()
    def obj = slurper.parseText(parsed)
    logDebug("Parsed payload from Gateway: " + obj)

    logDebug("Beacon State BEFORE Update: " + state.beacons)
    obj.beacons.each { beacon ->
        logDebug("beacon" + beacon.uuid + ", present: " + beacon.present)
        def b = state.beacons.find{ c -> c == beacon.uuid }
        if(b) {
            logDebug("Update Beacon: " + beacon.uuid)
            b.present = beacon.present
        }
        else {
            logDebug("Create New Beacon: " + beacon.uuid)
            b = createBeacon(beacon.uuid, beacon.present)
            state.beacons["${beacon.uuid}"] = b
        }
    }
    logDebug("Beacon State AFTER Update: " + state.beacons)
    
	state.beacons.each { beacon ->
        logDebug("Check Beacon: " + beacon.value.uuid)
		def isChild = getChildDevice(beacon.value.uuid)
		if (isChild) {
            logDebug("Child Beacon: " + beacon.value.uuid + ", present: " + isChild.currentValue("presence"))
            if(beacon.value.present) {
                isChild.sendEvent(name: "presence", value: "present")
            } else {
                isChild.sendEvent(name: "presence", value: "not present")
            }
    	}
    }
}

def createBeacon(uuid, present) {
    def beacon = [:]
    beacon.uuid = uuid
    beacon.present = present
    return beacon
}

def getBeacons() {
    def obj = [:] //define map
    obj.beacons = [] //define array 

    childApps.each { child ->
        def b = [:] 
        def presence = child.getSetting("presence")
        b.id = presence.id
        b.name = child.label
        b.uuid = child.getSetting("uuid")
        obj.beacons.push(b)
    }
    
    def result = JsonOutput.toJson(obj)
    render contentType: "application/json", data: result, status: 200
}

def logDebug(msg) {
    if(debugLog) log.debug(msg)
}

