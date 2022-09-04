def appVersion() { return "1.0.0" }

definition(
    parent: "ajardolino3:April Brother BLE Gateway Manager",
	name: "April Brother BLE Beacon",
	namespace: "ajardolino3",
	author: "Art Ardolino",
	description: "Child app for the Bluetooth Beacon Manager",
	category: "Bluetooth",
	iconUrl: "",
	iconX2Url: ""
)

preferences {
     page name: "mainPage"
}

def mainPage() {
	return dynamicPage(name: "mainPage", title: "Bluetooth Beacon", install: true, uninstall: true) {
		section("") {
			input "presence", "capability.presenceSensor", title: "Virtual Presence Device", multiple: false, required: true
            input "uuid", "string", title: "Beacon UUID", required: true
		}
	}
}

def installed() {
    // called when app is installed
}

def updated() {
    // called when settings are updated
    log.info settings.presence.displayName
    app.updateLabel(presence.displayName)
}

def uninstalled() {
    // called when app is uninstalled
}

