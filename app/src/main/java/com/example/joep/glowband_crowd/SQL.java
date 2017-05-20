package com.example.joep.glowband_crowd;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static android.content.ContentValues.TAG;

/**
 * Created by Joep on 20-5-2017.
 */

public class SQL extends AsyncTask<Object, Void , Void> {
    private Connection connect = null;
    private PreparedStatement preparedStatement = null;


    public SQL() throws ClassNotFoundException, SQLException
    {
        Class.forName("com.mysql.jdbc.Driver");
    }

    @Override
    protected Void doInBackground(Object... params)
    {
        LatLng NW = (LatLng) params[0];
        LatLng SE = (LatLng) params[1];
        int width = (int) params[2];
        int height = (int) params[3];

        createCrowdTable();

        addData(NW, SE, width, height);
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
    }

    protected void open()
    {
        try
        {
            connect = DriverManager.getConnection("jdbc:mysql://studmysql01.fhict.local/dbi310878", "dbi310878", "Iie72-HD");
        }
        catch (Exception e)
        {
            Log.i(TAG, "open():" + e);
        }
    }

    protected void close()
    {
        try
        {
            if (connect != null)
            {
                connect.close();
            }
        }
        catch (Exception e)
        {
            Log.i(TAG, "close():" + e);
        }
    }

    private void createCrowdTable()
    {
        String query1 = "DROP TABLE IF EXISTS glowstick_crowd;";
        String query2 = "CREATE TABLE glowstick_crowd(ID INT(10) PRIMARY KEY, WIDTH INT(10), HEIGHT INT(10), NWLA DOUBLE, NWLO DOUBLE, SELA DOUBLE, SELO DOUBLE);";

        try
        {
            open();

            preparedStatement = connect.prepareStatement(query1);

            preparedStatement.execute();
            Log.i(TAG, "Drop Crowd Table");

            preparedStatement = connect.prepareStatement(query2);

            preparedStatement.execute();
            Log.i(TAG, "Created Crowd Table");
        }
        catch (Exception e)
        {
            Log.i(TAG, "CreateCrowdTable:" + e);
        }
        finally
        {
            close();
        }
    }

    private void addData(LatLng NW, LatLng SE, int width, int height)
    {
        String query = "INSERT INTO glowstick_crowd(ID, WIDTH, HEIGHT, NWLA, NWLO, SELA, SELO) VALUES (?, ?, ?, ?, ?, ?, ?);";

        try
        {
            open();

            preparedStatement = connect.prepareStatement(query);

            preparedStatement.setInt(1, 1);
            preparedStatement.setInt(2, width);
            preparedStatement.setInt(3, height);

            preparedStatement.setDouble(4, NW.latitude);
            preparedStatement.setDouble(5, NW.longitude);

            preparedStatement.setDouble(6, SE.latitude);
            preparedStatement.setDouble(7, SE.longitude);

            preparedStatement.executeUpdate();
            Log.i(TAG, "Added Info to Table");
        }
        catch (Exception e)
        {
            Log.i(TAG, "CreateCrowdTable:" + e);
        }
        finally
        {
            close();
        }
    }
}
