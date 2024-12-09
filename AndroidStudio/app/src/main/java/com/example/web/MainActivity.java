package com.example.web;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.web.databinding.ActivityMainBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private boolean isFetchingToken = false;
    private Uri selectedImageUri;
    private ValueCallback<Uri[]> filePathCallback;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private boolean isFromNotification = false;
    private String sosLatitude;
    private String sosLongitude;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 사용자 ID 가져오기 (Intent 또는 SharedPreferences)
        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            userId = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("userId", "");
        }

        initView();
        FirebaseApp.initializeApp(this);
        requestNotificationPermission();
        requestLocationPermission();

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("url")) {
            String url = intent.getStringExtra("url");
            binding.webView.loadUrl("http://192.168.0.46:8080" + url);
        }
        handleIntent(getIntent()); // 처음 앱 실행 시 인텐트 데이터 확인
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initView() {
        WebSettings settings = binding.webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setDefaultTextEncodingName("UTF-8");
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        binding.webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        settings.setLoadsImagesAutomatically(true);  // 이미지 자동 로드 활성화
        settings.setJavaScriptEnabled(true);         // 자바스크립트 활성화

        binding.webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        binding.webView.setWebViewClient(new MyWebViewClient());
        binding.webView.setWebChromeClient(new MyWebChromeClient());
        // JavaScript 인터페이스 등록
        binding.webView.addJavascriptInterface(new JavascriptMethods(), "Android");
        binding.webView.loadUrl("http://172.30.1.53:8080/login");
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 2);
        } else {
            initLocation();
        }
    }

    private void showLocationPermissionRationale() {
        new AlertDialog.Builder(this)
                .setTitle("위치 권한 필요")
                .setMessage("앱에서 위치를 공유하려면 위치 접근 권한이 필요합니다.")
                .setPositiveButton("허용", (dialog, which) -> ActivityCompat.requestPermissions(
                        this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 2))
                .setNegativeButton("취소", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }


    private void initLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // LocationRequest를 Builder로 생성
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)  // 1초마다 위치 요청
                .setMinUpdateIntervalMillis(5000) // 최소 업데이트 간격 5초
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                for (Location location : locationResult.getLocations()) {
                    // 서버로 위치 정보 전송
                    sendLocationToServer(
                            location.getLatitude(),
                            location.getLongitude(),
                            location.hasAltitude() ? location.getAltitude() : null,
                            location.hasSpeed() ? location.getSpeed() : null,
                            location.hasBearing() ? location.getBearing() : null,
                            String.valueOf(System.currentTimeMillis()) // 예시: 현재 시간을 전달
                    );

                    // WebView 업데이트
                    binding.webView.post(() -> {
                        String jsCode = String.format(
                                "if(window.updateUserLocation) { window.updateUserLocation('%s', '%s'); }",
                                location.getLatitude(), location.getLongitude()
                        );
                        binding.webView.evaluateJavascript(jsCode, null);
                    });
                }
            }

        };
        // 위치 권한 확인 후 업데이트 시작
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }

    private void sendLocationToServer(double latitude, double longitude, Double altitude, Float speed, Float bearing, String time) {
        OkHttpClient client = new OkHttpClient();

        // 기록된 위치 정보를 전송하는 로직 (altitude, speed, bearing, time 모두 제공된 경우)
        if (altitude != null && speed != null && bearing != null && time != null) {
            String recordLocationUrl = "http://172.30.1.53:9000/api/recordlocation";
            String recordLocationJson = String.format(
                    "{\"latitude\":%f, \"longitude\":%f, \"altitude\":%f, \"speed\":%f, \"bearing\":%f, \"time\":\"%s\"}",
                    latitude, longitude, altitude, speed, bearing, time
            );

            RequestBody recordRequestBody = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"), recordLocationJson
            );
            Request recordRequest = new Request.Builder()
                    .url(recordLocationUrl)
                    .post(recordRequestBody)
                    .build();

            client.newCall(recordRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("MainActivity", "recordlocation 전송 실패: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        Log.d("MainActivity", "recordlocation 전송 성공: " + response.message());
                    } else {
                        Log.e("MainActivity", "recordlocation 전송 실패: " + response.message());
                    }
                    response.close();
                }
            });
        }

        // 사용자 ID가 없을 경우 로그 출력 후 메서드 종료
        if (userId == null || userId.isEmpty()) {
            Log.e("MainActivity", "userId가 설정되지 않았습니다.");
            return;
        }

        // 사용자 위치 정보를 서버에 전송하는 로직
        String locationUrl = "http://" + getNumber() + ".30.1.53:9000/api/location";
        String locationJson = String.format(
                "{\"userId\": \"%s\", \"latitude\": %f, \"longitude\": %f}", userId, latitude, longitude
        );

        RequestBody locationRequestBody = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), locationJson
        );
        Request locationRequest = new Request.Builder()
                .url(locationUrl)
                .post(locationRequestBody)
                .build();

        client.newCall(locationRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("MainActivity", "location 전송 실패: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d("MainActivity", "location 전송 성공: " + response.message());
                } else {
                    Log.e("MainActivity", "location 전송 실패: " + response.message());
                }
                response.close();
            }
        });
    }

    @NonNull
    private static String getNumber() {
        return "172";
    }


    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.hasExtra("latitude") && intent.hasExtra("longitude")) {
            sosLatitude = intent.getStringExtra("latitude");
            sosLongitude = intent.getStringExtra("longitude");
            isFromNotification = true; // 알림에서 실행됨을 표시
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
        displaySosLocationOnMap();

        String latitude = intent.getStringExtra("latitude");
        String longitude = intent.getStringExtra("longitude");

        if (latitude != null && longitude != null) {
            binding.webView.post(() -> {
                String jsCode = String.format(
                        "if (window.showLocationOnMap) { window.showLocationOnMap('%s', '%s'); }",
                        latitude, longitude
                );
                binding.webView.evaluateJavascript(jsCode, null);
            });
        }
    }

    private void addMarkerOnMap(String latitude, String longitude) {
        try {
            double lat = Double.parseDouble(latitude);
            double lon = Double.parseDouble(longitude);

            // 마커 추가 로직 (지도 연동 방식에 따라 구현)
            Log.d("MainActivity", "마커 추가: 위도=" + lat + ", 경도=" + lon);
        } catch (NumberFormatException e) {
            Log.e("MainActivity", "잘못된 좌표 값: " + latitude + ", " + longitude, e);
        }
    }


    //변경
    private void displaySosLocationOnMap() {
        if (isFromNotification && sosLatitude != null && sosLongitude != null) {
            binding.webView.post(() -> {
                String jsCode = String.format("if(window.showLocationOnMap) { window.showLocationOnMap('%s', '%s'); }", sosLatitude, sosLongitude);
                binding.webView.evaluateJavascript(jsCode, null);
            });
            isFromNotification = false;
        }
    }

    private void setupFileChooser() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (filePathCallback != null) {
                        Uri[] results = null;
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            results = new Uri[]{result.getData().getData()};
                        }
                        filePathCallback.onReceiveValue(results);
                        filePathCallback = null;
                    }
                }
        );
    }

    private class MyWebChromeClient extends WebChromeClient {
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                         FileChooserParams fileChooserParams) {
            if (MainActivity.this.filePathCallback != null) {
                MainActivity.this.filePathCallback.onReceiveValue(null);
            }
            MainActivity.this.filePathCallback = filePathCallback;

            Intent intent = fileChooserParams.createIntent();
            try {
                galleryLauncher.launch(intent);
            } catch (Exception e) {
                MainActivity.this.filePathCallback = null;
                Log.e("MainActivity", "파일 선택기 열기 실패", e);
                return false;
            }
            return true;
        }
    }

    private void sendImageUriToWebView(Uri imageUri) {
        String imageUriString = imageUri.toString();
        binding.webView.evaluateJavascript("window.handleSelectedImageUri('" + imageUriString + "');", null);
    }

    private void getFCMToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                String json = "{\"token\": \"\", \"permissionGranted\": false}";
                runOnUiThread(() -> binding.webView.evaluateJavascript(
                        "window.Android.onNotificationPermissionGranted('" + json + "');", null));
                return;
            }
            String token = task.getResult();
            String json = String.format("{\"token\": \"%s\", \"permissionGranted\": true}",
                    token != null ? token : "");
            runOnUiThread(() -> binding.webView.evaluateJavascript(
                    "window.Android.onNotificationPermissionGranted('" + json + "');", null));
        });
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            } else {
                requestFCMToken();
            }
        } else {
            requestFCMToken();
        }
    }

    private void requestFCMToken() {
        if (!isFetchingToken) {
            isFetchingToken = true;
            getFCMToken();
        }
    }

    //변경
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) { // 알림 권한 요청 코드
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestFCMToken();
            } else {
                String json = "{\"token\": \"\", \"permissionGranted\": false}";
                sendJsonToWebView(json);
            }
        } else if (requestCode == 2) { // 위치 권한 요청 코드
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initLocation();
            } else {
                Log.d("MainActivity", "위치 권한이 거부되었습니다.");
                // 위치 권한 거부 시 WebView에 알림 표시
                sendJsonToWebView("{\"error\": \"location_permission_denied\"}");
            }
        }
    }

    private void sendJsonToWebView(String json) {
        String escapedJson = json.replace("\"", "\\\"");
        binding.webView.evaluateJavascript(
                "window.Android.onNotificationPermissionGranted(\"" + escapedJson + "\");", null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    class MyWebViewClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            Log.d("MainActivity", "페이지 로드 완료: " + url);
        }

    }

    class JavascriptMethods {
        @JavascriptInterface
        // 추가
        public void sendUserIdToAndroid(String userId) {
            MainActivity.this.userId = userId; // WebView에서 전달된 userId 저장
            Log.d("MainActivity", "Received userId from WebView: " + userId);
        }
        //변경
        public void openNotificationSettings() {
            Intent intent = new Intent("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("android.provider.extra.APP_PACKAGE", getPackageName());
            startActivity(intent);
        }

        @JavascriptInterface
        public void openGallery() {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
        }

        @JavascriptInterface
        public void fetchFCMToken() {
            MainActivity.this.getFCMToken();
        }
    }
    // `onNotificationPermissionGranted` 함수 추가
    @JavascriptInterface
    public void onNotificationPermissionGranted(String json) {
        runOnUiThread(() -> {
            binding.webView.evaluateJavascript(
                    "window.onNotificationPermissionGranted(" + json + ");", null);
        });
    }
}