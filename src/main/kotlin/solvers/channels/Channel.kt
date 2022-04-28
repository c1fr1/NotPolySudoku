package solvers.channels

import Pos

class CellUpdate(xp : Int, yp : Int, val v : Int) : Pos(xp, yp) {
	override fun equals(other : Any?) : Boolean {
		return if (other is CellUpdate) {
			other.x == x && other.y == y && other.v == v
		} else false
	}
	fun copy() : CellUpdate {
		return CellUpdate(x, y, v)
	}

	override fun toString() : String {
		return "($x, $y) : $v"
	}
}

interface Channel {
	val freedom : Int
	val filled : Boolean

	fun update(cu : CellUpdate) = update(cu.x, cu.y, cu.v)
	fun update(x : Int, y : Int, v : Int) : Boolean

	fun negUpdate(cu : CellUpdate) = negUpdate(cu.x, cu.y, cu.v)
	fun negUpdate(x : Int, y : Int, v : Int) : Boolean

	fun getUpdate() : CellUpdate

	fun getPartialUpdate(f : (CellUpdate) -> Unit) : Boolean {return false}

	fun reset()
}
