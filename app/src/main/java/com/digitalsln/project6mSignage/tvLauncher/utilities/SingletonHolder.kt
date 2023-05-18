package com.digitalsln.project6mSignage.tvLauncher.utilities

open class SingletonHolder<out T>(private val constructor: () -> T) {

    @Volatile
    private var instance: T? = null

    fun getInstance(): T =
        instance ?: synchronized(this) {
            instance ?: constructor().also { instance = it }
        }
}