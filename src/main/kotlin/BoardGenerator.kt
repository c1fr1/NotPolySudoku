import solvers.ChannelSolver
import solvers.channels.CellUpdate
import solvers.mehImpossible
import solvers.mehSolve

typealias BoardGenerator = (ChannelSolver) -> ArrayList<CellUpdate>

fun makeBoard(cs : ChannelSolver, bg : BoardGenerator) : ChannelSolver {
	val hints = bg(cs)
	cs.reset()
	for (h in hints) {
		cs.update(h)
	}
	return cs
}

fun makeBoard(n : Int, bg : BoardGenerator) = bg(ChannelSolver(n))

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

