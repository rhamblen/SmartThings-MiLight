/**
 *  MiLight / EasyBulb / LimitlessLED Light Controller
 *
 *  Copyright 2015 Jared Jensen / jared /at/ cloudsy /dot/ com
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
metadata {
	definition (name: "MiLight Controller", namespace: "cloudsyjared", author: "Jared Jensen") {
		capability "Switch Level"
		capability "Actuator"
		capability "Switch"
        capability "Color Control"
        capability "Polling"
        
        capability "Refresh" 
        
        command "setAdjustedColor"
	}
    
    preferences {       
       input "mac", "string", title: "MAC Address",
       		  description: "The MAC address of your MiLight bridge", defaultValue: "DE:AD:BE:EF:CA:FE",
              required: true, displayDuringSetup: true 
       
       input "group", "number", title: "Group Number",
       		  description: "The group you wish to control (0-4), 0 = all", defaultValue: "0",
              required: true, displayDuringSetup: true
       }

	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821", nextState:"off"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"on"
			}
			tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action:"switch level.setLevel"
			}
            tileAttribute ("device.color", key: "COLOR_CONTROL") {
				attributeState "color", action:"setAdjustedColor"
			}
		}
   
		main(["switch"])
		details(["switch","levelSliderControl", "rgbSelector"])
	} 
}

// handle commands
def setLevel(percentage) {
	log.debug "setLevel ${percentage}"
	if (percentage < 1 && percentage > 0) {
		percentage = 1 // clamp to 1%
	}
        
	def path = buildPath("rgbw/brightness", percentage, group);
    
    return httpCall(path);
}

def setAdjustedColor(value) {
    log.debug "setAdjustedColor: ${value}"

    int r = value.red
    int g = value.green
    int b = value.blue
    
    def path = buildColorPath(r, g, b, group);

	return httpCall(path);
}

def on() {
	log.debug "Device setOn"
    def path = buildPath("rgbw", "on", group);
    
    return httpCall(path);
}

def off() {
	log.debug "Device setOff"
    
    def path = buildPath("rgbw", "off", group);
    
    return httpCall(path);
}

private buildPath(option, value, grp = 0) {
	def path = ""
    
	if(grp == 0 || grp == null) {
    	path = "$option/$value"
    } else {
    	path = "$option/$value/$grp"
    }
    
    log.debug "My path: $path"
    
    return path;
}

private buildColorPath(red, green, blue, grp = 0) {
	def path = ""
    def value = ""
    
    value = "rgbw/color/r/$red/g/$green/b/$blue"
    
	if(grp == 0 || grp == null) {
    	path = "$value"
    } else {
    	path = "$value/$grp"
    }
    
    log.debug "My path: $path"
    
    return path;
}

private httpCall(path) {
    def params = [
        uri:  'http://lights.cloudsy.com:9292/v1/',
        path: "$path",
        contentType: 'application/json',
        headers: [MAC:"$mac"]
    ]
    try {
        httpGet(params) {resp ->
            log.debug "resp data: ${resp.data}"
        }
    } catch (e) {
        log.error "error: $e"
    }
}