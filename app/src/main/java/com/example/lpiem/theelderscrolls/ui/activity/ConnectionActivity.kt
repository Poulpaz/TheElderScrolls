package com.example.lpiem.theelderscrolls.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.lpiem.theelderscrolls.R
import com.example.lpiem.theelderscrolls.datasource.NetworkEvent
import com.example.lpiem.theelderscrolls.datasource.request.RegisterData
import com.example.lpiem.theelderscrolls.manager.GoogleConnectionManager
import com.example.lpiem.theelderscrolls.utils.RxLifecycleDelegate
import com.example.lpiem.theelderscrolls.utils.disposedBy
import com.example.lpiem.theelderscrolls.viewmodel.ConnectionActivityViewModel
import com.facebook.*
import com.jakewharton.rxbinding2.view.clicks
import com.facebook.appevents.AppEventsLogger
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.android.synthetic.main.connection_activity.*
import timber.log.Timber
import org.kodein.di.generic.instance
import com.facebook.GraphRequest
import org.json.JSONException


class ConnectionActivity : BaseActivity() {

    private val viewModel: ConnectionActivityViewModel by instance(arg = this)

    private val RC_SIGN_IN = 0
    private var TAG = "ConnectionActivity"
    private var callbackManager: CallbackManager? = null
    private val googleManager: GoogleConnectionManager by instance()

    companion object {
        fun start(fromActivity: AppCompatActivity) {
            fromActivity.startActivity(
                    Intent(fromActivity, ConnectionActivity::class.java)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.connection_activity)
        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(this)

        b_login_google.setOnClickListener {
            loginWithGoogle()
        }

        b_login_facebook.setOnClickListener {
            loginWithFacebook()
        }

        viewModel.signInState
                .takeUntil(lifecycle(RxLifecycleDelegate.ActivityEvent.DESTROY))
                .subscribe(
                {
                    when (it) {
                        NetworkEvent.None -> {
                            // Nothing
                        }
                        NetworkEvent.InProgress -> {
                            onSignInStateInProgress()
                        }
                        is NetworkEvent.Error -> {
                            onSignInStateError()
                        }
                        is NetworkEvent.Success -> {
                            onSignInStateSuccess()
                        }
                    }
                }, { Timber.e(it) }
        ).disposedBy(viewDisposable)
    }

    //region Facebook & Google

    private fun onSignInStateSuccess() {
        startHome()
        progress_bar_connection_activity.visibility = View.GONE
    }

    private fun onSignInStateError() {
        AccessToken.getCurrentAccessToken()?.let {
            LoginManager.getInstance().logOut()
        }
        googleManager.getGoogleSignInClient().signOut()
        progress_bar_connection_activity.visibility = View.GONE
        Toast.makeText(this, getString(R.string.tv_error_login), Toast.LENGTH_SHORT).show()
    }

    private fun onSignInStateInProgress() {
        progress_bar_connection_activity.visibility = View.VISIBLE
    }

    private fun onSignUpStateSuccess(id : String) {
        viewModel.signIn(id)
        progress_bar_connection_activity.visibility = View.GONE
    }

    private fun onSignUpStateError() {
        Toast.makeText(this, getString(R.string.tv_error_signup), Toast.LENGTH_SHORT).show()
        progress_bar_connection_activity.visibility = View.GONE
    }

    private fun onSignUpStateInProgress() {
        progress_bar_connection_activity.visibility = View.VISIBLE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager?.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    //region Facebook

    fun loginWithFacebook() {

        callbackManager = CallbackManager.Factory.create()
        LoginManager.getInstance().registerCallback(callbackManager,
                object : FacebookCallback<LoginResult> {
                    override fun onSuccess(loginResult: LoginResult) {
                        signInUserWithFacebook(loginResult)
                    }

                    override fun onCancel() {}

                    override fun onError(error: FacebookException) {
                        displayErrorFacebook()
                    }
                })
    }

    private fun displayErrorFacebook(){
        Toast.makeText(this, getString(R.string.tv_error_signup), Toast.LENGTH_SHORT).show()
    }

    private fun signInUserWithFacebook(loginResult: LoginResult) {
        val request = GraphRequest.newMeRequest(
                loginResult.accessToken
        ) { _, _ ->

            try {

                val profile = Profile.getCurrentProfile()

                if (profile != null) {
                    val id = profile.id
                    val firstName = profile.firstName
                    val lastName = profile.lastName
                    val photoUri = Profile.getCurrentProfile().getProfilePictureUri(200, 200)
                    val registerData = RegisterData(id, firstName, lastName, 30, photoUri.toString())
                    viewModel.accountExistState.subscribe(
                            {
                                if (!it) {
                                    val dialog = AlertDialog.Builder(this)
                                    dialog.setTitle(R.string.tv_title_dialog_signup)
                                            .setMessage(R.string.tv_message_dialog_signup)
                                            .setNegativeButton(R.string.b_cancel_dialog_signup) { _, _ -> }
                                            .setPositiveButton(R.string.b_validate_dialog_signup) { _, _ ->
                                                signUpWithFacebook(registerData)
                                            }.show()
                                }
                            },
                            { Timber.e(it) }
                    ).disposedBy(viewDisposable)
                    viewModel.signIn(id)
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        request.executeAsync()
    }

    fun signUpWithFacebook(registerData : RegisterData){
        viewModel.signUp(registerData).subscribe(
                {
                    when (it) {
                        NetworkEvent.None -> {
                            // Nothing
                        }
                        NetworkEvent.InProgress -> {
                            onSignUpStateInProgress()
                        }
                        is NetworkEvent.Error -> {
                            onSignUpStateError()
                        }
                        is NetworkEvent.Success -> {
                            onSignUpStateSuccess(registerData.id)
                        }
                    }
                }, { Timber.e(it) }
        ).disposedBy(viewDisposable)
    }

    //region Google

    fun loginWithGoogle() {
        val signInIntent = googleManager.getGoogleSignInClient().getSignInIntent()
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            updateUI(account)
        } catch (e: ApiException) {
            Log.w(TAG, "signInResult:failed code=" + e.statusCode)
            updateUI(null)
        }
    }

    private fun updateUI(account: GoogleSignInAccount?) {
        if (account != null) {
            val id = account.id
            val firstName = account.givenName
            val lastName = account.familyName
            val photoUri = account.photoUrl
            if(id.isNullOrEmpty() || firstName.isNullOrEmpty() || lastName.isNullOrEmpty()){
                Toast.makeText(this, getString(R.string.tv_error_login), Toast.LENGTH_SHORT).show()
            } else {
                val registerData = RegisterData(id, firstName, lastName, 30, photoUri.toString())
                viewModel.accountExistState.subscribe(
                        {
                            if (!it) {
                                val dialog = AlertDialog.Builder(this)
                                dialog.setTitle(R.string.tv_title_dialog_signup)
                                        .setMessage(R.string.tv_message_dialog_signup)
                                        .setNegativeButton(R.string.b_cancel_dialog_signup, { _, _ -> })
                                        .setPositiveButton(R.string.b_validate_dialog_signup) { _, _ ->
                                            signUpWithFacebook(registerData)
                                        }.show()
                            }
                        },
                        { Timber.e(it) }
                ).disposedBy(viewDisposable)
                viewModel.signIn(id)
            }
        }
    }

    private fun startHome() {
        MainActivity.start(this@ConnectionActivity)
    }

    override fun onStop() {
        super.onStop()
        finish()
    }
}