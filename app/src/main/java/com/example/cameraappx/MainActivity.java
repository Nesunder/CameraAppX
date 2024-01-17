package com.example.cameraappx;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    ImageButton capture, toggleFLash, flipCamera;
    private PreviewView previewView;
    int cameraFacing = CameraSelector.LENS_FACING_BACK;

    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
        if (result) {
            startCamera(cameraFacing);
        }
    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.cameraPreview);
        capture = findViewById(R.id.capture);
        toggleFLash = findViewById(R.id.toggleFlash);
        flipCamera = findViewById(R.id.flipCamera);

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED) {
            activityResultLauncher.launch(Manifest.permission.CAMERA);
        } else {
            startCamera(cameraFacing);
        }

        flipCamera.setOnClickListener(v -> {
        if (cameraFacing == CameraSelector.LENS_FACING_BACK) {
            cameraFacing = CameraSelector.LENS_FACING_FRONT;
        } else {
            cameraFacing = CameraSelector.LENS_FACING_BACK;
        }
        startCamera(cameraFacing);
    });

    }

    public void startCamera(int cameraFacing) {
        int aspectRatio = aspectRatio(previewView.getWidth(), previewView.getHeight());
        ListenableFuture<ProcessCameraProvider> listenableFuture = ProcessCameraProvider
                .getInstance(this);

        listenableFuture.addListener(() -> {
        try {
            ProcessCameraProvider cameraProvider = (ProcessCameraProvider) listenableFuture.get();
            Preview preview = new Preview.Builder().build();

            ImageCapture imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                .build();

            CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(cameraFacing).build();

            cameraProvider.unbindAll();

            Camera camera = cameraProvider.bindToLifecycle(this,
            cameraSelector, preview, imageCapture);

            capture.setOnClickListener(v -> takePicture(imageCapture));

            toggleFLash.setOnClickListener(v -> setFlashIcon(camera));

            preview.setSurfaceProvider(previewView.getSurfaceProvider());

            if (aspectRatio == 1) {
                previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
            } else {
                previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);
            }

        } catch (ExecutionException | InterruptedException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
    }

    }, ContextCompat.getMainExecutor(this));

    }

    private void setFlashIcon(Camera camera) {
        if (camera.getCameraInfo().hasFlashUnit()) {
            if (camera.getCameraInfo().getTorchState().getValue() != null
                && camera.getCameraInfo().getTorchState().getValue() == 0) {
                camera.getCameraControl().enableTorch(true);
                toggleFLash.setImageResource(R.drawable.baseline_flash_off_24);
            } else {
                camera.getCameraControl().enableTorch(false);
                toggleFLash.setImageResource(R.drawable.baseline_flash_on_24);
            }
        } else {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "FLash is not available currently", Toast.LENGTH_SHORT).show());
        }
    }

    private void takePicture(ImageCapture imageCapture) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, System.currentTimeMillis() + ".jpg");
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

        ImageCapture.OutputFileOptions outputFileOptions =
        new ImageCapture.OutputFileOptions.Builder(
                getContentResolver(),
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        .build();

        imageCapture.takePicture(outputFileOptions, Executors.newCachedThreadPool(), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Image saved", Toast.LENGTH_LONG).show());
                startCamera(cameraFacing);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to save photo " +
                exception.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
        //startCamera(cameraFacing);

    }

    private int aspectRatio(int width, int height) {
        double previewRatio = (double) Math.max(width, height) / Math.min(width, height);
        if (Math.abs(previewRatio - 4.0 / 3.0) <= Math.abs(previewRatio - 16.0 / 9.0)) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }
}