package com.example.goahead.makemyday;

import android.content.Intent;
import android.database.Cursor;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.example.goahead.makemyday.data.WeatherContract;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment implements  LoaderManager.LoaderCallbacks<Cursor> {
    private final String LOG_TAG = DetailActivityFragment.class.getSimpleName();
    private final String FORECAST_SHARE_HSHTAG = " #MakeMyDayApp";

    private String mForecastStr;
    private String mLocation;

    private static final int DETAIL_LOADER = 0;

    public static final String LOCATION_KEY = "location";
    public static final String DATE_KEY = "forecast_date";
    private ShareActionProvider mShareActionProvider;
    public DetailActivityFragment() {
        setHasOptionsMenu(true);

    }

    private static final String[] FORECAST_COLUMN = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATETEXT,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_DEGREES,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING
    };


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mLocation != null) {
            outState.putString(LOCATION_KEY, mLocation);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            mLocation = savedInstanceState.getString(LOCATION_KEY);
        }
        getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mLocation != null && !mLocation.equals(Utility.getPreferredLocation(getActivity()))) {
            getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
        }
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
     /*   Intent intent=getActivity().getIntent();
        if(intent != null && intent.hasExtra(Intent.EXTRA_TEXT)){
            mForecastStr = intent.getStringExtra(Intent.EXTRA_TEXT);
            ((TextView)rootView.findViewById(R.id.detail_text)).setText(mForecastStr);
        }
        */
        return  rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu,MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.detailfragment, menu);
        MenuItem menuItem = menu.findItem(R.id.action_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        if(mShareActionProvider != null){
            mShareActionProvider.setShareIntent(createShareForecastIntent());
        }
        else {
            Log.d(LOG_TAG, "Share action provide is null");
        }

    }




    private Intent createShareForecastIntent(){
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                mForecastStr+FORECAST_SHARE_HSHTAG);
        return  shareIntent;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Intent intent = getActivity().getIntent();
        if (intent == null || !intent.hasExtra(DATE_KEY)) {
            return null;
        }
        String forecastDate = intent.getStringExtra(DATE_KEY);
        Log.d(LOG_TAG, forecastDate);
        // Sort order: Ascending by date
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATETEXT + " ASC";

        mLocation = Utility.getPreferredLocation(getActivity());
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                mLocation, forecastDate);


        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed
        return new CursorLoader(
                getActivity(),
                weatherForLocationUri,
                FORECAST_COLUMN,
                null,
                null,
                sortOrder
        );
    }
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (!data.moveToFirst()) {
            return;
        }

        String dateString = Utility.formatDate(
                data.getString(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATETEXT))
        );

        Log.d(LOG_TAG, dateString);

        String weatherDescription =
                data.getString(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC));

        boolean isMetric = Utility.isMetric(getActivity());
        String high = Utility.formatTemperature(
                getActivity(),data.getDouble(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP)), isMetric);
        String low = Utility.formatTemperature(
                getActivity(),data.getDouble(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP)), isMetric);

        mForecastStr = String.format("%s - %s - %s/%s",
                dateString, weatherDescription, high, low);

        ((TextView) getView().findViewById(R.id.detail_low_textview)).setText(low);
        ((TextView) getView().findViewById(R.id.detail_high_textview)).setText(high);
        ((TextView) getView().findViewById(R.id.detail_date_textview)).setText(dateString);
        ((TextView) getView().findViewById(R.id.detail_forecast_textview)).setText(weatherDescription);



        // If onCreateOptionsMenu has already happened, we need to update the share intent now.
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(createShareForecastIntent());
        }

    }




    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        getLoaderManager().restartLoader(DETAIL_LOADER, null, this);

    }
}
