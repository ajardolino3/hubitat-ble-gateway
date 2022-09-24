import groovy.json.*

def deviceVersion() { return "1.3.0" }

metadata {
	definition (
        name: "April Brother BLE Gateway", 
        namespace: "ajardolino3", 
        author: "Art Ardolino", 
        singleThreaded: true)  // important: required so gateway manager and gateway driver can communicate with each other 
    {
        capability "PresenceSensor"
        command "parsePayload", [[name: "payload", type: "STRING"]] 
	}   
    preferences {
		input ("debugLog", "bool", title: "Enable debug logging", defaultValue: false)
    }
}

def parsePayload(payload) {
    def slurper = new JsonSlurper()
    def parsed = slurper.parseText(payload)

    def obj = [:]
    obj.beacons = []
    
    def allbeacons = []

    parsed.devices.each{ device ->
        def type = device[0]
        def b = [:]
        b.type = "auto"
        b.mac = device[1]
        b.rssi = device[2]
        b.data = device[3]
        obj.beacons.push(b)
        logDebug(b)
    }
                  
    def json = JsonOutput.toJson(obj)
    updateDataValue("parsed", json)
}

def installed() {
}

def logDebug(msg) {
    if(debugLog)log.debug(msg)
}
