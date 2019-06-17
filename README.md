# TestFairyMediaPlayerGlue
A glue class for capturing media player events from Android TV apps.

## Usage with `MediaPlayer`

```java
android.media.MediaPlayer myMediaPlayer = ...;
TestFairyMediaPlayerGlue.PlayerWrapper wrapper = TestFairyMediaPlayerGlue.createByWrapping(myMediaPlayer);
// use wrapper to configure further listeners and behavior
```

## Usage with `MediaPlayerAdapter`

```java
PlaybackVideoFragment myPlaybackVideoFragment = ...;
VideoSupportFragmentGlueHost myGlueHost = VideoSupportFragmentGlueHost(myPlaybackVideoFragment);
MediaPlayerAdapter myPlayerAdapter = MediaPlayerAdapter(myPlaybackVideoFragment.getContext());
PlaybackTransportControlGlue transportGlue = PlaybackTransportControlGlue(myPlaybackVideoFragment.getActivity(), myPlayerAdapter);
transportGlue.setHost(myGlueHost);
TestFairyMediaPlayerGlue.PlayerAdapterWrapper wrapper = TestFairyMediaPlayerGlue.createByWrapping(myPlayerAdapter);
// use wrapper to configure further listeners and behavior
```
