// Copyright (c) 2014-2016 Akop Karapetyan
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

package org.akop.ararat.view.inputmethod;

import android.view.View;
import android.view.inputmethod.BaseInputConnection;


public class CrosswordInputConnection
		extends BaseInputConnection
{
	public interface OnInputEventListener
	{
		void onWordEntered(CharSequence text);
		void onWordCancelled();
		void onEditorAction(int actionCode);
	}

	private OnInputEventListener mInputEventListener;

	public CrosswordInputConnection(View targetView)
	{
		super(targetView, false);
	}

	public void setOnInputEventListener(OnInputEventListener listener)
	{
		mInputEventListener = listener;
	}

	@Override
	public boolean commitText(CharSequence text, int newCursorPosition)
	{
		if (mInputEventListener != null && text.length() == 0) {
			mInputEventListener.onWordCancelled();
		}

		return super.commitText(text, newCursorPosition);
	}

	@Override
	public boolean performEditorAction(int actionCode)
	{
		if (mInputEventListener != null) {
			mInputEventListener.onEditorAction(actionCode);
		}

		return super.performEditorAction(actionCode);
	}

	@Override
	public boolean setComposingText(CharSequence text, int newCursorPosition)
	{
		if (mInputEventListener != null) {
			mInputEventListener.onWordEntered(text);
		}

		return super.setComposingText(text, newCursorPosition);
	}
}
