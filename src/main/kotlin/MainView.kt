
import solvers.channels.CellUpdate
import engine.EnigView
import engine.entities.Camera2D
import engine.opengl.EnigWindow
import engine.opengl.Font
import engine.opengl.InputHandler
import engine.opengl.KeyState
import engine.opengl.bufferObjects.FBO
import engine.opengl.bufferObjects.VAO
import engine.opengl.jomlExtensions.times
import engine.opengl.shaders.ShaderProgram
import engine.opengl.shaders.ShaderType
import org.joml.Math.floor
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.*
import solvers.*

object MainView : EnigView() {

	lateinit var input : InputHandler

	lateinit var cam : Camera2D

	lateinit var font : Font
	lateinit var vao : VAO
	lateinit var shader : ShaderProgram
	lateinit var cshader : ShaderProgram

	var impossibilities = ArrayList<CellUpdate>()

	var selectedX = -1
	var selectedY = -1

	var numberBuffer = ""
	var quickEnter = false

	var board = Board()

	var solver = ChannelSolver(board)

	var buttons = ArrayList<Button>()

	override fun generateResources(window : EnigWindow) {
		input = window.inputHandler
		cam = Camera2D(window, 2f)
		cam.x += 0.5f

		font = Font("roboto.ttf", 128f, 4024, 512)
		vao = VAO(0f, 0f, 1f, 1f)
		shader = ShaderProgram("textShader")
		cshader = ShaderProgram("colorShader")

		makeButtons()
	}

	fun makeButtons() {
		addButton("Set [N]", GLFW_KEY_N) {
			val n = numberBuffer.toIntOrNull() ?: return@addButton
			solver = ChannelSolver(Board(n))
			board = solver.board
		}

		addButton("Channel [S]olve", GLFW_KEY_S) {solver.solve()}
		addButton("Sub[G]roup Elim", GLFW_KEY_G) {subgroupElimination(solver)}
		addButton("[M]eh Solve", GLFW_KEY_M) {mehSolve(solver)}
		addButton("Me[H]limination", GLFW_KEY_H) {mEhlimination(solver)}

		//addButton("Get State [C]", GLFW_KEY_C) {solver.printSolvingState()}
		addButton( "[R]eset Board", GLFW_KEY_R) {solver.reset()}

		addButton( "Save Board [P]", GLFW_KEY_P) {SaveManager.saveBoard(board)}
		addButton( "[L]oad Board", GLFW_KEY_L) {
			val targetSave = numberBuffer.toIntOrNull()
			val b = if (targetSave != null && targetSave < SaveManager.savedLevelCount - 1) {
				SaveManager.getBoard(targetSave)
			} else {
				SaveManager.getBoard(SaveManager.savedLevelCount - 1)
			}
			if (b.n == solver.n) {
				solver.import(b)
			} else {
				solver = ChannelSolver(Board(b.n))
				solver.import(b)
				board = solver.board
			}
		}

		addButton( "Random [E]ntry", GLFW_KEY_E) {solver.applyRandomGuess()}
		addButton( "Find [I]mpossible", GLFW_KEY_I) {
			var nextGuess = solver.getRandomGuess()
			while (nextGuess != null) {
				solver.update(nextGuess)
				mehSolve(solver)
				impossibilities = gahElimination(solver, false)
				if (impossibilities.isNotEmpty()) break
				nextGuess = solver.getRandomGuess()
			}
		}
		addButton("[T]ry Generation", GLFW_KEY_T) {
			makeBoard(solver, steadyGenerate)
		}

		/*addButton("Confirm Impossible [O]", GLFW_KEY_O) {
			for (imp in impossibilities) {
				println("$imp mehImpossible : ${solver.mehImpossible(imp)}")
			}
		}*/

	}

	fun addButton(name : String, key : Int, action : () -> Unit) {
		buttons.add(Button(name, key, action))
	}

	override fun loop(frameBirth : Long, dtime : Float) : Boolean {
		FBO.prepareDefaultRender()

		if (!Button.runningInstruction) checkInput()

		drawBoard()

 		if (!Button.runningInstruction) drawButtons()

		return false
	}

	fun drawButtons() {

		vao.prepareRender()
		cshader.enable()


		if (quickEnter) {
			drawButtonBackground(0, Vector3f(0.35f, 0.35f, 0.35f))
		} else {
			drawButtonBackground(0, Vector3f(0.25f, 0.25f, 0.25f))
		}

		for (i in buttons.indices) {
			drawButtonBackground(i + 1)
		}

		shader.enable()
		font.bind()

		renderButtonText(0, numberBuffer)

		for (i in buttons.indices) {
			renderButtonText(i + 1, buttons[i].name)
		}
	}

	fun drawButtonBackground(i : Int, color : Vector3f? = null) {
		val m = Button.getMatFor(cam, i)
		cshader[0, 0] = m
		val pos = Vector3f(input.glCursorX, -input.glCursorY, 0f) * m.invert()
		cshader[2, 0] = color ?: if (pos.x in 0f..1f && pos.y in 0f..1f) {
			Vector3f(0.1f, 0.1f, 0.1f)
		} else {
			Vector3f(0.4f, 0.4f, 0.4f)
		}

		vao.drawTriangles()
	}

	fun renderButtonText(i : Int, str : String) {
		font.getMats(str, Button.getTextMat(cam, i)) {ms, tcs ->
			for (j in ms.indices) {
				shader[ShaderType.VERTEX_SHADER, 0] = ms[j]
				shader[ShaderType.VERTEX_SHADER, 1] = tcs[j]
				vao.drawTriangles()
			}
		}
	}

	fun checkInput() {

		checkSelectionMovement()

		for (i in 0..9) {
			if (input.keys[GLFW_KEY_0 + i] == KeyState.Released) {
				numberBuffer += i
			}
		}

		if (input.keys[GLFW_KEY_ENTER] == KeyState.Released) {
			var num = numberBuffer.toIntOrNull() ?: -1
			if (num !in 0..board.dim) num = -1
			if (num != -1) {
				if (num == 0) {
					board[selectedX, selectedY] = 0
				} else {
					solver.update(selectedX, selectedY, num)
				}
			}
			if (input.keys[GLFW_KEY_RIGHT_SHIFT].isDown || quickEnter) {
				++selectedX
				if (selectedX >= board.dim) {
					selectedX = 0
					++selectedY
				}
				if (selectedY >= board.dim) {
					selectedY = 0
				}
				numberBuffer = ""
			}
		}

		if (input.keys[GLFW_KEY_BACKSPACE] == KeyState.Released) numberBuffer = ""

		if (Button.touching(input, cam, 0)) quickEnter = !quickEnter

		for (i in buttons.indices) {
			buttons[i](input, cam, i + 1)
		}
	}

	fun checkSelectionMovement() {
		if (input.keys[GLFW_KEY_ESCAPE] == KeyState.Released) {
			selectedX = -1
			selectedY = -1
		}

		if (input.mouseButtons[GLFW_MOUSE_BUTTON_LEFT] == KeyState.Released) {
			if (input.glCursorX < 1f / 3f) {
				selectedX = floor(board.dim * 3f * (input.glCursorX + 1f) / 4f).toInt()
				selectedY = floor(board.dim * (input.glCursorY + 1f) / 2f).toInt()
			}
		}

		if (input.keys[GLFW_KEY_LEFT] == KeyState.Released) {
			selectedX -= 1
		}

		if (input.keys[GLFW_KEY_RIGHT] == KeyState.Released || input.keys[GLFW_KEY_TAB] == KeyState.Released) {
			selectedX += 1
		}

		if (input.keys[GLFW_KEY_UP] == KeyState.Released) {
			selectedY -= 1
		}

		if (input.keys[GLFW_KEY_DOWN] == KeyState.Released) {
			selectedY += 1
		}

		if (selectedX >= 0 || selectedY >= 0) {
			if (selectedX < 0) {
				selectedX = board.dim - 1
				--selectedY
			}
			if (selectedX >= board.dim) {
				selectedX = 0
				++selectedY
			}
			if (selectedY < 0) {
				selectedY += board.dim
			}
			if (selectedY >= board.dim) {
				selectedY -= board.dim
			}
		}
	}

	fun drawBoard() {
		vao.prepareRender()

		cshader.enable()

		drawCells()
		drawSelection()
		drawPossibilities()
		drawImpossibilities()
		drawNums()

		vao.unbind()
	}

	fun drawCells() {

		val selectedNum = numberBuffer.toIntOrNull()

		for (x in 0 until board.dim) for (y in 0 until board.dim) {
			cshader[0, 0] = cam.getMatrix()
				.translate(-1f + 2f * x / board.dim, 1 - 2f * (y + 1) / board.dim, 0f)
				.scale(1.8f / (board.dim))
				.translate(0.05f, 0.05f, 0f)

			val color = if ((x / board.n) % 2 +  y / board.n % 2 == 1) {
				Vector3f(0.2f, 0.2f, 0.2f)
			} else {
				Vector3f(0.4f, 0.4f, 0.4f)
			}
			if (selectedNum != null && selectedNum in 1..board.dim) {
				if (board[x, y] == selectedNum) {
					color.y += 0.2f
				} else if (solver.cellChannelFor(x, y).options[selectedNum - 1]) {
					color.x += 0.15f
					color.y += 0.15f
				}
			}
			cshader[2, 0] = color
			vao.drawTriangles()
		}
	}

	fun drawPossibilities() {
		for (x in 0 until board.dim) for (y in 0 until board.dim) {
			for (lx in 0 until board.n) for (ly in 0 until board.n) {
				if (solver.cellChannels[x + board.dim * y].options[lx + board.n * ly]) {
					cshader[0, 0] = cam.getMatrix()
						.translate(-1f + 2f * x / board.dim, 1 - 2f * (y + 1) / board.dim, 0f)
						.scale(2f / (board.dim * board.n))
						.translate(lx.toFloat(), board.n - ly.toFloat() - 1f, 0f)
						.scale(0.6f)
						.translate(0.3f, 0.3f, 0f)
					cshader[2, 0] = Vector3f(0.3f, 0.3f, 0.3f)
					vao.drawTriangles()
				}
			}
		}
	}

	fun drawImpossibilities() {
		for (imp in impossibilities) {

			if (!solver.cellChannelFor(imp.x, imp.y).options[imp.v - 1]) continue

			val lx = (imp.v - 1) % board.n
			val ly = (imp.v - 1) / board.n

			cshader[0, 0] = cam.getMatrix()
				.translate(-1f + 2f * imp.x / board.dim, 1 - 2f * (imp.y + 1) / board.dim, 0f)
				.scale(2f / (board.dim * board.n))
				.translate(lx.toFloat(), board.n - ly.toFloat() - 1f, 0f)
				.scale(0.6f)
				.translate(0.3f, 0.3f, 0f)
			cshader[2, 0] = Vector3f(0.3f, 0.0f, 0.0f)
			vao.drawTriangles()
		}
	}

	fun drawSelection() {
		val cx = selectedX.toFloat()
		val cy = selectedY.toFloat()

		if (selectedX >= 0 && selectedY >= 0) {

			cshader[0, 0] = cam.getMatrix()
				.translate(-1f + 2f * cx / board.dim, 1 - 2f * (cy + 1) / board.dim, 0f)
				.scale(1.8f / board.dim)
				.translate(0.05f, 0.05f, 0f)
			cshader[2, 0] = Vector3f(0.1f, 0.1f, 0.1f)
			vao.drawTriangles()
		}
	}

	fun drawNums() {

		shader.enable()
		font.bind()

		shader[2, 0] = Vector3f(0.9f, 0.9f, 0.9f)
		for (ri in 1..board.dim) {
			for (ci in 1..board.dim) {
				drawTextForCell(ri, ci, board[ci - 1, ri - 1])
			}
		}
	}

	fun drawTextForCell(ri : Int, ci : Int, n : Int) {
		if (n in 1..board.dim) {
			val s = "$n"
			val xad = s.sumOf {c ->
				val cd = font.charData[c.code]
				cd.xadvance().toDouble()
			}.toFloat() / font.fontSize
			font.getMats(
				s, cam.getMatrix()
					.translate(
						-1f + 2f * (ci - 0.5f) / (board.dim),
						1f - 2f * (ri - 0.5f) / (board.dim),
						0f
					)
					.scale(2f / board.dim)
					.translate(-xad / 2f, -0.25f, 0f)
			) { w, tc ->
				for (i in w.indices) {
					shader[ShaderType.VERTEX_SHADER, 0] = w[i]
					shader[ShaderType.VERTEX_SHADER, 1] = tc[i]
					vao.drawTriangles()
				}
			}
		}
	}
}