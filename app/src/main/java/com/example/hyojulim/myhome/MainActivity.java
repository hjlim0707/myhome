package com.example.hyojulim.myhome;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.content.SharedPreferences;
import android.app.Fragment;
import android.app.FragmentTransaction;

/*
* MainActivity Class
* Stores Key information for SmartThings Access in stored Preferences
* Displays LoginFragment as first screen
* */
public class MainActivity extends ActionBarActivity {
    private SharedPreferences pref;
    private static final String API_KEY = "330836f8-dfb0-4474-8c41-e02030778511"; // API_KEY for SmartThings "Speech" SmartApp
    private static final String SECRET_KEY = "6a0ef411-52a2-4325-a319-c15c6ddc5305"; // SECRET_Key for SmartThings "Speech" SmartApp

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // set up shared preferences
        pref = getSharedPreferences("st_info", 0);
        SharedPreferences.Editor edit = pref.edit();
        edit.putString("API_KEY", API_KEY);
        edit.putString("SECRET_KEY", SECRET_KEY);
        edit.commit();
        // set up login fragment
        Fragment login = new LoginFragment();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.frame, login);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.addToBackStack(null);
        ft.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
