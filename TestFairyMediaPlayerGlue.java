package com.testfairy.tvtest.testfairy;

import android.media.MediaPlayer;
import android.media.MediaTimestamp;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v17.leanback.media.MediaPlayerAdapter;
import android.support.v17.leanback.media.PlayerAdapter;
import android.util.Log;
import com.testfairy.TestFairy;

// A glue class that can gather events from most commonly used media player libraries in Android. Gathered events are
// by default sent to TestFairy if a session is found.
//
// Example usage:
//
//   android.media.MediaPlayer myMediaPlayer = ...;
//   TestFairyMediaPlayerGlue.PlayerWrapper wrapper = TestFairyMediaPlayerGlue.createByWrapping(myMediaPlayer);
//
// or
//
//   PlaybackVideoFragment myPlaybackVideoFragment = ...;
//   VideoSupportFragmentGlueHost myGlueHost = VideoSupportFragmentGlueHost(myPlaybackVideoFragment);
//   MediaPlayerAdapter myPlayerAdapter = MediaPlayerAdapter(myPlaybackVideoFragment.getContext());
//   PlaybackTransportControlGlue transportGlue = PlaybackTransportControlGlue(myPlaybackVideoFragment.getActivity(), myPlayerAdapter);
//   transportGlue.setHost(myGlueHost);
//   TestFairyMediaPlayerGlue.PlayerWrapper wrapper = TestFairyMediaPlayerGlue.createByWrapping(myPlayerAdapter);
//
//
// Returned wrappers are able to assign additional listeners for all captured events without breaking the internal
// functionality and the TestFairy bridge.
//
public final class TestFairyMediaPlayerGlue {

    // State
    private final CreatedFrom createdFrom;
    private TestFairyBridge testFairyBridge;
    //////////////////////////////////////////////////////////////

    // Private constructor for internal use, includes bridging logic
    private TestFairyMediaPlayerGlue(CreatedFrom createdFrom) {
        // Source of creation (player or adapter or some other creation mechanism)
        this.createdFrom = createdFrom;

        // Default bridge, can be overridden with a setter
        this.testFairyBridge = new TestFairyBridge() {
            @Override
            public void onBufferingUpdate(int percent) {
                TestFairy.addEvent(String.format("Video Buffering: %%%d", percent));
            }

            @Override
            public void onPlaybackStateChange(boolean isPlaying) {
                TestFairy.addEvent(String.format("Video is %s", isPlaying ? "playing" : "paused"));
            }

            @Override
            public void onPlaybackPositionUpdate(int percent) {
                TestFairy.addEvent(String.format("Video position: %%%d", percent));
            }

            @Override
            public void onComplete() {
                TestFairy.addEvent("Video complete");
            }

            @Override
            public void onError(int reason, Object extra) {
                TestFairy.addEvent(String.format("Video error: Reason: %d - Extra: %s", reason, extra != null ? extra.toString() : "null"));
            }
        };
    }
    //////////////////////////////////////////////////////////////

    // Factories
    public static TestFairyMediaPlayerGlue.PlayerWrapper createByWrapping(@NonNull final MediaPlayer mediaPlayer) {
        if (mediaPlayer == null) {
            throw new NullPointerException("MediaPlayer cannot be null.");
        }

        final TestFairyMediaPlayerGlue listener = new TestFairyMediaPlayerGlue(new CreatedFrom.FromMediaPlayer() {
            private final Handler handler = new Handler();
            private Runnable currentPositionTracker;

            @Override
            protected void registerCurrentPositionTracker(final Runnable runnable) {
                unRegisterCurrentPositionTracker();

                currentPositionTracker = new Runnable() {
                    @Override
                    public void run() {
                        runnable.run();
                        handler.postDelayed(currentPositionTracker, 100);
                    }
                };

                handler.postDelayed(currentPositionTracker, 100);
            }

            @Override
            protected void unRegisterCurrentPositionTracker() {
                if (currentPositionTracker != null) {
                    handler.removeCallbacks(currentPositionTracker);
                    currentPositionTracker = null;
                }
            }

            @Override
            protected MediaPlayer getMediaPlayer() {
                return mediaPlayer;
            }
        });

        final CreatedFrom.FromMediaPlayer castedCreationMethod = (CreatedFrom.FromMediaPlayer) listener.createdFrom;
        castedCreationMethod.registerCurrentPositionTracker(CreatedFrom.FromMediaPlayer.createPositionTracker(mediaPlayer, listener));

        final PlayerWrapperImpl playerWrapper = listener.createPlayerWrapper();

        mediaPlayer.setOnBufferingUpdateListener(playerWrapper);
        mediaPlayer.setOnCompletionListener(playerWrapper);
        mediaPlayer.setOnErrorListener(playerWrapper);
        mediaPlayer.setOnMediaTimeDiscontinuityListener(playerWrapper);
        mediaPlayer.setOnSeekCompleteListener(playerWrapper);

        if (TestFairy.getSessionUrl() == null) {
            Log.w("TestFairyMediaPlayerGlue", "Media player events will not be sent unless you call TestFairy.begin()");
        }

        return playerWrapper;
    }

    public static PlayerAdapterWrapper createByWrapping(@NonNull final MediaPlayerAdapter playerAdapter) {
        if (playerAdapter == null) {
            throw new NullPointerException("MediaPlayerAdapter cannot be null.");
        }

        final TestFairyMediaPlayerGlue listener = new TestFairyMediaPlayerGlue(new CreatedFrom.FromMediaPlayerAdapter() {
            @Override
            protected void registerCurrentPositionTracker(Runnable _) {
                unRegisterCurrentPositionTracker();
                playerAdapter.setProgressUpdatingEnabled(true);
            }

            @Override
            protected void unRegisterCurrentPositionTracker() {
                playerAdapter.setProgressUpdatingEnabled(false);
            }
        });

        final PlayerAdapterWrapperImpl callbacksWrapper = listener.createPlayerAdapterCallbacksWrapper(playerAdapter.getCallback());

        playerAdapter.setCallback(callbacksWrapper);

        if (TestFairy.getSessionUrl() == null) {
            Log.w("TestFairyMediaPlayerGlue", "Media player events will not be sent unless you call TestFairy.begin()");
        }

        return callbacksWrapper;
    }
    //////////////////////////////////////////////////////////////

    // Wrapper creation
    private static abstract class PlayerWrapperImpl implements PlayerWrapper, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener,
            MediaPlayer.OnErrorListener, MediaPlayer.OnMediaTimeDiscontinuityListener, MediaPlayer.OnSeekCompleteListener {
    }

    private PlayerWrapperImpl createPlayerWrapper() {
        return new PlayerWrapperImpl() {

            private int lastKnownBufferingPercent = -1;

            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                if (createdFrom instanceof CreatedFrom.FromMediaPlayer) {
                    CreatedFrom.FromMediaPlayer castedCreationMethod = (CreatedFrom.FromMediaPlayer) createdFrom;

                    if (castedCreationMethod.onBufferingUpdateListener != null) {
                        castedCreationMethod.onBufferingUpdateListener.onBufferingUpdate(mp, percent);
                    }
                }

                if (lastKnownBufferingPercent != percent && testFairyBridge != null) {
                    testFairyBridge.onBufferingUpdate(percent);
                }
                lastKnownBufferingPercent = percent;
            }

            @Override
            public void onCompletion(MediaPlayer mp) {
                if (createdFrom instanceof CreatedFrom.FromMediaPlayer) {
                    CreatedFrom.FromMediaPlayer castedCreationMethod = (CreatedFrom.FromMediaPlayer) createdFrom;

                    if (castedCreationMethod.onCompletionListener != null) {
                        castedCreationMethod.onCompletionListener.onCompletion(mp);
                    }
                }

                if (testFairyBridge != null) {
                    testFairyBridge.onComplete();
                }
            }

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                boolean onErrorResult = false;

                if (createdFrom instanceof CreatedFrom.FromMediaPlayer) {
                    CreatedFrom.FromMediaPlayer castedCreationMethod = (CreatedFrom.FromMediaPlayer) createdFrom;

                    if (castedCreationMethod.onErrorListener != null) {
                        onErrorResult = castedCreationMethod.onErrorListener.onError(mp, what, extra);
                    }
                }

                if (testFairyBridge != null) {
                    testFairyBridge.onError(what, extra);
                }

                return onErrorResult;
            }

            private boolean lastKnownPlaybackStateIsPlaying = false;

            @Override
            public void onMediaTimeDiscontinuity(MediaPlayer mp, MediaTimestamp mts) {
                if (createdFrom instanceof CreatedFrom.FromMediaPlayer) {
                    CreatedFrom.FromMediaPlayer castedCreationMethod = (CreatedFrom.FromMediaPlayer) createdFrom;

                    if (castedCreationMethod.onMediaTimeDiscontinuityListener != null) {
                        castedCreationMethod.onMediaTimeDiscontinuityListener.onMediaTimeDiscontinuity(mp, mts);
                    }
                }

                if (mp.isPlaying() != lastKnownPlaybackStateIsPlaying && testFairyBridge != null) {
                    testFairyBridge.onPlaybackStateChange(mp.isPlaying());
                }
                lastKnownPlaybackStateIsPlaying = mp.isPlaying();
            }

            @Override
            public void onSeekComplete(MediaPlayer mp) {
                if (createdFrom instanceof CreatedFrom.FromMediaPlayer) {
                    CreatedFrom.FromMediaPlayer castedCreationMethod = (CreatedFrom.FromMediaPlayer) createdFrom;

                    if (castedCreationMethod.onSeekCompleteListener != null) {
                        castedCreationMethod.onSeekCompleteListener.onSeekComplete(mp);
                    }
                }
            }

            @Override
            public void setOnBufferingUpdateListener(MediaPlayer.OnBufferingUpdateListener onBufferingUpdateListener) {
                if (createdFrom instanceof CreatedFrom.FromMediaPlayer) {
                    CreatedFrom.FromMediaPlayer castedCreationMethod = (CreatedFrom.FromMediaPlayer) createdFrom;

                    castedCreationMethod.onBufferingUpdateListener = onBufferingUpdateListener;
                }
            }

            @Override
            public void setOnCompletionListener(MediaPlayer.OnCompletionListener onCompletionListener) {
                if (createdFrom instanceof CreatedFrom.FromMediaPlayer) {
                    CreatedFrom.FromMediaPlayer castedCreationMethod = (CreatedFrom.FromMediaPlayer) createdFrom;

                    castedCreationMethod.onCompletionListener = onCompletionListener;
                }
            }

            @Override
            public void setOnErrorListener(MediaPlayer.OnErrorListener onErrorListener) {
                if (createdFrom instanceof CreatedFrom.FromMediaPlayer) {
                    CreatedFrom.FromMediaPlayer castedCreationMethod = (CreatedFrom.FromMediaPlayer) createdFrom;

                    castedCreationMethod.onErrorListener = onErrorListener;
                }
            }

            @Override
            public void setOnMediaTimeDiscontinuityListener(MediaPlayer.OnMediaTimeDiscontinuityListener onMediaTimeDiscontinuityListener) {
                if (createdFrom instanceof CreatedFrom.FromMediaPlayer) {
                    CreatedFrom.FromMediaPlayer castedCreationMethod = (CreatedFrom.FromMediaPlayer) createdFrom;

                    castedCreationMethod.onMediaTimeDiscontinuityListener = onMediaTimeDiscontinuityListener;
                }
            }

            @Override
            public void setOnSeekCompleteListener(MediaPlayer.OnSeekCompleteListener onSeekCompleteListener) {
                if (createdFrom instanceof CreatedFrom.FromMediaPlayer) {
                    CreatedFrom.FromMediaPlayer castedCreationMethod = (CreatedFrom.FromMediaPlayer) createdFrom;

                    castedCreationMethod.onSeekCompleteListener = onSeekCompleteListener;
                }
            }

            @Override
            public void trackPlaybackPosition() {
                if (createdFrom instanceof CreatedFrom.FromMediaPlayer) {
                    CreatedFrom.FromMediaPlayer castedCreationMethod = (CreatedFrom.FromMediaPlayer) createdFrom;

                    TestFairyMediaPlayerGlue.this.createdFrom.registerCurrentPositionTracker(
                            CreatedFrom.FromMediaPlayer.createPositionTracker(
                                    castedCreationMethod.getMediaPlayer(),
                                    TestFairyMediaPlayerGlue.this
                            )
                    );
                }
            }

            @Override
            public void untrackPlaybackPosition() {
                TestFairyMediaPlayerGlue.this.createdFrom.unRegisterCurrentPositionTracker();
            }

            @Override
            public void setTestFairyBridge(TestFairyBridge bridge) {
                testFairyBridge = bridge;
            }

            @Override
            public TestFairyBridge getTestFairyBridge() {
                return testFairyBridge;
            }
        };
    }

    private static abstract class PlayerAdapterWrapperImpl extends PlayerAdapter.Callback implements PlayerAdapterWrapper {
    }

    private PlayerAdapterWrapperImpl createPlayerAdapterCallbacksWrapper(final PlayerAdapter.Callback originalCallbacks) {
        return new PlayerAdapterWrapperImpl() {

            private boolean lastKnownPlaybackStateIsPlaying = false;

            @Override
            public void onPlayStateChanged(PlayerAdapter adapter) {
                if (createdFrom instanceof CreatedFrom.FromMediaPlayerAdapter) {
                    CreatedFrom.FromMediaPlayerAdapter castedCreationMethod = (CreatedFrom.FromMediaPlayerAdapter) createdFrom;

                    if (castedCreationMethod.playerAdapterListenerCallbacks != null) {
                        castedCreationMethod.playerAdapterListenerCallbacks.onPlayStateChanged(adapter);
                    }

                    originalCallbacks.onPlayStateChanged(adapter);
                }


                if (adapter.isPlaying() != lastKnownPlaybackStateIsPlaying && testFairyBridge != null) {
                    testFairyBridge.onPlaybackStateChange(adapter.isPlaying());
                }
                lastKnownPlaybackStateIsPlaying = adapter.isPlaying();
            }

            @Override
            public void onPreparedStateChanged(PlayerAdapter adapter) {
                if (createdFrom instanceof CreatedFrom.FromMediaPlayerAdapter) {
                    CreatedFrom.FromMediaPlayerAdapter castedCreationMethod = (CreatedFrom.FromMediaPlayerAdapter) createdFrom;

                    if (castedCreationMethod.playerAdapterListenerCallbacks != null) {
                        castedCreationMethod.playerAdapterListenerCallbacks.onPreparedStateChanged(adapter);
                    }

                    originalCallbacks.onPreparedStateChanged(adapter);
                }
            }

            @Override
            public void onPlayCompleted(PlayerAdapter adapter) {
                if (createdFrom instanceof CreatedFrom.FromMediaPlayerAdapter) {
                    CreatedFrom.FromMediaPlayerAdapter castedCreationMethod = (CreatedFrom.FromMediaPlayerAdapter) createdFrom;

                    if (castedCreationMethod.playerAdapterListenerCallbacks != null) {
                        castedCreationMethod.playerAdapterListenerCallbacks.onPlayCompleted(adapter);
                    }

                    originalCallbacks.onPlayCompleted(adapter);
                }

                if (testFairyBridge != null) {
                    testFairyBridge.onComplete();
                }
            }

            private int lastKnownPlaybackPercent = -1;
            @Override
            public void onCurrentPositionChanged(PlayerAdapter adapter) {
                if (createdFrom instanceof CreatedFrom.FromMediaPlayerAdapter) {
                    CreatedFrom.FromMediaPlayerAdapter castedCreationMethod = (CreatedFrom.FromMediaPlayerAdapter) createdFrom;

                    if (castedCreationMethod.playerAdapterListenerCallbacks != null) {
                        castedCreationMethod.playerAdapterListenerCallbacks.onCurrentPositionChanged(adapter);
                    }

                    originalCallbacks.onCurrentPositionChanged(adapter);
                }

                if (adapter.getDuration() != 0) {
                    long currentPosition = adapter.getCurrentPosition();
                    long percent = (currentPosition * 100) / adapter.getDuration();

                    if (lastKnownPlaybackPercent != percent && testFairyBridge != null) {
                        testFairyBridge.onPlaybackPositionUpdate((int) percent);
                    }
                    lastKnownPlaybackPercent = (int) percent;
                }
            }

            private int lastKnownBufferingPercent = -1;

            @Override
            public void onBufferedPositionChanged(PlayerAdapter adapter) {
                if (createdFrom instanceof CreatedFrom.FromMediaPlayerAdapter) {
                    CreatedFrom.FromMediaPlayerAdapter castedCreationMethod = (CreatedFrom.FromMediaPlayerAdapter) createdFrom;

                    if (castedCreationMethod.playerAdapterListenerCallbacks != null) {
                        castedCreationMethod.playerAdapterListenerCallbacks.onBufferedPositionChanged(adapter);
                    }

                    originalCallbacks.onBufferedPositionChanged(adapter);
                }

                if (adapter.getDuration() != 0) {
                    long currentPosition = adapter.getBufferedPosition();
                    long percent = (currentPosition * 100) / adapter.getDuration();

                    if (lastKnownBufferingPercent != percent && testFairyBridge != null) {
                        testFairyBridge.onBufferingUpdate((int) percent);
                    }
                    lastKnownBufferingPercent = (int) percent;
                }
            }

            @Override
            public void onDurationChanged(PlayerAdapter adapter) {
                if (createdFrom instanceof CreatedFrom.FromMediaPlayerAdapter) {
                    CreatedFrom.FromMediaPlayerAdapter castedCreationMethod = (CreatedFrom.FromMediaPlayerAdapter) createdFrom;

                    if (castedCreationMethod.playerAdapterListenerCallbacks != null) {
                        castedCreationMethod.playerAdapterListenerCallbacks.onDurationChanged(adapter);
                    }

                    originalCallbacks.onDurationChanged(adapter);
                }
            }

            @Override
            public void onVideoSizeChanged(PlayerAdapter adapter, int width, int height) {
                if (createdFrom instanceof CreatedFrom.FromMediaPlayerAdapter) {
                    CreatedFrom.FromMediaPlayerAdapter castedCreationMethod = (CreatedFrom.FromMediaPlayerAdapter) createdFrom;

                    if (castedCreationMethod.playerAdapterListenerCallbacks != null) {
                        castedCreationMethod.playerAdapterListenerCallbacks.onVideoSizeChanged(adapter, width, height);
                    }

                    originalCallbacks.onVideoSizeChanged(adapter, width, height);
                }
            }

            @Override
            public void onError(PlayerAdapter adapter, int errorCode, String errorMessage) {
                if (createdFrom instanceof CreatedFrom.FromMediaPlayerAdapter) {
                    CreatedFrom.FromMediaPlayerAdapter castedCreationMethod = (CreatedFrom.FromMediaPlayerAdapter) createdFrom;

                    if (castedCreationMethod.playerAdapterListenerCallbacks != null) {
                        castedCreationMethod.playerAdapterListenerCallbacks.onError(adapter, errorCode, errorMessage);
                    }

                    originalCallbacks.onError(adapter, errorCode, errorMessage);
                }

                if (testFairyBridge != null) {
                    testFairyBridge.onError(errorCode, errorMessage);
                }

            }

            @Override
            public void onBufferingStateChanged(PlayerAdapter adapter, boolean start) {
                if (createdFrom instanceof CreatedFrom.FromMediaPlayerAdapter) {
                    CreatedFrom.FromMediaPlayerAdapter castedCreationMethod = (CreatedFrom.FromMediaPlayerAdapter) createdFrom;

                    if (castedCreationMethod.playerAdapterListenerCallbacks != null) {
                        castedCreationMethod.playerAdapterListenerCallbacks.onBufferingStateChanged(adapter, start);
                    }

                    originalCallbacks.onBufferingStateChanged(adapter, start);
                }
            }

            @Override
            public void onMetadataChanged(PlayerAdapter adapter) {
                if (createdFrom instanceof CreatedFrom.FromMediaPlayerAdapter) {
                    CreatedFrom.FromMediaPlayerAdapter castedCreationMethod = (CreatedFrom.FromMediaPlayerAdapter) createdFrom;

                    if (castedCreationMethod.playerAdapterListenerCallbacks != null) {
                        castedCreationMethod.playerAdapterListenerCallbacks.onMetadataChanged(adapter);
                    }

                    originalCallbacks.onMetadataChanged(adapter);
                }
            }

            @Override
            public void setCallbacks(PlayerAdapter.Callback callbacks) {
                if (createdFrom instanceof CreatedFrom.FromMediaPlayerAdapter) {
                    CreatedFrom.FromMediaPlayerAdapter castedCreationMethod = (CreatedFrom.FromMediaPlayerAdapter) createdFrom;

                    if (castedCreationMethod.playerAdapterListenerCallbacks != null) {
                        castedCreationMethod.playerAdapterListenerCallbacks = callbacks;
                    }
                }
            }

            @Override
            public void trackPlaybackPosition() {
                TestFairyMediaPlayerGlue.this.createdFrom.registerCurrentPositionTracker(null);
            }

            @Override
            public void untrackPlaybackPosition() {
                TestFairyMediaPlayerGlue.this.createdFrom.unRegisterCurrentPositionTracker();
            }

            @Override
            public void setTestFairyBridge(TestFairyBridge bridge) {
                testFairyBridge = bridge;
            }

            @Override
            public TestFairyBridge getTestFairyBridge() {
                return testFairyBridge;
            }
        };
    }
    //////////////////////////////////////////////////////////////

    // Creation state
    private static abstract class CreatedFrom {
        protected abstract void registerCurrentPositionTracker(final Runnable runnable);

        protected abstract void unRegisterCurrentPositionTracker();

        private abstract static class FromMediaPlayer extends CreatedFrom {
            private MediaPlayer.OnBufferingUpdateListener onBufferingUpdateListener;
            private MediaPlayer.OnCompletionListener onCompletionListener;
            private MediaPlayer.OnErrorListener onErrorListener;
            private MediaPlayer.OnMediaTimeDiscontinuityListener onMediaTimeDiscontinuityListener;
            private MediaPlayer.OnSeekCompleteListener onSeekCompleteListener;

            private static Runnable createPositionTracker(final MediaPlayer mediaPlayer, final TestFairyMediaPlayerGlue listener) {
                return new Runnable() {
                    private int lastKnownPlaybackPercent = -1;

                    @Override
                    public void run() {
                        if (mediaPlayer.getDuration() != 0) {
                            int currentPosition = mediaPlayer.getCurrentPosition();
                            int percent = (currentPosition * 100) / mediaPlayer.getDuration();

                            if (lastKnownPlaybackPercent != percent && listener.testFairyBridge != null) {
                                listener.testFairyBridge.onPlaybackPositionUpdate(percent);
                            }
                            lastKnownPlaybackPercent = percent;
                        }
                    }
                };
            }

            protected abstract MediaPlayer getMediaPlayer();
        }

        private abstract static class FromMediaPlayerAdapter extends CreatedFrom {
            private PlayerAdapter.Callback playerAdapterListenerCallbacks;
        }
    }
    //////////////////////////////////////////////////////////////

    // TestFairy Bridge
    public interface TestFairyBridge {
        void onBufferingUpdate(int percent);

        void onPlaybackStateChange(boolean isPlaying);

        void onPlaybackPositionUpdate(int percent);

        void onComplete();

        void onError(int reason, Object extra);
    }
    //////////////////////////////////////////////////////////////

    // Public Wrapper Interfaces
    public interface PlayerWrapper {
        void setOnBufferingUpdateListener(MediaPlayer.OnBufferingUpdateListener onBufferingUpdateListener);

        void setOnCompletionListener(MediaPlayer.OnCompletionListener onCompletionListener);

        void setOnErrorListener(MediaPlayer.OnErrorListener onErrorListener);

        void setOnMediaTimeDiscontinuityListener(MediaPlayer.OnMediaTimeDiscontinuityListener onMediaTimeDiscontinuityListener);

        void setOnSeekCompleteListener(MediaPlayer.OnSeekCompleteListener onSeekCompleteListener);

        void trackPlaybackPosition();

        void untrackPlaybackPosition();

        TestFairyBridge getTestFairyBridge();

        void setTestFairyBridge(TestFairyBridge bridge);
    }

    public interface PlayerAdapterWrapper {
        void setCallbacks(PlayerAdapter.Callback callbacks);

        void trackPlaybackPosition();

        void untrackPlaybackPosition();

        TestFairyBridge getTestFairyBridge();

        void setTestFairyBridge(TestFairyBridge bridge);
    }
    //////////////////////////////////////////////////////////////
}
