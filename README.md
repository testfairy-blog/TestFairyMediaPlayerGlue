# TestFairyMediaPlayerGlue
A glue class for capturing media player events from Android TV apps.

## Usage with `MediaPlayer`

```java
// Find/create a media player
android.media.MediaPlayer myMediaPlayer = ...;

// Initialize
TestFairyMediaPlayerGlue.PlayerWrapper wrapper = TestFairyMediaPlayerGlue.createByWrapping(myMediaPlayer);
// use wrapper to configure further listeners and behavior
```

## Usage with `MediaPlayerAdapter`

```java
// The usual suspects from the support library
PlaybackVideoFragment myPlaybackVideoFragment = ...;
VideoSupportFragmentGlueHost myGlueHost = VideoSupportFragmentGlueHost(myPlaybackVideoFragment);
MediaPlayerAdapter myPlayerAdapter = MediaPlayerAdapter(myPlaybackVideoFragment.getContext());
PlaybackTransportControlGlue transportGlue = PlaybackTransportControlGlue(myPlaybackVideoFragment.getActivity(), myPlayerAdapter);
transportGlue.setHost(myGlueHost);

// Initialize
TestFairyMediaPlayerGlue.PlayerAdapterWrapper wrapper = TestFairyMediaPlayerGlue.createByWrapping(myPlayerAdapter);
// use wrapper to configure further listeners and behavior
```

## Usage with `ExoPlayer`

```java
exoPlayer.addAnalyticsListener(new TestFairyExoPlayerAnalyticsListener(exoPlayer));
```
