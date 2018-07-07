package com.grarak.ytfetcher.views

import android.content.Context
import android.support.design.widget.TextInputEditText
import android.support.design.widget.TextInputLayout
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import com.grarak.ytfetcher.R

class EditTextView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : TextInputLayout(context, attrs, defStyleAttr) {

    val textInputEditText = TextInputEditText(context)

    var text: CharSequence
        set(value) {
            textInputEditText.setText(value)
        }
        get() = textInputEditText.text!!

    init {

        addView(textInputEditText, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val a = context.obtainStyledAttributes(
                attrs, R.styleable.EditTextView, defStyleAttr, 0)

        textInputEditText.setSingleLine(a.getBoolean(
                R.styleable.EditTextView_android_singleLine, false))

        a.recycle()
    }
}
