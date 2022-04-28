import solvers.ChannelSolver
import solvers.channels.CellUpdate
import solvers.mehImpossible
import solvers.mehSolve

typealias BoardGenerator = (ChannelSolver) -> ArrayList<CellUpdate>

fun makeBoard(cs : ChannelSolver, bg : BoardGenerator, reduced : Boolean = true) : ChannelSolver {
	val hints = bg(cs)
	if (reduced) {
		mehReduceHints(cs, hints)
	}
	cs.reset()
	for (h in hints) {
		cs.update(h)
	}
	return cs
}

fun makeBoard(n : Int, bg : BoardGenerator) = bg(ChannelSolver(n))

fun mehReduceHints(s : ChannelSolver, hints : ArrayList<CellUpdate>) {
	var i = 0
	while (i in hints.indices) {
		s.reset()
		for (hi in hints.indices) {
			if (hi != i) s.update(hints[hi])
		}
		mehSolve(s)
		if (s.board.checkCompleteCorrect()) {
			hints.removeAt(i)
		} else {
			++i
		}
	}
}

private fun getBoardHints(s : ChannelSolver) : ArrayList<CellUpdate> {
	val ret = ArrayList<CellUpdate>()

	for (cc in s.cellChannels.filter { it.filled }) {
		ret.add(CellUpdate(cc.x, cc.y, s.board[cc.x, cc.y]))
	}

	return ret
}

val mehGenerate : BoardGenerator = { solver ->
	val ret = getBoardHints(solver)

	var nextHint = solver.getRandomGuess()
	while (nextHint != null) {
		solver.update(nextHint)
		ret.add(nextHint)
		mehSolve(solver)
		nextHint = solver.getRandomGuess()
	}
	ret
}

val gahGenerate : BoardGenerator = { s ->
	val hints = getBoardHints(s)

	while (!s.board.checkCompleteCorrect()) {
		val update = s.getRandomGuess() ?: break
		if (mehImpossible(s, update)) {
			println("avoided catastrophe")
			s.negUpdate(update)
		} else {
			hints.add(update)
			s.update(update)
		}
		mehSolve(s)
	}
	if (!s.board.checkCompleteCorrect()) {
		println("failed to find good board")
	}
	hints
}

fun steadyGenerateRecursive(s : ChannelSolver, cu : CellUpdate) : ArrayList<CellUpdate>? {
	val c = s.copy()
	c.update(cu)
	mehSolve(c)
	if (c.board.checkCompleteCorrect()) {
		return arrayListOf()
	} else if (c.impossibleState()) {
		return null
	}

	MainView.solver = c

	val cc = c.cellChannels.filter { !it.filled }.random()
	val possibleUpdates = cc.options.indices.filter { cc.options[it] }.map { CellUpdate(cc.x, cc.y, it + 1) } as ArrayList

	while (possibleUpdates.isNotEmpty()) {
		val update = possibleUpdates.removeAt(possibleUpdates.indices.random())
		val finalHints = steadyGenerateRecursive(c, update)
		if (finalHints != null) {
			finalHints.add(update)
			return finalHints
		}
	}

	return null
}

val steadyGenerate : BoardGenerator = { s ->
	val hints = getBoardHints(s)

	mehSolve(s)

	val possibleUpdates = s.possibleUpdates()

	while (possibleUpdates.isNotEmpty()) {
		val update = possibleUpdates.removeAt(possibleUpdates.indices.random())
		val finalHints = steadyGenerateRecursive(s, update)
		if (finalHints != null) {
			hints.addAll(finalHints)
			hints.add(update)
			break
		}
	}
	MainView.solver = s

	hints
}

val reverseGreedyGenerate : BoardGenerator = { s ->
	mehSolve(s)

	var possibleUpdates = s.possibleUpdates()

	while (possibleUpdates.isNotEmpty()) {
		val cu = possibleUpdates.minByOrNull { s.cellChannelFor(it).freedom } ?: break
		s.update(cu)
		mehSolve(s)
		possibleUpdates = s.possibleUpdates()
	}


	if (!s.board.checkCompleteCorrect()) arrayListOf() else {

		val rs = s.copy()
		s.reset()

		val ret = ArrayList<CellUpdate>()
		while (!s.board.checkCompleteCorrect()) {
			val c = s.cellChannels.filter { !it.filled }.random()
			val cu = CellUpdate(c.x, c.y, rs.board[c.x, c.y])
			s.update(cu)
			ret.add(cu)
		}

		ret
	}
}

val completeIncorrectGenerate : BoardGenerator = {s ->
	mehSolve(s)

	var guess = s.getRandomGuess()
	while (guess != null) {
		s.update(guess)
		mehSolve(s)
		guess = s.getRandomGuess()
	}

	for (cc in s.cellChannels) {
		if (s.board[cc.x, cc.y] == 0) {
			s.update(cc.x, cc.y, cc.options.indices.random() + 1)
		}
	}
	getBoardHints(s)
}