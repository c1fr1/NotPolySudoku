package solvers

import Board
import Pos
import solvers.channels.*
import engine.measurePerformanceOnceSeconds
import sun.awt.Mutex

@Deprecated("replaced with functions and ChannelSolver")
class OldSolver(val board : Board) {

	private val n = board.n

	private val oneFreedomQueue = ArrayList<Channel>()
	private val negUpdateQueue = ArrayList<CellUpdate>()

	val cellChannels : Array<CellChannel> = Array(board.area) { CellChannel(it, board) }
	val boxChannels : Array<BoxChannel> = Array(board.area) { BoxChannel(it, board) }
	val rowChannels : Array<RowChannel> = Array(board.area) { RowChannel(it, board) }
	val colChannels : Array<ColChannel> = Array(board.area) { ColChannel(it, board) }

	val mutex = Mutex()

	val boardCases : HashMap<Int, Boolean> = HashMap(board.area * board.area)

	val channels : Array<Channel> = Array(4 * board.area) { i ->
		val type = i / board.area
		val li = i % board.area
		when (type) {
			0 -> cellChannels[li]
			1 -> boxChannels[li]
			2 -> rowChannels[li]
			else -> colChannels[li]
		}
	}

	val groups : Array<Array<Pos>> = Array(3 * board.dim) { i ->
		val type = i / board.dim
		val li = i % board.dim
		when (type) {
			0 -> Array(board.dim) { Pos(it, li) }
			1 -> Array(board.dim) { Pos(li, it) }
			else -> {
				val bx = (li / n) * n
				val by = (li % n) * n
				Array(board.dim) {
					val lx = it / n
					val ly = it % n
					Pos(bx + lx, by + ly)
				}
			}
		}
	}

	fun resetTo(b : Board) {
		mutex.lock()
		reset()
		for (x in b.data.indices) for (y in b.data.indices) {
			if (b[x, y] != 0) {
				update(x, y, b[x, y])
			}
		}
		mutex.unlock()
	}

	fun resetTo(clues : Collection<CellUpdate>) {
		mutex.lock()
		reset()
		for (cu in clues) update(cu)
		mutex.unlock()
	}

	fun channelSolve() : Boolean {
		/* PSEUDOCODE
		while (!done) { //repeat n^4 times
			val ch = oneFreedomChannelQueue.pop()
			if (ch.filled) continue
			for (channel in solvers.channels) { //must have polynomial solvers.channels
				channel.update(ch.cellUpdate)
				if (channel.freedom == 1) oneFreedomChannelQueue.push(channel)
			}
		}
		 */
		/* CHANNELS
		n ^ 4 Cell solvers.channels
		 */
		var progressed = false
		while (oneFreedomQueue.isNotEmpty() || negUpdateQueue.isNotEmpty()) {
			if (doChannelSolveStep()) progressed = true
		}
		return progressed
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

	fun mehSolve() : Boolean {
		var actuallyProgressed = false
		var progressed = true
		while (progressed) {
			progressed = channelSolve()
			progressed = subgroupElimination() || progressed
			if (progressed) actuallyProgressed = true
		}
		return actuallyProgressed
	}

	//returns if there is ANY solution
	fun bruteSolve() : Boolean {
		mehSolve()
		boardCases[board.hashCode()]?.let { return it }
		if (board.checkCompleteCorrect()) return true
		if (mehImpossible()) return false

		val negUpdates = ArrayList<CellUpdate>()
		val copy = board.copy()
		for (cc in cellChannels) {
			for (v in cc.options.indices) {
				if (cc.options[v]) {
					val cu = CellUpdate(cc.x, cc.y, v + 1)
					update(cu)
					if (bruteSolve()) return true
					boardCases[copy.hashCode()] = false
					resetTo(copy)
					for (ne in negUpdates) {
						negUpdate(ne)
					}
					negUpdates.add(cu)
					mehSolve()
				}
			}
		}
		return false
	}

	fun bruteCompleteable() : Boolean {
		val backup = board.copy()
		var ret : Boolean = false
		val time = measurePerformanceOnceSeconds { ret = bruteSolve() }
		println(time)
		resetTo(backup)
		boardCases[backup.hashCode()] = ret
		return ret
	}

	fun bruteFindImpossible() : ArrayList<CellUpdate> {
		reset()
		val start = getCompleteHints()
		if (board.checkCompleteCorrect()) return ArrayList()

		var impStep : CellUpdate? = null

		resetTo(start)

		while (!bruteCompleteable()) {
			impStep = start.removeLast()
			resetTo(start)
		}

		if (impStep == null) {
			return ArrayList()
		}

		resetTo(start)
		mehSolve()
		val copy = board.copy()
		val possibleMoves = ArrayList<CellUpdate>()
		val impossibleMoves = ArrayList<CellUpdate>()
		for (cc in cellChannels) {
			for (v in cc.options.indices.filter { i -> cc.options[i] }) {
				possibleMoves.add(CellUpdate(cc.x, cc.y, v + 1))
			}
		}

		while (possibleMoves.isNotEmpty()) {
			val cu = possibleMoves.removeFirst()
			update(cu)
			val completable = bruteSolve()
			if (!completable) {
				impossibleMoves.add(cu)
			} else {
				possibleMoves.removeAll { board[it.x, it.y] == it.v}
			}
			resetTo(copy)
			for (cu in impossibleMoves) negUpdate(cu)
			mehSolve()
		}

		resetTo(start)
		return impossibleMoves
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
			negUpdateQueue.add(CellUpdate(x, y, v))
			return true
		}
		return false
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

	fun cellChannelFor(x : Int, y : Int) : CellChannel {
		return cellChannels[x + y * board.dim]
	}
	fun cellChannelFor(p : Pos) = cellChannelFor(p.x, p.y)

	fun findCommonSubgroups() {
		for (group in groups) {
			val empties = group.filter { !cellChannelFor(it).filled }
			val numEmpty = empties.size
			for (s in 2..(numEmpty-2)) {
				getSubIndices(s, numEmpty) {ia ->
					val possibleValues = Array(board.dim) { ia.fold(false) {acc, i -> cellChannelFor(empties[i]).options[it] || acc} }
					if (possibleValues.count { it } == s) {
						for (i in possibleValues.indices.filter { possibleValues[it] }) {
							for (oPosI in empties.indices.filter { it !in ia}) {
								queueNegUpdate(CellUpdate(empties[oPosI].x, empties[oPosI].y, i + 1))
							}
						}
					}
				}
				/*
				for 5:
				(0 1) (0 2) (0 3) (0 4) (1 2) (1 3) (1 4) (2 3) (2 4) (3 4)
				(0 1 2) (0 1 3) (0 1 4) (0 2 3) (0 2 4) (0 3 4) (1 2 3) (1 2 4) (1 3 4) (2 3 4)
				for 6:
				(0 1) (0 2) (0 3) (0 4) (0 5) (1 2) (1 3) (1 4) (1 5) (2 3) (2 4) (2 5) (3 4) (3 5) (4 5)
				(0 1 2) (0 1 3) (0 1 4) (0 1 5) (0 2 3) (0 2 4) (0 2 5) (0 3 4) (0 3 5) (0 4 5) (1 2 3) (1 2 4) (1 2 5) (1 3 4) (1 3 5) (1 4 5) (2 3 4) (2 3 5) (2 4 5) (3 4 5)
				(0 1 2 3) (0 1 2 4) (0 1 2 5) (0 1 3 4) (0 1 3 5) (0 1 4 5) (0 2 3 4) (0 2 3 5) (0 2 4 5) (0 3 4 5) (1 2 3 4) (1 2 3 5) (1 2 4 5) (1 3 4 5) (2 3 4 5)
				 */
			}
		}
		while (negUpdateQueue.isNotEmpty()) {
			negUpdate(negUpdateQueue.removeFirst())
		}
	}

	fun subgroupElimination() : Boolean {
		/*
		if (valid subgroup) add neg updates
		for (cell in remainingEmpties) {
			if (cannot cull) {
				subgroupQueue.add(cell + subgroup)
			}
		}
		 */
		/*
		cullable if
		# of merged options > empties.size - 2
		# of merged options > potential emptyIs.size

		potential emptyIs.size = emptyIs.size + empties.size - emptyi
		 */

		var progressed = false

		for (group in groups) {
			val empties = group.filter {board[it] == 0}

			//populate subgroupQueue with the groups only containing one cell
			val subgroupQueue = ArrayList<OldSubGroup>()
			repeat(empties.size) {i ->
				subgroupQueue.add(OldSubGroup(arrayOf(i),
					IntArray(board.dim) {if (cellChannelFor(empties[i]).options[it]) 1 else 0}
				))
			}

			while (subgroupQueue.isNotEmpty()) {
				val subGroup = subgroupQueue.removeFirst()
				if (subGroup.valid) {
					repeat(empties.size) {i ->
						if (!subGroup.emptyIs.contains(i)) {
							val pos = empties[i]
							for (v in subGroup.mergedOptions.indices) if (subGroup.mergedOptions[v] > 0) {
								if (queueNegUpdate(pos.x, pos.y, v + 1)) progressed = true
							}
						}
					}
				}
				val startEI = subGroup.emptyIs.last() + 1
				for (emptyi in startEI until empties.size) {
					val nSubGroup = OldSubGroup(subGroup, emptyi, this, empties)
					val mergedOptionCount = nSubGroup.mergedOptions.count { it > 0 }

					if (mergedOptionCount > empties.size - 2) continue //cull

					val potentialEISize = nSubGroup.emptyIs.size + empties.size - emptyi - 1
					if (mergedOptionCount > potentialEISize) continue //cull

					subgroupQueue.add(nSubGroup)
				}
			}
		}

		while (negUpdateQueue.isNotEmpty()) {
			negUpdate(negUpdateQueue.removeFirst())
		}
		return progressed
	}

	fun mEhlimination() : Boolean {
		val opts = ArrayList<CellUpdate>()
		for (cc in cellChannels) for (oi in cc.options.indices) {
			if (cc.options[oi]) {
				opts.add(CellUpdate(cc.x, cc.y, oi + 1))
			}
		}

		val imps = opts.filter { mehImpossible(it) }
		imps.forEach {negUpdate(it)}
		return imps.isNotEmpty()
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

	fun finishBoard() {
		while (cellChannels.any { it.freedom > 0 }) {
			mehSolve()
			applyRandomGuess()
		}
	}

	fun getCompleteHints() : ArrayList<CellUpdate> {
		val ret = ArrayList<CellUpdate>()
		while (cellChannels.any { it.freedom > 0 } && !cellChannels.any { board[it.x, it.y] == 0 && it.freedom == 0 }) {
			mehSolve()
			val guess = getRandomGuess() ?: continue
			ret.add(guess)
			update(guess)
		}
		return ret
	}

	fun impossibleState() : Boolean {
		if (cellChannels.any { it.freedom == 0 && board[it.x, it.y] == 0}) return true

		if (boxChannels.any { it.freedom == 0 && board.box(it.x, it.y).all { v -> v != it.v } }) return true
		if (rowChannels.any { it.freedom == 0 && board.row(it.r).all { v -> v != it.v } }) return true
		if (colChannels.any { it.freedom == 0 && board.col(it.c).all { v -> v != it.v } }) return true

		return false
	}

	fun mehImpossible() : Boolean {
		val backup = board.copy()
		mehSolve()
		var ret = impossibleState()
		resetTo(backup)
		return ret
	}

	fun mehImpossible(cu : CellUpdate) : Boolean {
		val backup = board.copy()
		update(cu)
		mehSolve()
		var ret = impossibleState()
		resetTo(backup)
		return ret
	}

	fun channelSolveable() : Boolean {
		val backup = board
		channelSolve()
		val ret = board.checkCompleteCorrect()
		resetTo(backup)
		return ret
	}

	fun mehSolveable() : Boolean {
		val backup = board
		mehSolve()
		val ret = board.checkCompleteCorrect()
		resetTo(backup)
		return ret
	}

	fun mehSolveable(cu : CellUpdate) : Boolean {
		val backup = board
		update(cu)
		mehSolve()
		val ret = board.checkCompleteCorrect()
		resetTo(backup)
		return ret
	}

	fun printSolvingState() {
		if (board.checkCompleteCorrect()) {
			println("board is complete and correct")
		} else if (mehImpossible()) {
			println("board can be proven impossible by the meh solver")
		} else if (channelSolveable()) {
			println("board is solvable by the channel solver")
		} else if (mehSolveable()) {
			println("board is solvable by the meh solver")
		} else if (!bruteCompleteable()) {
			println("board is impossible")
		} else {
			println("board is completable")
		}
	}
}

class OldSubGroup(val emptyIs : Array<Int>, val mergedOptions : IntArray) {
	constructor(g : OldSubGroup, emptyi : Int, s : OldSolver, empties : List<Pos>) : this(
		Array(g.emptyIs.size + 1) { i -> if (i in g.emptyIs.indices) g.emptyIs[i] else emptyi },
		IntArray(s.board.dim) { i -> g.mergedOptions[i] + if (s.cellChannelFor(empties[emptyi]).options[i]) 1 else 0 }
	)
	val valid : Boolean
		get() {
			var ret = 0
			for (c in mergedOptions) {
				if (c == 1) {
					return false
				} else if (c > 0) {
					ret++
				}
			}
			return ret == emptyIs.size
		}
}

fun getSubIndices(size : Int, targSize : Int, f : (Array<Int>) -> Unit) {
	for (x in 0..(targSize - size)) {
		getSubIndices(arrayOf(x), size, targSize, f)
	}
}

fun getSubIndices(start : Array<Int>, size : Int, targSize : Int, f : (Array<Int>) -> Unit) {

	if (start.size == size) {
		f(start)
		return
	}

	val last = start.last()
	val remainingNums = size - start.size

	val startInd = last + 1

	for (x in startInd..(targSize - remainingNums)) {
		val a = Array(start.size + 1) {if (it in start.indices) start[it] else x}
		getSubIndices(a, size, targSize, f)
	}
}

/*

GROUP: set of coordinates that must have nonequal values

if there exists Partitions S & Q such that
the union of the possible values of S := L
||L|| == ||S||
||Q|| > 0
 the possibilites in Q can be subtracted by L
 */