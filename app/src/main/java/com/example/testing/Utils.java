package com.example.testing;

import android.os.Environment;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {

    private static final String TAG = "Utils";
    private static List<String> STOPWORDS = Arrays.asList("I", "of", "he","she", "and", "the", "can", "you", "please", "will","be", "to","in","after","then","before","above","a","an","do","does", "is");

    public static boolean isSDCARDAvailable(){
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)? true :false;
    }

    public static File createFolder(String path){
        File botsfolderPath = null;
        boolean isSDCARDAvailable = isSDCARDAvailable();
        Log.d(TAG, "isSDCARDAvailable ######################: " + isSDCARDAvailable);
        Log.d(TAG, "getExternalStorageDirectory ######################: " + Environment.getExternalStorageDirectory().toString());
        if(isSDCARDAvailable){
            final String folderPath = Environment.getExternalStorageDirectory() + path;
            File folder = new File(folderPath);
            if (!folder.exists()) {
                Log.d(TAG, "creating ######################: " );
                File wallpaperDirectory = new File(folderPath);
                boolean result = wallpaperDirectory.mkdirs();
                Log.d(TAG, "created  ######################: " +result + " , Path is : " +wallpaperDirectory.getPath());
                botsfolderPath = wallpaperDirectory;
            }else{
                botsfolderPath = new File(folderPath);
            }

        }
        return botsfolderPath;
    }

    public static void callApi(int method, String url, RequestQueue queue, final String token){
        JsonObjectRequest stringRequest = new JsonObjectRequest(0, url, new JSONObject(),
                new com.android.volley.Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Display the first 500 characters of the response string.
                        Log.d(TAG, "Response is: " + response.toString());
                    }
                }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "Did not work" );
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };
        queue.add(stringRequest);
    }

    //copying the file
    public static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    public static String removeStopWords(String sentence){
        ArrayList<String> allWords =
                Stream.of(sentence.toLowerCase().split(" "))
                        .collect(Collectors.toCollection(ArrayList<String>::new));
        allWords.removeAll(STOPWORDS);

       return allWords.stream().collect(Collectors.joining(" "));
    }
}
