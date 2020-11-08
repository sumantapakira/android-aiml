package com.example.testing;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import org.alicebot.ab.AIMLProcessor;
import org.alicebot.ab.Bot;
import org.alicebot.ab.Chat;
import org.alicebot.ab.Graphmaster;
import org.alicebot.ab.MagicBooleans;
import org.alicebot.ab.MagicStrings;
import org.alicebot.ab.PCAIMLProcessorExtension;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.StringTokenizer;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private final int REQ_CODE = 100;
    TextView textView;
    Chat chat;
    private TextToSpeech mTTS;
    private EditText mEditText;
    private SeekBar mSeekBarPitch;
    private SeekBar mSeekBarSpeed;
    private Button mButtonSpeak;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener fireAuthListener;
    private String idToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();

        //get current user
        final FirebaseUser user = firebaseAuth.getCurrentUser();
        Log.d(TAG, " user : " + user.getEmail());
        Log.d(TAG, " user : " + user.getDisplayName());

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                File yantraFile = getExternalFilesDir("/aem/bots/yantra");
                AssetManager assets = getApplicationContext().getAssets();

                try {
                    System.out.println("yantraFile.exists() : "+yantraFile.exists());
                    if (yantraFile.exists()) {
                       // boolean isDeleted = yantraFile.delete();
                       // System.out.println("isDeleted : "+isDeleted);
                       // yantraFile = getExternalFilesDir("/aem/bots/yantra");
                        for (String dir : assets.list("yantra")) {
                            String path1 = "yantra/" + dir;
                            for (String file : assets.list(path1)) {

                                String path2 = yantraFile.getPath() + "/" + dir + "/" + file;

                                File f = new File(yantraFile.getPath() + "/" + dir);
                                f.mkdirs();
                                if (f.exists()) {
                                    File f2 = new File(f.getPath() + "/" + file);
                                    f2.createNewFile();
                                    InputStream in = null;
                                    OutputStream out = null;
                                    in = assets.open("yantra/" + dir + "/" + file);
                                    out = new FileOutputStream(f2.getPath());
                                    //copy file from assets to the mobile's SD card or any secondary memory
                                    Utils.copyFile(in, out);
                                    in.close();
                                    in = null;
                                    out.flush();
                                    out.close();
                                    out = null;
                                }

                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                MagicStrings.root_path = getExternalFilesDir("/aem").getPath();
                System.out.println("Working Directory = " + MagicStrings.root_path);
                AIMLProcessor.extension = new PCAIMLProcessorExtension();

                Bot bot = new Bot("yantra", MagicStrings.root_path, "chat");
                chat = new Chat(bot);
            }
        });


        final RequestQueue queue = Volley.newRequestQueue(this);
        user.getIdToken(true)
                .addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                    public void onComplete(@NonNull Task<GetTokenResult> task) {
                        if (task.isSuccessful()) {
                             idToken = task.getResult().getToken();
                            Utils.callApi(0, "http://192.168.178.21:4202/bin/servlet/fetchcontent", queue, idToken);

                        } else {
                            // Handle error -> task.getException();
                        }
                    }
                });


        fireAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                // FirebaseUser user1 = firebaseAuth.getCurrentUser();

                if (user == null) {
                    //user not login
                    MainActivity.this.startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    MainActivity.this.finish();
                }
            }
        };


        mButtonSpeak = findViewById(R.id.button_speak);

        textView = findViewById(R.id.displaytext);

        ImageView speak = findViewById(R.id.speak);
        speak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Need to speak");
                try {
                    startActivityForResult(intent, REQ_CODE);
                } catch (ActivityNotFoundException a) {
                    Toast.makeText(getApplicationContext(),
                            "Sorry your device not supported",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        final String personalizedText = "Welcome " + user.getEmail() + " !, My name is Yantra. What can I do for you?";

        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = mTTS.setLanguage(Locale.ENGLISH);

                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported");
                    } else {
                        speak(personalizedText);
                        mButtonSpeak.setEnabled(true);
                    }
                } else {
                    Log.e("TTS", "Initialization failed");
                }
            }
        });


        mEditText = findViewById(R.id.edit_text);
        mSeekBarPitch = findViewById(R.id.seek_bar_pitch);
        mSeekBarSpeed = findViewById(R.id.seek_bar_speed);

        mButtonSpeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, " onClick : ");
                speak(personalizedText);
            }
        });
    }

    private void speak(String personalizedText) {
        float pitch = (float) mSeekBarPitch.getProgress() / 50;
        if (pitch < 0.1) pitch = 0.1f;
        float speed = (float) mSeekBarSpeed.getProgress() / 50;
        if (speed < 0.1) speed = 0.1f;

        mTTS.setPitch(pitch);
        mTTS.setSpeechRate(speed);

        mTTS.speak(personalizedText, TextToSpeech.QUEUE_FLUSH, null);
    }

    @Override
    protected void onDestroy() {
        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
        }

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    Log.d(TAG, result.get(0).toString());
                    textView.setText(result.get(0).toString());
                    String replyText = null;

                    MagicBooleans.trace_mode = false;
                    System.out.println("trace mode = " + MagicBooleans.trace_mode);
                    Graphmaster.enableShortCuts = true;
                    String filteredStopWords = Utils.removeStopWords(result.get(0).toString());
                    String response = chat.multisentenceRespond(filteredStopWords);

                    System.out.println("Robot Response : " + response);
                    replyText = response;

                    if (response.contains("hello")) {
                        replyText = response;
                        // callFirebaseAPI();
                    } else if (result.get(0).toString().contains("reject")) {
                        replyText = "Seems you are not in a good mood today";
                        // call AEM servlet
                        // scheduleWorkItem(String userName, String itemId, String time, String decision);
                    } else if (result.get(0).toString().contains("open")) {

                        String authorization = "admin" + ":" + "admin";
                        String authorizationBase64 = Base64.encodeToString(authorization.getBytes(), Base64.NO_WRAP);

                        /*Bundle bundle = new Bundle();
                        bundle.putString("Authorization", "Basic " + authorizationBase64);
                        browserIntent.putExtra(Browser.EXTRA_HEADERS, bundle);
                        startActivity(browserIntent);*/

                        final Bundle bundle = new Bundle();

                        FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser();
                        mUser.getIdToken(true)
                                .addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                                    public void onComplete(@NonNull Task<GetTokenResult> task) {
                                        if (task.isSuccessful()) {
                                            StringBuilder url = new StringBuilder("http://192.168.178.21:4202/content/we-retail/language-masters/en.html?wcmmode=disabled");


                                            String idToken = task.getResult().getToken();
                                            url.append("&");
                                            url.append("token");
                                            url.append("=");
                                            url.append(idToken);
                                            final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url.toString()));

                                           /* Log.d(TAG, " idToken : " +idToken );
                                            bundle.putString("Authorization", "Bearer " + idToken);
                                            browserIntent.putExtra(Browser.EXTRA_HEADERS, bundle);*/
                                            startActivity(browserIntent);

                                            // ...
                                        } else {
                                            // Handle error -> task.getException();
                                        }
                                    }
                                });


                    } else if (response.startsWith("Thanks! Your page named")) {
                        StringTokenizer t = new StringTokenizer(response);
                        String word ="";
                        String title="";
                        String jcrPath="";
                        String templateType="";
                        String prev ="";
                        while(t.hasMoreTokens())
                        {
                            word = t.nextToken();

                            if(prev.equals("named")){
                                title = word;
                            }
                            else if(prev.equals("type")){
                                templateType = word;
                            } else if(prev.equals("location")){
                                jcrPath = word;
                            }

                            prev = word;
                        }

                    } else if (result.get(0).toString().contains("later") || result.get(0).toString().contains("schedule")) {
                        replyText = "What time would like to schedule?";
                    } else {
                        replyText = response;
                    }

                    float pitch = (float) mSeekBarPitch.getProgress() / 50;
                    if (pitch < 0.1) pitch = 0.1f;
                    float speed = (float) mSeekBarSpeed.getProgress() / 50;
                    if (speed < 0.1) speed = 0.1f;

                    mTTS.setPitch(pitch);
                    mTTS.setSpeechRate(speed);

                    mTTS.speak(replyText, TextToSpeech.QUEUE_FLUSH, null);
                }
                break;
            }
        }
    }

    private void callFirebaseAPI() {
        Log.d(TAG, "*******************Calling api***************************");
       /* RequestParams params = new RequestParams();
        AsyncHttpClient client = new AsyncHttpClient();
        client.get("https://us-central1-aem-voice.cloudfunctions.net/processRequest", params, new JsonHttpResponseHandler());*/
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://www.google.com";

// Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new com.android.volley.Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        Log.d(TAG, "Response is: " + response.substring(0, 500));
                    }
                }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "Did not work");
            }
        });

// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }


}
