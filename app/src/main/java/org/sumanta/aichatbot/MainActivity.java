package org.sumanta.aichatbot;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import org.apache.commons.lang3.StringUtils;
import org.sumantapakira.aiml.Category;
import org.sumantapakira.aiml.History;
import org.sumantapakira.aiml.Response;
import org.sumantapakira.aiml.dto.FutureDTO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private final int REQ_CODE = 100;
    TextView textView;
    TextView robotView;
    org.sumantapakira.aiml.Chat chatBot;
    private TextToSpeech mTTS;
    private EditText mEditText;
    private SeekBar mSeekBarPitch;
    private SeekBar mSeekBarSpeed;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener fireAuthListener;
    private String idToken;
    private static String FIREBASE_UL = "https://chatbot-12d2b-default-rtdb.firebaseio.com/.json";
    ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermission(42, Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR);
        checkPermission(43, Manifest.permission.SET_ALARM, Manifest.permission.SET_ALARM);
        setContentView(R.layout.activity_main);

        AlarmManager objAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Calendar objCalendar = Calendar.getInstance();
        objCalendar.set(Calendar.YEAR, 2021);
        //objCalendar.set(Calendar.YEAR, objCalendar.get(Calendar.YEAR));
        objCalendar.set(Calendar.MONTH, 7);
        objCalendar.set(Calendar.DAY_OF_MONTH, 25);
        objCalendar.set(Calendar.HOUR_OF_DAY, 15);
        objCalendar.set(Calendar.MINUTE, 20);
        objCalendar.set(Calendar.SECOND, 0);
        objCalendar.set(Calendar.MILLISECOND, 0);
        objCalendar.set(Calendar.AM_PM, Calendar.PM);

        Intent alamShowIntent = new Intent(this, MainActivity.class);
        PendingIntent alarmPendingIntent = PendingIntent.getActivity(this, 0, alamShowIntent, 0);

        objAlarmManager.set(AlarmManager.RTC_WAKEUP, objCalendar.getTimeInMillis(), alarmPendingIntent);


        firebaseAuth = FirebaseAuth.getInstance();
        final FirebaseUser user = firebaseAuth.getCurrentUser();
        try {
            new JsonTask().execute(FIREBASE_UL);
        } catch (Exception e) {
            e.printStackTrace();
        }
        final RequestQueue queue = Volley.newRequestQueue(this);
        fireAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (user == null) {
                    MainActivity.this.startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    MainActivity.this.finish();
                }
            }
        };

        textView = findViewById(R.id.displaytext);
        robotView = findViewById(R.id.robottext);

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
                    }
                } else {
                    Log.e("TTS", "Initialization failed");
                }
            }
        });
        mEditText = findViewById(R.id.edit_text);
        mSeekBarPitch = findViewById(R.id.seek_bar_pitch);
        mSeekBarSpeed = findViewById(R.id.seek_bar_speed);
    }

    private void speak(String personalizedText) {
        if (personalizedText.startsWith("http")) {
            return;
        }
        float pitch = (float) mSeekBarPitch.getProgress() / 50;
        if (pitch < 0.1) pitch = 0.1f;
        float speed = (float) mSeekBarSpeed.getProgress() / 50;
        if (speed < 0.1) speed = 0.1f;

        mTTS.setPitch(pitch);
        mTTS.setSpeechRate(speed);
        robotView.setText("Yantra: " + personalizedText);

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
                    textView.setText("Me: " + result.get(0).toString());
                    ExecutorService service = null;
                    try {
                        service = Executors.newCachedThreadPool();
                        Response response = chatBot.multisentenceRespond(result.get(0).toString(), service);
                        String yantraResponse = StringUtils.EMPTY;

                        if (response != null && response.getAsyncResult() != null) {
                            while (!response.getAsyncResult().isDone()) {
                                yantraResponse = response.getResponse();
                                speak(response.getResponse());
                                Thread.sleep(3000);
                            }
                            if (response.getAsyncResult().isDone()) {
                                final ObjectNode node = new ObjectMapper().readValue(response.getAsyncResult().get(), ObjectNode.class);
                                String asyncVoice = response.getAsyncVoice() + " " + node.get(response.getResultkey()).asText();
                                yantraResponse = asyncVoice;
                                Thread.sleep(3000);
                                History<String> history = new History<String>();
                                history.add(node.get(response.getResultkey()).asText());
                                chatBot.thatHistory.add(history);
                                speak(asyncVoice);
                            }
                        } else {
                            if (response.getBrowserResponse() != null) {
                                speak(response.getBrowserResponse());
                                if (response.getResponse().startsWith("http://192")) {
                                    FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser();
                                    mUser.getIdToken(true)
                                            .addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                                                public void onComplete(@NonNull Task<GetTokenResult> task) {
                                                    if (task.isSuccessful()) {
                                                        StringBuilder url = new StringBuilder(response.getResponse());
                                                        String idToken = task.getResult().getToken();
                                                        url.append("?");
                                                        url.append("token");
                                                        url.append("=");
                                                        url.append(idToken);
                                                        final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url.toString()));
                                                        startActivity(browserIntent);
                                                    } else {
                                                        // Handle error -> task.getException();
                                                    }
                                                }
                                            });
                                } else {
                                    final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(response.getResponse()));
                                    startActivity(browserIntent);
                                }
                            }
                            speak(response.getResponse());
                            if (StringUtils.isNotBlank(response.getCalendarEventTitle())) {
                                Thread.sleep(1000);
                                ContentResolver contentResolver = getContentResolver();
                                String androidresponse = Utils.getCalendars(contentResolver, response.getCalendarEventTitle());
                                History<String> history = new History<String>();
                                history.add(androidresponse);
                                chatBot.thatHistory.add(history);
                                speak(androidresponse);
                            }
                        }
                        if (response.getDependson() != null) {
                            while (!response.getDependson().isDone()) {
                                Thread.sleep(3000);
                            }
                            FutureDTO futureDTO = response.getDependson().get();
                            final ObjectNode node = new ObjectMapper().readValue(response.getDependson().get().getResponse(), ObjectNode.class);
                            yantraResponse = futureDTO.getAsyncVoice() + " " + node.get(futureDTO.getResultKey()).asText();
                            speak(yantraResponse);

                            History<String> history = new History<String>();
                            history.add(node.get(futureDTO.getResultKey()).asText());
                            chatBot.thatHistory.add(history);
                        }
                    } catch (Exception ex) {
                        speak("Something went wrong, Please ask me another question!");
                        ex.printStackTrace();
                    } finally {
                        service.shutdown();
                    }


                }
                break;
            }
        }
    }


    private class JsonTask extends AsyncTask<String, String, String> {

        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(MainActivity.this);
            pd.setMessage("Please wait");
            pd.setCancelable(false);
            pd.show();
        }

        protected String doInBackground(String... params) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));
                StringBuffer buffer = new StringBuffer();
                String line = "";
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                    Log.d("Response: ", "> " + line);

                }
                return buffer.toString();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (pd.isShowing()) {
                pd.dismiss();
            }
            try {
                ArrayList<Category> listCategories = new ObjectMapper().readValue(result, new TypeReference<List<Category>>() {
                });
                org.sumantapakira.aiml.Bot bot = new org.sumantapakira.aiml.Bot("Yantra", org.sumantapakira.aiml.MagicStrings.root_path, "chat", listCategories);
                chatBot = new org.sumantapakira.aiml.Chat(bot);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void checkPermission(int callbackId, String... permissionsId) {
        boolean permissions = true;
        for (String p : permissionsId) {
            permissions = permissions && ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED;
        }
        if (!permissions)
            ActivityCompat.requestPermissions(this, permissionsId, callbackId);
    }

}
