package com.nvr.yoosee.ui

import android.os.Bundle
import android.os.Environment
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.nvr.yoosee.databinding.FragmentRecordingsBinding
import com.nvr.yoosee.ui.RecordingAdapter
import java.io.File

class RecordingsFragment : Fragment() {
    private var _b: FragmentRecordingsBinding? = null
    private val b get() = _b!!

    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentRecordingsBinding.inflate(inf, c, false); return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        val nvrDir = File(requireContext().getExternalFilesDir(null), "NVR")
        nvrDir.mkdirs()
        val files = nvrDir.listFiles()
            ?.filter { it.extension in listOf("mp4","jpg") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

        b.tvCount.text = "${files.size} archivos guardados"
        b.recycler.layoutManager = LinearLayoutManager(context)
        b.recycler.adapter = RecordingAdapter(files)
        b.emptyView.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
