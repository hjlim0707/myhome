package com.example.hyojulim.myhome;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.net.ParseException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.content.SharedPreferences;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.CompoundButton;
import android.content.Intent;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;


/**
 * SpeechFragment that displays switch to toggle listening and list of devices
 * When switch is on, SpeechService, which constantly runs speech recognition, is started
 */
public class SpeechFragment extends Fragment {
    private Switch listen_switch;
    private SharedPreferences pref;
    private View view;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        view = inflater.inflate(R.layout.speech_fragment, container, false);
        pref = getActivity().getSharedPreferences("st_info", 0);
        listen_switch = (Switch)view.findViewById(R.id.listenSwitch);
        new getDevicesAsyncTask().execute(); // Load list of authorized devices
        return view;
    }

    /*
    * Load list of devices by making a get request to SmartThings app
    * Use the device information to display on Fragment and pass to SpeechService
    */
    private class getDevicesAsyncTask extends AsyncTask<String, Void, String> {
        private ProgressDialog progress;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progress = new ProgressDialog(getActivity());
            progress.setMessage("Fetching Data ...");
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.setIndeterminate(true);
            progress.show();
        }

        @Override
        protected String doInBackground(String... urls) {
                String accessToken = pref.getString("accessToken", "");
                String url = pref.getString("url", "");
                String switches_url = "https://graph.api.smartthings.com"+url+"/devices";
                HttpClient httpclient = new DefaultHttpClient();
                HttpGet httpget = new HttpGet(switches_url);
                httpget.addHeader("Authorization", "bearer " + accessToken);
                try{
                    HttpResponse httpresponse = httpclient.execute(httpget);
                    if(httpresponse!=null){
                        String result = EntityUtils.toString(httpresponse.getEntity());
                        return result;
                    }
                }catch(IOException e){
                    Log.e(getClass().getName(),"IOException: "+e.getLocalizedMessage());
                }
                catch (ParseException e) {
                    Log.e(getClass().getName(),"ParseException: "+e.getLocalizedMessage());
                }
            return null;
        }

        @Override
        protected void onPostExecute(String result){
            /* Set up switch to start/stop constant listening service (SpeechService class) */
            final Intent intent = new Intent();
            intent.setClass(getActivity(), SpeechService.class);
            intent.putExtra("devices", result); // Pass list of devices as parameter
            listen_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        getActivity().startService(intent);// Listening is enabled
                    } else {
                        getActivity().stopService(intent);// Listening is disabled
                    }
                }
            });
            /* Build and display list of devices on SpeechFragment */
            try{
                JSONObject devices_array = new JSONObject(result);
                Log.i("devices_array", devices_array.toString());
                if(devices_array!=null){
                    String list= "";
                    Iterator deviceIterator = devices_array.keys();
                    while(deviceIterator.hasNext()){
                        String key = (String) deviceIterator.next();
                        list += key + "\n";
                        JSONArray sdevices_array = devices_array.getJSONArray(key);
                        for(int i = 0; i < sdevices_array.length(); i++){
                            JSONObject jsonobject = sdevices_array.getJSONObject(i);
                            list += (i+1) + ": " + jsonobject.getString("label") + "\n";
                        }
                        list+= "\n";
                    }
                    ((TextView) view.findViewById(R.id.deviceList)).setText(list);
                    progress.dismiss();
                }
            }
            catch (JSONException e) {
                Log.e(getClass().getName(),"JSONException: "+e.getLocalizedMessage());
            }
        }

    }
}
