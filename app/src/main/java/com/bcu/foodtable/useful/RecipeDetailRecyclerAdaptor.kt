package com.bcu.foodtable.useful

import android.content.Context
import android.os.CountDownTimer
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bcu.foodtable.R

class RecipeDetailRecyclerAdaptor(
    private var items: MutableList<String>,
    private val context: Context,
    private val onDoneButtonClick: (Int) -> Unit
) : RecyclerView.Adapter<RecipeDetailRecyclerAdaptor.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemListNumber: TextView = itemView.findViewById(R.id.ItemListNumber) // 순번
        val itemListName: TextView = itemView.findViewById(R.id.itemListName) // 텍스트
        val itemTitleNameText : TextView = itemView.findViewById(R.id.itemListTitleName) // 순번 옆 타이틀
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox2) // 체크박스
        val timerFrame: FrameLayout = itemView.findViewById(R.id.ItemTimerFrame) // 타이머 프레임
        val timerTitle: TextView = itemView.findViewById(R.id.ItemTimerTitle) // 타이머 제목
        val timerTime: TextView = itemView.findViewById(R.id.ItemTimerTime) // 타이머 시간
        val timerProgress: ProgressBar = itemView.findViewById(R.id.ItemTimerProgress) // 프로그레스바
        val startButton: Button = itemView.findViewById(R.id.ItemStartButton) // 타이머 시작 버튼
        val doneButton: Button = itemView.findViewById(R.id.itemDoneButton) // 완료 버튼
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe_detail_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // 순번 설정
        holder.itemListNumber.text = "${position + 1}."
        val regexSimple = Regex("^\\d+\\.\\s*\\(([^)]+)\\)\\s*(.*)")
        val matchResultSimple = regexSimple.find(item)
        if (matchResultSimple != null) {
            val category = matchResultSimple.groupValues[1]
            val content = matchResultSimple.groupValues[2]
            holder.itemTitleNameText.text=category
            holder.itemListName.text=content
        }


        // 추가적인 로직 (타이머 등) GONE 상태이기 때문에 이후 타이머 문자열이 포함되어 있으면 VISIBLE로 전환
        holder.checkBox.isChecked = false // 체크박스 초기화

        // 정규식을 통해 타이머가 존재하는지 확인
        val regex = Regex("(.*)\\s*\\((.*),(\\d{2}:\\d{2}:\\d{2})\\)")
        val matchResult = regex.find(item)


        if (matchResult != null) {
            if (position == 0) {
                holder.timerFrame.visibility = View.VISIBLE
            }

            val action = matchResult.groupValues[1].trim() // 앞의 문자열
            val method = matchResult.groupValues[2].trim() // 조리방식 문자열
            val timeStr = matchResult.groupValues[3].trim() // "hh:mm:ss" 의 시간 비슷한 문자열

            // 시간 문자열을 초로 변환
            val totalTimeInSeconds = convertTimeToSeconds(timeStr)

            // 타이머 시작/중지 버튼 클릭 리스너
            var isTimerRunning = false
            val timeLeft = totalTimeInSeconds

            if (matchResultSimple != null) {
                val category = matchResultSimple.groupValues[1]
                val contentMatch =  Regex("^(.*?)\\s*\\(").find(matchResultSimple.groupValues[2])

                holder.itemTitleNameText.text=category
                holder.itemListName.text=contentMatch!!.groupValues[1].trim()
            }
            // 타이머 아이템 초기화
            // 텍스트 설정
            holder.timerTitle.text = method
            // 프로그레스바 초기화
            holder.timerProgress.max = totalTimeInSeconds
            holder.timerProgress.progress = 0

            // 타이머 함수
            fun startTimer(holder: ViewHolder, timeLeft: Int) {
                val notificationHelper = NotificationHelper(context)
                val timer = object : CountDownTimer((timeLeft * 1000).toLong(), 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        val secondsLeft = millisUntilFinished / 1000
                        holder.timerProgress.progress = (totalTimeInSeconds - secondsLeft).toInt()

                        // 남은 시간 계산
                        val hours = secondsLeft / 3600
                        val minutes = (secondsLeft % 3600) / 60
                        val seconds = secondsLeft % 60
                        holder.timerTime.text =
                            String.format("%02d:%02d:%02d", hours, minutes, seconds)
                        notificationHelper.showTimerNotification(String.format("%02d:%02d:%02d", hours, minutes, seconds),method)
                    }

                    override fun onFinish() {
                        holder.timerProgress.progress = totalTimeInSeconds
                        holder.timerTime.text = "00:00:00"
                        holder.doneButton.visibility = View.VISIBLE
                        notificationHelper.cancelNotification()
                    }
                }

                // 타이머 시작
                timer.start()
            }

            // 타이머 시작 버튼
            holder.startButton.setOnClickListener {
                if (!isTimerRunning) {
                    // 타이머 시작
                    isTimerRunning = true
                    startTimer(holder, timeLeft)
                    holder.startButton.isClickable = false
                    holder.startButton.visibility = View.GONE
                }
            }

        } else {
            // 타이머가 존재하지 않는 경우
            holder.timerFrame.visibility = View.GONE
        }
        // 타이머 시작 버튼 동작 설정 // 미구현


        // 첫 번째 요소에게만 버튼 활성화
        if (position == 0 && (matchResult == null)) {
            holder.doneButton.visibility = View.VISIBLE
        } else {
            holder.doneButton.visibility = View.GONE
        }

        holder.doneButton.setOnClickListener() {
            onDoneButtonClick(position)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    // 아이템 리스트를 업데이트하는 메서드
    fun updateItems(newItems: List<String>) {
        items.clear() // 기존 리스트 클리어
        items.addAll(newItems) // 새 아이템 추가
        items.map { it.replace(Regex("^\\d+\\. "), "") }
        notifyDataSetChanged() // 어댑터에 변경 사항 알리기
    }

    // 시간 문자열 (hh:mm:ss)을 초로 변환
    private fun convertTimeToSeconds(time: String): Int {
        val timeParts = time.split(":")
        val hours = timeParts[0].toInt()
        val minutes = timeParts[1].toInt()
        val seconds = timeParts[2].toInt()
        return (hours * 3600) + (minutes * 60) + seconds
    }
}