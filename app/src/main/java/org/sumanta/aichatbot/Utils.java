package org.sumanta.aichatbot;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.CalendarContract;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {

    private static final String TAG = "Utils";
    private static List<String> STOPWORDS = Arrays.asList("I", "of", "he","she", "and", "the", "can", "you", "please", "will","be", "to","in","after","then","before","above","a","an","do","does", "is");

    public static boolean isSDCARDAvailable(){
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)? true :false;
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

    public static String removeStopWords(String sentence){
        ArrayList<String> allWords =
                Stream.of(sentence.toLowerCase().split(" "))
                        .collect(Collectors.toCollection(ArrayList<String>::new));
        allWords.removeAll(STOPWORDS);

       return allWords.stream().collect(Collectors.joining(" "));
    }

    public static String getCalendars(ContentResolver contentResolver, String eventTitle){
   String[] FIELDS = {
                CalendarContract.Events.CALENDAR_ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND
        };
         final Uri CALENDAR_URI = Uri.parse("content://com.android.calendar/calendars");
        Set<String> calendars = new HashSet<String>();
        Calendar startTime = Calendar.getInstance();

        startTime.set(Calendar.HOUR_OF_DAY,0);
        startTime.set(Calendar.MINUTE,0);
        startTime.set(Calendar.SECOND, 0);

        Calendar endTime= Calendar.getInstance();
        endTime.add(Calendar.DATE, 30);

        String selection = "(( " + CalendarContract.Events.DTSTART + " >= " + startTime.getTimeInMillis() + " )" +
                " AND ( " + CalendarContract.Events.TITLE + " == '" + eventTitle + "' )" +
                " AND ( " + CalendarContract.Events.DTEND + " <= " + endTime.getTimeInMillis() + " ) AND ( deleted != 1 ))";

        Cursor cursor = contentResolver.query(CalendarContract.Events.CONTENT_URI, FIELDS, selection, null, CalendarContract.Events.DTSTART + " ASC");

        List<String> events = new ArrayList<>();
        String eventsDate = StringUtils.EMPTY;
        if (cursor!=null && cursor.getCount()>0 && cursor.moveToFirst()) {
            do{
                String str1 = cursor.getString(1);
                Long appointmentDate = cursor.getLong(3);
                eventsDate = StringUtils.isAllEmpty(eventsDate) ?  getDate(appointmentDate) : eventsDate + " and " +  getDate(appointmentDate);
                events.add(str1);
            } while ( cursor.moveToNext());
        }
        return eventsDate;
    }

    public static String getDate(long milliSeconds) {
        SimpleDateFormat formatter = new SimpleDateFormat(
                "EEE, d MMM yyyy HH:mm:ss");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }
}
