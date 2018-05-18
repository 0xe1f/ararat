[![Build Status](https://travis-ci.org/0xe1f/ararat.svg?branch=master)](https://travis-ci.org/0xe1f/ararat)

Ararat
======

**Ararat** is a crossword library for Android, written for and used by
[alphacross](https://play.google.com/store/apps/details?id=org.akop.crosswords).
It includes:

* [Parsers](library/src/main/java/org/akop/ararat/io/) for various formats
* [CrosswordRenderer](library/src/main/java/org/akop/ararat/graphics/CrosswordRenderer.java),
which can render a crossword (and optionally, current state) to a `Canvas`
object
* [CrosswordView](library/src/main/java/org/akop/ararat/view/CrosswordView.java), the
`View` subclass that handles gameplay

For a demonstration, see the included [demo app](demo/).

![Screenshot](http://i.imgur.com/1lg7zhN.png)

The sample crossword included in the app is Double-A's by Ben Tausig:
http://www.inkwellxwords.com/iwxpuzzles.html

## Quick Start

Add dependency to `app/build.gradle`:

```
dependencies {
    implementation 'com.github.0xe1f:ararat:1.0.3'
}
```

Add view to `layout.xml`:

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
fun readPuzzle(): Crossword {
    resources.openRawResource(R.raw.puzzle).use {
        val cb = Crossword.Builder()
        PuzFormatter().read(cb, it)

        return cb.build()
    }
}
```

For the activity to respond to soft keyboard changes, set
`android:windowSoftInputMode` to `adjustResize` in the manifest:

```xml
<activity android:name=".MainActivity"
          android:windowSoftInputMode="adjustResize" />
```

## License

```
Copyright (c) 2014-2018 Akop Karapetyan

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