import groovy.json.*

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
        if(data.length() == 60) {
            def uuid = data.substring(18,18+32)
            def major = Integer.parseInt(data.substring(18+32,18+32+4),16)
            def minor = Integer.parseInt(data.substring(18+32+4,18+32+8),16)
            log.debug("iBeacon detected: " + uuid + ", major: " + major + ", minor: " + minor)
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
    log.debug(msg)
}
