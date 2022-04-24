package solvers

import solvers.channels.CellUpdate

fun mehSolve(s : ChannelSolver) : Boolean {
	var actuallyProgressed = false
	var progressed = true
	while (progressed) {
		progressed = s.solve()
		progressed = subgroupElimination(s) || progressed
		if (progressed) actuallyProgressed = true
	}
	return actuallyProgressed
}

fun mehSolvable(s : ChannelSolver) : Boolean {
	val c = s.copy()
	mehSolve(c)
	return c.board.checkCompleteCorrect()
}

fun mehSolvable(s : ChannelSolver, cu : CellUpdate) : Boolean {
	val c = s.copy()
	c.update(cu)
	mehSolve(c)
	return c.board.checkCompleteCorrect()
}

fun mehImpossible(s : ChannelSolver) : Boolean {
	val c = s.copy()
	mehSolve(c)
	return c.impossibleState()
}

fun mehImpossible(s : ChannelSolver, cu : CellUpdate) : Boolean {
	val c = s.copy()
	c.update(cu)
	mehSolve(c)
	return c.impossibleState()
}