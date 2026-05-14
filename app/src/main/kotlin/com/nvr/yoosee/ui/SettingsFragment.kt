package com.nvr.yoosee.ui

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.nvr.yoosee.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private var _b: FragmentSettingsBinding? = null
    private val b get() = _b!!

    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentSettingsBinding.inflate(inf, c, false); return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        val prefs = requireContext().getSharedPreferences("nvr_config", Context.MODE_PRIVATE)
        b.etIp.setText(prefs.getString("ip","192.168.1.50"))
        b.etPort.setText(prefs.getString("port","554"))
        b.etUser.setText(prefs.getString("user","admin"))
        b.etPass.setText(prefs.getString("pass","Test1234"))
        val streams = arrayOf("onvif1","onvif2")
        val cur = prefs.getString("stream","onvif1") ?: "onvif1"
        b.spinnerStream.setSelection(streams.indexOf(cur).coerceAtLeast(0))

        fun updateUrl() {
            val url = "rtsp://${b.etUser.text}:${b.etPass.text}@${b.etIp.text}:${b.etPort.text}/${streams[b.spinnerStream.selectedItemPosition]}"
            b.tvRtspPreview.text = url
        }
        updateUrl()
        b.etIp.addTextChangedListener(object: android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) = updateUrl()
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        b.btnSave.setOnClickListener {
            prefs.edit()
                .putString("ip",     b.etIp.text.toString())
                .putString("port",   b.etPort.text.toString())
                .putString("user",   b.etUser.text.toString())
                .putString("pass",   b.etPass.text.toString())
                .putString("stream", streams[b.spinnerStream.selectedItemPosition])
                .putInt("segment_min", when(b.spinnerSegment.selectedItemPosition) {
                    0 -> 5; 1 -> 10; 2 -> 30; else -> 60
                })
                .apply()
            Toast.makeText(context,"✅ Configuración guardada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
