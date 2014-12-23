package com.example.hyojulim.myhome;

import android.app.Service;
import android.media.AudioManager;
import android.content.Intent;
import android.speech.SpeechRecognizer;
import android.speech.RecognizerIntent;
import android.speech.RecognitionListener;
import android.content.Context;
import android.os.*;
import android.util.Log;

import java.lang.ref.WeakReference;
import android.widget.Toast;
import android.media.ToneGenerator;

import ai.wit.sdk.Wit;


/**
 * SpeechService class that runs speech recognition as a background service to allow constant listening
 * Constantly restarts SpeechRecognizer
 * Conducts an initial processing of the speech received to check whether the given speech is the keyword or command
 * Default keyword is "my home"
 */
public class SpeechService extends Service{
    static protected AudioManager mAudioManager;
    protected SpeechRecognizer mSpeechRecognizer;
    protected Intent mSpeechRecognizerIntent;
    protected final Messenger mServerMessenger = new Messenger(new IncomingHandler(this));

    protected boolean mIsListening;
    protected volatile boolean mIsCountDownOn;
    static private boolean mIsStreamSolo;

    static final int MSG_RECOGNIZER_START_LISTENING = 1;
    static final int MSG_RECOGNIZER_CANCEL = 2;

    protected final String keyword = "my home";
    protected boolean keyword_spoken = false;
    protected boolean start_beep = false;

    protected final String accessToken = "TXD2OTUYYNAXAFTUCRY5R34HYD4WIANC"; // accessToken for Wit instance
    Wit wit;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            Bundle b = intent.getExtras();
            String devices = b.getString("devices");
            wit = new Wit(accessToken, new WitActivity(this.getApplicationContext(), devices)); // instantiate Wit instance, pass in context and list of devices

            /* Set up for speech recognition */
            mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            mSpeechRecognizer.setRecognitionListener(new SpeechRecognitionListener());
            mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());

            Message message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
            try {
                mServerMessenger.send(message);
            } catch (RemoteException e) {
                Log.e(getClass().getName(), "Remote Exception on onStartCommand");
            }
        }
        return START_STICKY_COMPATIBILITY;
    }


    /* Handler to start/cancel speech recognition service by sending messages */
    protected class IncomingHandler extends Handler
    {
        private WeakReference<SpeechService> mtarget;

        IncomingHandler(SpeechService target)
        {
            mtarget = new WeakReference<SpeechService>(target);
        }

        @Override
        public void handleMessage(Message msg)
        {
            final SpeechService target = mtarget.get();
            switch (msg.what)
            {
                case MSG_RECOGNIZER_START_LISTENING:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                    {
                        /* turn off beep sound */
                        if (!mIsStreamSolo)
                        {
                            mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                            mIsStreamSolo = true;
                        }
                    }
                    if (!target.mIsListening)
                    {
                        /* Restart speech recognition to keep it continuous */
                        target.mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(target);
                        target.mSpeechRecognizer.setRecognitionListener(new SpeechRecognitionListener());
                        target.mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                        target.mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                        target.mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,target.getPackageName());
                        target.mSpeechRecognizer.startListening(target.mSpeechRecognizerIntent);
                        target.mIsListening = true;
                    }
                    break;

                case MSG_RECOGNIZER_CANCEL:
                    if (mIsStreamSolo)
                    {
                        mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                        mIsStreamSolo = false;
                    }
                    target.mSpeechRecognizer.destroy();
                    target.mIsListening = false;
                    break;
            }
        }
    }

    /* Count down timer for Jelly Bean work around
     * In Jelly Bean, if speech is not detected for more than 5 seconds, Speech recognition is cancelled
     */
    protected CountDownTimer mNoSpeechCountDown = new CountDownTimer(5000, 5000)
    {
        @Override
        public void onTick(long millisUntilFinished)
        {
        }

        @Override
        public void onFinish()
        {
            mIsCountDownOn = false;
            Message message = Message.obtain(null, MSG_RECOGNIZER_CANCEL);
            try
            {
                mServerMessenger.send(message);
                message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
                mServerMessenger.send(message);
            }
            catch (RemoteException e)
            {
                Log.e(getClass().getName(), "Remote Exception in CountDownTimer");
            }
        }
    };

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if (mIsCountDownOn)
        {
            mNoSpeechCountDown.cancel();
        }

        if (mIsStreamSolo)
        {
            mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
            mIsStreamSolo = false;
        }

        if (mSpeechRecognizer != null)
        {
            mSpeechRecognizer.destroy();
        }

    }

    protected class SpeechRecognitionListener implements RecognitionListener
    {
        final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_ALARM, 80);

        @Override
        public void onReadyForSpeech(Bundle params)
        {
            // workaround for Jelly_bean where speech recognition service halts automatically when no speech is detected for 5 seconds
            if ((Build.VERSION_CODES.KITKAT > Build.VERSION.SDK_INT) && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN))
            {
                mIsCountDownOn = true;
                mNoSpeechCountDown.start();
            }
            // make a beep sound after keyword is spoken, to alert when to speak command
            if(keyword_spoken && start_beep){
                tg.startTone(ToneGenerator.TONE_PROP_PROMPT);
                start_beep = false;
            }
            Log.d(getClass().getName(), "onReadyForSpeech");
        }

        @Override
        public void onBeginningOfSpeech()
        {
            if (mIsCountDownOn) // no need to count down when input is being processed
            {
                mIsCountDownOn = false;
                mNoSpeechCountDown.cancel();
            }
        }

        @Override
        public void onBufferReceived(byte[] buffer)
        {

        }

        @Override
        public void onEndOfSpeech()
        {
            // make double beep sound on end of command to indicate user
            if(keyword_spoken){
                tg.startTone(ToneGenerator.TONE_PROP_ACK);
            }
            Log.d(getClass().getName(), "onEndOfSpeech called");
        }

        @Override
        public void onEvent(int eventType, Bundle params)
        {

        }

        @Override
        public void onPartialResults(Bundle partialResults)
        {

        }


        /* Process sentence received by Listener
        * First, check if given phrase is the keyword
        * If word was keyword, change boolean to indicate that next phrase will be the command to be given to Wit
        */
        @Override
        public void onResults(Bundle results)
        {
            java.util.ArrayList data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (data.size() > 0) {
                for (int i = 0; i < data.size(); i++){
                    String word = (String)data.get(i);
                    if(keyword_spoken){
                        wit.captureTextIntent(word); // if keyword was spoken, send word to Wit
                        keyword_spoken = false;
                        break;
                    }
                    else {
                        if (word.matches(keyword)) {
                            keyword_spoken = true;
                            start_beep = true;
                            Toast.makeText(getApplicationContext(), "My Home!", Toast.LENGTH_SHORT).show(); // show Toast to indicate that keyword was received
                            break;
                        }
                    }
                }

                // Restart speech recognizer to keep the listening constant
                mIsListening = false;
                mIsCountDownOn = false;
                tg.release();
                Message message = Message.obtain(null, MSG_RECOGNIZER_CANCEL);
                try
                {
                    mServerMessenger.send(message);
                    message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
                    mServerMessenger.send(message);
                }
                catch (RemoteException e)
                {
                    Log.e(getClass().getName(), "Remote exception");
                }
            }
        }

        @Override
        public void onError(int error){
            if(error == 6){
                Log.e(getClass().getName(), "Error: Speech Timeout ");
            }
            else{
                Log.e(getClass().getName(), "onError called (" + error + ")");
            }
            if (mIsCountDownOn)
            {
                mIsCountDownOn = false;
                mNoSpeechCountDown.cancel();
            }
            // make double beep sound on end of session to indicate user
            if(keyword_spoken){
                tg.startTone(ToneGenerator.TONE_PROP_ACK);
                keyword_spoken = false;
            }

            if(error == 2){
                Toast.makeText(getApplicationContext(), "Network Error: Check Internet Connection ", Toast.LENGTH_SHORT).show();
            }
            else if(error == 4){
                Toast.makeText(getApplicationContext(), "Google Server Error", Toast.LENGTH_SHORT).show();
            }
            else if((error != 6) && (error != 7)){
                Toast.makeText(getApplicationContext(), "Speech Error: " + error, Toast.LENGTH_SHORT).show();
            }

            // Restart speech recognizer to keep the listening constant
            tg.release();
            mIsListening = false;
            Message message = Message.obtain(null, MSG_RECOGNIZER_CANCEL);
            try
            {
                mServerMessenger.send(message);
                message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
                mServerMessenger.send(message);
            }
            catch (RemoteException e)
            {
                Log.e(getClass().getName(), "Remote exception");
            }
        }

        @Override
        public void onRmsChanged(float rmsB)
        {
        }
    }
}

