package solvers.channels

import Board

class CellChannel(val x : Int, val y : Int, private val n : Int) : Channel {

	constructor(i : Int, b : Board) : this(i % b.dim, i / b.dim, b.n)

	val options = BooleanArray(n * n) {true}

	override var freedom : Int = n * n
		private set
	override val filled : Boolean
		get() = freedom == 0

	override fun update(x : Int, y : Int, v : Int) : Boolean {
		if (x == this.x && y == this.y) {
			freedom = 0
			for (i in options.indices) {options[i] = false}
			return false
		}
		if (options[v - 1]) {
			if (x == this.x || y == this.y ||(x / n == this.x / n && y / n == this.y / n)) {
				options[v - 1] = false
				--freedom
			}
		}
		return freedom == 1
	}

	override fun negUpdate(x : Int, y : Int, v : Int) : Boolean {
		if (x == this.x && y == this.y) {
			if (options[v - 1]) {
				options[v - 1] = false
				--freedom
			}
		}
		return freedom == 1
	}

	override fun getUpdate() = CellUpdate(x, y, options.indexOfFirst { it } + 1)

	override fun reset() {
		for (i in options.indices) {
			options[i] = true
		}
		freedom = n * n
	}

	fun copy() : CellChannel {
		val ret = CellChannel(x, y, n)
		options.copyInto(ret.options)
		ret.freedom = freedom
		return ret
	}
}