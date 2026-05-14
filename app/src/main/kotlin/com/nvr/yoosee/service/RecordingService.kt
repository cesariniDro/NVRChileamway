package com.nvr.yoosee.service

import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import java.io.*
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.*

class RecordingService : Service() {

    inner class LocalBinder : Binder() { fun getService() = this@RecordingService }
    private val binder = LocalBinder()

    private var recordingThread: Thread? = null
    private var nvrThread: Thread? = null
    @Volatile private var recording = false
    @Volatile private var nvrRunning = false
    @Volatile private var stopFlag = false

    override fun onBind(i: Intent): IBinder = binder

    fun isRecording() = recording
    fun isNvrRunning() = nvrRunning

    // ── Grabación simple ──────────────────────────────────────────────────────
    fun startRecording(rtspUrl: String) {
        if (recording) return
        recording = true
        stopFlag = false
        recordingThread = Thread {
            try {
                val out = outFile("ts")
                Log.d("NVR", "Grabando en: ${out.absolutePath}")
                streamToFile(rtspUrl, out, durationSec = null)
            } catch (e: Exception) {
                Log.e("NVR", "Error grabación: ${e.message}")
            } finally {
                recording = false
            }
        }.also { it.start() }
    }

    fun stopRecording() {
        stopFlag = true
        recordingThread?.interrupt()
        recording = false
    }

    // ── Modo NVR continuo ─────────────────────────────────────────────────────
    fun startNvr(rtspUrl: String, segMin: Int) {
        if (nvrRunning) return
        nvrRunning = true
        stopFlag = false
        nvrThread = Thread {
            var seg = 1
            while (!stopFlag) {
                try {
                    val out = outFile("ts")
                    Log.d("NVR", "NVR segmento $seg: ${out.name}")
                    streamToFile(rtspUrl, out, durationSec = segMin * 60L)
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    Log.e("NVR", "NVR seg $seg error: ${e.message}")
                    Thread.sleep(2000)
                }
                seg++
            }
            nvrRunning = false
        }.also { it.start() }
    }

    fun stopNvr() {
        stopFlag = true
        nvrThread?.interrupt()
        nvrRunning = false
    }

    fun takeSnapshot(rtspUrl: String) {
        Thread {
            try { streamToFile(rtspUrl, outFile("ts"), durationSec = 3L) }
            catch (e: Exception) { Log.e("NVR", "Snapshot error: ${e.message}") }
        }.start()
    }

    // ── Stream RTSP → archivo ─────────────────────────────────────────────────
    private fun streamToFile(rtspUrl: String, outFile: File, durationSec: Long?) {
        val regex = Regex("rtsp://(?:[^@]+@)?([^:/]+)(?::(\\d+))?")
        val m = regex.find(rtspUrl) ?: throw IOException("URL inválida: $rtspUrl")
        val host = m.groupValues[1]
        val port = m.groupValues[2].toIntOrNull() ?: 554

        Log.d("NVR", "Conectando a $host:$port")
        Socket(host, port).use { sock ->
            sock.soTimeout = 15000
            val writer = PrintWriter(BufferedWriter(OutputStreamWriter(sock.getOutputStream())), true)
            val reader = BufferedReader(InputStreamReader(sock.inputStream))

            fun sendCmd(cmd: String) {
                writer.print(cmd)
                writer.flush()
                var line = reader.readLine() ?: return
                while (line.isNotBlank()) {
                    line = reader.readLine() ?: return
                }
            }

            sendCmd("OPTIONS $rtspUrl RTSP/1.0\r\nCSeq: 1\r\nUser-Agent: NVRYoosee\r\n\r\n")
            sendCmd("DESCRIBE $rtspUrl RTSP/1.0\r\nCSeq: 2\r\nAccept: application/sdp\r\n\r\n")
            sendCmd("SETUP $rtspUrl/track1 RTSP/1.0\r\nCSeq: 3\r\nTransport: RTP/AVP/TCP;unicast;interleaved=0-1\r\n\r\n")
            sendCmd("PLAY $rtspUrl RTSP/1.0\r\nCSeq: 4\r\nRange: npt=0.000-\r\n\r\n")

            sock.soTimeout = 0 // sin timeout durante grabación
            val buf = ByteArray(65536)
            val startMs = System.currentTimeMillis()
            FileOutputStream(outFile).use { fos ->
                val ins = sock.getInputStream()
                while (!stopFlag && !Thread.currentThread().isInterrupted) {
                    if (durationSec != null) {
                        val elapsed = (System.currentTimeMillis() - startMs) / 1000
                        if (elapsed >= durationSec) break
                    }
                    val n = ins.read(buf)
                    if (n < 0) break
                    fos.write(buf, 0, n)
                }
            }
            Log.d("NVR", "Stream guardado: ${outFile.name} (${outFile.length()/1024} KB)")
        }
    }

    private fun outFile(ext: String): File {
        val dir = File(getExternalFilesDir(null), "NVR").also { it.mkdirs() }
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(dir, "cam_$ts.$ext")
    }

    override fun onDestroy() { stopRecording(); stopNvr(); super.onDestroy() }
}
