[![Build Status](https://travis-ci.org/pokebyte/ararat.svg?branch=master)](https://travis-ci.org/pokebyte/ararat)

ararat
======

**ararat** is a crossword library for Android, written for and used by
[alphacross](https://play.google.com/store/apps/details?id=org.akop.crosswords).
It includes:

* [Parsers](src/main/java/org/akop/ararat/io/) for various formats
* [CrosswordRenderer](src/main/java/org/akop/ararat/graphics/CrosswordRenderer.java),
which can render a crossword (and optionally, current state) to a `Canvas`
object
* [CrosswordView](src/main/java/org/akop/ararat/view/CrosswordView.java), the
`View` subclass that handles gameplay

For a demonstration, see [CrosswordDemo](https://github.com/pokebyte/CrosswordDemo).

License
=======

**ararat** is written by Akop Karapetyan.
Licensed under the [MIT License](LICENSE).
