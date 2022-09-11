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
    page name: "addBeacons"
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
            app(name: "anyOpenApp", appName: "April Brother BLE Beacon", namespace: "ajardolino3", title: "<b>Add a new Bluetooth Beacon</b>", multiple: true)
            paragraph("Gateway URL: <a href='${uri}'>${uri}</a>")
			input "debugLog", "bool", title: "Enable debug logging", submitOnChange: true, defaultValue: false
        }
    }
}

def addBeacons() {
    
    return dynamicPage(name: "addBeacons", title: "Add Beacons", install: false) {
        section(){
            input "name", type: "text", required: true, title: "Enter Name for Gateway"
        }
    }
}

private def makeUri(String extraPath) {
    return getFullLocalApiServerUrl() + extraPath + "gateway?access_token=${state.accessToken}"
}


def installed() {
    // called when app is installed
}

def updated() {
    // called when settings are updated
}

def uninstalled() {
    // called when app is uninstalled
}

mappings {
    // The root path - you can also map other paths or use parameters in paths and posted data
    path("/beacons") { action: [GET: "getBeacons", POST: "postBeacons"]}
    path("/gateway") { action: [POST: "postGateway"] }
}

def postGateway() {
    logDebug("POST received from Gateway: " + request.body)
    logDebug("Sending payload to device: " + gateway.name + ", payload: " + request.body)
    def result = gateway.parsePayload(request.body)
    def parsed = gateway.getDataValue("parsed")

    def slurper = new JsonSlurper()
    def obj = slurper.parseText(parsed)
    logDebug("Parsed payload from Gateway: " + obj)

    logDebug("Beacon State BEFORE Update: " + state.beacons)
    state.beacons = [:]
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

def postBeacons() {
    def slurper = new JsonSlurper()
    def obj = slurper.parseText(request.body)

    def result = JsonOutput.toJson(obj)
    state.beacons = obj
    logDebug(state.beacons)

    render contentType: "application/json", data: result, status: 200
}

def logDebug(msg) {
    if(debugLog) log.debug(msg)
}

