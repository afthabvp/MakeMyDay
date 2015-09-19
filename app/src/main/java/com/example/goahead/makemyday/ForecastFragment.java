package com.example.goahead.makemyday;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.example.goahead.makemyday.data.WeatherContract;
import com.example.goahead.makemyday.data.WeatherContract.LocationEntry;
import com.example.goahead.makemyday.data.WeatherContract.WeatherEntry;

/**
 * Created by spartan300 on 15/9/15.
 */
public class ForecastFragment extends Fragment {

    private ArrayAdapter<String> mForecastAdapter;
    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

    }
    @Override
    public void onCreateOptionsMenu(Menu menu,MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            updateWeather();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void updateWeather() {
        FetchWeatherTask fetchWeatherTask=new FetchWeatherTask(getActivity(), mForecastAdapter);
       // SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        //String location = prefs.getString(getString(R.string.pref_locaton_key),getString(R.string.pref_location_default));
        String location = Utility.getPreferredLocation(getActivity());;
        String units = Utility.getPreferredUnits(getActivity());

        //weatherTask.execute(location, units);
        fetchWeatherTask.execute(location);

    }

    @Override
    public void onStart(){
        super.onStart();
        updateWeather();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

         /*  String[] forecastArray = {
                    "Today - Sunny - 88/63",
                    "Tomarrow - Foggy - 70/40",
                    "weds - Cloudy - 72/63",
                    "thurs - Asteroids - 75/65",
                    "Fri - Heavy Rain - 65/56",
                    "Sat - HELP TRAPPED IN WEATHERSTATION - 60/51",
                    "Sun - Sunny - 80/68"

            };
            List<String> weekForecast = new ArrayList<String>(
                    Arrays.asList(forecastArray));
*/

            mForecastAdapter = new ArrayAdapter<String>(
                    getActivity(),
                    R.layout.list_item_forecast,
                    R.id.list_item_forecast_textview,
                   new ArrayList<String>()
                   // weekForecast
            );

            ListView listView=(ListView) rootView.findViewById(R.id.listview_forecast);
            listView.setAdapter(mForecastAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String forecastStr=mForecastAdapter.getItem(position);
               // Toast.makeText(getActivity(),forecastStr,Toast.LENGTH_SHORT).show(); //   Toast mainly to pop up the  click string
                Intent intent = new Intent(new Intent(getActivity(),DetailActivity.class)).
                        putExtra(Intent.EXTRA_TEXT,forecastStr);
                startActivity(intent);
            }
        });

        return rootView;

}



    public class FetchWeatherTask extends AsyncTask<String,Void,String[]> {
        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        private boolean DEBUG = true;


        private ArrayAdapter<String> mForecastAdapter;
        private final Context mContext;

        public FetchWeatherTask(Context context, ArrayAdapter<String> forecastAdapter) {
            mContext = context;
            mForecastAdapter = forecastAdapter;
        }

        private long insertLocationInDatabase(String locationSetting, String cityName, double lat, double lon) {
            Cursor cursor = mContext.getContentResolver().query(
                    LocationEntry.CONTENT_URI,
                    new String[]{LocationEntry._ID},
                    LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                    new String[]{locationSetting},
                    null);

            if (cursor.moveToFirst()) {
                Log.v(LOG_TAG, "Found it!");
                int locationIdIndex = cursor.getColumnIndex(LocationEntry._ID);
                return cursor.getLong(locationIdIndex);
            } else {
                Log.v(LOG_TAG, "Didn't find it in the database, inserting now!");
                ContentValues locationValues = new ContentValues();
                locationValues.put(LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
                locationValues.put(LocationEntry.COLUMN_CITY_NAME, cityName);
                locationValues.put(LocationEntry.COLUMN_COORD_LAT, lat);
                locationValues.put(LocationEntry.COLUMN_COORD_LONG, lon);

                Uri locationInsertUri = mContext.getContentResolver()
                        .insert(LocationEntry.CONTENT_URI, locationValues);

                return ContentUris.parseId(locationInsertUri);
            }
        }


        /* The date/time conversion code is going to be moved outside the asynctask later,
 * so for convenience we're breaking it out into its own method now.
 */
        private String getReadableDateString(long time) {
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            Date date = new Date(time * 1000);
            SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
            return format.format(date).toString();
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low) {
            // For presentation, assume the user doesn't care about tenths of a degree.

            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String unitType = sharedPrefs.getString(
                    getString(R.string.pref_units_key),
                    getString(R.string.pref_units_metric)
            );

            if (unitType.equals(getString(R.string.pref_units_imperial))) {
                high = (high * 1.8) + 32;
                low = (low * 1.8) + 32;
            } else if (!unitType.equals(getString(R.string.pref_units_metric))) {
                Log.d(LOG_TAG, "Unit not found " + unitType);

            }

            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }



        @Override
        protected String[] doInBackground(String... params) {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String forecastJsonStr = null;
            URL url = null;

            String format = "json";
            String unit = "metric";
            int numDays = 14;
            try {
                if (params.length == 0) {
                    return null;
                }
                String url_s = "http://api.openweathermap.org/data/2.5/forecast/daily?q=" + params[0] + "&mode="+format+"&units="+unit+"&cnt="+numDays+"";
                url = new URL(url_s);


                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");


                urlConnection.connect();

                InputStream inputStream = null;

                inputStream = urlConnection.getInputStream();

                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    forecastJsonStr = null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }
                if (buffer.length() == 0) {
                    return null;
                }
                forecastJsonStr = buffer.toString();
                Log.d(LOG_TAG, "result" + forecastJsonStr);
            } catch (MalformedURLException e1) {
                Log.e(LOG_TAG, "error1", e1);
                return null;
            } catch (IOException e) {
                Log.e(LOG_TAG, "error2", e);
                e.printStackTrace();
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e2) {
                        Log.e(LOG_TAG, "error closing reader ", e2);
                    }
                }
            }


            try {
              // return getWeatherDataFromJson(forecastJsonStr, 7);
                return getWeatherDataFromJson(forecastJsonStr, numDays, params[0]);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                mForecastAdapter.clear();
                for (String s : result) {
                    mForecastAdapter.add(s);

                }


            }
        }



        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         * <p>
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";
            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            String[] resultStrs = new String[numDays];
            for (int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime;
                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay + i);
                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }

            for (String res : resultStrs) {
                Log.d(LOG_TAG, "Entries :" + res);

            }
            return resultStrs;
        }


        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays,
                                                String locationSetting)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.

            // Location information
            final String OWM_CITY = "city";
            final String OWM_CITY_NAME = "name";
            final String OWM_COORD = "coord";
            final String OWM_COORD_LAT = "lat";
            final String OWM_COORD_LONG = "lon";

            // Weather information.  Each day's forecast info is an element of the "list" array.
            final String OWM_LIST = "list";

            final String OWM_DATETIME = "dt";
            final String OWM_PRESSURE = "pressure";
            final String OWM_HUMIDITY = "humidity";
            final String OWM_WINDSPEED = "speed";
            final String OWM_WIND_DIRECTION = "deg";

            // All temperatures are children of the "temp" object.
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";

            final String OWM_WEATHER = "weather";
            final String OWM_DESCRIPTION = "main";
            final String OWM_WEATHER_ID = "id";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
            String cityName = cityJson.getString(OWM_CITY_NAME);
            JSONObject coordJSON = cityJson.getJSONObject(OWM_COORD);
            double cityLatitude = coordJSON.getLong(OWM_COORD_LAT);
            double cityLongitude = coordJSON.getLong(OWM_COORD_LONG);

            Log.v(LOG_TAG, cityName + ", with coord: " + cityLatitude + " " + cityLongitude);

            // Insert the location into the database.
            // The function referenced here is not yet implemented, so we've commented it out for now.
            long locationID = insertLocationInDatabase(
                    locationSetting, cityName, cityLatitude, cityLongitude);

            String[] resultStrs = new String[numDays];
            // Get and insert the new weather information into the database
            Vector<ContentValues> cVVector = new Vector<ContentValues>(weatherArray.length());

            for (int i = 0; i < weatherArray.length(); i++) {
                // These are the values that will be collected.

                long dateTime;
                double pressure;
                int humidity;
                double windSpeed;
                double windDirection;

                double high;
                double low;

                String description;
                int weatherId;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The da/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                dateTime = dayForecast.getLong(OWM_DATETIME);

                pressure = dayForecast.getDouble(OWM_PRESSURE);
                humidity = dayForecast.getInt(OWM_HUMIDITY);
                windSpeed = dayForecast.getDouble(OWM_WINDSPEED);
                windDirection = dayForecast.getDouble(OWM_WIND_DIRECTION);

                // Description is in a child array called "weather", which is 1 element long.
                // That element also contains a weather code.
                JSONObject weatherObject =
                        dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);
                weatherId = weatherObject.getInt(OWM_WEATHER_ID);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                high = temperatureObject.getDouble(OWM_MAX);
                low = temperatureObject.getDouble(OWM_MIN);

                ContentValues weatherValues = new ContentValues();

                weatherValues.put(WeatherEntry.COLUMN_LOC_KEY, locationID);
                weatherValues.put(WeatherEntry.COLUMN_DATETEXT,
                        WeatherContract.getDbDateString(new Date(dateTime * 1000L)));
                weatherValues.put(WeatherEntry.COLUMN_HUMIDITY, humidity);
                weatherValues.put(WeatherEntry.COLUMN_PRESSURE, pressure);
                weatherValues.put(WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
                weatherValues.put(WeatherEntry.COLUMN_DEGREES, windDirection);
                weatherValues.put(WeatherEntry.COLUMN_MAX_TEMP, high);
                weatherValues.put(WeatherEntry.COLUMN_MIN_TEMP, low);
                weatherValues.put(WeatherEntry.COLUMN_SHORT_DESC, description);
                weatherValues.put(WeatherEntry.COLUMN_WEATHER_ID, weatherId);

                cVVector.add(weatherValues);
                String highAndLow = formatHighLows(high, low);

                String day = getReadableDateString(dateTime);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }

            if (cVVector.size() > 0) {
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);
                int rowsInserted = mContext.getContentResolver()
                        .bulkInsert(WeatherEntry.CONTENT_URI, cvArray);
                Log.v(LOG_TAG, "inserted " + rowsInserted + " rows of weather data");
                // Use a DEBUG variable to gate whether or not you do this, so you can easily
                // turn it on and off, and so that it's easy to see what you can rip out if
                // you ever want to remove it.
                if (DEBUG) {
                    Cursor weatherCursor = mContext.getContentResolver().query(
                            WeatherEntry.CONTENT_URI,
                            null,
                            null,
                            null,
                            null
                    );

                    if (weatherCursor.moveToFirst()) {
                        ContentValues resultValues = new ContentValues();
                        DatabaseUtils.cursorRowToContentValues(weatherCursor, resultValues);
                        Log.v(LOG_TAG, "Query succeeded! **********");
                        for (String key : resultValues.keySet()) {
                            Log.v(LOG_TAG, key + ": " + resultValues.getAsString(key));
                        }
                    } else {
                        Log.v(LOG_TAG, "Query failed! :( **********");
                    }
                }
            }
            return resultStrs;
        }

    }
}
