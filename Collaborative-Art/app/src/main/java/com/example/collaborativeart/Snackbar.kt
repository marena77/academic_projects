package com.example.collaborativeart

import android.view.View
import android.widget.Button
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar

/** Keeps track of snackbar instance to support [dismissSnackbar]. */
private var snackbarInstance: Snackbar? = null

/**
 * Dismisses a possibly visible [Snackbar].
 */
fun dismissSnackbar() {
    snackbarInstance?.dismiss()
}

/**
 * View extension that shows a [Snackbar] with the
 * provided resource string and an optional [Button].
 */
fun View.showSnackbar(
    resId: Int,
    length: Int,
    actionMessage: CharSequence? = null,
    action: (View) -> Unit = {}
) {
    showSnackbar(context.getString(resId), length, actionMessage, action)
}

/**
 * View extension that shows a [Snackbar] with the
 * provided resource string and an optional [Button].
 */
fun View.showSnackbar(
    resId: Int,
    length: Int,
    actionMessage: Int,
    action: (View) -> Unit = {}
) {
    showSnackbar(
        context.getString(resId),
        length,
        context.getString(actionMessage),
        action
    )
}

/**
 * View extension that shows a [Snackbar] with the
 * provided message string and an optional [Button].
 */
fun View.showSnackbar(
    msg: String,
    length: Int,
    actionMessage: CharSequence? = null,
    action: (View) -> Unit = {}
) {
    Snackbar.make(this, msg, length).apply {
        if (actionMessage != null || length == Snackbar.LENGTH_INDEFINITE) {
            setAction(actionMessage ?: context.getString(android.R.string.ok)) {
                action(view)
            }
        }

        // Add callback to set and clear Snackbar property
        // when Snackbar is shown and dismissed.
        addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
            override fun onShown(snackbar: Snackbar?) {
                snackbarInstance = snackbar
                super.onShown(snackbar)
            }

            override fun onDismissed(snackbar: Snackbar?, event: Int) {
                snackbarInstance = null
                super.onDismissed(snackbar, event)
            }
        })

        show()
    }
}