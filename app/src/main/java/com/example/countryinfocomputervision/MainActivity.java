package com.example.countryinfocomputervision;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
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
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.ColorInfo;
import com.google.api.services.vision.v1.model.DominantColorsAnnotation;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.api.services.vision.v1.model.ImageProperties;
import com.google.api.services.vision.v1.model.LocationInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    // Components
    private Bitmap imgBitmap;
    private ImageView amImg;
    private TextView amTxtResult;
    // Cloud Vision
    private Feature feature;
    private static final String CLOUD_VISION_API_KEY = "AIzaSyB5MkIB5lNnQH1kC1tZ3ATeEsv7z66moKs";
    // Country Data
    private RequestQueue request;
    private StringRequest stringRequest;
    private JSONObject countriesObject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        amImg = findViewById(R.id.amImg);
        amTxtResult = findViewById(R.id.amTxtResult);
        imgBitmap = null;
        feature = new Feature();
        feature.setType("LANDMARK_DETECTION");
        feature.setMaxResults(10);

        getCountryData();
    }

    private void getCountryData() {
        request = Volley.newRequestQueue(MainActivity.this);
        String URL = "http://www.geognos.com/api/en/countries/info/all.json";
        stringRequest = new StringRequest(Request.Method.GET, URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                countriesObject = null;
                try {
                    countriesObject = new JSONObject(response);
                    if (countriesObject.getJSONObject("Results") == null) {
                        Toast.makeText(MainActivity.this, "No hay información", Toast.LENGTH_LONG).show();
                        return;
                    } else {
                        countriesObject = countriesObject.getJSONObject("Results");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "Sucedió un error en la consulta del listado de los países", Toast.LENGTH_LONG).show();
            }
        });
        request.add(stringRequest);
    }

    public void uploadImage(View v) {
        startActivityForResult(new Intent(Intent.ACTION_PICK).setType("image/*"), 1);
    }

    public void detectCountry(View v) throws IOException {
        if (imgBitmap == null) {
            Toast.makeText(this, "No hay imagen seleccionada", Toast.LENGTH_SHORT).show();
        } else {
            detectLandmarks(imgBitmap, feature);
        }
    }

    private void detectLandmarks(Bitmap bitmap, Feature feature) {
        List<Feature> featureList = new ArrayList<>();
        featureList.add(feature);
        List<AnnotateImageRequest> annotateImageRequests = new ArrayList<>();
        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();
        annotateImageRequest.setFeatures(featureList);
        annotateImageRequest.setImage(getImageEncode(imgBitmap));
        annotateImageRequests.add(annotateImageRequest);

        new AsyncTask<Object, Void, String>() {
            @Override
            protected String doInBackground(Object... objects) {
                try {
                    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
                    VisionRequestInitializer requestInitializer = new VisionRequestInitializer(CLOUD_VISION_API_KEY);
                    Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
                    builder.setVisionRequestInitializer(requestInitializer);
                    Vision vision = builder.build();
                    BatchAnnotateImagesRequest batchAnnotateImagesRequest = new BatchAnnotateImagesRequest();
                    batchAnnotateImagesRequest.setRequests(annotateImageRequests);
                    Vision.Images.Annotate annotateRequest = vision.images().annotate(batchAnnotateImagesRequest);
                    annotateRequest.setDisableGZipContent(true);
                    BatchAnnotateImagesResponse response = annotateRequest.execute();
                    return convertResponseToString(response);
                } catch (IOException e) {
                    Log.e("ERROR", "Ha salido un error al ejecutar la API Cloud Vision: " + e.getMessage());
                }
                return "";
            }

            protected  void onPostExecute(String result) {
                String countryCode = "";
                try {
                    if (result != "") {
                        countryCode = searchCountryCode(result);
                        startActivity(new Intent(MainActivity.this, ResultsActivity.class).putExtra("countryCode", countryCode));
                    } else {
                        amTxtResult.setText("No hay información");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    private String formatAnnotation(List<EntityAnnotation> entityAnnotation) {
        String msg = "";
        if (entityAnnotation != null) {
            for (EntityAnnotation entity : entityAnnotation) {
                LocationInfo info = entity.getLocations().listIterator().next();
                msg = info.getLatLng().getLatitude() + " " +  info.getLatLng().getLongitude();
                msg += ":";
            }
        } else {
            msg = "";
        }
        return msg;
    }

    private String convertResponseToString(BatchAnnotateImagesResponse response) {
        AnnotateImageResponse imageResponses = response.getResponses().get(0);
        List<EntityAnnotation> entityAnnotations;
        String msg = "";
        // LANDMARK_DETECTION
        entityAnnotations = imageResponses.getLandmarkAnnotations();
        msg = formatAnnotation(entityAnnotations);
        return msg;
    }

    private Image getImageEncode(Bitmap bitmap) {
        Image base64EncodedImage = new Image();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        base64EncodedImage.encodeContent(imageBytes);
        return base64EncodedImage;
    }

    private String searchCountryCode(String points) throws JSONException {
        String[] pointsArray = points.split(":");
        for (String point : pointsArray) {
            String[] latlng = point.split(" ");
            Iterator<String> temp = countriesObject.keys();
            while (temp.hasNext()) {
                String key = temp.next();
                JSONObject country = countriesObject.getJSONObject(key);
                JSONObject geoRect = country.getJSONObject("GeoRectangle");
                if (Double.valueOf(latlng[0]) <= geoRect.getDouble("North") &&
                Double.valueOf(latlng[0]) >= geoRect.getDouble("South") &&
                        Double.valueOf(latlng[1]) <= geoRect.getDouble("East") &&
                        Double.valueOf(latlng[1]) >= geoRect.getDouble("West")) {
                    return country.getJSONObject("CountryCodes").getString("iso2");
                }
            }
        }
        return "";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            // Clear
            amImg.setImageResource(android.R.color.transparent);
            amTxtResult.setText("");
            imgBitmap = null;
            //

            Uri imgUri = data.getData();
            try {
                imgBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imgUri);
            } catch (IOException e) {
                Log.e("ERROR", "Ha salido un error en la conversión de Uri a Bitmap: " + e.getMessage());
            }
            amImg.setImageBitmap(imgBitmap);
        } else {
            Toast.makeText(this, "No tiene permisos para leer el almacenamiento de su celular", Toast.LENGTH_SHORT).show();
        }
    }
}