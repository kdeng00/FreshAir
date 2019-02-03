package com.example.freshair

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.app.LoaderManager.LoaderCallbacks
import android.content.CursorLoader
import android.content.Loader
import android.database.Cursor
import kotlin.collections.HashMap
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.TextView

import java.util.ArrayList
import android.Manifest.permission.READ_CONTACTS
import android.support.design.widget.TextInputEditText

import kotlinx.android.synthetic.main.activity_create_account.*

import com.example.freshair.database.DBContract
import com.example.freshair.database.UsersDBHelper
import com.example.freshair.models.User as Um

/**
 * A login screen that offers login via email/password.
 */
class CreateAccountActivity : AppCompatActivity(), LoaderCallbacks<Cursor> {
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private var mAuthTask: UserCreateTask? = null
    private var focusView: View? = null
    private var cancel: Boolean = false

    private var fieldValues: HashMap<String, String> = HashMap<String, String>()

    private lateinit var  textInput : TextInputEditText

    lateinit var usersDBHelper: UsersDBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)
        setupActionBar()

        usersDBHelper = UsersDBHelper(this)

        create_account_button.setOnClickListener { createAccount() }
    }

    private fun populateAutoComplete() {
        if (!mayRequestContacts()) {
            return
        }

        loaderManager.initLoader(0, null, this)
    }

    private fun mayRequestContacts(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(email, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                .setAction(android.R.string.ok,
                    { requestPermissions(arrayOf(READ_CONTACTS), REQUEST_READ_CONTACTS) })
        } else {
            requestPermissions(arrayOf(READ_CONTACTS), REQUEST_READ_CONTACTS)
        }
        return false
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete()
            }
        }
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private fun setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun createAccount() {
        if (mAuthTask != null) {
            return
        }

        configureFieldValues()

        cancel = false
        focusView = null

        validateFields()


        if (cancel) {
            focusView?.requestFocus()
        } else {
            showProgress(true)
            var user  = Um(fieldValues["firstname"].toString(), fieldValues["lastname"].toString(), fieldValues["email"].toString(),
                fieldValues["username"].toString(), fieldValues["password"].toString())
            mAuthTask = UserCreateTask(user)
            mAuthTask!!.execute(null as Void?)
        }
    }

    private fun configureFieldValues() {
        fieldValues["firstname"] =  firstname.text.toString()
        fieldValues["lastname"] =  lastname.text.toString()
        fieldValues["email"] =  email.text.toString()
        fieldValues["username"] =  username.text.toString()
        fieldValues["password"] =  password.text.toString()
        fieldValues["passwordConfirm"] =  passwordConfirm.text.toString()
    }
    private  fun validateFields() {
        if (areFieldsEmpty()) {
            return
        }

        if (!isPasswordValid(fieldValues["password"].toString())) {
            password.error = getString(R.string.error_invalid_password)
            focusView = password
            cancel = true
        }

        if (!isPasswordMatching(fieldValues["password"].toString(),fieldValues["passwordConfirm"].toString())) {
            password.error = "Passwords do not match"
            focusView = password
            cancel = true
        }

        if (!isEmailValid(fieldValues["email"].toString())) {
            email.error = getString(R.string.error_invalid_email)
            focusView = email
            cancel = true
        }
    }
    private fun areFieldsEmpty() : Boolean {

        var fieldLabels = LinkedHashSet<String>()
        fieldLabels.add("firstname")
        fieldLabels.add("lastname")
        fieldLabels.add("email")
        fieldLabels.add("username")
        fieldLabels.add("password")
        fieldLabels.add("passwordConfirm")

        for (field: String in fieldLabels) {
            when (field) {
                "firstname" -> {
                    if (TextUtils.isEmpty(fieldValues[field].toString())) {
                        firstname.error = getString(R.string.error_field_required)
                        focusView = email
                        cancel = true
                        return true
                    }
                }
                "lastname" -> {
                    if (TextUtils.isEmpty(fieldValues[field].toString())) {
                        lastname.error = getString(R.string.error_field_required)
                        focusView = lastname
                        cancel = true
                        return true
                    }
                }
                "email" -> {
                    if (TextUtils.isEmpty(fieldValues[field].toString())) {
                        focusView = email
                        cancel = true
                        return true
                    }
                }
                "username" -> {
                    if (TextUtils.isEmpty(fieldValues[field].toString())) {
                        focusView = username
                        cancel = true
                        return true
                    }
                }
                "password" -> {
                    if (TextUtils.isEmpty(fieldValues[field].toString())) {
                        focusView = password
                        cancel = true
                        return true
                    }
                }
                "passwordConfirm" -> {
                    if (TextUtils.isEmpty(fieldValues[field].toString())) {
                        focusView = passwordConfirm
                        cancel = true
                        return true
                    }
                }
            }
        }

        return false
    }

    private fun isEmailValid(email: String): Boolean {
        return email.contains("@")
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length > 4
    }

    private fun isPasswordMatching(password: String, passwordConfirm: String) : Boolean {
        var result = true

        if (!password.equals(passwordConfirm, ignoreCase = true)) {
            result = false
         }

        return result
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private fun showProgress(show: Boolean) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

            login_form.visibility = if (show) View.GONE else View.VISIBLE
            login_form.animate()
                .setDuration(shortAnimTime)
                .alpha((if (show) 0 else 1).toFloat())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        login_form.visibility = if (show) View.GONE else View.VISIBLE
                    }
                })

            login_progress.visibility = if (show) View.VISIBLE else View.GONE
            login_progress.animate()
                .setDuration(shortAnimTime)
                .alpha((if (show) 1 else 0).toFloat())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        login_progress.visibility = if (show) View.VISIBLE else View.GONE
                    }
                })
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            login_progress.visibility = if (show) View.VISIBLE else View.GONE
            login_form.visibility = if (show) View.GONE else View.VISIBLE
        }
    }

    override fun onCreateLoader(i: Int, bundle: Bundle?): Loader<Cursor> {
        return CursorLoader(
            this,
            // Retrieve data rows for the device user's 'profile' contact.
            Uri.withAppendedPath(
                ContactsContract.Profile.CONTENT_URI,
                ContactsContract.Contacts.Data.CONTENT_DIRECTORY
            ), ProfileQuery.PROJECTION,

            // Select only email addresses.
            ContactsContract.Contacts.Data.MIMETYPE + " = ?", arrayOf(
                ContactsContract.CommonDataKinds.Email
                    .CONTENT_ITEM_TYPE
            ),

            // Show primary email addresses first. Note that there won't be
            // a primary email address if the user hasn't specified one.
            ContactsContract.Contacts.Data.IS_PRIMARY + " DESC"
        )
    }

    override fun onLoadFinished(cursorLoader: Loader<Cursor>, cursor: Cursor) {
        val emails = ArrayList<String>()
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS))
            cursor.moveToNext()
        }

        //addEmailsToAutoComplete(emails)
    }

    override fun onLoaderReset(cursorLoader: Loader<Cursor>) {

    }

    object ProfileQuery {
        val PROJECTION = arrayOf(
            ContactsContract.CommonDataKinds.Email.ADDRESS,
            ContactsContract.CommonDataKinds.Email.IS_PRIMARY
        )
        val ADDRESS = 0
        val IS_PRIMARY = 1
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    inner class UserCreateTask internal constructor(private val user: Um) :
        AsyncTask<Void, Void, Boolean>() {

        val mEmail = user.email
        val mPassword = user.password

        override fun doInBackground(vararg params: Void): Boolean? {
            // TODO: attempt authentication against a network service.

            try {
                // Simulate network access.
                Thread.sleep(2000)
            } catch (e: InterruptedException) {
                return false
            }

            return DUMMY_CREDENTIALS
                .map { it.split(":") }
                .firstOrNull { it[0] == mEmail }
                ?.let {
                    // Account exists, return true if the password matches.
                    it[1] == mPassword
                }
                ?: true
        }

        override fun onPostExecute(success: Boolean?) {
            mAuthTask = null
            showProgress(false)

            if (success!!) {
                finish()
            } else {
                passwordConfirm.error = getString(R.string.error_incorrect_password)
                passwordConfirm.requestFocus()
            }
        }

        override fun onCancelled() {
            mAuthTask = null
            showProgress(false)
        }
    }

    companion object {

        /**
         * Id to identity READ_CONTACTS permission request.
         */
        private val REQUEST_READ_CONTACTS = 0

        /**
         * A dummy authentication store containing known user names and passwords.
         * TODO: remove after connecting to a real authentication system.
         */
        private val DUMMY_CREDENTIALS = arrayOf("foo@example.com:hello", "bar@example.com:world")
    }
}