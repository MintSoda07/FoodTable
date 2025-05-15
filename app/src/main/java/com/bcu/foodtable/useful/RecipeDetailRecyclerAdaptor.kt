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

    private var completedSteps = BooleanArray(items.size) { false }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemListNumber: TextView = itemView.findViewById(R.id.ItemListNumber)
        val itemListName: TextView = itemView.findViewById(R.id.itemListName)
        val itemTitleNameText: TextView = itemView.findViewById(R.id.itemListTitleName)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox2)
        val timerFrame: FrameLayout = itemView.findViewById(R.id.ItemTimerFrame)
        val timerTitle: TextView = itemView.findViewById(R.id.ItemTimerTitle)
        val timerTime: TextView = itemView.findViewById(R.id.ItemTimerTime)
        val timerProgress: ProgressBar = itemView.findViewById(R.id.ItemTimerProgress)
        val startButton: Button = itemView.findViewById(R.id.ItemStartButton)
        val doneButton: Button = itemView.findViewById(R.id.itemDoneButton)
        val stopButton: Button = itemView.findViewById(R.id.ItemStopButton)
        val skipButton: Button = itemView.findViewById(R.id.ItemSkipButton)
        var timer: CountDownTimer? = null
        var isTimerRunning = false
        var isTimerFinished = false
        var hasTimer = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe_detail_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.itemListNumber.text = "${position + 1}."

        val regexSimple = Regex("^\\d+\\.\\s*\\(([^)]+)\\)\\s*(.*)")
        val matchResultSimple = regexSimple.find(item)
        if (matchResultSimple != null) {
            holder.itemTitleNameText.text = matchResultSimple.groupValues[1]
            holder.itemListName.text = matchResultSimple.groupValues[2]
        }

        holder.checkBox.isChecked = false
        val regex = Regex("(.*)\\s*\\((.*),(\\d{2}:\\d{2}:\\d{2})\\)")
        val matchResult = regex.find(item)

        holder.hasTimer = matchResult != null

        if (holder.hasTimer) {
            val method = matchResult!!.groupValues[2].trim()
            val timeStr = matchResult.groupValues[3].trim()
            val totalTimeInSeconds = convertTimeToSeconds(timeStr)

            holder.timerTitle.text = method
            holder.timerProgress.max = totalTimeInSeconds
            holder.timerProgress.progress = 0

            // 첫 번째 단계는 항상 표시, 그 외에는 이전 단계 완료 여부 확인
            holder.timerFrame.visibility = if (position == 0 || completedSteps.getOrElse(position - 1) { false }) View.VISIBLE else View.GONE

            holder.doneButton.visibility = View.GONE // 처음에는 완료 버튼 숨김

            fun startTimer() {
                holder.timer = object : CountDownTimer((totalTimeInSeconds * 1000).toLong(), 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        val secondsLeft = millisUntilFinished / 1000
                        holder.timerProgress.progress = (totalTimeInSeconds - secondsLeft).toInt()
                        holder.timerTime.text = String.format(
                            "%02d:%02d:%02d",
                            secondsLeft / 3600,
                            (secondsLeft % 3600) / 60,
                            secondsLeft % 60
                        )
                    }

                    override fun onFinish() {
                        holder.timerProgress.progress = totalTimeInSeconds
                        holder.timerTime.text = "00:00:00"
                        holder.doneButton.visibility = View.VISIBLE // 타이머가 끝나면 완료 버튼 표시
                        holder.isTimerFinished = true
                    }
                }.start()
                holder.isTimerRunning = true
                holder.isTimerFinished = false
                holder.startButton.visibility = View.GONE
                holder.stopButton.visibility = View.VISIBLE
                holder.skipButton.visibility = View.VISIBLE
            }

            holder.startButton.setOnClickListener {
                if (!holder.isTimerRunning) startTimer()
            }

            holder.stopButton.setOnClickListener {
                holder.timer?.cancel()
                holder.isTimerRunning = false
                holder.startButton.visibility = View.VISIBLE
                holder.stopButton.visibility = View.GONE
                holder.skipButton.visibility = View.GONE
            }

            holder.skipButton.setOnClickListener {
                holder.timer?.cancel()
                holder.isTimerRunning = false
                holder.timerProgress.progress = totalTimeInSeconds
                holder.timerTime.text = "00:00:00"
                holder.doneButton.visibility = View.VISIBLE // 스킵하면 바로 완료 버튼 표시
                holder.isTimerFinished = true
                holder.startButton.visibility = View.GONE
                holder.stopButton.visibility = View.GONE
                holder.skipButton.visibility = View.GONE
            }
        } else {
            holder.timerFrame.visibility = View.GONE
        }

        // 완료 버튼의 표시 여부 결정
        holder.doneButton.visibility =
            if (position == 0) {
                if (holder.hasTimer) View.GONE else View.VISIBLE // 첫 번째 단계가 타이머가 아니면 완료 버튼 보이게 함
            } else if (completedSteps.getOrElse(position - 1) { false } && (!holder.hasTimer || holder.isTimerFinished)) {
                View.VISIBLE // 이전 단계가 완료되었고, 타이머가 있다면 종료된 경우만 버튼 표시
            } else {
                View.GONE
            }

        holder.doneButton.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                completedSteps[currentPosition] = true
                holder.timerFrame.visibility = View.GONE
                onDoneButtonClick(currentPosition)

                // 다음 단계 버튼 활성화 (다음 아이템이 존재할 경우에만)
                if (currentPosition + 1 < items.size) {
                    notifyItemChanged(currentPosition + 1)
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<String>) {
        items.clear()
        items.addAll(newItems)

        // 새로운 리스트 크기에 맞게 completedSteps 배열 재생성
        completedSteps = BooleanArray(newItems.size) { false }

        notifyDataSetChanged()
    }
    fun getCurrentStepIndex(): Int {
        return completedSteps.lastIndexOf(true) + 1
    }

    private fun convertTimeToSeconds(time: String): Int {
        val (hours, minutes, seconds) = time.split(":").map { it.toInt() }
        return (hours * 3600) + (minutes * 60) + seconds
    }
}
