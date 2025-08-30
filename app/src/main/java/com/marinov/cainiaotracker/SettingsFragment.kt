package com.marinov.cainiaotracker
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
class SettingsFragment : Fragment() {
    private val tag = "SettingsFragment"
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var progressBar: ProgressBar? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        setupUI(view)
        return view
    }
    override fun onResume() {
        super.onResume()
        // Verificar se temos uma URL de atualização da MainActivity
        val mainActivity = activity as? MainActivity
        val updateUrl = mainActivity?.getUpdateUrlFromNotification()
        updateUrl?.let {
            promptForUpdate(it)
        }
    }
    private fun setupUI(view: View) {
        val btnCheckUpdate = view.findViewById<Button>(R.id.btn_check_update)
        val btnGithub = view.findViewById<Button>(R.id.btn_github)
        btnGithub.setOnClickListener {
            openUrl("https://github.com/gmb7886")
        }
        btnCheckUpdate.setOnClickListener {
            checkUpdate()
        }
    }
    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(tag, "Erro ao abrir URL", e)
        }
    }
    private fun checkUpdate() = coroutineScope.launch {
        try {
            val (json, responseCode) = withContext(Dispatchers.IO) {
                val url = URL("https://api.github.com/repos/gmb7886/CainiaoTracker/releases/latest")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                connection.setRequestProperty("User-Agent", "CainiaoTracker-Android")
                connection.connectTimeout = 10000
                connection.connect()
                try {
                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        connection.inputStream.use { input ->
                            JSONObject(input.readText()) to connection.responseCode
                        }
                    } else {
                        null to connection.responseCode
                    }
                } finally {
                    connection.disconnect()
                }
            }
            if (json != null) {
                processReleaseData(json)
            } else {
                showError("Erro na conexão: Código $responseCode")
            }
        } catch (e: Exception) {
            Log.e(tag, "Erro na verificação", e)
            showError("Erro: ${e.message}")
        }
    }
    private fun InputStream.readText(): String {
        return BufferedReader(InputStreamReader(this)).use { it.readText() }
    }
    private fun processReleaseData(release: JSONObject) {
        activity?.runOnUiThread {
            val latest = release.getString("tag_name")
            val current = getCurrentVersionName()
            if (isVersionGreater(latest, current)) {
                val assets = release.getJSONArray("assets")
                var apkUrl: String? = null
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.getString("name").endsWith(".apk")) {
                        apkUrl = asset.getString("browser_download_url")
                        break
                    }
                }
                apkUrl?.let { promptForUpdate(it) } ?: showError("Arquivo APK não encontrado no release.")
            } else {
                showMessage("Você já está na versão mais recente.")
            }
        }
    }
    private fun getCurrentVersionName(): String {
        return try {
            requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
    private fun getCurrentApplicationId(): String {
        return requireContext().packageName
    }
    private fun isVersionGreater(newVersion: String, currentVersion: String): Boolean {
        val newParts = newVersion.removePrefix("v").split(".").map { it.toInt() }
        val currentParts = currentVersion.removePrefix("v").split(".").map { it.toInt() }
        val maxParts = maxOf(newParts.size, currentParts.size)
        for (i in 0 until maxParts) {
            val newPart = newParts.getOrNull(i) ?: 0
            val currentPart = currentParts.getOrNull(i) ?: 0
            if (newPart > currentPart) return true
            if (newPart < currentPart) return false
        }
        return false
    }
    private fun promptForUpdate(url: String) {
        activity?.runOnUiThread {
            AlertDialog.Builder(requireContext())
                .setTitle("Atualização Disponível")
                .setMessage("Deseja baixar e instalar a versão mais recente?")
                .setPositiveButton("Sim") { _, _ -> startManualDownload(url) }
                .setNegativeButton("Não", null)
                .show()
        }
    }
    private fun startManualDownload(apkUrl: String) {
        coroutineScope.launch {
            val progressDialog = createProgressDialog().apply { show() }
            try {
                val apkFile = withContext(Dispatchers.IO) { downloadApk(apkUrl) }
                progressDialog.dismiss()
                if (apkFile != null && apkFile.exists() && apkFile.length() > 0) {
                    showInstallDialog(apkFile)
                } else {
                    showError("Falha ao baixar o arquivo. O arquivo está vazio ou não foi baixado corretamente.")
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                Log.e(tag, "Erro no download", e)
                showError("Falha no download: ${e.message}")
            }
        }
    }
    @SuppressLint("InflateParams")
    private fun createProgressDialog(): AlertDialog {
        val view = layoutInflater.inflate(R.layout.dialog_download_progress, null)
        progressBar = view.findViewById(R.id.progress_bar)
        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setCancelable(false)
            .create()
    }
    private suspend fun downloadApk(apkUrl: String): File? = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Iniciando download do APK: $apkUrl")
            val connection = URL(apkUrl).openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "CainiaoTracker-Android")
            connection.setRequestProperty("Accept", "application/octet-stream")
            connection.connect()
            Log.d(tag, "Código de resposta: ${connection.responseCode}")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(tag, "Falha no download: código ${connection.responseCode}")
                return@withContext null
            }
            val fileLength = connection.contentLength
            Log.d(tag, "Tamanho do arquivo: $fileLength")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val outputDir = File(downloadsDir, "CainiaoTracker").apply {
                if (exists()) deleteRecursively()
                mkdirs()
            }
            val outputFile = File(outputDir, "app_release.apk")
            connection.inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    var total: Long = 0
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        total += bytesRead
                        if (fileLength > 0) {
                            val progress = (total * 100 / fileLength).toInt()
                            withContext(Dispatchers.Main) {
                                progressBar?.progress = progress
                            }
                        }
                    }
                }
            }
            Log.d(tag, "Download concluído. Tamanho do arquivo salvo: ${outputFile.length()}")
            if (outputFile.length() == 0L) {
                Log.e(tag, "Arquivo vazio após download.")
                return@withContext null
            }
            outputFile
        } catch (e: Exception) {
            Log.e(tag, "Erro no download", e)
            null
        }
    }
    private fun showInstallDialog(apkFile: File) {
        activity?.runOnUiThread {
            try {
                if (!apkFile.exists()) {
                    showError("Arquivo APK não encontrado")
                    return@runOnUiThread
                }
                val apkUri = FileProvider.getUriForFile(
                    requireContext(),
                    "${getCurrentApplicationId()}.provider",
                    apkFile
                )
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (installIntent.resolveActivity(requireContext().packageManager) != null) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Download concluído")
                        .setMessage("Deseja instalar a atualização agora?")
                        .setPositiveButton("Instalar") { _, _ -> startActivity(installIntent) }
                        .setNegativeButton("Cancelar", null)
                        .show()
                } else {
                    showError("Nenhum aplicativo encontrado para instalar o APK")
                }
            } catch (e: Exception) {
                Log.e(tag, "Erro na instalação", e)
                showError("Erro ao iniciar a instalação: ${e.message}")
            }
        }
    }
    private fun showMessage(msg: String) {
        activity?.runOnUiThread {
            AlertDialog.Builder(requireContext())
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show()
        }
    }
    private fun showError(msg: String) {
        activity?.runOnUiThread {
            AlertDialog.Builder(requireContext())
                .setTitle("Erro")
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        progressBar = null
    }
}