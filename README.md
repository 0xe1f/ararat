[![Build Status](https://travis-ci.org/0xe1f/ararat.svg?branch=master)](https://travis-ci.org/0xe1f/ararat)
[![Sonatype Nexus (Releases)](https://img.shields.io/nexus/r/https/oss.sonatype.org/com.github.0xe1f/ararat.svg)](https://repo1.maven.org/maven2/com/github/0xe1f/ararat/)
[![License](https://img.shields.io/badge/license-MIT-4EB1BA.svg)](https://opensource.org/licenses/MIT)

Ararat
======

**Ararat** is a crossword library for Android, written for and used by
[alphacross](https://play.google.com/store/apps/details?id=org.akop.crosswords).
It includes:

* [Parsers](library/src/main/java/org/akop/ararat/io/) for various formats
* [CrosswordRenderer](library/src/main/java/org/akop/ararat/graphics/CrosswordRenderer.kt),
which can render a crossword (and optionally, current state) to a `Canvas`
* [CrosswordView](library/src/main/java/org/akop/ararat/view/CrosswordView.kt),
the `View` subclass that handles gameplay

For a demonstration, see the included [demo app](demo/).

![Screenshot](https://i.imgur.com/1lg7zhN.png)

The sample crossword included in the app is Double-A's by Ben Tausig.

## Quick Start

Add dependency to `app/build.gradle` (check to make sure you're using the
[latest version](https://repo1.maven.org/maven2/com/github/0xe1f/ararat/maven-metadata.xml)):

```
dependencies {
    implementation 'com.github.0xe1f:ararat:1.0.15'
}
```

Add view to your layout:

```xml
<org.akop.ararat.view.CrosswordView
    android:id="@+id/crossword"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

Initialize the view in `OnCreate()`:

```kotlin
val crosswordView = findViewById<CrosswordView>(R.id.crossword)
crosswordView.crossword = readPuzzle()
```

Existing puzzles are usually read from a stream, e.g.:

```kotlin
fun readPuzzle(): Crossword = resources.openRawResource(R.raw.puzzle).use { s ->
    buildCrossword { PuzFormatter().read(this, s) }
}
```

For the activity to respond to soft keyboard changes, set
`android:windowSoftInputMode` to `adjustResize` in the manifest:

```xml
<activity android:name=".MainActivity"
          android:windowSoftInputMode="adjustResize" />
```

## Keyboard Support

`CrosswordView` was designed to work with the Android soft keyboard.
Unfortunately, with the exception of maybe Google and the AOSP codebase,
virtually no one follows the rules of Android IME, including many third-party
manufacturers and authors of soft keyboards. They will often mistakenly assume
that any view has full support of things like autocorrect and swiping -
concepts that don't work in the limited scope of input needed for crosswords,
and will ignore any attempts to disable those features.

If you're serious about your own crossword app, your best long term bet is
to use your own
[KeyboardView](https://developer.android.com/reference/android/inputmethodservice/KeyboardView)
and have it provide the input instead.

## License

```
Copyright (c) 2014-2022 Akop Karapetyan

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
