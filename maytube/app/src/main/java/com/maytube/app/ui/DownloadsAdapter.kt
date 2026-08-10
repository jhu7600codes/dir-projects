package com.maytube.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.maytube.app.R
import java.io.File
import java.text.DateFormat
import java.util.Locale

class DownloadsAdapter(
    private var files: List<File>,
    private val onOpen: (File) -> Unit,
    private val onDelete: (File) -> Unit
) : RecyclerView.Adapter<DownloadsAdapter.ViewHolder>() {

    class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val fileName: TextView = itemView.findViewById(R.id.fileName)
        val fileMeta: TextView = itemView.findViewById(R.id.fileMeta)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
    }

    fun submitList(newFiles: List<File>) {
        files = newFiles
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_download, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = files[position]
        holder.fileName.text = file.nameWithoutExtension
        val sizeMb = file.length() / (1024.0 * 1024.0)
        val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
            .format(file.lastModified())
        holder.fileMeta.text = String.format(Locale.getDefault(), "%.1f MB · %s", sizeMb, date)
        holder.itemView.setOnClickListener { onOpen(file) }
        holder.deleteButton.setOnClickListener { onDelete(file) }
    }

    override fun getItemCount(): Int = files.size
}
