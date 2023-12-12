import groovy.json.*

def deviceVersion() { return "1.3.1" }

metadata {
	definition (
        name: "BLE Beacon Modified", 
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
        attribute "type", "string"
        attribute "since", "string"
        attribute "depCheck", "number"
	}
    preferences() {    	
        section(""){
            input "logEnable", "bool", title: "Enable logging", required: true, defaultValue: true
        }
    }
}

def installed() {
    sendEvent(name: "presence", value: "not present")
    sendEvent(name: "rssi", value: 999999999)
    sendEvent(name: "power", value: 999999999)
    sendEvent(name: "distance", value: 999999999)
    sendEvent(name: "type", value: "unknown")
    sendEvent(name: "since", value: "unknown")
    sendEvent(name: "depCheck", value: 0)
}

def arrived() {
    sendEvent(name: "presence", value: "present", descriptionText: "BLE beacon has arrived.")
    def theWhen = new Date()
    sendEvent(name: "since", value: theWhen)
    if(logEnable) log.info "Beacon has Arrived"
}

def departed() {
    sendEvent(name: "presence", value: "not present", descriptionText: "BLE beacon has departed.")
    sendEvent(name: "rssi", value: 999999999)
    sendEvent(name: "power", value: 999999999)
    sendEvent(name: "distance", value: 999999999)
    def theWhen = new Date()
    sendEvent(name: "since", value: theWhen)
    sendEvent(name: "depCheck", value: 0)
    if(logEnable) log.info "Beacon has Departed"
}
