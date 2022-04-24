package solvers.channels

import Board

class BoxChannel(val x : Int, val y : Int, val v : Int, private val n : Int) : Channel {

	constructor(i : Int, b : Board) : this(i % b.n, (i / b.n) % b.n, i / (b.n * b.n) + 1, b.n)

	private var hasGivenPartialUpdate = false
	val options = BooleanArray(n * n) {true}
	override var freedom : Int = n * n
		private set
	override val filled : Boolean
		get() = freedom == 0

	override fun update(x : Int, y : Int, v : Int) : Boolean {
		return if (x / n == this.x && y / n == this.y) {
			if (v == this.v) {
				freedom = 0
				false
			} else {
				val i = x % n + n * (y % n)
				if (options[i]) {
					--freedom
					options[i] = false
				}
				freedom == 1
			}
		} else if (x / n == this.x && v == this.v) {
			for (ly in 0 until n) {
				val i = (x % n) + n * ly
				if (options[i]) {
					--freedom
					options[i] = false
				}
			}
			freedom == 1
		} else if (y / n == this.y && v == this.v) {
			for (lx in 0 until n) {
				val i = lx + n * (y % n)
				if (options[i]) {
					--freedom
					options[i] = false
				}
			}
			freedom == 1
		} else return false
	}

	override fun negUpdate(x : Int, y : Int, v : Int) : Boolean {
		if (v == this.v) {
			if (x / n == this.x && y / n == this.y) {
				val i = x % n + n * (y % n)
				if (options[i]) {
					options[i] = false
					--freedom
				}
			}
		}
		return freedom == 1
	}

	override fun getUpdate() : CellUpdate {
		val i = options.indexOfFirst { it }
		return CellUpdate(i % n + x * n, i / n + y * n, v)
	}

	override fun getPartialUpdate(f : (CellUpdate) -> Unit) : Boolean {
		if (hasGivenPartialUpdate || filled) return false

		for (testRow in 0 until n) {
			val isolatedToRow = (0 until n).all { r -> r == testRow || (0 until n).all {
				!options[it + r * n]
			} }
			if (isolatedToRow) {
				for (rx in 0 until (n * n)) {
					if (x != rx / n) {
						f(CellUpdate(rx, testRow + n * y, v))
					}
				}
				hasGivenPartialUpdate = true
				return true
			}
		}

		for (testCol in 0 until n) {
			val isolatedToCol = (0 until n).all { c -> c == testCol || (0 until n).all {
				!options[c + it * n]
			} }
			if (isolatedToCol) {
				for (cy in 0 until (n * n)) {
					if (y != cy / n) {
						f(CellUpdate(testCol + n * x, cy, v))
					}
				}
				hasGivenPartialUpdate = true
				return true
			}
		}
		return false
	}

	override fun reset() {
		for (i in options.indices) {
			options[i] = true
		}
		freedom = n * n
		hasGivenPartialUpdate = false
	}

	fun copy() : BoxChannel {
		val ret = BoxChannel(x, y, v, n)
		ret.hasGivenPartialUpdate = hasGivenPartialUpdate
		for (i in ret.options.indices) {
			ret.options[i] = options[i]
		}
		ret.freedom = freedom
		return ret
	}
}