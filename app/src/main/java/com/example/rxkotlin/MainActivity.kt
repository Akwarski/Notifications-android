package com.example.rxkotlin

import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ImageSpan
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isEmpty
import com.google.android.material.snackbar.Snackbar
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<MotionLayout>(R.id.motionLayout).transitionToEnd()
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
