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
        var title = ""
        var description = ""

        // 타이머가 있는 경우의 정규식 패턴
        val timerRegex = Regex("^\\d*\\.?\\s*\\((.*?)\\)\\s*(.*?)\\s*\\((.*?),(\\d{2}:\\d{2}:\\d{2})\\)\\s*$")
        // 타이머가 없는 경우의 정규식 패턴
        val noTimerRegex = Regex("^\\d*\\.?\\s*\\((.*?)\\)\\s*(.*)$")

        val timerMatch = timerRegex.find(item.removePrefix("○"))
        val noTimerMatch = noTimerRegex.find(item.removePrefix("○"))

        when {
            timerMatch != null -> {
                holder.hasTimer = true
                title = timerMatch.groupValues[1].trim()
                description = timerMatch.groupValues[2].trim()
                val cookingMethod = timerMatch.groupValues[3].trim()
                val timeStr = timerMatch.groupValues[4].trim()
                val totalTimeInSeconds = convertTimeToSeconds(timeStr)

                holder.itemTitleNameText.text = title
                holder.itemListName.text = description

                holder.timerTitle.text = cookingMethod
                holder.timerTime.text = timeStr
                holder.timerProgress.max = totalTimeInSeconds
                holder.timerProgress.progress = 0

                holder.timerFrame.visibility =
                    if (position == 0 || completedSteps.getOrElse(position - 1) { false }) View.VISIBLE else View.GONE

                setupTimer(holder, totalTimeInSeconds)
            }
            noTimerMatch != null -> {
                holder.hasTimer = false
                title = noTimerMatch.groupValues[1].trim()
                description = noTimerMatch.groupValues[2].trim()
                
                holder.itemTitleNameText.text = title
                holder.itemListName.text = description
                holder.timerFrame.visibility = View.GONE
            }
            else -> {
                // 매칭되지 않는 경우 전체 텍스트를 description으로 처리
                holder.hasTimer = false
                holder.itemTitleNameText.text = ""
                holder.itemListName.text = item.removePrefix("○").trim()
                holder.timerFrame.visibility = View.GONE
            }
        }

        holder.itemListNumber.text = "${position + 1}."
        holder.checkBox.isChecked = completedSteps[position]

        holder.doneButton.visibility =
            if (position == 0) {
                if (holder.hasTimer) View.GONE else View.VISIBLE
            } else if (completedSteps.getOrElse(position - 1) { false } && (!holder.hasTimer || holder.isTimerFinished)) {
                View.VISIBLE
            } else {
                View.GONE
            }

        holder.doneButton.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition in items.indices) {
                completedSteps[currentPosition] = true
                holder.timerFrame.visibility = View.GONE
                onDoneButtonClick(currentPosition)

                val nextPosition = currentPosition + 1
                if (nextPosition in items.indices) {
                    notifyItemChanged(nextPosition)
                }
            }
        }
    }

    private fun setupTimer(holder: ViewHolder, totalTimeInSeconds: Int) {
        holder.startButton.setOnClickListener {
            if (!holder.isTimerRunning) {
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
                        holder.isTimerFinished = true
                        holder.doneButton.visibility = View.VISIBLE
                    }
                }.start()
                holder.isTimerRunning = true
                holder.startButton.visibility = View.GONE
                holder.stopButton.visibility = View.VISIBLE
                holder.skipButton.visibility = View.VISIBLE
            }
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
            holder.doneButton.visibility = View.VISIBLE
            holder.isTimerFinished = true
            holder.startButton.visibility = View.GONE
            holder.stopButton.visibility = View.GONE
            holder.skipButton.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<String>) {
        items.clear()
        items.addAll(newItems)
        completedSteps = BooleanArray(newItems.size) { false }
        notifyDataSetChanged()
    }

    private fun convertTimeToSeconds(time: String): Int {
        return try {
            val parts = time.split(":").map { it.toInt() }
            if (parts.size == 3) {
                (parts[0] * 3600) + (parts[1] * 60) + parts[2]
            } else 0
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }
}