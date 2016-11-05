ReadMe for AudioUnitExamples
-----------------------------
Version 2.0 (February 2016)

/* ----------------------------- */

NOTE: While AudioUnitExamples presents up to date (as of OS X 10.11.x) examples of Version 2 Audio Unit Plugins, with the release of OS X 10.11 Apple introduced the Version 3 Audio Unit Extension API.

The Audio Unit Extensions API introduces a mechanism for developers to deliver Audio Units to users on iOS and OS X as App Extensions.
The same API is available on both iOS and OS X and provides a bridging mechanism for existing Version 2 Audio Units and hosts to work with new Version 3 Audio Units and hosts.

For more information regarding Version 3 Audio Unit Extensions, see the following resources:

Audio Unit Extensions 2015 WWDC Video:

    https://developer.apple.com/videos/play/wwdc2015-508/

App Extension Programming Guide:

    https://developer.apple.com/library/ios/documentation/General/Conceptual/ExtensibilityPG/

AudioUnitV3Example:

    https://developer.apple.com/library/ios/samplecode/AudioUnitV3Example/Introduction/Intro.html

We encourage Audio Unit developers to adopt and move to the Version 3 Audio Unit Extension model for iOS 9 and OS X 10.11 forward.

/* ----------------------------- */

AudioUnitExamples is a collection of Version 2 AudioUnit sample code. Each project demonstrates how to create an AudioUnit of a specific type (i.e. Effect, Generator, Instrument, MIDI Processor and Offline Effect).

AudioUnitEffectExample
	This sample builds a simple low pass filter as an Effect AudioUnit with custom view. 
AudioUnitGeneratorExample
	This sample builds a pink noise generator as a Generator AudioUnit. 
AudioUnitInstrumentExample
	This sample builds a basic sin wave synth as an Instrument AudioUnit.
AudioUnitOfflineEffectExample
	This sample builds a simple Offline Effect AudioUnit.
AudioUnitMidiProcessorExample
    This sample buids a pass through midi processor. AU's of this type process midi input and produce midi output but do not produce any audio.
StarterAudioUnitExample (TremoloUnit)
	This sample is referenced in the AudioUnit programming guide. 

The tutorial for Audio Unit Programming Guide is available in the ADC Reference Library at this location:

	http://developer.apple.com/documentation/MusicAudio/Conceptual/AudioUnitProgrammingGuide/

Technical note TN2247 describes how to support sandboxing in an AudioUnit

	https://developer.apple.com/library/ios/technotes/tn2247

Technical note TN2276 contains legacy information regarding Component Manager Based Audio Units (OS X 10.6 and earlier) and Audio
Unit Plugins (10.7 and newer). This information is presented for completeness only. The AudioUnitExamples projects all build AUPlugins.

    https://developer.apple.com/library/mac/technotes/tn2276

Installation
------------
To install one of the sample audio unit for testing, place the built audio unit (e.g. FilterDemo.component) to the following directory. 

	~/Library/Audio/Plug-Ins/Components/

or to:

	/Library/Audio/Plug-Ins/Components/


Testing the Audio Unit
----------------------
To test your Audio Unit after installing it, use an Audio Unit hosting application such as "AU Lab".  AU Lab is part of the Audio Tools for Xcode. 

Please refer to the following technical Q&A (QA1731) on how to download Audio Tools.

	https://developer.apple.com/library/mac/qa/qa1731


Sample Requirements
-------------------
This sample project requires:
	
	Mac OS X v10.11.2 or later
	Xcode 7.2.1 or later
	
Feedback
--------
To send feedback to Apple about this sample project, use the feedback form at 
this location:

	http://developer.apple.com/contact/

Copyright (C) 2004-2016 Apple Inc. All rights reserved.
