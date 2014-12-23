package com.example.hyojulim.myhome;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.net.ParseException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.view.Window;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Fragment that displays login screen and processes authorization
 * Handles oAuth process to access SmartThings account
 */
public class LoginFragment extends Fragment {
    private TextView login;
    private WebView webView;
    private Dialog auth_dialog;
    private SharedPreferences pref;
    private Context context;
    private ProgressDialog progress;

    @Override
    public View onCreateView(LayoutInflater inflater,ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.login_fragment, container, false);
        login = (TextView)view.findViewById(R.id.login);
        pref = getActivity().getSharedPreferences("st_info", 0);
        context = getActivity().getApplicationContext();
        login.setOnClickListener(new LoginProcess());
        return view;
    }

    /* Attach click listener on login button */
    private class LoginProcess implements OnClickListener {
        private static final String REDIRECT_URI = "https://smartthings.com";
        private static final String AUTH_URL = "https://graph.api.smartthings.com/oauth/authorize";
        private static final String TOKEN_URL = "https://graph.api.smartthings.com/oauth/token";
        private static final String RESPONSE_TYPE_VALUE = "code";

        @Override
        public void onClick(View v) {
            auth_dialog = new Dialog(getActivity()); // use dialog to display SmartThings webpage
            auth_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            auth_dialog.setContentView(R.layout.auth_fragment);
            webView = (WebView)auth_dialog.findViewById(R.id.st_webView);
            webView.getSettings().setJavaScriptEnabled(true);
            String authUrl = AUTH_URL + "?response_type=" + RESPONSE_TYPE_VALUE + "&client_id="
                    + pref.getString("API_KEY", "") + "&redirect_uri=" + REDIRECT_URI + "&scope=app";
            webView.loadUrl(authUrl);
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String authorizationUrl) {
                    if (authorizationUrl.startsWith(REDIRECT_URI)) {
                        Uri uri = Uri.parse(authorizationUrl);
                        String authorizationToken = uri.getQueryParameter(RESPONSE_TYPE_VALUE);
                        if (authorizationToken == null) {
                            auth_dialog.dismiss();
                            Toast.makeText(context, "The user doesn't allow authorization", Toast.LENGTH_SHORT);
                            return true;
                        }
                        Log.i("Authorize", "Auth token received: " + authorizationToken);
                        String accessTokenUrl = TOKEN_URL + "?grant_type=authorization_code&client_id=" + pref.getString("API_KEY", "")
                                + "&client_secret=" + pref.getString("SECRET_KEY", "")
                                + "&redirect_uri=" + REDIRECT_URI + "&code=" + authorizationToken + "&scope=app";
                        auth_dialog.dismiss();
                        new getAccessAsyncTask().execute(accessTokenUrl);
                    } else {
                        webView.loadUrl(authorizationUrl);
                    }
                    return true;
                }
            });
            auth_dialog.show();
            auth_dialog.setCancelable(true);
        }
    }

    /* AsyncTask that runs to retrieve SmartThings accessToken for OAuth */
    private class getAccessAsyncTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progress = new ProgressDialog(getActivity());
            progress.setMessage("Accessing SmartThings ...");
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.setIndeterminate(true);
            progress.show();
        }

        @Override
        protected String doInBackground(String... urls) {
            if(urls.length>0){
                String url = urls[0];
                HttpClient httpClient = new DefaultHttpClient();
                HttpPost httppost = new HttpPost(url);
                try{
                    HttpResponse response = httpClient.execute(httppost);
                    if(response!=null){
                        if(response.getStatusLine().getStatusCode()==200){
                            String result = EntityUtils.toString(response.getEntity());
                            JSONObject resultJson = new JSONObject(result);
                            String accessToken = resultJson.has("access_token") ? resultJson.getString("access_token") : null;
                            SharedPreferences.Editor editor = pref.edit();
                            editor.putString("accessToken", accessToken); //Store access token in shared preferences
                            editor.commit();
                            return accessToken;
                        }
                    }
                }catch(IOException e){
                    Toast.makeText(context, "IOException", Toast.LENGTH_SHORT).show();
                    Log.e("Authorize","Error Http response "+e.getLocalizedMessage());
                }
                catch (ParseException e) {
                    Toast.makeText(context, "ParseException", Toast.LENGTH_SHORT).show();
                    Log.e("Authorize","Error Parsing Http response "+e.getLocalizedMessage());
                } catch (JSONException e) {
                    Toast.makeText(context, "JSONException", Toast.LENGTH_SHORT).show();
                    Log.e("Authorize","Error Parsing Http response "+e.getLocalizedMessage());
                }
            }
            return null;
        }

        /* When accessToken is retrieved, move on to get Endpoint URLs */
        @Override
        protected void onPostExecute(String accessToken){
            if(accessToken!=null){
                new getEndpointAsyncTask().execute();
            }
        }
    }

    /* AsyncTask that retrieves the Endpoint URLs to specified SmartApp
     * Retrieved URL will be used to send HTTP requests to SmartThings
     */
    private class getEndpointAsyncTask extends AsyncTask<String, Void, String>{
        @Override
        protected String doInBackground(String... args) {
            String endpointUrl = "";
            String url = "";
            endpointUrl = "https://graph.api.smartthings.com/api/smartapps/endpoints/" + pref.getString("API_KEY", "") + "?access_token=" + pref.getString("accessToken", "");
            HttpClient client = new DefaultHttpClient();
            HttpGet get = new HttpGet(endpointUrl);
            try{
                HttpResponse response = client.execute(get);
                if(response!=null){
                    String result = EntityUtils.toString(response.getEntity());
                    JSONArray ja = new JSONArray(result);
                    url = ja.getJSONObject(0).getString("url");
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("url", url);
                    editor.commit();
                    return url;
                }
            }catch(IOException e){
                Toast.makeText(context, "IOException", Toast.LENGTH_SHORT).show();
                Log.e("Authorize","Error Http response "+e.getLocalizedMessage());
            }
            catch (ParseException e) {
                Toast.makeText(context, "ParseException", Toast.LENGTH_SHORT).show();
                Log.e("Authorize","Error Parsing Http response "+e.getLocalizedMessage());
            } catch (JSONException e) {
                Toast.makeText(context, "JSONException", Toast.LENGTH_SHORT).show();
                Log.e("Authorize","Error Parsing Http response "+e.getLocalizedMessage());
            }
            return null;
        }

        /* When all steps for authorization is done, display SpeechFragment */
        @Override
        protected void onPostExecute(String url){
            progress.hide();
            Fragment speech = new SpeechFragment();
            FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction();
            ft.replace(R.id.frame, speech);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.addToBackStack(null);
            ft.commit();
        }
    }
}
