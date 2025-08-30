package com.marinov.cainiaotracker

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class TrackingActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingLayout: LinearLayout
    private lateinit var noInternetLayout: LinearLayout
    private lateinit var retryButton: MaterialButton

    private val allowedDomain = "global.cainiao.com"
    private var isDarkTheme = false
    private val handler = Handler(Looper.getMainLooper())
    private var continuousRemovalRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracking)
        configureSystemBarsForLegacyDevices()
        // Inicializa e configura as views
        initializeViews()
        setupWebView()

        // Configura o comportamento do botão "Voltar"
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        })

        // Verifica a conexão com a internet para decidir o que carregar
        val url = intent.getStringExtra(EXTRA_URL)
        if (!isOnline()) {
            showNoInternetUI()
        } else {
            if (url != null) {
                webView.loadUrl(url)
            } else {
                finish() // Finaliza se a URL for nula
            }
        }
    }
    @SuppressLint("ObsoleteSdkInt")



    private fun configureSystemBarsForLegacyDevices() {



        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {



            val isDarkMode = when (AppCompatDelegate.getDefaultNightMode()) {



                AppCompatDelegate.MODE_NIGHT_YES -> true



                AppCompatDelegate.MODE_NIGHT_NO -> false



                else -> {



                    val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK



                    currentNightMode == Configuration.UI_MODE_NIGHT_YES



                }



            }



            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {



                window.apply {



                    @Suppress("DEPRECATION")



                    clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)



                    addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)



                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {



                        @Suppress("DEPRECATION")



                        statusBarColor = Color.BLACK



                        @Suppress("DEPRECATION")



                        navigationBarColor = Color.BLACK



                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {



                            @Suppress("DEPRECATION")



                            var flags = decorView.systemUiVisibility



                            @Suppress("DEPRECATION")



                            flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()



                            @Suppress("DEPRECATION")



                            decorView.systemUiVisibility = flags



                        }



                    } else {



                        @Suppress("DEPRECATION")



                        navigationBarColor = if (isDarkMode) {



                            ContextCompat.getColor(this@TrackingActivity, R.color.fundo)



                        } else {



                            ContextCompat.getColor(this@TrackingActivity, R.color.fundo)



                        }



                    }



                }



            }



            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {



                @Suppress("DEPRECATION")



                var flags = window.decorView.systemUiVisibility



                if (isDarkMode) {



                    @Suppress("DEPRECATION")



                    flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()



                } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {



                    @Suppress("DEPRECATION")



                    flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR



                }



                @Suppress("DEPRECATION")



                window.decorView.systemUiVisibility = flags



            }



            if (!isDarkMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {



                @Suppress("DEPRECATION")



                var flags = window.decorView.systemUiVisibility



                @Suppress("DEPRECATION")



                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR



                @Suppress("DEPRECATION")



                window.decorView.systemUiVisibility = flags



            }



        }



    }
    private fun initializeViews() {
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        loadingLayout = findViewById(R.id.loadingLayout)
        noInternetLayout = findViewById(R.id.noInternetLayout)
        retryButton = findViewById(R.id.retryButton)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar_webview)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        retryButton.setOnClickListener {
            if (isOnline()) {
                showLoadingUI()
                // Se a webview nunca carregou uma URL, carrega a URL inicial. Senão, apenas recarrega.
                if (webView.url.isNullOrEmpty()) {
                    val url = intent.getStringExtra(EXTRA_URL)
                    if (url != null) {
                        webView.loadUrl(url)
                    } else {
                        finish()
                    }
                } else {
                    webView.reload()
                }
            }
        }
    }

    private fun setupWebView() {
        // Verifica o tema do sistema
        isDarkTheme = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            // Configurações de segurança
            allowContentAccess = false
            allowFileAccess = false
            // Desabilita o zoom
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
        }

        // Desabilita a seleção de texto
        webView.setOnLongClickListener {
            true // Retorna true para consumir o evento e desativar a seleção de texto
        }

        // Desabilita a seleção de texto para versões mais antigas do Android
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            webView.isLongClickable = false
        }


        // --- IMPLEMENTAÇÃO DO TEMA ESCURO (TÉCNICA ATUALIZADA) ---
        // Força o tema escuro no WebView de forma nativa quando disponível
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(
                webView.settings,
                if (isDarkTheme) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF
            )
            // Usa a estratégia de escurecimento do tema da web para melhores resultados
            if (isDarkTheme && WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
                WebSettingsCompat.setForceDarkStrategy(
                    webView.settings,
                    WebSettingsCompat.DARK_STRATEGY_WEB_THEME_DARKENING_ONLY
                )
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
                startContinuousElementRemoval()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE

                // Aplica o tema escuro e remove elementos via JS como fallback
                applyDarkThemeViaJs()
                removeElements()

                // Mostra o WebView com animação após o carregamento
                showWebViewWithAnimation()
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                // Se houver erro de carregamento e estiver offline, mostra a tela de erro
                if (!isOnline()) {
                    showNoInternetUI()
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                return if (!url.contains(allowedDomain)) {
                    // Abre links externos no navegador padrão
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                    } catch (_: Exception) {
                        // Trata exceção caso não haja app para abrir o link
                    }
                    true
                } else {
                    false
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
            }
        }
    }

    // Mostra o WebView com uma animação suave de fade-in
    private fun showWebViewWithAnimation() {
        loadingLayout.visibility = View.GONE
        noInternetLayout.visibility = View.GONE
        webView.alpha = 0f
        webView.visibility = View.VISIBLE
        webView.animate().alpha(1f).duration = 300
    }

    // Mostra a UI de carregamento
    private fun showLoadingUI() {
        loadingLayout.visibility = View.VISIBLE
        noInternetLayout.visibility = View.GONE
        webView.alpha = 0f // Esconde o WebView para a recarga
    }

    // Mostra a UI de "sem internet"
    private fun showNoInternetUI() {
        loadingLayout.visibility = View.GONE
        noInternetLayout.visibility = View.VISIBLE
        webView.visibility = View.GONE
        progressBar.visibility = View.GONE
    }

    private fun removeElements() {
        val jsCode = """
            (function() {
                var selectors = [
                    "body > div.puckinn-cookie-consent", // Cookie banner
                    "#root > div > div:nth-child(1) > div", // Header superior
                    "#root > div > div.detail--searchWrapper--2C3_pEZ.undefined", // Search wrapper
                    "#root > div > div.detail--headerFixed--ABe4AjX > div", // Header fixo
                    "#tracking-detail-wrapper > div.footer-wrapper.fit-footer-wrapper > div", // Footer
                    "#quick-score-root > div" // Quick score
                ];
                selectors.forEach(function(selector) {
                    var element = document.querySelector(selector);
                    if (element) element.style.display = 'none';
                });
            })();
        """.trimIndent()
        webView.evaluateJavascript(jsCode, null)
    }

    private fun applyDarkThemeViaJs() {
        if (!isDarkTheme) return

        val jsCode = """
            (function() {
                var style = document.createElement('style');
                style.innerHTML = `
                    html {
                        filter: invert(1) hue-rotate(180deg) !important;
                        background: #121212 !important;
                    }
                    img, picture, video, iframe {
                        filter: invert(1) hue-rotate(180deg) !important;
                    }
                `;
                document.head.appendChild(style);
            })();
        """.trimIndent()
        webView.evaluateJavascript(jsCode, null)
    }

    private fun startContinuousElementRemoval() {
        stopContinuousElementRemoval() // Garante que não haja runnables duplicados
        continuousRemovalRunnable = Runnable {
            removeElements()
            applyDarkThemeViaJs() // Garante que o tema persista
            handler.postDelayed(continuousRemovalRunnable!!, 300) // Executa a cada 300ms
        }
        handler.post(continuousRemovalRunnable!!)
    }

    private fun stopContinuousElementRemoval() {
        continuousRemovalRunnable?.let { handler.removeCallbacks(it) }
    }

    // --- GERENCIAMENTO DO CICLO DE VIDA ---
    override fun onResume() {
        super.onResume()
        webView.onResume()
        // Reinicia a remoção contínua ao voltar para o app
        if (webView.url != null) {
            startContinuousElementRemoval()
        }
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
        // Para a remoção contínua para economizar recursos
        stopContinuousElementRemoval()
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
        // Limpa o handler para evitar memory leaks
        stopContinuousElementRemoval()
    }

    // --- FUNÇÃO DE UTILIDADE ---
    private fun isOnline(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    companion object {
        const val EXTRA_URL = "extra_url"
    }
}
