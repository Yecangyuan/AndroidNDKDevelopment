package com.simley.lib_base

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

public abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    protected val binding: VB by lazy { getViewBinding(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initView()
        initData()
    }

    protected fun initView() {}

    protected fun initData() {}

    abstract fun getViewBinding(layoutInflater: LayoutInflater): VB

}