import engine.entities.Camera
import engine.opengl.InputHandler
import engine.opengl.KeyState
import engine.opengl.jomlExtensions.times
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW
import kotlin.concurrent.thread

class Button(var name : String, var key : Int, var spawnThread : Boolean = true, var performAction : () -> Unit) {
	operator fun invoke() {
		if (spawnThread) thread {
			if (runningInstruction) return@thread
			runningInstruction = true
			performAction()
			runningInstruction = false
		} else {
			performAction()
		}
	}
	operator fun invoke(input : InputHandler, cam : Camera, i : Int) {
		if (input.keys[key] == KeyState.Released || touching(input, cam, i)) this()
	}

	companion object {
		private val buttonsPerCol = 16
		private val scale = 1f / buttonsPerCol
		var runningInstruction = false
		fun getMatFor(cam : Camera, i : Int) : Matrix4f {
			val col = if (i < buttonsPerCol) 1f else 1.5f
			val row = i % buttonsPerCol
			return cam.getMatrix()
				.translate(col, 1f - 2 * row * scale, 0f)
				.scale(0.5f, -scale * 2, 1f)
				.translate(0.2f * scale, 0.1f, 0f)
				.scale(1f - 0.4f * scale, 0.8f, 1f)
		}

		fun getTextMat(cam : Camera, i : Int) : Matrix4f {
			val col = if (i < buttonsPerCol) 1f else 1.5f
			val row = i % buttonsPerCol
			return cam.getMatrix()
				.translate(col, 1f - 2 * row * scale, 0f)
				.scale(scale)
				.translate(0.3f, -1.25f, 0f)
		}

		fun touching(input : InputHandler, cam : Camera, i : Int) : Boolean {
			val pos = Vector3f(input.glCursorX, -input.glCursorY, 0f) * getMatFor(cam, i).invert()
			return pos.x in 0f..1f && pos.y in 0f..1f && input.mouseButtons[GLFW.GLFW_MOUSE_BUTTON_LEFT] == KeyState.Released
		}
	}
}