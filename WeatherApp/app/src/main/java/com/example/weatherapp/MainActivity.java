package com.example.weatherapp;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private EditText user_field;
    private Button user_button;
    private TextView result_info;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        user_field = findViewById(R.id.user_field);
        user_button = findViewById(R.id.user_button);
        result_info = findViewById(R.id.result_info);

        user_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (user_field.getText().toString().trim().equals(""))
                    Toast.makeText(MainActivity.this, R.string.No_user_input_text, Toast.LENGTH_LONG).show();
                else {
                    String city = user_field.getText().toString();
                    String url = "https://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=db752b0213593434d7dd2eb5e97302ba&units=metric&lang=ru";

                    new GetURLData().execute(url);
                }
            }
        });
    }

    private class GetURLData extends AsyncTask<String, String, String> {

        protected void onPreExecute() {
            super.onPreExecute();
            result_info.setText("Ожидайте...");
        }

        @Override
        protected String doInBackground(String... strings) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(strings[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuffer buffer = new StringBuffer();
                String line = "";

                while ((line = reader.readLine()) != null)
                    buffer.append(line).append("\n");

                return buffer.toString();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null)
                    connection.disconnect();

                try {
                    if (reader != null)
                        reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            try {
                JSONObject jsonObject = new JSONObject(result);

                String city = user_field.getText().toString();
                int dt_lite = jsonObject.getInt("dt");
                double temp = jsonObject.getJSONObject("main").getDouble("temp");
                double feels_like = jsonObject.getJSONObject("main").getDouble("feels_like");
                double wind_speed = (jsonObject.getJSONObject("wind").getDouble("speed"));
                double pressure = jsonObject.getJSONObject("main").getDouble("pressure");
                double humidity = jsonObject.getJSONObject("main").getDouble("humidity");
                int visibility = (jsonObject.getInt("visibility")) / 100;


                String dt = DateFormat.getDateTimeInstance().format(new Date(dt_lite * 1000L));


                SQLiteDatabase db = getBaseContext().openOrCreateDatabase("app.db", MODE_PRIVATE, null);
                db.execSQL("CREATE TABLE IF NOT EXISTS weather (city TEXT,date TEXT, temper REAL,feels_like REAL,wind_speed REAL,pressure REAL,humidity REAL,visibility INTEGER, UNIQUE(date))");
                db.execSQL("DELETE FROM weather;");
                db.execSQL("INSERT OR IGNORE INTO weather VALUES ('" + city + "','" + dt + "'," + temp + "," + feels_like + "," + wind_speed + "," + pressure + "," + humidity + "," + visibility + ");");

                Cursor query = db.rawQuery("SELECT * FROM weather;", null);
                TextView textView = findViewById(R.id.result_info);
                textView.setText("");

                while (query.moveToNext()) {
                    String ci = query.getString(0);
                    String da = query.getString(1);
                    int te = (int) query.getDouble(2);
                    int fl = (int) query.getDouble(3);
                    int ws = (int) query.getDouble(4);
                    int pr = (int) query.getDouble(5);
                    int hu = (int) query.getDouble(6);
                    int vi = (int) query.getDouble(7);

                    textView.append("Город: " + ci + "\n" +
                            "Дата: " + da + "\n" +
                            "Температура: " + te + " °C\n" +
                            "Ощущается как: " + fl + " °C\n" +
                            "Скорость ветра : " + ws + " м/с\n" +
                            "Давление : " + pr + " мм рт\n" +
                            "Влажность : " + hu + " %\n" +
                            "Видимость : " + vi + " %"
                    );
                }
                query.close();
                db.close();
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

    }
}