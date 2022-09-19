import groovy.json.*

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
	}   
}

def installed() {
    sendEvent(name: "presence", value: "not present")
}

def arrived() {
    sendEvent(name: "presence", value: "present", descriptionText: "BLE beacon has arrived.")
}

def departed() {
    sendEvent(name: "presence", value: "not present", descriptionText: "BLE beacon has departed.")
}