myhome
======
#### Overview
- *myhome* application provides a constant listening service that allows to send basic voice commands to SmartThings devices
- With the service constantly listening, saying "my home" will prompt a beep sound
- If the user gives a command after the beep sound, the command will be sent to Wit's API and will be parsed based on the rules specified in Wit's console
- On retrieving the parsed result from Wit, the application will match up the device and action to SmartThings
- If the command is valid, the application will send a HTTPRequest to the *Speech* SmartApp that has control access to the SmartThings devices

#### Installation Instructions
1. Download Android Studio from http://developer.android.com/sdk/index.html
2. From Android Studio, click "Check out from Version Control" and select "Github"
3. Enter your github credentials and clone "/myhome.git" repository
4. Connect Android device and run application on your device

#### How to use Application
1. Open up application and login to SmartThings
2. After authorizing access, slide "listening" button to start/stop constant listening
3. When constant listening is on, wake up recognition by saying the keyword "my home"
4. If "my home" is understood, a toast notification and beep sound will occur
5. After beep sound, speak command (eg. Turn on kitchen light)
6. At end of command, double beep will occur to notify that the app listened
7. If successful, the specified SmartThings device will respond to the command

#### Available Commands
1. Turn on/off light/switch (id)
  - (id) can be specified either as a number based on the device list or by the label given to the device
  - eg. If a switch has the name "kitchen" in the SmartThings app and the switch is listed as number 1 on the *myhome* device list, 
  the switch will respond to "Turn on light 1", "Turn on switch 1", "Turn on kitchen light", "Turn on light in kitchen" 
2. Unlock/lock lock (id)
  - (id) same restrictions as switches
  
#### Limitations
* Mic Access: When constant listening is on, Mic Access is solely dedicated to the *myhome* application, thus to respond to phone calls etc. should turn of listening 
* Command by Device Name: 
   - The application can only match up device names from the speech command to the SmartThings device, if Wit.ai has been trained/notified of the specific name
   - Currently, application can only match up devices with the name of "kitchen", "living room" , or "room" 
   - To allow the application to parse more device names, add the new device name under the "id" entity in Wit.ai's console
   - The best results are when devices are specified by number
* Timimg of Commands: 
  - As Android's SpeechRecognition library doesn't naturally provide constant listening, to enable this functionality, the application is constantly renewing the listener
  - Thus, there may be times speech falls through when the service is renewing. In order to make sure that the command was listened, please follow the beep sound
