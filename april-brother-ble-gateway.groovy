import groovy.json.*

def deviceVersion() { return "1.1.0" }

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
    logDebug("parsePayload received: " + payload)
    def slurper = new JsonSlurper()
    def parsed = slurper.parseText(payload)

    def obj = [:]
    obj.beacons = []

    parsed.devices.each{ device ->
        def type = device[0]
        def mac = device[1]
        def rssi = device[2]
        def data = device[3]
        logDebug("beacon detected: type: ${type}, mac: ${mac}, rssi: ${rssi}, data: ${data}")
        
        // beacon data starting with 0201 is an iBeacon
        // substring(6,8) indicates length, and a length of 1A = 26 bytes (contains UUID (16 bytes), Major (4 bytes), Minor (4 bytes), and transmit power (2 bytes))
        // see Apple's iBeacon specification for more information: https://developer.apple.com/ibeacon/
        if(data.length() == 60 && data.substring(0,4) == "0201" && data.substring(6,8) == "1A") {
            def uuid = data.substring(18,18+32)
            def major = Integer.parseInt(data.substring(18+32,18+32+4),16)
            def minor = Integer.parseInt(data.substring(18+32+4,18+32+8),16)
            logDebug("iBeacon detected: " + uuid + ", major: " + major + ", minor: " + minor)
            def beacon = [:]
            beacon.type = "iBeacon"
            beacon.uuid = uuid
            beacon.major = major
            beacon.minor = minor
            obj.beacons.push(beacon)
        }
    }
                  
    def json = JsonOutput.toJson(obj)
    updateDataValue("parsed", json)
    logDebug("parsePayload parsed: " + json)
}

def installed() {
}

def logDebug(msg) {
    if(debugLog)log.debug(msg)
}
