import java.io.File
import java.io.FileWriter

object SaveManager {
	var savedLevelCount : Int = 0
	init {
		while (File("boards/b${++savedLevelCount}").exists());
	}

	fun saveBoard(b : Board) {
		val fr = FileWriter("boards/b${savedLevelCount++}")
		fr.write(b.toString())
		fr.flush()
		fr.close()
	}

	fun getBoard(i : Int) : Board {
		if (i < savedLevelCount) {
			return Board("boards/b${i}")
		} else {
			return Board(3)
		}
	}
}