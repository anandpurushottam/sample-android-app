package com.vdocipher.sampleapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;
import com.vdocipher.aegis.player.VdoPlayer;
import com.vdocipher.aegis.player.VdoPlayerFragment;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class OnlinePlayerActivity extends AppCompatActivity implements VdoPlayer.OnInitializationListener {

    private final String TAG = "OnlinePlayerActivity";

    private VdoPlayer player;
    private VdoPlayerFragment playerFragment;
    private ImageButton playPauseButton;
    private TextView currTime, duration;
    private SeekBar seekBar;
    private ProgressBar bufferingIcon;

    private AsyncHttpClient client = new AsyncHttpClient();
    private String otp;
    private boolean isPlaying = false;
    private boolean controlsShowing = false;
    private boolean isFullscreen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate called");
        setContentView(R.layout.activity_online_player);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        seekBar = (SeekBar)findViewById(R.id.seekbar);
        seekBar.setEnabled(false);
        currTime = (TextView)findViewById(R.id.current_time);
        duration = (TextView)findViewById(R.id.duration);
        playerFragment = (VdoPlayerFragment)getFragmentManager().findFragmentById(R.id.online_vdo_player_fragment);
        playPauseButton = (ImageButton)findViewById(R.id.play_pause_button);
        bufferingIcon = (ProgressBar) findViewById(R.id.loading_icon);
        showLoadingIcon(false);
        showControls(false);

        if (savedInstanceState != null) {
            otp = savedInstanceState.getString("otp", null);
            Log.v(TAG, "otp: " + otp);
        }
        getSampleOtpAndStartPlayer();
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop called");
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (otp != null) outState.putString("otp", otp);
    }

    private void getSampleOtpAndStartPlayer() {
        final String videoId = "********";
        final String OTP_URL = "https://api.vdocipher.com/v2/otp/?video=" + videoId;
        RequestParams params = new RequestParams();
        params.put("clientSecretKey", "********");

        if (otp == null) {
            client.post(OTP_URL, params, new TextHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, String responseString) {
                    try {
                        JSONObject jObject = new JSONObject(responseString);
                        otp = jObject.getString("otp");
                        Log.v(TAG, "otp: " + otp);
                        // create vdoInitParams
                        VdoPlayer.VdoInitParams vdoParams1 = new VdoPlayer.VdoInitParams(otp, false, null, null);
                        // initialize vdoPlayerFragment with otp and a VdoPlayer.OnInitializationListener
                        playerFragment.initialize(vdoParams1, OnlinePlayerActivity.this);
                        showLoadingIcon(true);
                    } catch (JSONException e) {
                        Log.v(TAG, Log.getStackTraceString(e));
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    Log.v(TAG, "status code: " + responseString);
                }
            });
        } else {
            // create vdoInitParams
            VdoPlayer.VdoInitParams vdoParams1 = new VdoPlayer.VdoInitParams(otp, false, null, null);
            // initialize vdoPlayerFragment with otp and a VdoPlayer.OnInitializationListener
            playerFragment.initialize(vdoParams1, OnlinePlayerActivity.this);
            showLoadingIcon(true);
        }
    }

    private View.OnClickListener playPauseListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (player == null) return;
            if (isPlaying) {
                player.pause();
            } else {
                player.play();
            }
        }
    };

    private View.OnClickListener playerTapListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showControls(!controlsShowing);
        }
    };

    private void showControls(boolean show) {
        int visibility = show ? View.VISIBLE : View.INVISIBLE;
        playPauseButton.setVisibility(visibility);
        (findViewById(R.id.bottom_panel)).setVisibility(visibility);
        controlsShowing = show;
    }

    @Override
    public void onInitializationSuccess(VdoPlayer player, boolean wasRestored) {
        Log.v(TAG, "onInitializationSuccess");
        this.player = player;
        player.setOnPlaybackEventListener(playbackListener);
        Log.v(TAG, "player duration = " + player.getDuration());
        duration.setText(Utils.digitalClockTime(player.getDuration()));
        seekBar.setMax(player.getDuration());
        seekBar.setEnabled(true);
        seekBar.setOnSeekBarChangeListener(seekbarChangeListener);
        playPauseButton.setOnClickListener(playPauseListener);

        (findViewById(R.id.player_region)).setOnClickListener(playerTapListener);
        showControls(true);
    }

    @Override
    public void onInitializationFailure(VdoPlayer.InitializationResult result) {
        Log.v(TAG, "onInitializationFailure: " + result.name());
        Toast.makeText(OnlinePlayerActivity.this, "initialization failure: " + result.name(), Toast.LENGTH_LONG).show();
        showLoadingIcon(false);
    }

    private VdoPlayer.OnPlaybackEventListener playbackListener = new VdoPlayer.OnPlaybackEventListener() {
        @Override
        public void onPlaying() {
            Log.v(TAG, "onPlaying");
            isPlaying = true;
            playPauseButton.setImageResource(R.drawable.ic_action_pause_light);
        }

        @Override
        public void onPaused() {
            Log.v(TAG, "onPaused");
            isPlaying = false;
            playPauseButton.setImageResource(R.drawable.ic_action_play_light);
        }

        @Override
        public void onStopped() {
            Log.v(TAG, "onStopped");
            playPauseButton.setEnabled(false);
        }

        @Override
        public void onBuffering(boolean isBuffering) {
            Log.v(TAG, isBuffering ? "buffering started" : "buffering stopped");
            showLoadingIcon(isBuffering);
            playPauseButton.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onBufferUpdate(int bufferTime) {
            seekBar.setSecondaryProgress(bufferTime);
        }

        @Override
        public void onSeekTo(int millis) {
            Log.v(TAG, "onSeekTo: " + String.valueOf(millis));
        }

        @Override
        public void onProgress(int millis) {
            seekBar.setProgress(millis);
            currTime.setText(Utils.digitalClockTime(millis));
        }

        @Override
        public void onError(VdoPlayer.PlaybackErrorReason playbackErrorReason) {
            Log.e(TAG, playbackErrorReason.name());
        }
    };

    private void showLoadingIcon(final boolean showIcon) {
        if (showIcon) {
            bufferingIcon.setVisibility(View.VISIBLE);
            bufferingIcon.bringToFront();
        } else {
            bufferingIcon.setVisibility(View.INVISIBLE);
            bufferingIcon.requestLayout();
        }
    }

    private SeekBar.OnSeekBarChangeListener seekbarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {
            // nothing much to do here
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // nothing much to do here
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            player.seekTo(seekBar.getProgress());
        }
    };
}
