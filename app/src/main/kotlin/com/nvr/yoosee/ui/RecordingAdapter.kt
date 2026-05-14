package com.nvr.yoosee.ui

import android.content.Intent
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.nvr.yoosee.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class RecordingAdapter(private val files: List<File>) :
    RecyclerView.Adapter<RecordingAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tv_name)
        val meta: TextView = v.findViewById(R.id.tv_meta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_recording, parent, false))

    override fun getItemCount() = files.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val file = files[position]
        holder.name.text = file.name
        val sizeMb = "%.1f MB".format(file.length() / 1_048_576.0)
        val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            .format(Date(file.lastModified()))
        holder.meta.text = "$sizeMb  ·  $date"

        holder.itemView.setOnClickListener {
            try {
                val uri = FileProvider.getUriForFile(
                    it.context, "${it.context.packageName}.provider", file)
                val mime = if (file.extension == "jpg") "image/jpeg" else "video/*"
                it.context.startActivity(
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mime)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(it.context, "No hay app para abrir este archivo", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
