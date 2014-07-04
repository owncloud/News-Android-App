package de.luhmer.owncloudnewsreader.services;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.view.SurfaceHolder;

import java.io.IOException;

import de.greenrobot.event.EventBus;
import de.luhmer.owncloudnewsreader.events.podcast.OpenAudioPodcastEvent;
import de.luhmer.owncloudnewsreader.events.podcast.RegisterVideoOutput;
import de.luhmer.owncloudnewsreader.events.podcast.TogglePlayerStateEvent;
import de.luhmer.owncloudnewsreader.events.podcast.UpdatePodcastStatusEvent;
import de.luhmer.owncloudnewsreader.events.podcast.WindPodcast;
import de.luhmer.owncloudnewsreader.view.PodcastNotification;

public class PodcastPlaybackService extends Service {

    PodcastNotification podcastNotification;

    @Override
    public void onCreate() {
        podcastNotification = new PodcastNotification(this);

        super.onCreate();
    }

    public PodcastPlaybackService() {
        mediaPlayer = new MediaPlayer();
        mHandler = new Handler();
        eventBus = EventBus.getDefault();

        eventBus.register(this);



        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                play();
                isPreparing = false;
            }
        });

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                pause();//Send the over signal
            }
        });


        //openFile("/sdcard/Music/#Musik/Finest Tunes/Netsky - Running Low (Ft. Beth Ditto).mp3");
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    private EventBus eventBus;
    private Handler mHandler;
    private MediaPlayer mediaPlayer;
    private String mediaTitle;

    public static final int delay = 500; //In milliseconds

    private boolean isPreparing = false;

    public void openFile(String pathToFile, String mediaTitle) {
        try {
            this.mediaTitle = mediaTitle;

            if(mediaPlayer.isPlaying())
                pause();

            isPreparing = true;
            mHandler.postDelayed(mUpdateTimeTask, 0);

            mediaPlayer.reset();
            mediaPlayer.setDataSource(pathToFile);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
            isPreparing = false;
        }
    }

    /**
     * Background Runnable thread
     * */
    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            sendMediaStatus();

            mHandler.postDelayed(this, delay);
        }
    };

    public void onEvent(TogglePlayerStateEvent event) {
        if(mediaPlayer.isPlaying()) {
            pause();
        } else {
            play();
        }
    }

    public void onEvent(WindPodcast event) {
        if(mediaPlayer != null) {
            double totalDuration = mediaPlayer.getDuration();
            int position = (int)((totalDuration / 100d) * event.toPositionInPercent);
            mediaPlayer.seekTo(position);
        }
    }

    public void onEventBackgroundThread(OpenAudioPodcastEvent event) {
        openFile(event.pathToFile, event.mediaTitle);
    }

    public void onEvent(RegisterVideoOutput videoOutput) {
        if(mediaPlayer != null) {
            //getHolder().addCallback(videoOutput);
            videoOutput.surfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
    }




    public void play() {
        mediaPlayer.start();

        mHandler.removeCallbacks(mUpdateTimeTask);
        mHandler.postDelayed(mUpdateTimeTask, 0);
    }

    public void pause() {
        if(mediaPlayer.isPlaying())
            mediaPlayer.pause();

        mHandler.removeCallbacks(mUpdateTimeTask);

        sendMediaStatus();
    }

    public void sendMediaStatus() {
        long totalDuration = 0;
        long currentDuration = 0;
        if(!isPreparing) {
            totalDuration = mediaPlayer.getDuration();
            currentDuration = mediaPlayer.getCurrentPosition();
        }

            /*
            // Displaying Total Duration time
            songTotalDurationLabel.setText(""+utils.milliSecondsToTimer(totalDuration));
            // Displaying time completed playing
            songCurrentDurationLabel.setText(""+utils.milliSecondsToTimer(currentDuration));

            // Updating progress bar
            int progress = (int)(utils.getProgressPercentage(currentDuration, totalDuration));
            //Log.d("Progress", ""+progress);
            songProgressBar.setProgress(progress);
            */

        UpdatePodcastStatusEvent audioPodcastEvent = new UpdatePodcastStatusEvent(currentDuration, totalDuration, mediaPlayer.isPlaying(), mediaTitle, isPreparing);
        eventBus.post(audioPodcastEvent);
    }




    SurfaceHolder.Callback mSHCallback = new SurfaceHolder.Callback()
    {
        public void surfaceChanged(SurfaceHolder holder, int format,
                                   int w, int h)
        {
            mSurfaceWidth = w;
            mSurfaceHeight = h;
            if (mIsPrepared && mVideoWidth == w && mVideoHeight == h) {
                if (mSeekWhenPrepared != 0) {
                    mMediaPlayer.seekTo(mSeekWhenPrepared);
                }
                mMediaPlayer.start();
                if (mMediaController != null) {
                    mMediaController.show();
                }
            }
        }

        public void surfaceCreated(SurfaceHolder holder)
        {
            mSurfaceHolder = holder;
            openVideo();
        }

        public void surfaceDestroyed(SurfaceHolder holder)
        {
            // after we return from this we can't use the surface any more
            mSurfaceHolder = null;
            if (mMediaController != null) mMediaController.hide();
            if (mMediaPlayer != null) {
                mMediaPlayer.reset();
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
        }
    };


}