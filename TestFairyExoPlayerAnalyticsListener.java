package com.google.android.exoplayer2.demo;

import android.os.Handler;
import android.view.Surface;
import androidx.annotation.Nullable;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.testfairy.TestFairy;
import java.io.IOException;
import java.util.Locale;

public class TestFairyExoPlayerAnalyticsListener implements AnalyticsListener {

  private final Handler handler = new Handler();
  private final ExoPlayer player;

  private Runnable currentPositionTracker;
  private long lastKnownPlaybackPercent = -1;

  public TestFairyExoPlayerAnalyticsListener(ExoPlayer player) {
    this.player = player;
  }

  private void registerCurrentPositionTracker() {
    unRegisterCurrentPositionTracker();

    currentPositionTracker = new Runnable() {
      @Override
      public void run() {
        long currentPosition = player.getCurrentPosition();
        long percent = (currentPosition * 100) / player.getDuration();

        if (lastKnownPlaybackPercent != percent) {
          lastKnownPlaybackPercent = percent;
          TestFairy.addEvent(String.format(Locale.ENGLISH, "Playback position %d%%", percent));
        }

        handler.postDelayed(currentPositionTracker, 100);
      }
    };

    handler.postDelayed(currentPositionTracker, 100);
  }

  private void unRegisterCurrentPositionTracker() {
    if (currentPositionTracker != null) {
      handler.removeCallbacks(currentPositionTracker);
      currentPositionTracker = null;
    }
  }

  @Override
  public void onPlayerStateChanged(EventTime eventTime, boolean playWhenReady,
      int playbackState) {
    if (playWhenReady && playbackState == Player.STATE_READY) {
      if (currentPositionTracker == null) {
        registerCurrentPositionTracker();
      }

      TestFairy.addEvent("Playback is playing");
    } else if (playWhenReady) {
      unRegisterCurrentPositionTracker();

      if (player.getDuration() != 0 && player.getDuration() <= player.getCurrentPosition()) {
        TestFairy.addEvent("Playback has completed");
      } else {
        TestFairy.addEvent("Playback is buffering or paused automatically");
      }
    } else {
      unRegisterCurrentPositionTracker();

      TestFairy.addEvent("Playback is paused");
    }
  }

  @Override
  public void onTimelineChanged(EventTime eventTime, int reason) {
  }

  @Override
  public void onPositionDiscontinuity(EventTime eventTime, int reason) {
    switch (reason) {
      case SimpleExoPlayer.DISCONTINUITY_REASON_PERIOD_TRANSITION:
        TestFairy.addEvent("Video stutters due to period transition");
        break;
      case SimpleExoPlayer.DISCONTINUITY_REASON_SEEK:
        TestFairy.addEvent("Video stutters due to a seek");
        break;
      case SimpleExoPlayer.DISCONTINUITY_REASON_SEEK_ADJUSTMENT:
        TestFairy.addEvent("Video stutters due to seel adjustment");
        break;
      case SimpleExoPlayer.DISCONTINUITY_REASON_AD_INSERTION:
        TestFairy.addEvent("Video stutters due to an inserted ad");
        break;
      case SimpleExoPlayer.DISCONTINUITY_REASON_INTERNAL:
        TestFairy.addEvent("Video stutters due to an internal problem");
        break;
    }
  }

  @Override
  public void onSeekStarted(EventTime eventTime) {
  }

  @Override
  public void onSeekProcessed(EventTime eventTime) {
    long currentPosition = player.getCurrentPosition();
    long percent = (currentPosition * 100) / player.getDuration();

    lastKnownPlaybackPercent = percent;

    TestFairy
        .addEvent(String.format(Locale.ENGLISH, "Playback seeks to position %d%%", percent));
  }

  @Override
  public void onPlaybackParametersChanged(EventTime eventTime,
      PlaybackParameters playbackParameters) {
  }

  @Override
  public void onRepeatModeChanged(EventTime eventTime, int repeatMode) {
    switch (repeatMode) {
      case ExoPlayer.REPEAT_MODE_OFF:
        TestFairy.addEvent("Repeat mode has been changed to OFF");
      case ExoPlayer.REPEAT_MODE_ONE:
        TestFairy.addEvent("Repeat mode has been changed to ONE");
      case ExoPlayer.REPEAT_MODE_ALL:
        TestFairy.addEvent("Repeat mode has been changed to ALL");
        break;
    }
  }

  @Override
  public void onShuffleModeChanged(EventTime eventTime, boolean shuffleModeEnabled) {
    if (shuffleModeEnabled) {
      TestFairy.addEvent("Shuffle mode is enabled");
    } else {
      TestFairy.addEvent("Shuffle mode is disabled");
    }
  }

  @Override
  public void onLoadingChanged(EventTime eventTime, boolean isLoading) {
  }

  @Override
  public void onPlayerError(EventTime eventTime, ExoPlaybackException error) {
    if (error.type == ExoPlaybackException.TYPE_SOURCE) {
      IOException cause = error.getSourceException();

      if (cause instanceof HttpDataSource.HttpDataSourceException) {
        // An HTTP error occurred.
        HttpDataSource.HttpDataSourceException httpError = (HttpDataSource.HttpDataSourceException) cause;

        // It's possible to find out more about the error both by casting and by
        // querying the cause.
        if (httpError instanceof HttpDataSource.InvalidResponseCodeException) {
          // Cast to InvalidResponseCodeException and retrieve the response code,
          // message and headers.
          HttpDataSource.InvalidResponseCodeException ex = (HttpDataSource.InvalidResponseCodeException) httpError;

          TestFairy.addEvent(
              String.format(Locale.ENGLISH, "Http error during playback - %d", ex.responseCode));
          TestFairy.logThrowable(cause);
        } else {
          // Try calling httpError.getCause() to retrieve the underlying cause,
          // although note that it may be null.
          TestFairy.addEvent("Http error during playback before response");

          Throwable innerCause = httpError.getCause();
          if (innerCause != null) {
            TestFairy.logThrowable(innerCause);
          } else {
            TestFairy.logThrowable(cause);
          }
        }
      } else {
        TestFairy.addEvent(String.format(Locale.ENGLISH, "Player error - %s", error.toString()));
        TestFairy.logThrowable(cause);
      }
    }
  }

  @Override
  public void onTracksChanged(EventTime eventTime, TrackGroupArray trackGroups,
      TrackSelectionArray trackSelections) {
    lastKnownPlaybackPercent = -1;
    TestFairy.addEvent("A new video has been loaded");
  }

  @Override
  public void onLoadStarted(EventTime eventTime,
      MediaSourceEventListener.LoadEventInfo loadEventInfo,
      MediaSourceEventListener.MediaLoadData mediaLoadData) {
  }

  @Override
  public void onLoadCompleted(EventTime eventTime,
      MediaSourceEventListener.LoadEventInfo loadEventInfo,
      MediaSourceEventListener.MediaLoadData mediaLoadData) {
    TestFairy.addEvent(
        String.format(Locale.ENGLISH, "Playback is buffering %d%%", player.getBufferedPercentage())
    );
  }

  @Override
  public void onLoadCanceled(EventTime eventTime,
      MediaSourceEventListener.LoadEventInfo loadEventInfo,
      MediaSourceEventListener.MediaLoadData mediaLoadData) {
  }

  @Override
  public void onLoadError(EventTime eventTime,
      MediaSourceEventListener.LoadEventInfo loadEventInfo,
      MediaSourceEventListener.MediaLoadData mediaLoadData, IOException error,
      boolean wasCanceled) {
    TestFairy.addEvent("Error during loading");
    TestFairy.logThrowable(error);
  }

  @Override
  public void onDownstreamFormatChanged(EventTime eventTime,
      MediaSourceEventListener.MediaLoadData mediaLoadData) {
  }

  @Override
  public void onUpstreamDiscarded(EventTime eventTime,
      MediaSourceEventListener.MediaLoadData mediaLoadData) {
  }

  @Override
  public void onMediaPeriodCreated(EventTime eventTime) {
  }

  @Override
  public void onMediaPeriodReleased(EventTime eventTime) {
  }

  @Override
  public void onReadingStarted(EventTime eventTime) {
  }

  @Override
  public void onBandwidthEstimate(EventTime eventTime, int totalLoadTimeMs,
      long totalBytesLoaded, long bitrateEstimate) {
  }

  @Override
  public void onSurfaceSizeChanged(EventTime eventTime, int width, int height) {
  }

  @Override
  public void onMetadata(EventTime eventTime, Metadata metadata) {
  }

  @Override
  public void onDecoderEnabled(EventTime eventTime, int trackType,
      DecoderCounters decoderCounters) {
  }

  @Override
  public void onDecoderInitialized(EventTime eventTime, int trackType, String decoderName,
      long initializationDurationMs) {
  }

  @Override
  public void onDecoderInputFormatChanged(EventTime eventTime, int trackType, Format format) {
  }

  @Override
  public void onDecoderDisabled(EventTime eventTime, int trackType,
      DecoderCounters decoderCounters) {
  }

  @Override
  public void onAudioSessionId(EventTime eventTime, int audioSessionId) {
  }

  @Override
  public void onAudioAttributesChanged(EventTime eventTime, AudioAttributes audioAttributes) {
  }

  @Override
  public void onVolumeChanged(EventTime eventTime, float volume) {
    TestFairy.addEvent(String.format(Locale.ENGLISH, "Volume level has changed to %s",
        Float.valueOf(volume * 100f).intValue()));
  }

  @Override
  public void onAudioUnderrun(EventTime eventTime, int bufferSize, long bufferSizeMs,
      long elapsedSinceLastFeedMs) {
  }

  @Override
  public void onDroppedVideoFrames(EventTime eventTime, int droppedFrames, long elapsedMs) {
    TestFairy.addEvent(
        String.format(Locale.ENGLISH, "Video has dropped %d frames in %dms", droppedFrames,
            elapsedMs));
  }

  @Override
  public void onVideoSizeChanged(EventTime eventTime, int width, int height,
      int unappliedRotationDegrees, float pixelWidthHeightRatio) {
  }

  @Override
  public void onRenderedFirstFrame(EventTime eventTime, @Nullable Surface surface) {
  }

  @Override
  public void onDrmSessionAcquired(EventTime eventTime) {
  }

  @Override
  public void onDrmKeysLoaded(EventTime eventTime) {
  }

  @Override
  public void onDrmSessionManagerError(EventTime eventTime, Exception error) {
    TestFairy.addEvent("Drm session manager error occured");
    TestFairy.logThrowable(error);
  }

  @Override
  public void onDrmKeysRestored(EventTime eventTime) {
  }

  @Override
  public void onDrmKeysRemoved(EventTime eventTime) {
  }

  @Override
  public void onDrmSessionReleased(EventTime eventTime) {
  }
}
