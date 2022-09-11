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
    log.debug("parsePayload received: " + payload)
    def slurper = new JsonSlurper()
    def parsed = slurper.parseText(payload)
    parsed.beacons[0].uuid = parsed.beacons[0].uuid + "abc"
    def json = JsonOutput.toJson(parsed)
    updateDataValue("parsed", json)
    log.debug("parsePayload parsed: " + json)
}

def installed() {
}