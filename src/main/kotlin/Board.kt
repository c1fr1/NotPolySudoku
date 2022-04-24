import solvers.channels.CellUpdate
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStreamReader
import kotlin.math.sqrt

class Board {

	constructor(path : String) {
		val reader = BufferedInputStream(File(path).inputStream())
		val lines = InputStreamReader(reader).readLines()
		n = sqrt(lines.size.toDouble()).toInt()
		dim = n * n
		area = dim * dim
		val a = lines.map { l -> l.split(' ').mapNotNull { it.toIntOrNull() } }
		data = Array(dim) {y -> Array(dim) {x -> a[y][x]} }
	}

	constructor(n : Int = 3) {
		this.n = n
		dim = n * n
		area = dim * dim
		data = Array(dim) {Array(dim) {0} }
	}

	val n : Int

	val data : Array<Array<Int>>

	val dim : Int

	val area : Int

	operator fun get(x : Int, y : Int) = data[y][x]
	operator fun set(x : Int, y : Int, v : Int) {
		data[y][x] = v
	}

	operator fun get(pos : Pos) = data[pos.y][pos.x]
	operator fun set(pos : Pos, v : Int) = set(pos.x, pos.y, v)

	fun row(y : Int) = data[y]
	fun col(x : Int) = (0 until this.dim).map { data[it][x] }
	fun box(x : Int, y : Int) = Array(n * n) { i ->
		data[y * n + i / n][x * n + i % n]
	}

	fun rowToString(i : Int) : String {
		return data[i].fold(StringBuilder()) {acc, x -> acc.append("$x ")}.toString()
	}

	override fun toString() : String {
		return data.indices.fold(StringBuilder()) {acc, i -> acc.append(rowToString(i) + "\n")}.toString()
	}

	fun update(cu : CellUpdate) = set(cu.x, cu.y, cu.v)

	fun checkCompleteCorrect() : Boolean {
		for (y in 0 until this.dim) {
			if ((1..this.dim).any { !data[y].contains(it) }) {
				return false
			}
		}
		for (x in 0 until this.dim) {
			if ((1..this.dim).any { !col(x).contains(it) }) {
				return false
			}
		}
		for (x in 0 until n) {
			for (y in 0 until n) {
				val box = box(x, y)
				if ((1..this.dim).any { !box.contains(it) }) {
					return false
				}
			}
		}
		return true
	}

	fun copy() : Board {
		val ret = Board(n)
		for (x in data.indices) for (y in data.indices) {
			ret[x, y] = get(x, y)
		}
		return ret
	}
}

open class Pos(var x : Int, var y : Int)
