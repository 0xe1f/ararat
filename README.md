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
    implementation 'com.github.0xe1f:ararat:1.0'
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

For the view to respond to soft keyboard changes, set the `Activity`'s
`android:windowSoftInputMode` to `adjustResize` in the manifest:

```xml
<activity android:name=".MainActivity"
          android:windowSoftInputMode="adjustResize" />
```

## License

**ararat** is written by Akop Karapetyan.
Licensed under the [MIT License](LICENSE).
