package solvers

import solvers.channels.CellUpdate

fun gahElimination(s : ChannelSolver, apply : Boolean = true) : ArrayList<CellUpdate> {
	val possibleUpdates = s.possibleUpdates()

	val ret = ArrayList<CellUpdate>()

	while (possibleUpdates.isNotEmpty()) {
		val c = s.copy()
		val cu = possibleUpdates.removeFirst()
		c.update(cu)
		mehSolve(c)
		if (c.impossibleState()) {
			if (apply) s.negUpdate(cu)
			ret.add(cu)
		} else {
			possibleUpdates.removeAll { c.board[it.x, it.y] == it.v }
		}
	}
	return ret
}

fun gahSolve(s : ChannelSolver) {
	mehSolve(s)
	while (!s.board.checkCompleteCorrect()) {
		val update = s.getRandomGuess() ?: return
		if (!mehImpossible(s, update)) {
			s.negUpdate(update)
		} else {
			s.update(update)
		}
		mehSolve(s)
	}
}
