package com.shacha.picword


enum class RotateType {
    ROTATE_TO_LANDSCAPE,
    ROTATE_TO_PORTRAIT
}

enum class RotateDirection(val index: Int, val description: String) {
    IGNORE(0, "忽略"),
    CLOCKWISE(1, "顺时针"),
    COUNTERCLOCKWISE(2, "逆时针");

    companion object {
        val descriptions by lazy { RotateDirection.entries.map { it.description } }
        fun valueOf(index: Int) = when (index) {
            0 -> IGNORE
            1 -> CLOCKWISE
            2 -> COUNTERCLOCKWISE
            else -> throw IllegalStateException()
        }
    }
}