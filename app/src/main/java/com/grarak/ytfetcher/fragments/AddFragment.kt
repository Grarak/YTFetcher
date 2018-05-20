package com.grarak.ytfetcher.fragments

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.design.widget.FloatingActionButton
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Interpolator
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import com.grarak.ytfetcher.R
import com.grarak.ytfetcher.utils.Utils
import io.codetail.animation.ViewAnimationUtils

class AddFragment : TitleFragment() {

    private var editTextParent: View? = null
    private var editTextView: EditText? = null
    private var add: FloatingActionButton? = null

    var text: CharSequence?
        set(value) {
            text?.run {
                editTextView?.setText(this)
                editTextView?.setSelection(length)
            }
        }
        get() = editTextView?.text
    var hint: CharSequence? = null
        set(value) {
            field = value
            editTextView?.hint = value
        }
    var onOpenListener: OnOpenListener? = null
    var onConfirmListener: OnConfirmListener? = null

    @DrawableRes
    var imageResource: Int = 0
        set(value) {
            field = value
            add?.setImageResource(value)
        }

    override val layoutXml: Int = R.layout.fragment_add

    interface OnOpenListener {
        fun onOpen(fragment: AddFragment)
    }

    interface OnConfirmListener {
        fun onConfirm(text: CharSequence)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = super.onCreateView(inflater, container, savedInstanceState)

        editTextParent = rootView!!.findViewById(R.id.edittext_parent)
        editTextView = rootView.findViewById(R.id.edittext)
        editTextView!!.hint = hint
        editTextView!!.setOnEditorActionListener({ _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                onDone()
                return@setOnEditorActionListener true
            }
            false
        })

        rootView.findViewById<View>(R.id.delete_btn).setOnClickListener { editTextView!!.setText("") }

        add = rootView.findViewById(R.id.add)
        add!!.setOnClickListener {
            if (editTextParent!!.visibility == View.INVISIBLE) {
                showEditText(true)
            } else {
                onDone()
            }
        }
        if (imageResource > 0) {
            add!!.setImageResource(imageResource)
        }

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("show")) {
                editTextParent!!.visibility = View.VISIBLE
                Utils.showKeyboard(editTextView!!)
            }
            editTextView!!.setText(savedInstanceState.getCharSequence("text"))
        }
        return rootView
    }

    private fun onDone() {
        if (onConfirmListener != null) {
            onConfirmListener!!.onConfirm(editTextView!!.text)
        }
        showEditText(false)
        editTextView!!.setText("")
    }

    private fun showEditText(show: Boolean) {
        val animator = ViewAnimationUtils.createCircularReveal(editTextParent!!,
                editTextParent!!.width, 0, 0f, editTextParent!!.width.toFloat())
        if (!show) {
            animator.interpolator = Interpolator { Math.abs(it - 1f) }
        }
        editTextParent!!.visibility = View.VISIBLE
        editTextView!!.setText("")
        if (show) {
            onOpenListener?.onOpen(this)
            Utils.showKeyboard(editTextView!!)
        } else {
            Utils.hideKeyboard(editTextView!!)
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                if (!show) {
                    editTextParent!!.visibility = View.INVISIBLE
                }
            }
        })
        animator.duration = 250
        animator.start()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean("show", editTextParent!!.visibility == View.VISIBLE)
        outState.putCharSequence("text", editTextView!!.text)
    }

    override fun onViewPagerResume() {
        super.onViewPagerResume()

        if (editTextParent!!.visibility == View.VISIBLE) {
            Utils.showKeyboard(editTextView!!)
        }
    }

    override fun onViewPagerPause() {
        super.onViewPagerPause()

        Utils.hideKeyboard(editTextView!!)
    }

    override fun onBackPressed(): Boolean {
        if (editTextParent!!.visibility == View.VISIBLE) {
            showEditText(false)
            return true
        }
        return false
    }
}
