package com.nvr.yoosee.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.nvr.yoosee.R
import com.nvr.yoosee.databinding.FragmentLiveBinding
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

class LiveFragment : Fragment() {

    private var _b: FragmentLiveBinding? = null
    private val b get() = _b!!

    private var libVlc: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null
    private var connected = false

    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentLiveBinding.inflate(inf, c, false)
        return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)
        val prefs = requireContext().getSharedPreferences("nvr_config", Context.MODE_PRIVATE)

        fun getRtsp(): String {
            val ip   = prefs.getString("ip",   "192.168.1.2")  ?: "192.168.1.2"
            val port = prefs.getString("port", "554")           ?: "554"
            val user = prefs.getString("user", "admin")         ?: "admin"
            val pass = prefs.getString("pass", "Cesarini123")   ?: ""
            val strm = prefs.getString("stream","onvif1")       ?: "onvif1"
            return "rtsp://$user:$pass@$ip:$port/$strm"
        }

        b.tvRtspUrl.text = getRtsp().replace(
            prefs.getString("pass","") ?: "", "****")

        b.btnConnect.setOnClickListener {
            if (!connected) startStream(getRtsp()) else stopStream()
        }

        b.btnRecord.setOnClickListener {
            val svc = (activity as? MainActivity)?.getService() ?: run {
                Toast.makeText(context, "Servicio no disponible", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!svc.isRecording()) {
                svc.startRecording(getRtsp())
                b.btnRecord.text = "⏹  DETENER"
                b.btnRecord.setBackgroundColor(0xFFB71C1C.toInt())
                b.recIndicator.visibility = View.VISIBLE
                Toast.makeText(context, "⏺ Grabando...", Toast.LENGTH_SHORT).show()
            } else {
                svc.stopRecording()
                b.btnRecord.text = "⏺  GRABAR"
                b.btnRecord.setBackgroundColor(0xFF1a0808.toInt())
                b.recIndicator.visibility = View.GONE
                Toast.makeText(context, "Grabación guardada en /NVR/", Toast.LENGTH_SHORT).show()
            }
        }

        b.btnNvr.setOnClickListener {
            val svc = (activity as? MainActivity)?.getService() ?: run {
                Toast.makeText(context, "Servicio no disponible", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!svc.isNvrRunning()) {
                val seg = prefs.getInt("segment_min", 10)
                svc.startNvr(getRtsp(), seg)
                b.btnNvr.text = "⏹  STOP NVR"
                b.btnNvr.setBackgroundColor(0xFF0D47A1.toInt())
                Toast.makeText(context, "NVR AUTO — segmentos ${seg}min", Toast.LENGTH_SHORT).show()
            } else {
                svc.stopNvr()
                b.btnNvr.text = "🔄  NVR AUTO"
                b.btnNvr.setBackgroundColor(0xFF080e1a.toInt())
                Toast.makeText(context, "NVR detenido", Toast.LENGTH_SHORT).show()
            }
        }

        b.btnSnap.setOnClickListener {
            (activity as? MainActivity)?.getService()?.takeSnapshot(getRtsp())
            Toast.makeText(context, "📸 Captura guardada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startStream(url: String) {
        try {
            val ctx = requireContext()
            b.tvNoSignal.visibility = View.GONE
            b.tvStatus.text = "CONECTANDO..."
            b.tvStatus.setTextColor(0xFFFFB320.toInt())

            // Opciones VLC para RTSP: TCP forzado, buffer reducido para baja latencia
            val options = arrayListOf(
                "--rtsp-tcp",
                "--network-caching=300",
                "--clock-jitter=0",
                "--clock-synchro=0",
                "--no-audio"
            )

            libVlc = LibVLC(ctx, options)
            mediaPlayer = MediaPlayer(libVlc).apply {
                // Conectar el video al VLCVideoLayout
                attachViews(b.vlcLayout, null, false, false)

                val media = Media(libVlc, android.net.Uri.parse(url)).apply {
                    setHWDecoderEnabled(true, false)  // hardware decode H.265
                    addOption(":rtsp-tcp")
                }
                setMedia(media)
                media.release()

                setEventListener { event ->
                    activity?.runOnUiThread {
                        when (event.type) {
                            MediaPlayer.Event.Playing -> {
                                connected = true
                                b.btnConnect.text = "⏹  DESCONECTAR"
                                b.btnConnect.setBackgroundColor(0xFF1B5E20.toInt())
                                b.statusDot.setBackgroundResource(R.drawable.dot_green)
                                b.tvStatus.text = "EN VIVO"
                                b.tvStatus.setTextColor(0xFF00e676.toInt())
                            }
                            MediaPlayer.Event.EncounteredError -> {
                                Toast.makeText(ctx, "Error de stream — verifica la URL", Toast.LENGTH_LONG).show()
                                stopStream()
                            }
                            MediaPlayer.Event.Stopped -> stopStream()
                        }
                    }
                }
                play()
            }
        } catch (e: Exception) {
            Log.e("NVR", "startStream error: ${e.message}")
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopStream() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.detachViews()
            mediaPlayer?.release()
            libVlc?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        libVlc = null
        connected = false

        activity?.runOnUiThread {
            _b?.let {
                it.btnConnect.text = "▶  CONECTAR"
                it.btnConnect.setBackgroundColor(0xFF0b1a0f.toInt())
                it.statusDot.setBackgroundResource(R.drawable.dot_red)
                it.tvStatus.text = "DESCONECTADO"
                it.tvStatus.setTextColor(0xFFff3b3b.toInt())
                it.tvNoSignal.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        stopStream(); _b = null
        super.onDestroyView()
    }
}
