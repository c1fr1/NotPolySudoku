package solvers.channels

import Board

class ColChannel(val c : Int, val v : Int, private val n : Int) : Channel {

	constructor(i : Int, b : Board) : this(i / b.dim, i % b.dim + 1, b.n)

	private var hasGivenPartialUpdate = false
	val options = BooleanArray(n * n) {true}
	override var freedom : Int = n * n
	override val filled : Boolean
		get() = freedom == 0

	override fun update(x : Int, y : Int, v : Int) : Boolean {
		if (x == c) {
			if (v == this.v) {
				freedom = 0
				return false
			} else if (options[y]) {
				options[y] = false
				--freedom
			}
		} else if (x / n == c / n && v == this.v) {
			for (dy in 0 until n) {
				val i = (y / n) * n + dy
				if (options[i]) {
					options[i] = false
					--freedom
				}
			}
		} else if (v == this.v) {
			if (options[y]) {
				options[y] = false
				--freedom
			}
		}
		return freedom == 1
	}

	override fun negUpdate(x : Int, y : Int, v : Int) : Boolean {
		if (x == c && v == this.v) {
			if (options[y]) {
				options[y] = false
				--freedom
			}
		}
		return freedom == 1
	}

	override fun getUpdate() = CellUpdate(c, options.indexOfFirst { it }, v)
	override fun getPartialUpdate(f : (CellUpdate) -> Unit) : Boolean {
		if (hasGivenPartialUpdate || filled) return false
		for (testY in 0 until n) {
			if ((0 until (n * n)).all { it / n == testY || !options[it] }) {
				for (lx in 0 until n) for (ly in 0 until n) {
					if (lx != c % n) {
						f(CellUpdate(n * (c / n) + lx, testY * n + ly, v))
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

	fun copy() : ColChannel {
		val ret = ColChannel(c, v, n)
		ret.hasGivenPartialUpdate = hasGivenPartialUpdate
		options.copyInto(ret.options)
		ret.freedom = freedom
		return ret
	}
}