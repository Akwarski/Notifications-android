package com.example.rxkotlin

import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var button : Button
    lateinit var warring: Drawable
    var TAG = "main_activity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //findViewById<MotionLayout>(R.id.motionLayout).transitionToEnd()
        warring = getDrawable(R.drawable.ic_error_black_24dp)!!
        warring.bounds = Rect(0, 0, warring.intrinsicWidth, warring.intrinsicHeight)


        emailField()
        passwordField()

        button = findViewById(R.id.button)
        button.setOnClickListener {
            if(et.text.isEmpty() || password.text.isNullOrEmpty()){
                et.error = "Invalid e-mail"
                Toast.makeText(applicationContext, "Invalid Password", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if(emailWrapper.error != null && passwordWrapper.error != null){
                et.error = "Invalid e-mail"
                Toast.makeText(applicationContext, "Invalid Password", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if(emailWrapper.error != null){
                et.error = "Invalid e-mail"
                return@setOnClickListener
            }else
                et.setCompoundDrawables(null, null, null, null)

            if(passwordWrapper.error != null) {
                Toast.makeText(applicationContext, "Invalid Password", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            Snackbar.make(motionLayout, "Successful", Snackbar.LENGTH_LONG).show()
        }

        token()
    }

    private fun token(){
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "getInstanceId failed", task.exception)
                    return@OnCompleteListener
                }

                // Get new Instance ID token
                val token = task.result?.token

                // Log and toast
                val msg = getString(R.string.msg_token_fmt, token)
                Log.d("foool", msg)
                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
            })
    }

    fun runtimeEnableAutoInit() {
        // [START fcm_runtime_enable_auto_init]
        FirebaseMessaging.getInstance().isAutoInitEnabled = true
        // [END fcm_runtime_enable_auto_init]
    }

    private fun emailField(){
        RxTextView.afterTextChangeEvents(et)
            .skipInitialValue()
            .map {
                emailWrapper.error = null
                et.setCompoundDrawables(null, null, null, null)
                it.view().text.toString()
            }
            .debounce(100, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread())
            .compose(verifyEmailPattern())
            .compose(retryWhenError{
                emailWrapper.error = it.message
            }).subscribe()
    }

    private fun passwordField(){
        RxTextView.afterTextChangeEvents(password)
            .skipInitialValue()
            .map{
                passwordWrapper.error = null
                it.view().text.toString()
            }
            .debounce(500,TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread())
            .compose(moreThanSix)
            .compose(retryWhenError {
                passwordWrapper.error = it.message
            })
            .subscribe()
    }

    private fun verifyEmailPattern():ObservableTransformer<String,String>{
        return ObservableTransformer { observable ->
            observable.flatMap {
                Observable.just(it).map { it.trim() }
                    .filter {
                        Patterns.EMAIL_ADDRESS.matcher(it).matches()
                    }
                    .singleOrError()
                    .onErrorResumeNext {
                        if (it is NoSuchElementException){
                            //et.setError("", warring)
                            et.setCompoundDrawables(null, null, warring, null)

                            Single.error(Exception("Email not valid"))
                        }
                        else{
                            Single.error(it)
                        }
                    }.toObservable()
            }
        }
    }

    /*private fun notify(): Observable<Notification>{
        *//*return Observable.fromCallable(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return FirebaseInstanceId.getInstance().getToken();
            }
        });*//*


        Observable.fromCallable()
            .subscribe{

            }
    }*/

    /*private fun notifySecond(): io.reactivex.Notification<Any>{

    }*/

    private val moreThanSix = ObservableTransformer<String,String> { observable->
        observable.flatMap {
            Observable.just(it).map { it.trim() }
                .filter{it.length>6}
                .singleOrError()
                .onErrorResumeNext{
                    if(it is NoSuchElementException){
                        //password.setError("", getDrawable(R.drawable.ic_warning_black_24dp))
                        Single.error(Exception("Password should have more than 6 chars"))
                    }else
                        Single.error(it)
                }.toObservable()
        }
    }

    private inline fun retryWhenError(crossinline onError: (e: Throwable) -> Unit):
            ObservableTransformer<String,String> = ObservableTransformer { observable ->
        observable.retryWhen{ errors->
            errors.flatMap{
                onError(it)
                Observable.just("")
            }
        }
    }
}
