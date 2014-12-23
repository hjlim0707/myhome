package com.example.hyojulim.myhome;

import ai.wit.sdk.IWitListener;
import ai.wit.sdk.model.WitOutcome;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.SharedPreferences;
import android.util.Log;
import android.content.Context;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Class that implements IWitListener, which allows connection with Wit's API
 * Sends raw sentence received from speech recognition to Wit's API
 * Use WitOutcome received from Wit's API to determine which action/device should be called upon
 */
public class WitActivity implements IWitListener {

    private Context context;
    private JSONObject devices_array;
    private SharedPreferences pref;

    public WitActivity(Context context, String devices){
        this.context = context;
        pref = context.getSharedPreferences("st_info", 0); // set up shared preferences
        try {
            devices_array = new JSONObject(devices); // list of devices
        }catch(JSONException e){
            Log.e(getClass().getName(),"JSONException: "+e.getLocalizedMessage());
        }
    }

    /**
     * Called when the Wit request is completed.
     * @param outcomes ArrayList of model.WitOutcome - null in case of error
     * @param messageId String containing the message id - null in case of error
     * @param error - Error, null if there is no error
     */
    @Override
    public void witDidGraspIntent(ArrayList<WitOutcome> outcomes,  String messageId, Error error){
        if(error!=null){
            Toast.makeText(this.context, "Wit Error", Toast.LENGTH_SHORT).show();
            return;
        }
        else{
            if(outcomes.size()!=0){
                Toast.makeText(this.context, "Wit Processing", Toast.LENGTH_SHORT).show();
                parseWitOutcome(outcomes.get(0)); // parse outcome to retrieve action and device
            }
            else{
                Toast.makeText(this.context, "Wit Could Not Process", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Parse outcome from Wit
     * Determine which action should be performed on which device
     * Device can be specified either by number (based on index in device array) or by the device name
     * Currently works on turning on/off switches and lock/unlock locks
     */
    public void parseWitOutcome(WitOutcome outcome){
        Log.d("OUTCOME", outcome.get_entities().toString());
        String intent = outcome.get_intent();
        if(intent == null){
            Toast.makeText(this.context, "No Matching Intent", Toast.LENGTH_SHORT).show();
            return;
        }
        /* If intent is to update light */
        else if(intent.equals("update_light")){
            // Get list of switches from devices_array
            JSONArray switches_array = null;
            try{
                switches_array = devices_array.getJSONArray("Switches");
            }catch(JSONException e){
                Log.e(getClass().getName(),"JSONException: "+e.getLocalizedMessage());
            }
            if(switches_array == null){
                Toast.makeText(this.context, "No Switches Available", Toast.LENGTH_SHORT).show();
                return;
            }
            // Parse action(on/off) from Wit response
            String onOff = null;
            if(outcome.get_entities().containsKey("on_off")){
                onOff = outcome.get_entities().get("on_off").toString();
                try{
                    JSONArray onOffArr = new JSONArray(onOff);
                    onOff = onOffArr.getJSONObject(0).getString("value");
                }catch(JSONException e){
                    Log.e(getClass().getName(),"JSONException: "+e.getLocalizedMessage());
                }
            }
            if(onOff == null){ // If no specific action is given, just toggle
                onOff = "toggle";
            }
            // If switch id is specified
            if(outcome.get_entities().containsKey("id")){
                String idS = outcome.get_entities().get("id").toString();
                try{
                    JSONArray ja = new JSONArray(idS);
                    Log.d("JSONARRAY", ja.toString());
                    // Retrieve device id from speech result
                    Integer number = null;
                    String name = null;
                    try{
                        number = ja.getJSONObject(0).getInt("value") - 1; // check if id is given as number, -1 so that number corresponds to array index
                    }catch(JSONException e){
                        name = ja.getJSONObject(0).getString("value"); // if id is not integer, it is considered as name of switch
                    }
                    String id = null;
                    // If number is given, check if it is a valid id
                    if(number!=null){
                        if((number >= 0) && (number < switches_array.length())){
                            id = switches_array.getJSONObject(number).getString("id");
                        }
                        else{
                            Toast.makeText(this.context, "Switch Not Available", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    // If string is given, check if specified string matches device name
                    else if(name!=null){
                        if(name.equalsIgnoreCase("light") || name.equalsIgnoreCase("switch")){ // if id not specified but is singular, use first switch as target device
                            id = switches_array.getJSONObject(0).getString("id");
                        }
                        else if(name.equalsIgnoreCase("lights") || name.equalsIgnoreCase("switches")){ // if plural, turn on/off all switches
                            id = "all";
                        }
                        else{
                            // check if string corresponds to name of device
                            for(int k = 0; k < switches_array.length(); k++){
                                String label = switches_array.getJSONObject(k).getString("label");
                                if(name.equalsIgnoreCase(label)){
                                    Log.d("label", label);
                                    id = switches_array.getJSONObject(k).getString("id");
                                    break;
                                }
                            }
                        }
                    }
                    else{
                        Toast.makeText(this.context, "No Switch Specified", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // If id is null, return
                    if(id == null){
                        Toast.makeText(this.context, "Switch Not Available", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // If all switches should be turned on/off, loop through list of switches and send HTTP request to each
                    if(id.equalsIgnoreCase("all")){
                        try{
                            for(int i = 0; i < switches_array.length(); i++){
                                id = switches_array.getJSONObject(i).getString("id");
                                HashMap<String, String> args = new HashMap<String, String>(); // store necessary information in hashmap to send over to asynctask
                                args.put("url", pref.getString("url", ""));
                                args.put("accessToken", pref.getString("accessToken", ""));
                                args.put("device", "switches");
                                args.put("action", onOff);
                                args.put("id", id);
                                new UpdateDeviceAsyncTask(args).execute(); // http request asynctask
                            }
                        }catch(JSONException e){
                            Log.e("Authorize","Error Parsing Http response "+e.getLocalizedMessage());
                        }
                    }
                    // Send HTTP request to specified device to perform action
                    else{
                        Log.d("id", id);
                        HashMap<String, String> args = new HashMap<String, String>();
                        args.put("url", pref.getString("url", ""));
                        args.put("accessToken", pref.getString("accessToken", ""));
                        args.put("device", "switches");
                        args.put("action", onOff);
                        args.put("id", id);
                        new UpdateDeviceAsyncTask(args).execute(); // http request asynctask
                    }
                }catch(JSONException e){
                    Log.e(getClass().getName(),"JSONException: "+e.getLocalizedMessage());
                }
            }
        }
        /* If intent is to update lock */
        else if(intent.equals("update_lock")){
            // Get list of locks from devices_array
            JSONArray locks_array = null;
            try{
                locks_array = devices_array.getJSONArray("Locks");
            }catch(JSONException e){
                Log.e(getClass().getName(),"JSONException: "+e.getLocalizedMessage());
            }
            if(locks_array == null){
                Toast.makeText(this.context, "No Locks Available", Toast.LENGTH_SHORT).show();
                return;
            }
            // Parse action(on/off) from Wit response
            String lockUnlock = null;
            if(outcome.get_entities().containsKey("lock")){
                lockUnlock = outcome.get_entities().get("lock").toString();
                try{
                    JSONArray lockUnlockArr = new JSONArray(lockUnlock);
                    lockUnlock = lockUnlockArr.getJSONObject(0).getString("value");
                }catch(JSONException e){
                    Log.e(getClass().getName(),"JSONException: "+e.getLocalizedMessage());
                }
            }
            if(lockUnlock == null){ // If no specific action is given, return
                Toast.makeText(this.context, "No Action Specified", Toast.LENGTH_SHORT).show();
                return;
            }
            // If lock id is specified
            if(outcome.get_entities().containsKey("id")){
                String idS = outcome.get_entities().get("id").toString();
                try{
                    JSONArray ja = new JSONArray(idS);

                    Log.d("JSONARRAY- lock", ja.toString());
                    // Retrieve device id from speech result
                    Integer number = null;
                    String name = null;
                    try{
                        number = ja.getJSONObject(0).getInt("value") - 1; // check if id is given as number, -1 so that number corresponds to array index
                    }catch(JSONException e){
                        name = ja.getJSONObject(0).getString("value"); // if id is not integer, it is considered as name of lock
                    }
                    String id = null;
                    // If number is given, check if it is a valid id
                    if(number!=null){
                        if((number >= 0) && (number < locks_array.length())){
                            id = locks_array.getJSONObject(number).getString("id");
                        }
                        else{
                            Toast.makeText(this.context, "Lock Not Available", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    // If string is given, check if specified string matches device name
                    else if(name!=null){
                        if(name.equalsIgnoreCase("locks")){ // if plural, lock/unlock all locks
                            id = "all";
                        }
                        else{
                            // check if string corresponds to name of device
                            for(int k = 0; k < locks_array.length(); k++){
                                String label = locks_array.getJSONObject(k).getString("label");
                                if(name.equalsIgnoreCase(label)){
                                    Log.d("label - lock", label);
                                    id = locks_array.getJSONObject(k).getString("id");
                                    break;
                                }
                            }
                        }
                    }
                    else{
                        Toast.makeText(this.context, "No Lock Specified", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // If id is null, return
                    if(id == null){
                        Toast.makeText(this.context, "Lock Not Available", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // If all switches should be turned on/off, loop through list of switches and send HTTP request to each
                    if(id.equalsIgnoreCase("all")){
                        try{
                            for(int i = 0; i < locks_array.length(); i++){
                                id = locks_array.getJSONObject(i).getString("id");
                                HashMap<String, String> args = new HashMap<String, String>(); // store necessary information in hashmap to send over to asynctask
                                args.put("url", pref.getString("url", ""));
                                args.put("accessToken", pref.getString("accessToken", ""));
                                args.put("device", "locks");
                                args.put("action", lockUnlock);
                                args.put("id", id);
                                new UpdateDeviceAsyncTask(args).execute(); // http request asynctask
                            }
                        }catch(JSONException e){
                            Log.e(getClass().getName(),"JSONException: "+e.getLocalizedMessage());
                        }
                    }
                    // Send HTTP request to specified device to perform action
                    else{
                        Log.d("id", id);
                        HashMap<String, String> args = new HashMap<String, String>();
                        args.put("url", pref.getString("url", ""));
                        args.put("accessToken", pref.getString("accessToken", ""));
                        args.put("device", "locks");
                        args.put("action", lockUnlock);
                        args.put("id", id);
                        new UpdateDeviceAsyncTask(args).execute(); // http request asynctask
                    }
                }catch(JSONException e){
                    Log.e(getClass().getName(),"JSONException: "+e.getLocalizedMessage());
                }
            }
        }
    }

    /**
     * Called when the streaming of the audio data to the Wit API starts.
     * The streaming to the Wit API starts right after calling one of the start methods when
     * detectSpeechStop is equal to Wit.vadConfig.disabled or Wit.vadConfig.detectSpeechStop.
     * If Wit.vad is equal to Wit.vadConfig.full, the streaming to the Wit API starts only when the SDK
     * detected a voice activity.
     */
    @Override
    public void witDidStartListening(){
        Log.i("wit", "Witting...");
    }

    /**
     * Called when Wit stop recording the audio input.
     */
    @Override
    public void witDidStopListening(){
        Toast.makeText(this.context, "Processing...", Toast.LENGTH_LONG).show();
        Log.i("wit", "Processing...");
    }

    /**
     * When using the hands free voice activity detection option (Wit.vadConfig.full), this callback will be called when the microphone started to listen
     * and is waiting to detect voice activity in order to start streaming the data to the Wit API.
     * This function will not be called if the Wit.vad is not equal to Wit.vadConfig.full
     */
    @Override
    public void witActivityDetectorStarted(){
        Toast.makeText(this.context, "Processing...", Toast.LENGTH_LONG).show();
        Log.i("wit", "Listening...");
    }

    /**
     * Using this function allow the developer to generate a custom message id.
     * Example: return "CUSTOM-ID" + UUID.randomUUID.toString();
     * If you want to let the Wit API generate the message id, you can just return null;
     * @return a unique (String) UUID or a null
     */
    @Override
    public String witGenerateMessageId(){
        return null;
    }
}
