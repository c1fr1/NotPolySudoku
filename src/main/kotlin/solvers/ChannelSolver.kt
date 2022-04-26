package solvers

import Board
import Pos
import solvers.channels.*

class ChannelSolver {

	private constructor(o : ChannelSolver) {

		board = o.board.copy()

		n = o.n
		cellChannels = Array(o.cellChannels.size) {o.cellChannels[it].copy()}
		boxChannels = Array(o.boxChannels.size) {o.boxChannels[it].copy()}
		rowChannels = Array(o.rowChannels.size) {o.rowChannels[it].copy()}
		colChannels = Array(o.colChannels.size) {o.colChannels[it].copy()}

		channels = Array(4 * board.area) { i ->
			val type = i / board.area
			val li = i % board.area
			when (type) {
				0 -> cellChannels[li]
				1 -> boxChannels[li]
				2 -> rowChannels[li]
				else -> colChannels[li]
			}
		}
	}

	constructor(b : Board) {
		this.board = b
		n = b.n
		cellChannels = Array(b.area) { CellChannel(it, b) }
		boxChannels = Array(b.area) { BoxChannel(it, b) }
		rowChannels = Array(b.area) { RowChannel(it, b) }
		colChannels = Array(b.area) { ColChannel(it, b) }

		val area = n * n * n * n
		channels = Array(4 * area) { i ->
			val type = i / area
			val li = i % area
			when (type) {
				0 -> cellChannels[li]
				1 -> boxChannels[li]
				2 -> rowChannels[li]
				else -> colChannels[li]
			}
		}
		import(b)
	}

	constructor(n : Int) : this(Board(n))

	val board : Board

	val n : Int
	val cellChannels : Array<CellChannel>
	val boxChannels : Array<BoxChannel>
	val rowChannels : Array<RowChannel>
	val colChannels : Array<ColChannel>

	val channels : Array<Channel>

	private val oneFreedomQueue = ArrayList<Channel>()
	private val negUpdateQueue = ArrayList<CellUpdate>()

	fun import(b : Board) {
		reset()
		for (x in 0 until b.dim) for (y in 0 until b.dim) {
			if (b[x, y] != 0) {
				update(x, y, b[x, y])
			}
		}
	}

	fun solve() : Boolean {
		var progressed = false
		while (oneFreedomQueue.isNotEmpty() || negUpdateQueue.isNotEmpty()) {
			if (doChannelSolveStep()) progressed = true
		}
		return progressed
	}

	fun clearNegQueue() {
		while (negUpdateQueue.isNotEmpty()) {
			negUpdate(negUpdateQueue.removeFirst())
		}
	}

	fun doChannelSolveStep() : Boolean {
		if (oneFreedomQueue.isNotEmpty()) {
			var ch = oneFreedomQueue.removeFirst()
			while (ch.filled || ch.freedom == 0) {
				if (oneFreedomQueue.isEmpty()) {
					return false
				}
				ch = oneFreedomQueue.removeFirst()
			}

			val update = ch.getUpdate()
			update(update)
			return true
		} else if (negUpdateQueue.isNotEmpty()) {
			val ne = negUpdateQueue.removeFirst()
			negUpdate(ne)
			return true
		}
		return false
	}

	fun update(cu : CellUpdate) = update(cu.x, cu.y, cu.v)

	fun update(x : Int, y : Int, v : Int) {
		board[x, y] = v

		for (channel in channels) {
			if (!channel.filled) {
				if (channel.update(x, y, v)) oneFreedomQueue.add(channel)
				channel.getPartialUpdate { queueNegUpdate(it) }
			}
		}
	}

	fun negUpdate(cu : CellUpdate) = negUpdate(cu.x, cu.y, cu.v)
	fun negUpdate(x : Int, y : Int, v : Int) {
		for (channel in channels) {
			if (!channel.filled) {
				if (channel.negUpdate(x, y, v)) oneFreedomQueue.add(channel)
				channel.getPartialUpdate { queueNegUpdate(it) }
			}
		}
	}

	fun queueNegUpdate(cu : CellUpdate) = queueNegUpdate(cu.x, cu.y, cu.v)
	fun queueNegUpdate(x : Int, y : Int, v : Int) : Boolean {
		if (cellChannelFor(x, y).options[v - 1]) {
			if (!negUpdateQueue.contains(CellUpdate(x, y, v))) {
				negUpdateQueue.add(CellUpdate(x, y, v))
			}
			return true
		}
		return false
	}

	fun cellChannelFor(x : Int, y : Int) : CellChannel {
		return cellChannels[x + y * board.dim]
	}
	fun cellChannelFor(p : Pos) = cellChannelFor(p.x, p.y)

	fun boxChannelFor(x : Int, y : Int, v : Int) : BoxChannel {
		val bx = x / n
		val by = y / n
		return boxChannels[bx + n * by + board.dim * (v - 1)]
	}
	fun boxChannelFor(cu : CellUpdate) = boxChannelFor(cu.x, cu.y, cu.v)

	fun colChannelFor(x : Int, v : Int) : ColChannel {
		return colChannels[v - 1 + x * board.dim]
	}
	fun colChannelFor(cu : CellUpdate) = colChannelFor(cu.x, cu.v)

	fun rowChannelFor(y : Int, v : Int) : RowChannel {
		return rowChannels[v - 1 + y * board.dim]
	}
	fun rowChannelFor(cu : CellUpdate) = rowChannelFor(cu.y, cu.v)

	fun copy() : ChannelSolver {
		return ChannelSolver(this)
	}

	fun reset() {
		for (x in 0 until board.dim) {
			for (y in 0 until board.dim) {
				board[x, y] = 0
			}
		}
		for (c in channels) {
			c.reset()
		}
		oneFreedomQueue.clear()
		negUpdateQueue.clear()
	}

	fun impossibleState() : Boolean {
		if (cellChannels.any { it.freedom == 0 && board[it.x, it.y] == 0}) return true

		if (boxChannels.any { it.freedom == 0 && board.box(it.x, it.y).all { v -> v != it.v } }) return true
		if (rowChannels.any { it.freedom == 0 && board.row(it.r).all { v -> v != it.v } }) return true
		if (colChannels.any { it.freedom == 0 && board.col(it.c).all { v -> v != it.v } }) return true

		return false
	}

	fun getRandomGuess() : CellUpdate? {
		val cc = cellChannels.filter { !it.filled }.randomOrNull() ?: return null
		val v = cc.options.indices.filter { i -> cc.options[i] }.random() + 1
		return CellUpdate(cc.x, cc.y, v)
	}

	fun applyRandomGuess() : Boolean {
		update(getRandomGuess() ?: return false)
		return true
	}

	fun possibleUpdates() : ArrayList<CellUpdate> {
		val possibleUpdates = ArrayList<CellUpdate>()
		for (channel in cellChannels.filter { !it.filled }) {
			for (opt in channel.options.indices.filter { channel.options[it] }) {
				possibleUpdates.add(CellUpdate(channel.x, channel.y, opt + 1))
			}
		}
		return possibleUpdates
	}
}