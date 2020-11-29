package ru.nntu.lprserver;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
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

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private ProgressBar progressBar;

    private ImageView cameraImageView;

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
            // handle it
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            try {
                Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
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
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                // TODO: extract to constants
                .addFormDataPart("license_plate_image", "license_plate_image.jpg",
                        RequestBody.create(MediaType.parse("image/*jpg"), imageData))
                .addFormDataPart("country_code", "us")
                .build();
        // TODO: make url configurable
        HttpUrl localUrl = HttpUrl.parse("http://192.168.0.248:8090/lpr/recognize");
        Request request = new Request.Builder()
                .url(localUrl)
                .post(requestBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                licensePlateTextView.setText(call.toString());
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
                    String number = responseObject.getString("number");
                    double confidence = responseObject.getDouble("confidence");
                    licensePlateTextView.setText(
                            String.format("%s (confidence: %s)", number, confidence));
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
        } else if (response.code() == HttpURLConnection.HTTP_NOT_FOUND) {
            // TODO: use text values from strings.xml
            Toast.makeText(getApplicationContext(), "Could not recognize license plate",
                    Toast.LENGTH_LONG).show();
        } else if (response.code() == HttpURLConnection.HTTP_BAD_REQUEST ||
                response.code() == HttpURLConnection.HTTP_INTERNAL_ERROR) {
            Toast.makeText(getApplicationContext(), "Internal error happened",
                    Toast.LENGTH_LONG).show();
        }
    }
}