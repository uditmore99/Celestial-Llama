package com.example.llama

import kotlinx.coroutines.*
import android.app.ActivityManager
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.text.format.Formatter
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import com.example.llama.ui.theme.LlamaAndroidTheme
import java.io.File



class MainActivity(
    activityManager: ActivityManager? = null,
    downloadManager: DownloadManager? = null,
    clipboardManager: ClipboardManager? = null,
): ComponentActivity() {
    private val tag: String? = this::class.simpleName

    private val activityManager by lazy { activityManager ?: getSystemService<ActivityManager>()!! }
    private val downloadManager by lazy { downloadManager ?: getSystemService<DownloadManager>()!! }
    private val clipboardManager by lazy { clipboardManager ?: getSystemService<ClipboardManager>()!! }

    private val viewModel: MainViewModel by viewModels()

    private val freeMemory = mutableStateOf("")
    private val totalMemory = mutableStateOf("")
    private val usedMemory = mutableStateOf("")


//    fun updateMemory(free: String, total: String) {
//        freeMemory.value = free
//        totalMemory.value = total
//    }


//     Get a MemoryInfo object for the device's current memory status.
//    private fun availableMemory(): ActivityManager.MemoryInfo {
//        return ActivityManager.MemoryInfo().also { memoryInfo ->
//            activityManager.getMemoryInfo(memoryInfo)
//        }
//    }



//    private fun updateMemoryStatus() {
//        lifecycleScope.launch {
//            while (isActive) { // Continues until the lifecycle ends or is cancelled
//                val memoryInfo = availableMemory()
//                val free = Formatter.formatFileSize(this@MainActivity, memoryInfo.availMem)
//                val total = Formatter.formatFileSize(this@MainActivity, memoryInfo.totalMem)
//
//
//
//                delay(2000) // Wait for 2 seconds before updating again
//            }
//        }
//    }
//
//    private fun availableMemory(): ActivityManager.MemoryInfo {
//        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
//        return ActivityManager.MemoryInfo().also {
//            activityManager.getMemoryInfo(it)
//        }
//    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)




        StrictMode.setVmPolicy(
            VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build()
        )

        updateMemoryStatus()

//        val free = Formatter.formatFileSize(this, availableMemory().availMem)
//        val total = Formatter.formatFileSize(this, availableMemory().totalMem)

//        viewModel.log("Current memory: $free / $total")
        viewModel.log("Downloads directory: ${getExternalFilesDir(null)}")

        val extFilesDir = getExternalFilesDir(null)

        val models = listOf(
            Downloadable(
                "Gemma 1.1 2B IT Q4_K_M (Q4_0, 1.6 GiB)",
                Uri.parse("https://huggingface.co/bartowski/gemma-1.1-2b-it-GGUF/resolve/main/gemma-1.1-2b-it-Q4_K_M.gguf?download=true"),
                File(extFilesDir, "gemma-1.1-2b-it-Q4_K_M.gguf"),
            )
        )

        setContent {
            LlamaAndroidTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainCompose(
                        viewModel,
                        clipboardManager,
                        downloadManager,
                        models,
                        freeMemory,
                        totalMemory,

                    )
                }

            }
        }
    }

    private fun updateMemoryStatus() {
        lifecycleScope.launch {
            while (isActive) { // Continues until the lifecycle ends or is cancelled
                val memoryInfo = availableMemory()
                totalMemory.value = Formatter.formatFileSize(this@MainActivity, memoryInfo.totalMem)
                freeMemory.value = Formatter.formatFileSize(this@MainActivity, (memoryInfo.totalMem - memoryInfo.availMem))



//                viewModel.log("Current memory: $freeMemory / $totalMemory")

                delay(2000) // Wait for 2 seconds before updating again
            }
        }
    }

    private fun availableMemory(): ActivityManager.MemoryInfo {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        return ActivityManager.MemoryInfo().also {
            activityManager.getMemoryInfo(it)
        }
    }
}




@Composable
fun MainCompose(
    viewModel: MainViewModel,
    clipboard: ClipboardManager,
    dm: DownloadManager,
    models: List<Downloadable>,
    freeMemory: State<String>,
    totalMemory: State<String>,

) {
    Column {
        Text(text = "Current Memory: ${freeMemory.value} / ${totalMemory.value}")
        val scrollState = rememberLazyListState()

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(state = scrollState) {
                items(viewModel.messages) {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyLarge.copy(color = LocalContentColor.current),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
        OutlinedTextField(
            value = viewModel.message,
            onValueChange = { viewModel.updateMessage(it) },
            label = { Text("Message") },
        )
        Row {
            Button({ viewModel.send() }) { Text("Send") }
//            Button({ viewModel.bench(8, 4, 1) }) { Text("Bench") }
            Button({ viewModel.clear() }) { Text("Clear") }
            Button({
                viewModel.messages.joinToString("\n").let {
                    clipboard.setPrimaryClip(ClipData.newPlainText("", it))
                }
            }) { Text("Copy") }
        }

        Column {
            for (model in models) {
                Downloadable.Button(viewModel, dm, model)
            }
        }
    }
}
