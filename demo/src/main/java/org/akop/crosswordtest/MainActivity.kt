// Copyright (c) Akop Karapetyan
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

package org.akop.crosswordtest

import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat

import org.akop.ararat.core.Crossword
import org.akop.ararat.core.buildCrossword
import org.akop.ararat.io.PuzFormatter
import org.akop.ararat.view.CrosswordView


// Crossword: Double-A's by Ben Tausig
// http://www.inkwellxwords.com/iwxpuzzles.html
class MainActivity : AppCompatActivity(), CrosswordView.OnLongPressListener, CrosswordView.OnStateChangeListener, CrosswordView.OnSelectionChangeListener {

    private lateinit var crosswordView: CrosswordView
    private var hint: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        crosswordView = findViewById(R.id.crossword)
        hint = findViewById(R.id.hint)

        val puzzle = readPuzzle(R.raw.puzzle)

        title = getString(R.string.title_by_author,
                puzzle.title, puzzle.author)

        with (crosswordView) {
            crossword = puzzle
            onLongPressListener = this@MainActivity
            onStateChangeListener = this@MainActivity
            onSelectionChangeListener = this@MainActivity
            inputValidator = { ch -> !ch.first().isISOControl() }
            undoMode = CrosswordView.UNDO_NONE
            markerDisplayMode = CrosswordView.MARKER_CHEAT
        }
        onSelectionChanged(crosswordView,
                crosswordView.selectedWord,
                crosswordView.selectedCell)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        crosswordView.restoreState(savedInstanceState.getParcelable("state")!!)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable("state", crosswordView.state)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_restart -> crosswordView.reset()
            R.id.menu_solve_cell -> crosswordView.solveChar(
                    crosswordView.selectedWord!!,
                    crosswordView.selectedCell)
            R.id.menu_solve_word -> crosswordView.solveWord(
                    crosswordView.selectedWord!!)
            R.id.menu_solve_puzzle -> crosswordView.solveCrossword()
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    override fun onCellLongPressed(view: CrosswordView,
                                   word: Crossword.Word, cell: Int) {
        Toast.makeText(this, "Show popup menu for " + word.hint!!,
                Toast.LENGTH_SHORT).show()
    }

    override fun onCrosswordChanged(view: CrosswordView) {}

    override fun onCrosswordSolved(view: CrosswordView) {
        Toast.makeText(this, R.string.youve_solved_the_puzzle,
                Toast.LENGTH_SHORT).show()
    }

    override fun onCrosswordUnsolved(view: CrosswordView) { }

    private fun readPuzzle(@RawRes resourceId: Int): Crossword =
            resources.openRawResource(resourceId).use { s ->
                buildCrossword { PuzFormatter().read(this, s) }
            }

    override fun onSelectionChanged(view: CrosswordView,
                                    word: Crossword.Word?, position: Int) {
        hint!!.text = when (word?.direction) {
            Crossword.Word.DIR_ACROSS -> getString(R.string.across, word.number, word.hint)
            Crossword.Word.DIR_DOWN -> getString(R.string.down, word.number, word.hint)
            else -> ""
        }
    }
}
