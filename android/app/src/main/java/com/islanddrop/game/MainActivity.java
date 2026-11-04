package com.islanddrop.game;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.content.res.AssetManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.io.InputStream;

/**
 * Full-screen WebView that plays the game. Every copy of the app runs a small
 * game server (LanServer) so it can host a Wi-Fi match or serve the game to friends.
 */
public class MainActivity extends Activity {

    private static LanServer server;
    private WebView web;
    private WifiManager.MulticastLock multicastLock;
    private PermissionRequest pendingMic;
    private static final int MIC_REQUEST = 7;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        startServer();

        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            multicastLock = wifi.createMulticastLock("islanddrop");
            multicastLock.setReferenceCounted(false);
            multicastLock.acquire();
        }

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        handleMicRequest(request);
                    }
                });
            }
        });
        web.addJavascriptInterface(new Bridge(), "Lan");
        setContentView(web);
        hideSystemUi();
        web.loadUrl("http://127.0.0.1:" + LanServer.PORT + "/");
    }

    /** Voice chat: the page asks for the microphone, so ask Android for it first. */
    private void handleMicRequest(PermissionRequest request) {
        boolean wantsMic = false;
        for (String r : request.getResources()) {
            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(r)) wantsMic = true;
        }
        if (!wantsMic) {
            request.deny();
            return;
        }
        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (pendingMic != null) pendingMic.deny();
            pendingMic = request;
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_REQUEST);
            return;
        }
        request.grant(new String[]{PermissionRequest.RESOURCE_AUDIO_CAPTURE});
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode != MIC_REQUEST || pendingMic == null) return;
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            pendingMic.grant(new String[]{PermissionRequest.RESOURCE_AUDIO_CAPTURE});
        } else {
            pendingMic.deny();
        }
        pendingMic = null;
    }

    private void startServer() {
        if (server != null && server.isRunning()) return;
        final AssetManager assets = getAssets();
        server = new LanServer(new LanServer.Assets() {
            public InputStream open(String path) throws IOException {
                return assets.open("www/" + path);
            }
        }, Build.MODEL);
        try {
            server.start();
        } catch (IOException e) {
            // Port already in use: an earlier copy of the server is still serving the game.
        }
    }

    @SuppressWarnings("deprecation")
    private void hideSystemUi() {
        if (web == null) return;
        web.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUi();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (web != null) web.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) web.onResume();
        hideSystemUi();
    }

    @Override
    protected void onDestroy() {
        if (multicastLock != null && multicastLock.isHeld()) multicastLock.release();
        if (web != null) web.destroy();
        if (isFinishing() && server != null) {
            server.stop();
            server = null;
        }
        super.onDestroy();
    }

    /** Exposed to the game page as window.Lan. */
    public class Bridge {
        @JavascriptInterface
        public String getIp() {
            return LanServer.localIp();
        }

        @JavascriptInterface
        public String scan() {
            return LanServer.scan(2200);
        }

        /** Voice chat can switch the phone to the quiet earpiece; keep game sound on the loudspeaker. */
        @JavascriptInterface
        public void speaker() {
            runOnUiThread(new Runnable() {
                @SuppressWarnings("deprecation")
                public void run() {
                    AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    if (am != null) am.setSpeakerphoneOn(true);
                }
            });
        }
    }
}
