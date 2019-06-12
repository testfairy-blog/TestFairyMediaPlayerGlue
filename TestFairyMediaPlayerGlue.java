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
    private final TestFairyBridge testFairyBridge;
    //////////////////////////////////////////////////////////////

    // Private constructor for internal use, includes bridging logic
    private TestFairyMediaPlayerGlue(CreatedFrom createdFrom) {
        this.createdFrom = createdFrom;

        this.testFairyBridge = new TestFairyBridge() {
            @Override
            public void onBufferingUpdate(int percent) {
                // TODO : call TestFairy.addEvent
            }

            @Override
            public void onPlaybackStateChange(boolean isPlaying) {
                // TODO : call TestFairy.addEvent
            }

            @Override
            public void onPlaybackPositionUpdate(int percent) {
                // TODO : call TestFairy.addEvent
            }

            @Override
            public void onComplete() {
                // TODO : call TestFairy.addEvent
            }

            @Override
            public void onError(String reason, String message) {
                // TODO : call TestFairy.addEvent
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
                        handler.postDelayed(runnable, 100);
                    }
                };

                handler.postDelayed(runnable, 100);
            }

            @Override
            protected void unRegisterCurrentPositionTracker() {
                if (currentPositionTracker != null) {
                    handler.removeCallbacks(currentPositionTracker);
                    currentPositionTracker = null;
                }
            }
        });

        final CreatedFrom.FromMediaPlayer castedCreationMethod = (CreatedFrom.FromMediaPlayer) listener.createdFrom;
        castedCreationMethod.registerCurrentPositionTracker(new Runnable() {
            @Override
            public void run() {
                // TODO : bridge to TestFairy
            }
        });

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
            protected void registerCurrentPositionTracker(Runnable runnable) {
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
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                if (createdFrom instanceof CreatedFrom.FromMediaPlayer) {
                    CreatedFrom.FromMediaPlayer castedCreationMethod = (CreatedFrom.FromMediaPlayer) createdFrom;

                    if (castedCreationMethod.onBufferingUpdateListener != null) {
                        castedCreationMethod.onBufferingUpdateListener.onBufferingUpdate(mp, percent);
                    }
                }

                // TODO : bridge to TestFairy
            }

            @Override
            public void onCompletion(MediaPlayer mp) {
                if (createdFrom instanceof CreatedFrom.FromMediaPlayer) {
                    CreatedFrom.FromMediaPlayer castedCreationMethod = (CreatedFrom.FromMediaPlayer) createdFrom;

                    if (castedCreationMethod.onCompletionListener != null) {
                        castedCreationMethod.onCompletionListener.onCompletion(mp);
                    }
                }

                // TODO : bridge to TestFairy
            }

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                if (createdFrom instanceof CreatedFrom.FromMediaPlayer) {
                    CreatedFrom.FromMediaPlayer castedCreationMethod = (CreatedFrom.FromMediaPlayer) createdFrom;

                    if (castedCreationMethod.onErrorListener != null) {
                        castedCreationMethod.onErrorListener.onError(mp, what, extra);
                    }
                }

                return false;

                // TODO : bridge to TestFairy
            }

            @Override
            public void onMediaTimeDiscontinuity(MediaPlayer mp, MediaTimestamp mts) {
                if (createdFrom instanceof CreatedFrom.FromMediaPlayer) {
                    CreatedFrom.FromMediaPlayer castedCreationMethod = (CreatedFrom.FromMediaPlayer) createdFrom;

                    if (castedCreationMethod.onMediaTimeDiscontinuityListener != null) {
                        castedCreationMethod.onMediaTimeDiscontinuityListener.onMediaTimeDiscontinuity(mp, mts);
                    }
                }

                // TODO : bridge to TestFairy
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
        };
    }

    private static abstract class PlayerAdapterWrapperImpl extends PlayerAdapter.Callback implements PlayerAdapterWrapper {
    }

    private PlayerAdapterWrapperImpl createPlayerAdapterCallbacksWrapper(final PlayerAdapter.Callback originalCallbacks) {
        return new PlayerAdapterWrapperImpl() {
            @Override
            public void onPlayStateChanged(PlayerAdapter adapter) {
                if (createdFrom instanceof CreatedFrom.FromMediaPlayerAdapter) {
                    CreatedFrom.FromMediaPlayerAdapter castedCreationMethod = (CreatedFrom.FromMediaPlayerAdapter) createdFrom;

                    if (castedCreationMethod.playerAdapterListenerCallbacks != null) {
                        castedCreationMethod.playerAdapterListenerCallbacks.onPlayStateChanged(adapter);
                    }

                    originalCallbacks.onPlayStateChanged(adapter);
                }

                // TODO : bridge to TestFairy
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

                // TODO : bridge to TestFairy
            }

            @Override
            public void onCurrentPositionChanged(PlayerAdapter adapter) {
                if (createdFrom instanceof CreatedFrom.FromMediaPlayerAdapter) {
                    CreatedFrom.FromMediaPlayerAdapter castedCreationMethod = (CreatedFrom.FromMediaPlayerAdapter) createdFrom;

                    if (castedCreationMethod.playerAdapterListenerCallbacks != null) {
                        castedCreationMethod.playerAdapterListenerCallbacks.onCurrentPositionChanged(adapter);
                    }

                    originalCallbacks.onCurrentPositionChanged(adapter);
                }

                // TODO : bridge to TestFairy
            }

            @Override
            public void onBufferedPositionChanged(PlayerAdapter adapter) {
                if (createdFrom instanceof CreatedFrom.FromMediaPlayerAdapter) {
                    CreatedFrom.FromMediaPlayerAdapter castedCreationMethod = (CreatedFrom.FromMediaPlayerAdapter) createdFrom;

                    if (castedCreationMethod.playerAdapterListenerCallbacks != null) {
                        castedCreationMethod.playerAdapterListenerCallbacks.onBufferedPositionChanged(adapter);
                    }

                    originalCallbacks.onBufferedPositionChanged(adapter);
                }

                // TODO : bridge to TestFairy
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

                // TODO : bridge to TestFairy
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
        }

        private abstract static class FromMediaPlayerAdapter extends CreatedFrom {
            private PlayerAdapter.Callback playerAdapterListenerCallbacks;
        }
    }
    //////////////////////////////////////////////////////////////

    // TestFairy Bridge
    private interface TestFairyBridge {
        void onBufferingUpdate(int percent);

        void onPlaybackStateChange(boolean isPlaying);

        void onPlaybackPositionUpdate(int percent);

        void onComplete();

        void onError(String reason, String message);
    }
    //////////////////////////////////////////////////////////////

    // Public Wrapper Interfaces
    public interface PlayerWrapper {
        void setOnBufferingUpdateListener(MediaPlayer.OnBufferingUpdateListener onBufferingUpdateListener);

        void setOnCompletionListener(MediaPlayer.OnCompletionListener onCompletionListener);

        void setOnErrorListener(MediaPlayer.OnErrorListener onErrorListener);

        void setOnMediaTimeDiscontinuityListener(MediaPlayer.OnMediaTimeDiscontinuityListener onMediaTimeDiscontinuityListener);

        void setOnSeekCompleteListener(MediaPlayer.OnSeekCompleteListener onSeekCompleteListener);
    }

    public interface PlayerAdapterWrapper {
        void setCallbacks(PlayerAdapter.Callback callbacks);
    }
    //////////////////////////////////////////////////////////////
}
