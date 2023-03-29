package com.simley.lib_mvp

open class BasePresenter<T : IBaseView> : IPresenter<T> {

    private var mView: T? = null
//
//    private var compositeDisposable = CompositeDisposable()
//
    override fun attachView(mRootView: T) {
        this.mView = mRootView
    }
//
    override fun detachView() {
//        // 保证activity结束时取消所有正在执行的订阅
//        if (!compositeDisposable.isDisposed) {
//            compositeDisposable.clear()
//        }
//        mView = null
    }
//
//    private val isViewAttached: Boolean
//        get() = mView != null
//
//    fun checkViewAttached() {
//        if (!isViewAttached) throw MvpViewNotAttachedException()
//    }
//
//    fun addSubscription(disposable: Disposable) {
//        compositeDisposable.add(disposable)
//    }
//
//    private class MvpViewNotAttachedException internal constructor() :
//        RuntimeException("Please call IPresenter.attachView(IBaseView) before" + " requesting data to the IPresenter")

}