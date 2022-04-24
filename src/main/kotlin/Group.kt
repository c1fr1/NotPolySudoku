object Groups {
	fun iterate(n : Int, f : (Array<Pos>) -> Unit) {
		val range = 0 until (n * n)
		val array = Array(n * n) {Pos(0, 0)}
		for (ri in range) {
			for (i in range) {
				array[i].x = i
				array[i].y = ri
			}
			f(array)
		}
		for (ci in range) {
			for (i in range) {
				array[i].x = ci
				array[i].y = i
			}
			f(array)
		}
		for (bx in 0 until n) for (by in 0 until n) {
			for (i in range) {
				val lx = i / n
				val ly = i % n
				array[i].x = bx * n + lx
				array[i].y = by * n + ly
			}
			f(array)
		}
	}
}
