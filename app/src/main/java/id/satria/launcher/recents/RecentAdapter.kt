package id.satria.launcher.recents

import android.app.ActivityManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import id.satria.launcher.R

class RecentAdapter(
        private val onTaskClick: (ActivityManager.RecentTaskInfo) -> Unit,
        private val onTaskDismiss: (ActivityManager.RecentTaskInfo) -> Unit
) : RecyclerView.Adapter<RecentAdapter.ViewHolder>() {

    private val taskList = mutableListOf<ActivityManager.RecentTaskInfo>()

    fun submitList(tasks: List<ActivityManager.RecentTaskInfo>) {
        taskList.apply {
            clear()
            addAll(tasks)
        }
        notifyDataSetChanged()
    }

    fun getTaskAt(pos: Int) = taskList[pos]
    fun getAllTasks() = taskList.toList()

    fun removeItem(pos: Int) {
        taskList.removeAt(pos)
        notifyItemRemoved(pos)
    }

    fun clearAll() {
        val count = taskList.size
        taskList.clear()
        notifyItemRangeRemoved(0, count)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_recent_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val task = taskList[position]
        val context = holder.itemView.context
        val pm = context.packageManager

        val pkg = task.baseIntent?.component?.packageName ?: ""
        try {
            val appInfo = pm.getApplicationInfo(pkg, 0)
            holder.tvAppName.text = pm.getApplicationLabel(appInfo)
            holder.ivAppIcon.setImageDrawable(pm.getApplicationIcon(appInfo))
        } catch (e: Exception) {
            holder.tvAppName.text = "Unknown"
        }

        // AsyncTask via Helper untuk thumbnail
        holder.ivThumbnail.setImageBitmap(null)
        ThumbnailHelper.loadThumbnailAsync(
                context,
                holder.ivThumbnail,
                task.persistentId,
                task.taskDescription
        )

        holder.itemView.setOnClickListener { onTaskClick(task) }
        holder.btnDismiss.setOnClickListener {
            val currentPos = holder.bindingAdapterPosition
            if (currentPos != RecyclerView.NO_POSITION) {
                onTaskDismiss(getTaskAt(currentPos))
                removeItem(currentPos)
            }
        }
    }

    override fun getItemCount() = taskList.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumbnail: ImageView = view.findViewById(R.id.ivThumbnail)
        val ivAppIcon: ImageView = view.findViewById(R.id.ivAppIcon)
        val tvAppName: TextView = view.findViewById(R.id.tvAppName)
        val btnDismiss: ImageButton = view.findViewById(R.id.btnDismiss)
    }
}
