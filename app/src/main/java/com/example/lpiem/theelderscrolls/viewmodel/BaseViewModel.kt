package com.example.lpiem.theelderscrolls.viewmodel

import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable

open class BaseViewModel : ViewModel() {

    protected val disposeBag = CompositeDisposable()

    override fun onCleared() {
        super.onCleared()
        disposeBag.clear()
    }
}