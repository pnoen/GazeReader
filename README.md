# GazeReader

<p align="center" width="100%">
    <img width="15%" src="app/src/main/res/drawable/logo.png">
</p>

GazeReader is a hands-free eBook reader that utilises gaze modality. The application operates with [Seeso Eye Tracking Software](https://seeso.io/). Seeso provides the ability for accurate eye tracking by simply calibrating. You are able to select interactable elements by fixating on a location. In the current version, only preset eBooks are provided. Users are also able to bookmark their progress to resume reading in the future.

This project has used Seeso's [sample project](https://github.com/visualcamp/seeso-sample-android) as the base to maximise the performance of the eye tracker.

## Installation

1. Install [Android Studio](https://developer.android.com/studio)
2. In Android Studio, go to [```File > Project Structure > Dependencies```] and add a ```JAR\AAR Dependency```.
3. Type in the path ```libs/gazetracker-release.aar``` and select ```implementation```. Press ```OK```. If unsure, images shown [here](https://docs.seeso.io/nonversioning/quick-start/android-quick-start).
4. Repeat step 2 and 3. This time with the path ```libs/libgaze-release.aar```.
5. Register an account and request for a development key [here](https://seeso.io/).
6. Replace the text ```devKey``` with the key in [```app > src > main > java > com > example > gazereader > GazeTrackerManager.java```].
7. Connect your Android device with a USB connection. ```USB debugging``` will need to be enabled.
8. Run the app with the green play button in the toolbar.

## References

* https://seeso.io/
* https://github.com/visualcamp/seeso-sample-android
* https://github.com/documentnode/epub4j
