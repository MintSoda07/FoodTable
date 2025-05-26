import androidx.compose.runtime.*
import kotlinx.coroutines.*

import androidx.compose.runtime.*
import kotlinx.coroutines.*

class StepTimerState(durationMillis: Long) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _remainingTime = mutableStateOf(durationMillis)
    val remainingTime: State<Long> = _remainingTime

    private val total = durationMillis
    private var isPaused = false
    private var job: Job? = null

    fun start(onFinish: () -> Unit) {
        job?.cancel()
        job = scope.launch {
            while (_remainingTime.value > 0) {
                if (!isPaused) {
                    delay(1000L)
                    _remainingTime.value -= 1000L
                } else {
                    delay(100L)
                }
            }
            onFinish()
        }
    }

    fun pause() {
        isPaused = true
    }

    fun resume() {
        isPaused = false
    }

    fun stop() {
        job?.cancel()
        _remainingTime.value = total
    }
}
