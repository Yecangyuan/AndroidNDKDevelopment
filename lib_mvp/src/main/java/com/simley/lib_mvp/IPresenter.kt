package com.simley.lib_mvp

interface IPresenter<in V: IBaseView> {

    fun attachView(mRootView: V)

    fun detachView()

}