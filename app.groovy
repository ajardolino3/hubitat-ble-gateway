import groovy.json.*

def appVersion() { return "1.0.0" }

definition(
	name: "April Brother BLE Gateway Manager",
	namespace: "ajardolino3",
	author: "Art Ardolino",
	description: "Links bluetooth beacons to virtual presence devices using a April Brother Bluetooth Gateway",
	category: "Bluetooth",
	iconUrl: "",
	iconX2Url: "",
	singleInstance: true,
	documentationLink: "",
	importUrl: ""
)

preferences() {
    page name: "mainPage"
}

def mainPage() {
    if (!state.accessToken) {   
        // Enable OAuth in the app settings or this call will fail
        createAccessToken() 
    }
    String uri = makeUri("/");
    return dynamicPage(name: "mainPage", title: "April Brother BLE Gateway Manager", uninstall: true, install: true) {
        section(){ 
            paragraph("This app uses an <a href=\"https://store.aprbrother.com/product/ab-ble-gateway-4-0\" target=\"_blank\">April Brother BLE Gateway (v4)</a> to associate beacons with Virtual Presence devices to automate events.")
            app(name: "anyOpenApp", appName: "April Brother BLE Beacon", namespace: "ajardolino3", title: "<b>Add a new Bluetooth Beacon</b>", multiple: true)
            paragraph("Gateway URL: <a href='${uri}'>${uri}</a>")
			input "debugLog", "bool", title: "Enable debug logging", submitOnChange: true, defaultValue: false
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

