package com.example.goahead.makemyday;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by spartan300 on 15/9/15.
 */
public class ForecastFragment extends Fragment {
    private ListAdapter mForecastAdapter;
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
            FetchWeatherTask fetchWeatherTask=new FetchWeatherTask();
            fetchWeatherTask.execute("94043");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

           String[] forecastArray = {
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




            mForecastAdapter = new ArrayAdapter<String>(
                    getActivity(),
                    R.layout.list_item_forecast,
                    R.id.list_item_forecast_textview,
                    weekForecast
            );

            ListView listView=(ListView) rootView.findViewById(R.id.listview_forecast);
            listView.setAdapter(mForecastAdapter);

        return rootView;

}



    public class FetchWeatherTask extends AsyncTask<String,Void,Void> {
        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();


        @Override
        protected Void doInBackground(String... params) {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String forecastJsonStr = null;
            URL url = null;
            try {
                if(params.length == 0){
                    return null;
                }
                String url_s = "http://api.openweathermap.org/data/2.5/forecast/daily?q="+params[0]+"&mode=xml&units=metric&cnt=7";
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
                while ((line = reader.readLine())!= null){
                    buffer.append(line + "\n");
                }
                if(buffer.length() == 0){
                    return  null;
                }
                forecastJsonStr=buffer.toString();
                Log.d(LOG_TAG,"result"+forecastJsonStr);
            } catch (MalformedURLException e1) {
                Log.e(LOG_TAG,"error1",e1);
                return  null;
            } catch (IOException e) {
                Log.e(LOG_TAG, "error2", e);
                e.printStackTrace();
                return  null;
            }finally {
                if(urlConnection != null){
                    urlConnection.disconnect();
                }
                if(reader !=null){
                    try {
                        reader.close();
                    } catch (IOException e2) {
                        Log.e(LOG_TAG, "error closing reader ",e2);
                    }
                }
            }


            return null;
        }



    }
}