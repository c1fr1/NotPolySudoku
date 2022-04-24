import engine.opengl.EnigContext
import engine.opengl.EnigWindow
import engine.opengl.GLContextPreset

fun main(args : Array<String>) {
	EnigContext.init()
	val window = EnigWindow(1500, 1000, "Poly Sudoku Solver", GLContextPreset.standard2D)

	val view = MainView()
	view.runInGLSafe(window)

	EnigContext.terminate()
}

