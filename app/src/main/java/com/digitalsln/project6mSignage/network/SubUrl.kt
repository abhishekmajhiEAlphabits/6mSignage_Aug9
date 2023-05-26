package com.digitalsln.project6mSignage.network

object SubUrl {
    var screenCodeUrl = "GBVVC123"

    private fun getWeekDay(day: Int): Int {
        when (day) {
            0 -> {
                return 0
            }
            1 -> {
                return 1
            }
            2 -> {
                return 2
            }
            3 -> {
                return 3
            }
            4 -> {
                return 4
            }
            5 -> {
                return 5
            }
            6 -> {
                return 6
            }
        }
        return 0
    }
}