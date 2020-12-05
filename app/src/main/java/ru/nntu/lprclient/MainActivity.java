package ru.nntu.lprclient;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Main activity.
 */
public class MainActivity extends AppCompatActivity {

    private static final String MULTIPART_LICENSE_PLATE_IMAGE_ID = "license_plate_image";

    private static final String MULTIPART_LICENSE_PLATE_IMAGE_FILENAME = "license_plate_image.jpg";

    private static final String MULTIPART_LICENSE_PLATE_IMAGE_MEDIA_TYPE = "image/*jpg";

    private static final String MULTIPART_COUNTRY_CODE_ID = "country_code";

    private static final String API_RESPONSE_NUMBER_ATTRIBUTE = "number";

    private static final String API_RESPONSE_CONFIDENCE_ATTRIBUTE = "confidence";

    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private ProgressBar progressBar;

    private ImageView cameraImageView;

    private Spinner countryCodesSpinner;

    private TextView licensePlateTextView;

    private Button startCameraButton;

    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = findViewById(R.id.progress_bar);

        cameraImageView = findViewById(R.id.camera_image_view);

        licensePlateTextView = findViewById(R.id.license_plate_text_view);

        startCameraButton = findViewById(R.id.start_camera_btn);
        startCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCamera();
            }
        });

        countryCodesSpinner = (Spinner) findViewById(R.id.country_codes_spinner);

        // create adapter
        ArrayAdapter<CountryCodeEnum> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, CountryCodeEnum.values());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        countryCodesSpinner.setAdapter(adapter);
    }

    private void startCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "License plate picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "License plate picture");
        imageUri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        try {
            startActivityForResult(takePhotoIntent, REQUEST_IMAGE_CAPTURE);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            try {
                // clean previous result, if exists
                licensePlateTextView.setText("");

                Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),
                        imageUri);
                cameraImageView.setImageBitmap(imageBitmap);
                progressBar.setVisibility(View.VISIBLE);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                sendRequest(stream.toByteArray());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendRequest(byte[] imageData) {
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(300, TimeUnit.SECONDS)
                .build();
        String countryCode = ((CountryCodeEnum) countryCodesSpinner.getSelectedItem()).getApiCode();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(MULTIPART_LICENSE_PLATE_IMAGE_ID,
                        MULTIPART_LICENSE_PLATE_IMAGE_FILENAME,
                        RequestBody.create(
                                MediaType.parse(MULTIPART_LICENSE_PLATE_IMAGE_MEDIA_TYPE),
                                imageData))
                .addFormDataPart(MULTIPART_COUNTRY_CODE_ID, countryCode)
                .build();
        HttpUrl localUrl = HttpUrl.parse(
                BuildConfig.API_HOST_URL + ":"
                        + BuildConfig.API_HOST_PORT
                        + BuildConfig.API_RECOGNIZE_PATH
        );

        if (localUrl == null) {
            showToast(R.string.api_communication_error_toast_message);
            return;
        }

        Request request = new Request.Builder()
                .url(localUrl)
                .post(requestBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                showToast(R.string.api_communication_error_toast_message);
                progressBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onResponse(Call call, Response response) {
                progressBar.setVisibility(View.INVISIBLE);
                handleResponse(response);
            }
        });
    }

    private void handleResponse(Response response) {
        if (response.code() == HttpURLConnection.HTTP_OK) {
            if (response.body() != null) {
                JSONObject responseObject = null;
                try {
                    responseObject = new JSONObject(response.body().string());
                    String number = responseObject.getString(API_RESPONSE_NUMBER_ATTRIBUTE);
                    double confidence = responseObject.getDouble(API_RESPONSE_CONFIDENCE_ATTRIBUTE);
                    licensePlateTextView.setText(
                            String.format("%s (" + getResources()
                                            .getString(R.string.result_confidence_text) + ": %s)",
                                    number, confidence));
                } catch (IOException | JSONException e) {
                    showToast(R.string.api_communication_error_toast_message);
                    e.printStackTrace();
                }
            }
        } else if (response.code() == HttpURLConnection.HTTP_NOT_FOUND) {
            showToast(R.string.could_not_recognize_toast_message);
        } else if (response.code() == HttpURLConnection.HTTP_BAD_REQUEST ||
                response.code() == HttpURLConnection.HTTP_INTERNAL_ERROR) {
            showToast(R.string.api_communication_error_toast_message);
        }
    }

    private void showToast(int stringId) {
        // must be called from UI thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),
                        getResources().getString(stringId),
                        Toast.LENGTH_LONG)
                        .show();
            }
        });
    }
}