import groovy.json.*

def deviceVersion() { return "1.2.0" }

metadata {
	definition (
        name: "BLE Beacon", 
        namespace: "ajardolino3", 
        author: "Art Ardolino")
    {
        capability "Beacon"
        capability "PresenceSensor"
        command "arrived"
        command "departed"
        attribute "presence", "enum", ["not present", "present"]
        attribute "rssi", "number"
        attribute "power", "number"
        attribute "distance", "number"
	}   
}

def installed() {
    sendEvent(name: "presence", value: "not present")
    sendEvent(name: "rssi", value: 999999999)
    sendEvent(name: "power", value: 999999999)
    sendEvent(name: "distance", value: 999999999)
}

def arrived() {
    sendEvent(name: "presence", value: "present", descriptionText: "BLE beacon has arrived.")
}

def departed() {
    sendEvent(name: "presence", value: "not present", descriptionText: "BLE beacon has departed.")
    sendEvent(name: "rssi", value: 999999999)
    sendEvent(name: "power", value: 999999999)
    sendEvent(name: "distance", value: 999999999)
}