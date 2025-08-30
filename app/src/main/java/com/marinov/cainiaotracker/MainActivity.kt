package com.marinov.cainiaotracker







import android.Manifest



import android.annotation.SuppressLint



import android.content.pm.PackageManager



import android.content.res.Configuration



import android.graphics.Color



import android.os.Build



import android.os.Bundle



import android.view.View



import android.view.WindowManager



import androidx.appcompat.app.AppCompatActivity



import androidx.appcompat.app.AppCompatDelegate



import androidx.core.app.ActivityCompat



import androidx.core.content.ContextCompat



import androidx.navigation.fragment.NavHostFragment



import androidx.navigation.ui.setupWithNavController



import androidx.work.ExistingPeriodicWorkPolicy



import androidx.work.PeriodicWorkRequest



import androidx.work.WorkManager



import com.marinov.cainiaotracker.databinding.ActivityMainBinding



import java.util.concurrent.TimeUnit







class MainActivity : AppCompatActivity() {



    companion object {



        private const val REQUEST_NOTIFICATION_PERMISSION = 100



    }



    private lateinit var binding: ActivityMainBinding







    override fun onCreate(savedInstanceState: Bundle?) {



        super.onCreate(savedInstanceState)



        binding = ActivityMainBinding.inflate(layoutInflater)



        setContentView(binding.root)







        setSupportActionBar(binding.topAppBar)







        // Configura o NavController com a BottomNavigationView



        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment



        val navController = navHostFragment.navController



        binding.bottomNavigation.setupWithNavController(navController)



        solicitarPermissaoNotificacao()



        // fix later iniciarUpdateWorker()



        configureSystemBarsForLegacyDevices()



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



                            ContextCompat.getColor(this@MainActivity, R.color.borda)



                        } else {



                            ContextCompat.getColor(this@MainActivity, R.color.fundo)



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







    private fun solicitarPermissaoNotificacao() {



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {



            if (ContextCompat.checkSelfPermission(



                    this,



                    Manifest.permission.POST_NOTIFICATIONS



                ) != PackageManager.PERMISSION_GRANTED



            ) {



                ActivityCompat.requestPermissions(



                    this,



                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),



                    REQUEST_NOTIFICATION_PERMISSION



                )



            }



        }



    }







    private fun iniciarUpdateWorker() {



        val updateWork = PeriodicWorkRequest.Builder(



            UpdateCheckWorker::class.java,



            15,



            TimeUnit.MINUTES



        ).build()



        WorkManager.getInstance(this).enqueueUniquePeriodicWork(



            "UpdateCheckWorker",



            ExistingPeriodicWorkPolicy.KEEP,



            updateWork



        )



    }



}