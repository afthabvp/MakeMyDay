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
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
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
import android.widget.TextView;

import com.example.goahead.makemyday.data.WeatherContract;
import com.example.goahead.makemyday.data.WeatherContract.LocationEntry;
import com.example.goahead.makemyday.data.WeatherContract.WeatherEntry;

/**
 * Created by spartan300 on 15/9/15.
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private boolean mUseTodayLayout;

    private ForecastAdapter mForecastAdapter;

    private String mLocation;
    private ListView  listView;
    private int mPosition = ListView.INVALID_POSITION;
    private static final String SELECTED_KEY = "selected_position";

    private static final int FORECAST_LOADER = 0;
    public ForecastFragment() {
    }




    // For the forecast view we're showing only a small subset of the stored data.
    // Specify the columns we need.
    private static final String[] FORECAST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherEntry.TABLE_NAME + "." + WeatherEntry._ID,
            WeatherEntry.COLUMN_DATETEXT,
            WeatherEntry.COLUMN_SHORT_DESC,
            WeatherEntry.COLUMN_MAX_TEMP,
            WeatherEntry.COLUMN_MIN_TEMP,
            LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherEntry.COLUMN_WEATHER_ID
    };


    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    public static final int COL_WEATHER_ID = 0;
    public static final int COL_WEATHER_DATE = 1;
    public static final int COL_WEATHER_DESC = 2;
    public static final int COL_WEATHER_MAX_TEMP = 3;
    public static final int COL_WEATHER_MIN_TEMP = 4;
    public static final int COL_LOCATION_SETTING = 5;
    public static final int COL_WEATHER_CONDITION_ID = 6;

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        public void onItemSelected(String date);
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
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
        FetchWeatherTask fetchWeatherTask=new FetchWeatherTask(getActivity());
       // SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        //String location = prefs.getString(getString(R.string.pref_locaton_key),getString(R.string.pref_location_default));
        String location = Utility.getPreferredLocation(getActivity());

        //weatherTask.execute(location, units);
        fetchWeatherTask.execute(location);

    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        // When tablets rotate, the currently selected list item needs to be saved.
        // When no item is selected, mPosition will be set to Listview.INVALID_POSITION,
        // so check for that before storing.
        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(SELECTED_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }
    @Override
    public void onStart(){
        super.onStart();
        updateWeather();
    }
    @Override
    public void onResume() {
        super.onResume();
        if (mLocation != null && !mLocation.equals(Utility.getPreferredLocation(getActivity()))) {
            getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // The ArrayAdapter will take data from a source and
        // use it to populate the ListView it's attached to.
        mForecastAdapter = new ForecastAdapter(getActivity(), null, 0);
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(mForecastAdapter);





        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = mForecastAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
//                    String dateString = Utility.formatDate(cursor.getString(COL_WEATHER_DATE));
//                    String weatherDesc = cursor.getString(COL_WEATHER_DESC);
//
//                    boolean isMetric = Utility.isMetric(getActivity());
//                    String high = Utility.formatTemperature(
//                            cursor.getDouble(COL_WEATHER_MAX_TEMP), isMetric
//                    );
//
//                    String low = Utility.formatTemperature(
//                            cursor.getDouble(COL_WEATHER_MIN_TEMP), isMetric
//                    );
//
//                    String detailString = String.format("%s - %s - %s/%s",
//                            dateString, weatherDesc, high, low);


                    ((Callback)getActivity())
                            .onItemSelected(cursor.getString(COL_WEATHER_DATE));
                   // Intent detailActivityIntent = new Intent(getActivity(), DetailActivity.class);
                    //detailActivityIntent.putExtra(DetailActivityFragment.DATE_KEY, cursor.getString(COL_WEATHER_DATE));
                    //startActivity(detailActivityIntent);
                }
                mPosition = position;

            }
        });
        // If there's instance state, mine it for useful information.
        // The end-goal here is that the user never knows that turning their device sideways
        // does crazy lifecycle related things.  It should feel like some stuff stretched out,
        // or magically appeared to take advantage of room, but data or place in the app was never
        // actually *lost*.
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            // The listview probably hasn't even been populated yet.  Actually perform the
            // swapout in onLoadFinished.
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }
        mForecastAdapter.setUseTodayLayout(mUseTodayLayout);
    return rootView;

}

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        mLocation = Utility.getPreferredLocation(getActivity());

        // Sort order:  Ascending, by date.
        String sortOrder = WeatherEntry.COLUMN_DATETEXT + " ASC";



        // To only show current and future dates, get the String representation for today,
        // and filter the query to return weather only for dates after or including today.
        // Only return data after today.
        String startDate = WeatherContract.getDbDateString(new Date());

        Uri weatherForLocationUri = WeatherEntry.buildWeatherLocationWithStartDate(
                mLocation, startDate);

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(
                getActivity(),
                weatherForLocationUri,
                FORECAST_COLUMNS,
                null,
                null,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mForecastAdapter.swapCursor(data);
        if (mPosition != ListView.INVALID_POSITION) {
            // If we don't need to restart the loader, and there's a desired position to restore
            // to, do so now.
            listView.smoothScrollToPosition(mPosition);
        }


    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mForecastAdapter.swapCursor(null);

    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
        if (mForecastAdapter != null) {
            mForecastAdapter.setUseTodayLayout(mUseTodayLayout);
        }
    }


    public class FetchWeatherTask extends AsyncTask<String,Void,String[]> {
        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        private boolean DEBUG = true;


        private final Context mContext;

        public FetchWeatherTask(Context context) {
            mContext = context;
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
                String url_s = "http://api.openweathermap.org/data/2.5/forecast/daily?q=" + params[0] + "&mode="+format+"&units="+unit+"&cnt="+numDays+"&APPID=561f8515049db171c5bed8b8dbfb0669";
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
                getWeatherDataFromJson(forecastJsonStr, numDays, params[0]);
                return null;
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
            }
            return null;
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
            long locationID = addLocation(locationSetting, cityName, cityLatitude, cityLongitude);

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

            insertWeatherIntoDatabase(cVVector);

            return resultStrs;
        }



    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void insertWeatherIntoDatabase(Vector<ContentValues> CVVector) {
        if (CVVector.size() > 0) {
            ContentValues[] contentValuesArray = new ContentValues[CVVector.size()];
            CVVector.toArray(contentValuesArray);


            int rowsInserted = mContext.getContentResolver().bulkInsert(WeatherEntry.CONTENT_URI, contentValuesArray);

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
    }

        /**
         * Helper method to handle insertion of a new location in the weather database.
         *
         * @param locationSetting The location string used to request updates from the server.
         * @param cityName A human-readable city name, e.g "Mountain View"
         * @param lat the latitude of the city
         * @param lon the longitude of the city
         * @return the row ID of the added location.
         */
        private long addLocation(String locationSetting, String cityName, double lat, double lon) {

            Log.v(LOG_TAG, "inserting " + cityName + ", with coord: " + lat + ", " + lon);

            // First, check if the location with this city name exists in the db
            Cursor cursor = mContext.getContentResolver().query(
                    LocationEntry.CONTENT_URI,
                    new String[]{LocationEntry._ID},
                    LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                    new String[]{locationSetting},
                    null);

            if (cursor.moveToFirst()) {
                Log.v(LOG_TAG, "Found it in the database!");
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

    }
}
