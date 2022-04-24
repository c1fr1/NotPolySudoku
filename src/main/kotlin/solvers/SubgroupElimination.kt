package solvers

import Groups
import Pos

fun subgroupElimination(s : ChannelSolver) : Boolean {
	var progressed = false
	Groups.iterate(s.n) { group ->
		val empties = group.filter { s.board[it] == 0 }

		//populate subgroupQueue with the groups only containing one cell
		val subgroupQueue = ArrayList<SubGroup>()
		repeat(empties.size) { i ->
			subgroupQueue.add(SubGroup(arrayOf(i),
				IntArray(s.board.dim) { if (s.cellChannelFor(empties[i]).options[it]) 1 else 0 }
			))
		}

		while (subgroupQueue.isNotEmpty()) {
			val subGroup = subgroupQueue.removeFirst()
			if (subGroup.valid) {
				repeat(empties.size) { i ->
					if (!subGroup.emptyIs.contains(i)) {
						val pos = empties[i]
						for (v in subGroup.mergedOptions.indices) if (subGroup.mergedOptions[v] > 0) {
							if (s.queueNegUpdate(pos.x, pos.y, v + 1)) progressed = true
						}
					}
				}
			}
			val startEI = subGroup.emptyIs.last() + 1
			for (emptyi in startEI until empties.size) {
				val nSubGroup = SubGroup(subGroup, emptyi, s, empties)
				val mergedOptionCount = nSubGroup.mergedOptions.count { it > 0 }

				if (mergedOptionCount > empties.size - 2) continue //cull

				val potentialEISize = nSubGroup.emptyIs.size + empties.size - emptyi - 1
				if (mergedOptionCount > potentialEISize) continue //cull

				subgroupQueue.add(nSubGroup)
			}
		}
	}

	s.clearNegQueue()
	return progressed
}

private class SubGroup(val emptyIs : Array<Int>, val mergedOptions : IntArray) {
	constructor(g : SubGroup, emptyi : Int, s : ChannelSolver, empties : List<Pos>) : this(
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