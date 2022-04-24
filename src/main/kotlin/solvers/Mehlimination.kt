package solvers

import solvers.channels.CellUpdate

fun mEhlimination(solver : ChannelSolver) : Boolean {
	val opts = ArrayList<CellUpdate>()
	for (cc in solver.cellChannels) for (oi in cc.options.indices) {
		if (cc.options[oi]) {
			opts.add(CellUpdate(cc.x, cc.y, oi + 1))
		}
	}

	var ret = false

	while (opts.isNotEmpty()) {
		val c = solver.copy()
		val cu = opts.removeFirst()
		c.update(cu)
		mehSolve(c)
		if (c.impossibleState()) {
			ret = true
			solver.queueNegUpdate(cu)
		} else {
			opts.removeAll {c.board[it] == it.v}
		}
	}

	solver.clearNegQueue()

	return ret
}