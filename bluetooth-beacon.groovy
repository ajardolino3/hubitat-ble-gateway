import groovy.json.*

metadata {
	definition (
        name: "Bluetooth Beacon", 
        namespace: "ajardolino3", 
        author: "Art Ardolino")
    {
        capability "Beacon"
        attribute "presence", "enum", ["not present", "present"]
	}   
}

def installed() {
    sendEvent(name: "presence", value: "not present")
}