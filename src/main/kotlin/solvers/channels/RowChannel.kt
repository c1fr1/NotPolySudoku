package solvers.channels

import Board

class RowChannel(val r : Int, val v : Int, private val n : Int) : Channel {

	constructor(i : Int, b : Board) : this(i / b.dim, i % b.dim + 1, b.n)

	private var hasGivenPartialUpdate = false
	val options = BooleanArray(n * n) {true}
	override var freedom : Int = n * n
	override val filled : Boolean
		get() = freedom == 0

	override fun update(x : Int, y : Int, v : Int) : Boolean {
		if (y == r) {
			if (v == this.v) {
				freedom = 0
				return false
			} else if (options[x]) {
				options[x] = false
				--freedom
			}
		} else if (y / n == r / n && v == this.v) {
			for (dx in 0 until n) {
				val i = (x / n) * n + dx
				if (options[i]) {
					options[i] = false
					--freedom
				}
			}
		} else if (v == this.v) {
			if (options[x]) {
				options[x] = false
				--freedom
			}
		}
		return freedom == 1
	}

	override fun negUpdate(x : Int, y : Int, v : Int) : Boolean {
		if (y == r && v == this.v) {
			if (options[x]) {
				options[x] = false
				--freedom
			}
		}
		return freedom == 1
	}

	override fun getUpdate() = CellUpdate(options.indexOfFirst { it }, r, v)
	override fun getPartialUpdate(f : (CellUpdate) -> Unit) : Boolean {
		if (hasGivenPartialUpdate || filled) return false
		for (testX in 0 until n) {
			if ((0 until (n * n)).all { it / n == testX || !options[it] }) {
				for (lx in 0 until n) for (ly in 0 until n) {
					if (ly != r % n) {
						f(CellUpdate(n * testX + lx, (r / n) * n + ly, v))
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

	fun copy() : RowChannel {
		val ret = RowChannel(r, v, n)
		ret.hasGivenPartialUpdate = hasGivenPartialUpdate
		options.copyInto(ret.options)
		ret.freedom = freedom
		return ret
	}
}