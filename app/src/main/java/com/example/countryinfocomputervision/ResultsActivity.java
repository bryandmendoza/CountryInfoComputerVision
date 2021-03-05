package com.example.countryinfocomputervision;

import androidx.fragment.app.FragmentActivity;

import android.graphics.Color;
import android.media.Image;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

public class ResultsActivity extends FragmentActivity implements OnMapReadyCallback {
    private TextView arTxtCountry, arTxtContentData;
    private ImageView arImgCountry;
    private GoogleMap mMap;
    //Country Data
    private RequestQueue request;
    private StringRequest stringRequest;
    private JSONObject countryObject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        arTxtCountry = findViewById(R.id.arTxtCountry);
        arTxtContentData = findViewById(R.id.arTxtContentData);
        arImgCountry = findViewById(R.id.arImgCountry);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.arMap);
        mapFragment.getMapAsync(this);

        Bundle b = this.getIntent().getExtras();
        String countryCode = b.getString("countryCode");
        if (countryCode != "") {
            getCountryData(countryCode);
            Glide.with( this.getApplicationContext()).load("http://www.geognos.com/api/en/countries/flag/"+countryCode+".png").into(arImgCountry);
        } else {
            Toast.makeText(this, "No se ha encontrado la información del país", Toast.LENGTH_LONG).show();
        }

    }

    public void exitActivity(View v) {
        finish();
    }

    private void getCountryData(String countryCode) {
        request = Volley.newRequestQueue(ResultsActivity.this);
        String URL = "http://www.geognos.com/api/en/countries/info/"+countryCode+".json";
        stringRequest = new StringRequest(Request.Method.GET, URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                countryObject = null;
                try {
                    countryObject = new JSONObject(response);
                    if (countryObject.getJSONObject("Results") == null) {
                        Toast.makeText(ResultsActivity.this, "No hay información", Toast.LENGTH_LONG).show();
                        return;
                    } else {
                        countryObject = countryObject.getJSONObject("Results");
                        try {
                            arTxtCountry.setText(countryObject.getString("Name"));
                            arTxtContentData.setText("Capital: "+countryObject.getJSONObject("Capital").getString("Name")+"\n"+
                                    "Code ISO 2: "+countryObject.getJSONObject("CountryCodes").getString("iso2")+"\n"+
                                    "Tel Prefix: "+countryObject.getString("TelPref"));
                            mMap.getUiSettings().setZoomControlsEnabled(false);
                            mMap.getUiSettings().setAllGesturesEnabled(true);
                            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                            Double centerLat = countryObject.getJSONArray("GeoPt").getDouble(0);
                            Double centerLng = countryObject.getJSONArray("GeoPt").getDouble(1);
                            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(centerLat, centerLng), 4);
                            mMap.moveCamera(cameraUpdate);
                            Double pointNorth = countryObject.getJSONObject("GeoRectangle").getDouble("North");
                            Double pointSouth = countryObject.getJSONObject("GeoRectangle").getDouble("South");
                            Double pointEast = countryObject.getJSONObject("GeoRectangle").getDouble("East");
                            Double pointWest = countryObject.getJSONObject("GeoRectangle").getDouble("West");
                            PolylineOptions lines = new PolylineOptions()
                                    .add(new LatLng(pointNorth, pointWest))
                                    .add(new LatLng(pointNorth, pointEast))
                                    .add(new LatLng(pointSouth, pointEast))
                                    .add(new LatLng(pointSouth, pointWest))
                                    .add(new LatLng(pointNorth, pointWest));
                            lines.width(8);
                            lines.color(Color.BLUE);
                            mMap.addPolyline(lines);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(ResultsActivity.this, "Sucedió un error en la consulta de la información del país", Toast.LENGTH_LONG).show();
            }
        });
        request.add(stringRequest);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        /*LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/
    }
}