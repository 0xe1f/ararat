[![Build Status](https://travis-ci.org/0xe1f/ararat.svg?branch=master)](https://travis-ci.org/0xe1f/ararat)

ararat
======

**ararat** is a crossword library for Android, written for and used by
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

License
=======

**ararat** is written by Akop Karapetyan.
Licensed under the [MIT License](LICENSE).
