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

    // ← 추가: 외부에서 읽을 수 있는 읽기 전용 플래그
    var isRunning by mutableStateOf(false)
        private set
    fun start(onFinish: () -> Unit) {
        // 이미 돌고 있으면 재시작하지 않음
        if (isRunning) return

        // 1) 플래그 세팅
        isRunning = true

        // 2) 이전 잡 취소
        job?.cancel()

        // 3) 카운트다운 시작
        job = scope.launch {
            while (_remainingTime.value > 0) {
                if (!isPaused) {
                    delay(1000L)
                    _remainingTime.value -= 1000L
                } else {
                    delay(100L)
                }
            }
            // 4) 종료 플래그 해제 전에 콜백
            onFinish()
            // 5) 플래그 해제
            isRunning = false
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
