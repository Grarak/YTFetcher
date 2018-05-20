package com.grarak.ytfetcher

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.view.View
import android.widget.Button

import com.grarak.ytfetcher.utils.Settings
import com.grarak.ytfetcher.utils.Utils
import com.grarak.ytfetcher.utils.server.Status
import com.grarak.ytfetcher.utils.server.user.User
import com.grarak.ytfetcher.utils.server.user.UserServer
import com.grarak.ytfetcher.views.EditTextView

class LoginActivity : BaseActivity() {

    private var server: UserServer? = null

    private var serverView: EditTextView? = null
    private var usernameView: EditTextView? = null
    private var passwordView: EditTextView? = null
    private var confirmPasswordView: EditTextView? = null

    private var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = User.restore(this)
        if (user != null && user.verified) {
            launchMainActivity(user)
            return
        }

        setContentView(R.layout.activity_login)

        serverView = findViewById(R.id.server_edit)
        serverView!!.textInputEditText.inputType = InputType.TYPE_TEXT_VARIATION_FILTER
        usernameView = findViewById(R.id.username_edit)
        usernameView!!.textInputEditText.inputType = InputType.TYPE_TEXT_VARIATION_FILTER
        passwordView = findViewById(R.id.password_edit)
        passwordView!!.textInputEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        confirmPasswordView = findViewById(R.id.confirm_password_edit)
        confirmPasswordView!!.textInputEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        if (savedInstanceState == null) {
            val url = Settings.getServerUrl(this)
            if (!url.isEmpty()) {
                serverView!!.text = url
                usernameView!!.requestFocus()
            }
        } else {
            serverView!!.text = savedInstanceState.getString("server_url")
            usernameView!!.text = savedInstanceState.getString("username")
            passwordView!!.text = savedInstanceState.getString("password")
            confirmPasswordView!!.text = savedInstanceState.getString("confirm_password")
            if (savedInstanceState.getBoolean("progress")) {
                showProgress()
            }
        }

        findViewById<View>(R.id.done_btn).setOnClickListener {
            var serverText = serverView!!.text.toString()
            if (serverText.isEmpty()) {
                Utils.toast(R.string.server_empty, this)
                return@setOnClickListener
            } else if (!serverText.startsWith("http")) {
                serverText = "http://$serverText"
                serverView!!.text = serverText
            }

            if (usernameView!!.text.length <= 3) {
                Utils.toast(R.string.username_short, this)
                return@setOnClickListener
            } else if (!usernameView!!.text.toString().matches("^[a-zA-Z0-9_]*$".toRegex())) {
                Utils.toast(R.string.username_error, this)
                return@setOnClickListener
            }

            if (passwordView!!.text.length <= 4) {
                Utils.toast(R.string.password_short, this)
                return@setOnClickListener
            }

            val signup = confirmPasswordView!!.visibility == View.VISIBLE
            if (signup && confirmPasswordView!!.text
                            .toString() != passwordView!!.text.toString()) {
                Utils.toast(R.string.password_no_match, this)
                return@setOnClickListener
            }

            Settings.setServerUrl(serverText, this@LoginActivity)
            server = UserServer(serverText)
            val postUser = User()
            postUser.name = usernameView!!.text.toString()
            postUser.password = Utils.encode(passwordView!!.text.toString())

            val userCallback = object : UserServer.UserCallback {
                override fun onSuccess(user: User) {
                    hideProgress()

                    if (!user.verified) {
                        AlertDialog.Builder(this@LoginActivity)
                                .setMessage(R.string.not_verified)
                                .setPositiveButton(R.string.ok, null).show()
                    } else {
                        user.save(this@LoginActivity)
                        launchMainActivity(user)
                    }
                }

                override fun onFailure(code: Int) {
                    hideProgress()
                    if (code == Status.UserAlreadyExists) {
                        Utils.toast(R.string.username_exists, this@LoginActivity)
                    } else if (code == Status.InvalidPassword) {
                        Utils.toast(R.string.username_password_wrong, this@LoginActivity)
                    } else {
                        Utils.toast(R.string.server_offline, this@LoginActivity)
                    }
                }
            }

            showProgress()
            if (signup) {
                server!!.signUp(postUser, userCallback)
            } else {
                server!!.login(postUser, userCallback)
            }
        }

        val switchBtn = findViewById<Button>(R.id.switch_btn)
        switchBtn.setOnClickListener { _ ->
            confirmPasswordView!!.visibility = if (confirmPasswordView!!.visibility == View.VISIBLE)
                View.GONE
            else
                View.VISIBLE
            switchBtn.setText(if (confirmPasswordView!!.visibility == View.VISIBLE)
                R.string.switch_login
            else
                R.string.switch_signup)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (serverView != null) {
            outState.putString("server_url", serverView!!.text.toString())
            outState.putString("username", usernameView!!.text.toString())
            outState.putString("password", passwordView!!.text.toString())
            outState.putString("confirm_password", confirmPasswordView!!.text.toString())
            outState.putBoolean("progress", progressDialog != null)
        }
    }

    override fun onPause() {
        super.onPause()

        server?.close()
        hideProgress()
    }

    private fun launchMainActivity(user: User) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra(MainActivity.USER_INTENT, user)
        startActivity(intent)
        finish()
    }

    private fun showProgress() {
        if (currentFocus != null) {
            Utils.hideKeyboard(currentFocus!!)
        }
        progressDialog = ProgressDialog(this)
        progressDialog!!.setMessage(getString(R.string.loading))
        progressDialog!!.setCancelable(false)
        progressDialog!!.show()
    }

    private fun hideProgress() {
        if (progressDialog != null) {
            progressDialog!!.dismiss()
            progressDialog = null
        }
    }
}
