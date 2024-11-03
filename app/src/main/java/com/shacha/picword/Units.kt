package com.shacha.picword

sealed class Units(open val value: Number) {
    companion object {
        inline operator fun <reified T : Units> T.plus(that: T) =
            T::class.constructors.first().call(value.toDouble() + that.toDouble())

        inline operator fun <reified T : Units> T.minus(that: T) =
            T::class.constructors.first().call(value.toDouble() - that.toDouble())

        inline operator fun <reified T : Units> T.times(that: T) =
            T::class.constructors.first().call(value.toDouble() * that.toDouble())

        inline operator fun <reified T : Units> T.div(that: T) =
            T::class.constructors.first().call(value.toDouble() / that.toDouble())

        operator fun Number.plus(that: Units): Double = this.toDouble() + that.toDouble()
        operator fun Number.minus(that: Units): Double = this.toDouble() - that.toDouble()
        operator fun Number.times(that: Units): Double = this.toDouble() * that.toDouble()
        operator fun Number.div(that: Units): Double = this.toDouble() / that.toDouble()

        val Number.mm get() = Millimeter(this)
        val Number.px get() = Pixel(this)
        val Number.cm get() = Centimeter(this)
        val Number.emu get() = EnglishMetricUnits(this)
    }

    abstract fun toPixel(): Pixel
    abstract fun toMillimeter(): Millimeter
    abstract fun toCentimeter(): Centimeter
    abstract fun toEMU(): EnglishMetricUnits
    fun toDouble() = value.toDouble()
    fun toInt() = value.toInt()
    operator fun plus(that: Number): Double = value.toDouble() + that.toDouble()
    operator fun minus(that: Number): Double = value.toDouble() - that.toDouble()
    operator fun times(that: Number): Double = value.toDouble() * that.toDouble()
    operator fun div(that: Number): Double = value.toDouble() / that.toDouble()

    data class Pixel(override val value: Number) : Units(value) {
        override fun toPixel() = this
        override fun toMillimeter() = Millimeter(this / 96.0 * 25.4)
        override fun toCentimeter() = toMillimeter().toCentimeter()
        override fun toEMU() = EnglishMetricUnits(this * org.apache.poi.util.Units.EMU_PER_PIXEL)
    }

    data class Millimeter(override val value: Number) : Units(value) {
        override fun toPixel() = Pixel(this / 25.4 * 96.0)
        override fun toMillimeter() = this
        override fun toCentimeter() = Centimeter(this / 10)
        override fun toEMU() = toPixel().toEMU()
    }

    data class Centimeter(override val value: Number) : Units(value) {
        override fun toPixel() = toMillimeter().toPixel()
        override fun toMillimeter() = Millimeter(this * 10)
        override fun toCentimeter() = this
        override fun toEMU() = toPixel().toEMU()
    }

    data class EnglishMetricUnits(override val value: Number) : Units(value) {
        override fun toPixel() = Pixel(this / org.apache.poi.util.Units.EMU_PER_PIXEL)
        override fun toMillimeter() = toPixel().toMillimeter()
        override fun toCentimeter() = toMillimeter().toCentimeter()
        override fun toEMU() = this
    }
}

