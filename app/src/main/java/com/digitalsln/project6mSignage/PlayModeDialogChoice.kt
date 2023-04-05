package com.digitalsln.project6mSignage

enum class PlayModeDialogChoice(val code: Int) {
    REAL(0),
    TEST(1);

    companion object {
        fun getChoice(code: Int): PlayModeDialogChoice {
            return when (code) {
                0 -> REAL
                1 -> TEST
                else -> TEST
            }
        }
    }
}