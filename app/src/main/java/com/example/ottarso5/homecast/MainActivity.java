package com.example.ottarso5.homecast;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import com.google.android.gms.cast.Cast.MessageReceivedCallback;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;

import java.io.IOException;

/**
 * Main activity to send messages to the receiver.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String baseUrl = "https://homecast.mybluemix.net/";

//    private static final int REQUEST_CODE_VOICE_RECOGNITION = 1;

    private CastContext mCastContext;
    private CastSession mCastSession;
    private HelloWorldChannel mHelloWorldChannel;

    private SessionManagerListener<CastSession> mSessionManagerListener
            = new SessionManagerListener<CastSession>() {
        @Override
        public void onSessionStarting(CastSession castSession) {
            // ignore
        }

        @Override
        public void onSessionStarted(CastSession castSession, String sessionId) {
            Log.d(TAG, "Session started");
            mCastSession = castSession;
            invalidateOptionsMenu();
            startCustomMessageChannel();
        }

        @Override
        public void onSessionStartFailed(CastSession castSession, int error) {
            // ignore
        }

        @Override
        public void onSessionEnding(CastSession castSession) {
            // ignore
        }

        @Override
        public void onSessionEnded(CastSession castSession, int error) {
            Log.d(TAG, "Session ended");
            if (mCastSession == castSession) {
                cleanupSession();
            }
            invalidateOptionsMenu();
        }

        @Override
        public void onSessionSuspended(CastSession castSession, int reason) {
            // ignore
        }

        @Override
        public void onSessionResuming(CastSession castSession, String sessionId) {
            // ignore
        }

        @Override
        public void onSessionResumed(CastSession castSession, boolean wasSuspended) {
            Log.d(TAG, "Session resumed");
            mCastSession = castSession;
            invalidateOptionsMenu();
        }

        @Override
        public void onSessionResumeFailed(CastSession castSession, int error) {
            // ignore
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup the CastContext
        mCastContext = CastContext.getSharedInstance(this);
        mCastContext.registerLifecycleCallbacksBeforeIceCreamSandwich(this, savedInstanceState);

        WebView webView = (WebView) findViewById(R.id.webview);
        webView.loadUrl(baseUrl);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        webView.addJavascriptInterface(new WebAppInterface(this), "android");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        // Setup the menu item for connecting to cast devices
        CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.media_route_menu_item);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register cast session listener
        mCastContext.getSessionManager().addSessionManagerListener(mSessionManagerListener,
                CastSession.class);
        if (mCastSession == null) {
            // Get the current session if there is one
            mCastSession = mCastContext.getSessionManager().getCurrentCastSession();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister cast session listener
        mCastContext.getSessionManager().removeSessionManagerListener(mSessionManagerListener,
                CastSession.class);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanupSession();
    }

    private void cleanupSession() {
        closeCustomMessageChannel();
        mCastSession = null;
    }

    private void startCustomMessageChannel() {
        if (mCastSession != null && mHelloWorldChannel == null) {
            mHelloWorldChannel = new HelloWorldChannel(getString(R.string.cast_namespace));
            try {
                mCastSession.setMessageReceivedCallbacks(mHelloWorldChannel.getNamespace(),
                        mHelloWorldChannel);
                Log.d(TAG, "Message channel started");
            } catch (IOException e) {
                Log.d(TAG, "Error starting message channel", e);
                mHelloWorldChannel = null;
            }
        }
    }

    private void closeCustomMessageChannel() {
        if (mCastSession != null && mHelloWorldChannel != null) {
            try {
                mCastSession.removeMessageReceivedCallbacks(mHelloWorldChannel.getNamespace());
                Log.d(TAG, "Message channel closed");
            } catch (IOException e) {
                Log.d(TAG, "Error closing message channel", e);
            } finally {
                mHelloWorldChannel = null;
            }
        }
    }

    /*
     * Send a message to the receiver app
     */
    private void sendMessage(String message) {
        if (mHelloWorldChannel != null) {
            mCastSession.sendMessage(mHelloWorldChannel.getNamespace(), message);
        } else {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Custom message channel
     */
    static class HelloWorldChannel implements MessageReceivedCallback {

        private final String mNamespace;

        /**
         * @param namespace the namespace used for sending messages
         */
        HelloWorldChannel(String namespace) {
            mNamespace = namespace;
        }

        /**
         * @return the namespace used for sending messages
         */
        public String getNamespace() {
            return mNamespace;
        }

        /*
         * Receive message from the receiver app
         */
        @Override
        public void onMessageReceived(CastDevice castDevice, String namespace, String message) {
            Log.d(TAG, "onMessageReceived: " + message);
        }

    }

}