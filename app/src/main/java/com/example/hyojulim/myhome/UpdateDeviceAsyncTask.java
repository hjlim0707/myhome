package com.example.hyojulim.myhome;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.util.HashMap;

/**
 * Class that extends AsyncTask and sends update device http requests to SmartApp
 * Receives specific device and action information as parameters from WitActivity Class
 * doInBackground method formats url and sends http request
 */
public class UpdateDeviceAsyncTask extends AsyncTask<Void, Void, Boolean> {
    private HashMap<String, String> map;

    public UpdateDeviceAsyncTask(HashMap<String,String> arg){
        map = arg;
    }
    @Override
    protected Boolean doInBackground(Void... args) {
        try{
            String switchUrl = "https://graph.api.smartthings.com" + map.get("url") + "/" + map.get("device") + "/" + map.get("id") + "/" + map.get("action") + "?access_token=" + map.get("accessToken");
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet get = new HttpGet(switchUrl);
            HttpResponse response = httpClient.execute(get);
            if(response!=null) {
                if (response.getStatusLine().getStatusCode() == 200) {
                    return true;
                }
            }
            return false;
        }catch(IOException e){
            Log.e(getClass().getName(),"IOException: "+e.getLocalizedMessage());
        }
        return false;
    }
    @Override
    protected void onPostExecute(Boolean result) {
        if(result == false){
            Log.e(getClass().getName(),"Error with UpdateDevice HTTP request");
        }
    }
}
