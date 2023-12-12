import groovy.json.*

def appVersion() { return "1.4.1" }

definition(
	name: "BLE Gateway Manager",
	namespace: "ajardolino3",
	author: "Art Ardolino",
	description: "Processes incoming bluetooth beacon data for home automation",
	category: "Bluetooth",
	iconUrl: "",
	iconX2Url: "",
	singleInstance: true,
	documentationLink: "https://github.com/ajardolino3/hubitat-ble-gateway/blob/main/README.md",
    importUrl: "",
    singleThreaded: true // important: required so gateway manager and gateway driver can communicate with each other
)

preferences() {
    page name: "mainPage"
    page name: "addBeaconPage"
    page name: "addBeaconStatusPage"
    page name: "listBeaconsPage"
}

def mainPage() {
    if (!state.accessToken) {   
        // Enable OAuth in the app settings or this call will fail
        createAccessToken() 
    }
    String uri = makeUri("/");
    return dynamicPage(name: "mainPage", title: "BLE Gateway Manager", uninstall: true, install: true) {
        section(){ 
            paragraph("The BLE Gateway Manager processes data from a Bluetooth (BLE) Gateway to detect the presence of beacons.")
			input "gateway", "capability.presenceSensor", title: "Select your BLE Gateway Device", multiple: false, required: true
            paragraph("Configure your gateway to use this URL: <a href='${uri}'>${uri}</a>")
			href "addBeaconPage", title: "<b>Add New Beacon</b>", description: "Adds new beacon device."
			href "listBeaconsPage", title: "<b>List Beacons</b>", description: "Lists added beacon devices."
            input "departRetries", "number", title: "Number of retries before marking as Departed", submitOnChange: true, defaultValue: 2, width:6
            input "envValue", "number", title: "Environment Value (2..4) <small>* This will help with the distance calculation</small>", range: '2..4', submitOnChange: true, defaultValue: 2, width:6
            input "nonCompliant", "bool", title: "Enable non-compliant device support", submitOnChange: true, defaultValue: false
			input "debugLog", "bool", title: "Enable debug logging", submitOnChange: true, defaultValue: false
            input "descLog", "bool", title: "Enable description logging", submitOnChange: true, defaultValue: false
        }
    }
}

def listBeaconsPage() {
    def devices = getChildDevices()
    def message = ""
    devices.each{ beacon ->
        message += "\t${beacon.label} (ID: ${beacon.getDeviceNetworkId()})\n"
    }
    return dynamicPage(name: "listBeaconsPage", title: "List Beacons", install: false, nextPage: mainPage) {
        section() {
            paragraph "The following beacon devices were added using the 'Add New Beacon' feature, and are setup to interface with the selected gateway."
            paragraph message
        }
    }
}

def addBeaconPage() {
	def newBeacons = [:]
	state.beacons.each { beacon ->
		def isChild = getChildDevice(beacon.value.dni)
		if (!isChild && beacon.value.present) {
            newBeacons["${beacon.value.dni}"] = "${beacon.value.type}: ${beacon.value.dni}"
		}
	}
    
    return dynamicPage(name: "addBeaconPage", title: "Add New Beacon", install: false, nextPage: addBeaconStatusPage) {
        section(){
            paragraph "NOTE: Beacon must be actively broadcasting and detected by the gateway to appear in this list.  If the beacon does not appear and is actively broadcasting, wait a few seconds and then refresh this page and try again."
			input ("selectedAddBeacon", "enum",
				   required: true,
				   multiple: false,
				   title: "Select a beacon to add (${newBeacons.size() ?: 0} new beacons detected)",
				   description: "Use the dropdown to select beacon.",
                   options: newBeacons)
            input "selectedBeaconName", "string", required: true, title: "Give this Beacon a Name"
        }
    }
}

def addBeaconStatusPage() {
    def status = []
    logDebug("beacon: " + selectedAddBeacon)
    def isChild = getChildDevice(selectedAddBeacon)
    def success = false
    def err = ""
    if (!isChild) {
        try {
            addChildDevice("ajardolino3", "BLE Beacon", selectedAddBeacon, ["label": selectedBeaconName])
            success = true
        }
        catch(exception) {
            error = exception
        }
    }
    if(success) {
        message = "New beacon added successfully (" + selectedBeaconName + ")."
    } else {
        message = "Unable to add beacon: " + err;
    }
	app?.removeSetting("selectedAddBeacon")
	app?.removeSetting("selectedBeaconName")
    
	return dynamicPage(name:"addBeaconStatusPage",
					   title: "Add Beacon Status",
					   nextPage: mainPage,
					   install: false) {
	 	section() {
            paragraph message
		}
	}
}

private def makeUri(String extraPath) {
    return getFullLocalApiServerUrl() + extraPath + "gateway?access_token=${state.accessToken}"
}


def installed() {
    // called when app is installed
    state.beacons = [:]
}

def updated() {
    // called when settings are updated
}

def uninstalled() {
    // called when app is uninstalled
}

mappings {
    path("/gateway") { action: [POST: "postGateway"] }
}

// receives incoming post requests from the gateway, sends to the gateway driver for parsing, then updates state data from parsed request
def postGateway() {
    logDebug("POST received from Gateway: " + request.body)
    logDebug("Sending payload to device: " + gateway.name + ", payload: " + request.body)
    def result = null
    try
    {
        result = gateway.parsePayload(request.body)
    }
    catch(error)
    {
        log.error("Unable to parse payload: " + error)
        return
    }
    def parsed = gateway.getDataValue("parsed")

    def slurper = new JsonSlurper()
    def obj = slurper.parseText(parsed)
    logDebug("Parsed payload from Gateway: " + obj)
    
    state.beacons.each { beacon ->
        beacon.value.present = false
    }
    
    def detected = String.format("%tF %<tH:%<tM", java.time.LocalDateTime.now())
    obj.beacons.each { beacon ->
        beacon.dni = ""
        if(beacon.type)
        {
            switch(beacon.type) {
                case "iBeacon":
                    if(beacon.uuid instanceof String && beacon.major instanceof Integer && beacon.minor instanceof Integer) {
                        beacon.dni = "${beacon.uuid}:${beacon.major}:${beacon.minor}"
                        logDebug("iBeacon detected:" + beacon.dni)
                    } else {
                        logDebug("iBeacon type has invalid uuid, major, or minor types")
                    }
                    break
                case "auto":
                    if(beacon.data instanceof String && beacon.rssi instanceof Integer)
                    {
                        def pobj = parseBeaconData(beacon)
                        if(pobj.success) {
                            setBeaconData(pobj)
                        } else {
                            logDebug("error parsing beacon: " + pobj.error)
                        }
                    }
                    break
                default:
                    log.error("unknown beacon type: " + beacon.type)
                    break
            }
        }
        if(beacon.dni.length()>0)
        {
            if(beacon.type=="eddystone-UID") {
                logDebug("eddystone-UID detected: namespace: ${beacon.namespace}, instance: ${beacon.instance}, power: ${beacon.power}, rssi: ${beacon.rssi}, distance: ${beacon.distance}, dni: ${beacon.dni}")
            }
            else {
                logDebug("${beacon.type} detected: uuid: ${beacon.uuid}, major: ${beacon.major}, minor: ${beacon.minor}, power: ${beacon.power}, rssi: ${beacon.rssi}, distance: ${beacon.distance}, dni: ${beacon.dni}")
            }
            
            def b = state.beacons["${beacon.dni}"]
            if(!b) {
                b = [:]
                b.dni = beacon.dni
            }
            b.present = true
            b.detected = detected
            b.rssi = beacon.rssi * -1
            b.distance = beacon.distance
            b.power = beacon.power * -1
            b.type = beacon.type
            state.beacons["${beacon.dni}"] = b
        }
    }
    
    def newstate = [:]
    departRetries = departRetries ?: 2
    
	state.beacons.each { beacon ->
        def b = beacon.value
		def isChild = getChildDevice(b.dni)
		if (isChild) {
            depCheck = isChild.currentValue("depCheck") ?: 0
            //if(descLog) log.info("Beacon ${b.dni}, Dep Check: ${depCheck}")
            logDebug("beacon ID: " + b.dni + " present: " + b.present)
            if(isChild.currentValue("presence") == "present" && !b.present) {
                depCheck += 1
                if(descLog) log.info("Beacon ${b.dni}, ${depCheck} vs ${departRetries} (1)")
                if(depCheck >= departRetries) {
                    if(descLog) log.info("Beacon ${b.dni}, departed (${depCheck} vs ${departRetries}) (2)")
                    isChild.departed()
                } else {
                    sendEvent(isChild, [name: "depCheck", value: depCheck, descriptionText: "depCheck set"])
                }
            }
            else if(isChild.currentValue("presence") == "not present" && b.present) {
                sendEvent(isChild, [name: "depCheck", value: 0, descriptionText: "depCheck set"])
                if(descLog) log.info("Beacon ${b.dni}, arrived")
                isChild.arrived()
            } else {
                if(depCheck > 0) {
                    sendEvent(isChild, [name: "depCheck", value: 0, descriptionText: "depCheck set"])
                    if(descLog) log.info("Beacon ${b.dni}, Reseting: ${depCheck} vs ${departRetries} (3)")
                }
            }
            if(!b.present) {
                b.rssi = 999999999
                b.power = 999999999
                b.distance = 999999999
            }
            if(b.present) {
                sendEvent(isChild, [name: "type", value: b.type, descriptionText: "Type set"])
                sendEvent(isChild, [name: "rssi", value: b.rssi, descriptionText: "RSSI value set"])
                sendEvent(isChild, [name: "power", value: b.power, descriptionText: "Power value set"])
                sendEvent(isChild, [name: "distance", value: b.distance, descriptionText: "Distance value set"])
                sendEvent(isChild, [name: "depCheck", value: 0, descriptionText: "depCheck set"])
            }
    	}
        if(isChild || b.present) {
            newstate["${b.dni}"] = b
        }
    }
        
    state.beacons = newstate
}

def parseBeaconData(beacon){
    def data = beacon.data
    if(nonCompliant) {
        if(data.length() == 56) data = "02011A" + data // altbeacon fix
        else if(data.length() == 54) data = "02011A" + data // ibeacon fix
        else if(data.length() == 52) data = "020106" + data // eddystone-UID fix
    }
    def rssi = beacon.rssi
    def obj = [:]
    obj.success = true
    obj.parsed = []
    obj.beacon = beacon
    def i = 0
    while(data.length()>=2 && i<100){
        i=i+1
        def p = [:]
        try{
            
        def next = data.substring(0,2)
        data = data.substring(2)
        p.length = Integer.parseInt(next,16)
        p.type = data.substring(0,2)
        p.segment = data.substring(2,2*p.length)
        data = data.substring(2*p.length)
        obj.parsed.push(p)
        }catch(error){
            p.error = error
            obj.parsed.push(p)
            obj.success = false
            break
        }
    }
    return obj
}

def setBeaconData(obj) {
    def pd = obj.parsed
    obj.beacon.power = 999999999
    obj.beacon.distance = obj.beacon.power
    def power = ""
    logDebug("set beacon data: " + obj);
    if(pd.size() == 2 && (pd[1].length == 26 || pd[1].length == 27) && pd[1].type == "FF")
    {
        obj.beacon.type = "iBeacon"
        if(pd[1].length==27) obj.beacon.type = "altBeacon"
        obj.beacon.uuid = pd[1].segment.substring(8,40)
        obj.beacon.major = Integer.parseInt(pd[1].segment.substring(40,44),16)
        obj.beacon.minor = Integer.parseInt(pd[1].segment.substring(44,48),16)
        obj.beacon.dni = "${obj.beacon.uuid}:${obj.beacon.major}:${obj.beacon.minor}"
        power = pd[1].segment.substring(48,50)
    }
    else if(pd.size() == 3 && pd[2].length == 23 && pd[2].type == "16")
    {
        obj.beacon.type = "eddystone-UID"
        obj.beacon.namespace = pd.segment[2].substring(8,28)
        obj.beacon.instance = pd.segment[2].substring(28,40)
        obj.beacon.dni = "${obj.beacon.namespace}:${obj.beacon.instance}"
        power = pd.segment[2].substring(6,8)
    }
    else if(nonCompliant && pd.size() == 3 && pd[2].length == 21 && pd[2].type == "16") // eddystone-uid fix
    {
        obj.beacon.type = "eddystone-UID"
        obj.beacon.namespace = pd.segment[2].substring(8,28)
        obj.beacon.instance = pd.segment[2].substring(28,40)
        obj.beacon.dni = "${obj.beacon.namespace}:${obj.beacon.instance}"
        power = pd.segment[2].substring(6,6)
    } 
    if(power.length()>0) {
        try
        {
            BigInteger bigInt = BigInteger.valueOf(Integer.parseInt(power, 16))
            obj.beacon.power = (bigInt.toByteArray())[1]
            if(obj.beacon.type == "eddystone-UID") obj.beacon.power -= 41 // subtract 41 to adjust to power at 1 meter
            obj.beacon.distance = Math.round(getDistanceInFeet(obj.beacon.power, obj.beacon.rssi))
        }
        catch(error)
        {
        }
    }
}

def getDistanceInFeet(measuredPower, rssi) {
    def ratio_db = measuredPower - rssi
    def dBm = envValue ?: 2 //environment value (2-4)
    def meters = Math.pow(10, ratio_db / (10*dBm))
    return meters * 3.28084 // convert to feet
}


def logDebug(msg) {
    if(debugLog) log.debug(msg)
}
