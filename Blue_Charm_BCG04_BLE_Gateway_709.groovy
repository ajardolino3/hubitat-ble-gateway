import groovy.json.*

def deviceVersion() { return "1.4.2" }

metadata {
	definition (
        name: "Blue Charm BCG04 BLE Gateway", 
        namespace: "skotman01", 
        author: "Scott Heath", 
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

//    def allbeacons = []
    parsed.obj.each{ device ->
        def type = device.type
        def b = [:]
        b.type = "auto"
        if(device.uuid != null)
        {
            b.type = "iBeacon"
            b.uuid = device.uuid
            b.major = device.majorID
            b.minor = device.minorID
//            logDebug(device)
        }
        else
        {
            b.data = device.data1
        }
        
        b.rssi = device.rssi
        obj.beacons.push(b)
    }

    def json = JsonOutput.toJson(obj)
    updateDataValue("parsed", json)
    
}

def installed() {
}

def logDebug(msg) {
    if(debugLog)log.debug(msg)
}
