package com.example.joep.glowband_crowd;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.solver.widgets.Rectangle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import static com.example.joep.glowband_crowd.R.id.map;
import static com.example.joep.glowband_crowd.R.id.seekBarWidth;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, com.google.android.gms.location.LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {


    //Google Location:
    //SKD manager -> Update google play services
    //In app build.gradle -> add dependencies -> compile 'com.google.android.gms:play-location:10.2.6'
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mLastLocation;
    private static final int REQUESTCODE_FINELOCATION = 730;

    public static final String TAG = MainActivity.class.getSimpleName();

    private Button buttonGo;
    private EditText textFieldSearchLocation;
    private TextView textViewInfo;
    private Button buttonDelete;
    private Button buttonSave;

    private SeekBar seekBarWidth;
    private SeekBar seekBarHeight;
    private TextView textViewWidthValue;
    private TextView textViewHeightValue;
    private Button buttonDrawPixels;

    private crowdStates mCrowdStates = crowdStates.NO_POINTS_SELECTED;
    private Circle c1;
    private Circle c2;
    private Polygon crowdPolygone;
    private List<Polygon> pixels = new ArrayList<>();

    private int width;
    private int height;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_acitivty);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);

        //CreateGoogleServices
        SetupGoogleServices();

        //CreateLocationRequest
        SetupLocationRequest();

        //Initialize UI Stuff
        SetupUIComponents();
    }

    protected void SetupGoogleServices()
    {
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null)
        {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    protected void SetupLocationRequest()
    {
        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(30 * 1000)        // 30 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds
    }


    protected void SetupUIComponents()
    {
        buttonGo = (Button) findViewById(R.id.buttonGo);
        buttonDelete = (Button) findViewById(R.id.buttonDelete);
        buttonSave = (Button) findViewById(R.id.buttonSave);
        textFieldSearchLocation = (EditText) findViewById(R.id.textFieldSearchLocation);
        textViewInfo = (TextView) findViewById(R.id.textViewInfo);

        seekBarWidth = (SeekBar) findViewById(R.id.seekBarWidth);
        seekBarHeight = (SeekBar) findViewById(R.id.seekBarHeight);
        textViewWidthValue = (TextView) findViewById(R.id.textViewWidthValue);
        textViewHeightValue = (TextView) findViewById(R.id.textViewHeightValue);
        buttonDrawPixels = (Button) findViewById(R.id.buttonDrawPixels);

        buttonGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SearchAddress();
            }
        });

        textFieldSearchLocation.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    SearchAddress();
                    return true;
                }
                return false;
            }
        });

        buttonDelete.setEnabled(false);
        buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCrowdStates = crowdStates.NO_POINTS_SELECTED;
                if(c1 != null)
                    c1.remove();
                if(c2 != null)
                    c2.remove();
                if(crowdPolygone != null)
                    crowdPolygone.remove();
                textViewInfo.setText("Select two points!");
                buttonDelete.setEnabled(false);
                buttonSave.setEnabled(false);
                buttonDrawPixels.setEnabled(false);

                DeletePixels();
            }
        });

        buttonSave.setEnabled(false);
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AsyncTask sqlTask;

                Toast.makeText(getApplicationContext(), "Saving...", Toast.LENGTH_SHORT).show();

                try
                {
                    LatLng NW = FindNW(c1.getCenter(), c2.getCenter());
                    LatLng SE = FindSE(c1.getCenter(), c2.getCenter());

                    sqlTask = new SQL().execute(NW, SE, width , height);
                }
                catch (ClassNotFoundException e)
                {
                    e.printStackTrace();
                }
                catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });


        width = seekBarWidth.getProgress();
        seekBarWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(progress <= 0)
                    progress = 1;
                textViewWidthValue.setText(String.valueOf(progress));
                width = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        height = seekBarHeight.getProgress();
        seekBarHeight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(progress <= 0)
                    progress = 1;
                textViewHeightValue.setText(String.valueOf(progress));
                height = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        buttonDrawPixels.setEnabled(false);
        buttonDrawPixels.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCrowdStates == crowdStates.TWO_POINTS_SELECTED)
                {
                    DrawPixels();
                }
            }
        });
    }

    protected void DeletePixels()
    {
        if(pixels.size() > 0)
        {
            for (int i = 0; i < pixels.size(); i++)
            {
                pixels.get(i).remove();
            }
            pixels.clear();
        }
    }

    protected void DrawPixels()
    {
        DeletePixels();

        LatLng NW = FindNW(c1.getCenter(), c2.getCenter());
        double pixelLat = (FindNW(c1.getCenter(), c2.getCenter()).latitude - FindSE(c1.getCenter(), c2.getCenter()).latitude) / height;
        double pixelLon = (FindSE(c1.getCenter(), c2.getCenter()).longitude - FindNW(c1.getCenter(), c2.getCenter()).longitude) / width;

        for (int x = 0; x < width; x++)
        {
            for (int y = 0; y < height; y++)
            {
                PolygonOptions polygonOptions = new PolygonOptions()
                        .add(
                                new LatLng(NW.latitude - (y * pixelLat), NW.longitude + (x * pixelLon))                   //NW
                                , new LatLng(NW.latitude - (y * pixelLat), (NW.longitude + pixelLon)  + (x * pixelLon))              //NE
                                , new LatLng((NW.latitude - pixelLat) - (y * pixelLat), (NW.longitude + pixelLon)  + (x * pixelLon))   //SE
                                , new LatLng((NW.latitude - pixelLat)  - (y * pixelLat), NW.longitude + (x * pixelLon)))       //SW
                        .fillColor(Color.TRANSPARENT)
                        .strokeColor(Color.BLUE)
                        .strokeWidth(1);

                Polygon poly = mMap.addPolygon(polygonOptions);
                pixels.add(poly);
            }
        }
    }

    protected void SearchAddress()
    {
        try
        {
            Geocoder geoCoder = new Geocoder(getApplicationContext());
            List<Address> addresses = geoCoder.getFromLocationName(textFieldSearchLocation.getText().toString(), 5);

            if(addresses != null)
            {
                if(addresses.size() > 0)
                {
                    LatLng searchedLatLng = new LatLng(addresses.get(0).getLatitude(), addresses.get(0).getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(searchedLatLng));
                    mMap.addMarker(new MarkerOptions()
                            .position(searchedLatLng)
                            .icon(BitmapDescriptorFactory.defaultMarker()
                            ).title(addresses.get(0).toString()) //TODO: Clean address label
                    );
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onResume() {
        mGoogleApiClient.connect();
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng clickedPoint) {

                CircleOptions circleOptions = new CircleOptions()
                        .center(clickedPoint)
                        .zIndex(20)
                        .radius(1)
                        .fillColor(Color.CYAN)
                        .strokeColor(Color.TRANSPARENT);

                switch (mCrowdStates)
                {
                    case NO_POINTS_SELECTED:
                        c1 = mMap.addCircle(circleOptions);
                        mCrowdStates = crowdStates.ONE_POINT_SELECTED;
                        buttonDelete.setEnabled(true);
                        break;
                    case ONE_POINT_SELECTED:
                        c2 = mMap.addCircle(circleOptions);
                        mCrowdStates = crowdStates.TWO_POINTS_SELECTED;

                        PolygonOptions polygonOptions = new PolygonOptions()
                                .add(
                                        c1.getCenter()                                                      //NW C1.latlng
                                        , new LatLng(c1.getCenter().latitude, c2.getCenter().longitude)     //NE C1.lat C2.lon
                                        , c2.getCenter()                                                    //SE C2.latlon
                                        , new LatLng(c2.getCenter().latitude, c1.getCenter().longitude))    //SW C2.lat C1.lon
                                .fillColor(Color.TRANSPARENT)
                                .strokeColor(Color.RED);

                        crowdPolygone = mMap.addPolygon(polygonOptions);
                        textViewInfo.setText("Crowd Defined!\n Save or Delete the Crowd");
                        buttonSave.setEnabled(true);
                        buttonDrawPixels.setEnabled(true);
                        break;
                    case TWO_POINTS_SELECTED:

                        break;
                }
            }
        });
    }

    private enum crowdStates
    {
        NO_POINTS_SELECTED,
        ONE_POINT_SELECTED,
        TWO_POINTS_SELECTED
    }

    private LatLng FindNW(LatLng latlng1, LatLng latlng2)
    {
        double nwLat;
        double nwLon;

        if(latlng1.latitude > latlng2.latitude)
            nwLat = latlng1.latitude;
        else
            nwLat = latlng2.latitude;

        if(latlng1.longitude < latlng2.longitude)
            nwLon = latlng1.longitude;
        else
            nwLon = latlng2.longitude;

        return new LatLng(nwLat, nwLon);
    }

    private LatLng FindSE(LatLng latlng1, LatLng latlng2)
    {
        double seLat;
        double seLon;

        if(latlng1.latitude < latlng2.latitude)
            seLat = latlng1.latitude;
        else
            seLat = latlng2.latitude;

        if(latlng1.longitude > latlng2.longitude)
            seLon = latlng1.longitude;
        else
            seLon = latlng2.longitude;

        return new LatLng(seLat, seLon);
    }

    protected void LocationUpdated(Location newLocation)
    {
        Log.d(TAG, "New Location" + newLocation.toString());
        mLastLocation = newLocation;
        //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), 15));
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10 * 1000);
        mLocationRequest.setFastestInterval(5 * 1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode)
        {
            case REQUESTCODE_FINELOCATION:
            {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Log.d(TAG, "FineLocationPermission granted after request");
                    // permission was granted, yay!
                    if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                        mMap.setMyLocationEnabled(true);

                        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                        if(location != null)
                        {
                            LocationUpdated(location);
                        }
                    }
                }
                else
                {
                    Log.d(TAG, "FineLocationPermission not granted after request");
                    // permission denied, boo!
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        createLocationRequest();

        Log.d(TAG, "Check location permissions");
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION))
            {
                Log.d(TAG, "FineLocationPermission show info");
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            }
            else
            {
                Log.d(TAG, "FineLocationPermission request permission");
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUESTCODE_FINELOCATION);
            }
        }
        else
        {
            Log.d(TAG, "FineLocationPermission granted");

            mMap.setMyLocationEnabled(true);

            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            if(location != null)
            {
                LocationUpdated(location);
            }
        }
    }


    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended. Please reconnect.");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
    }

    @Override
    public void onLocationChanged(Location location) {
        if(location != null)
        {
            LocationUpdated(location);
        }
    }
}
