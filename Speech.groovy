/**
 *  Speech
 *
 *  Copyright 2014 Brown CS
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Speech",
    namespace: "brown.smartthings",
    author: "Brown CS",
    description: "Control through speech recognition",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)


preferences {
	section("Allow Endpoint to Control These Things...") {
		input "switches", "capability.switch", title: "Which Switches?", multiple: true, required: false
        input "locks", "capability.lock", title: "Which Locks?", multiple: true, required: false
	}
}
 
mappings {
 	
    path("/devices") {
    	action: [
        	GET: "listDevices"
        ]
    }
	path("/switches") {
		action: [
			GET: "listSwitches"
		]
	}
	path("/switches/:id/:command") {
		action: [
			GET: "updateSwitch"
		]
	}   
    path("/locks") {
		action: [
			GET: "listLocks"
		]
	}
    path("/locks/:id/:command") {
		action: [
			GET: "updateLock"
		]
	}   
    
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def listDevices(){
    def d_map = [:]
    def s_list = switches.collect{deviceFormat(it)}	
    if(s_list.size()!=0){
    	d_map.put("Switches", s_list)
    }
    
    def l_list = locks.collect{deviceFormat(it)}
    if(l_list.size()!=0){
    	d_map.put("Locks", l_list)
    }
    d_map
}

def listSwitches() {
	switches.collect{deviceFormat(it,"switch")}
}
 
def showSwitch() {
	show(switches, "switch")
}

void updateSwitch() {
	update(switches)
}

def listLocks() {
	locks.collect{deviceFormat(it,"lock")}
}
 
def showLock() {
	show(locks, "lock")
}

void updateLock() {
	update(locks)
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
}

private void update(devices) {
    
    def command = params.command
	if (command) 
    {
		def device = devices.find { it.id == params.id }
		if (!device) {
			httpError(404, "Device not found")
		} else {
        	if(command == "toggle")
       		{
            	if(device.currentValue('switch') == "on")
                  device.off();
                else
                  device.on();
       		}
       		else
       		{
				device."$command"()
            }
		}
	}
} 
 
private deviceFormat(it) {
	it ? [id: it.id, label: it.label] : null
}