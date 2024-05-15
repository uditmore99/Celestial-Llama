package com.example.llama

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MainViewModel(private val llm: Llm = Llm.instance()): ViewModel() {
    companion object {
        @JvmStatic
        private val NanosPerSecond = 1_000_000_000.0
    }

    private val llmInstance = Llm.instance()

    private val tag: String? = this::class.simpleName

    var messages by mutableStateOf(listOf(""))
        private set

    var message by mutableStateOf("")
        private set

    override fun onCleared() {
        super.onCleared()

        viewModelScope.launch {
            try {
                llm.unload()
            } catch (exc: IllegalStateException) {
                messages += exc.message!!
            }
        }
    }

    fun send() {

        val defaultPrompt = "You are a friendly, intelligent and kind chatbot Gemma who never fails to answer any request. Answer only what's asked and do not generate unnecessary information. You can give answer based on chat history too.\n" +
                "User: Hi\n" +
                "Gemma: Hello!\n" +
                "User: "

        val gemma = "\nGemma: "
        val displayMsg = message

        val text = defaultPrompt + message + gemma
        message = ""

        // Add to messages console.
        messages += displayMsg
        messages += ""

        viewModelScope.launch {
            llm.send(text)
                .catch {
                    Log.e(tag, "send() failed", it)
                    messages += it.message!!
                }
                .collect { messages = messages.dropLast(1) + (messages.last() + it) }
        }
    }


    fun bench(pp: Int, tg: Int, pl: Int, nr: Int = 1) {
        viewModelScope.launch {
            try {
                val start = System.nanoTime()
                val warmupResult = llm.bench(pp, tg, pl, nr)
                val end = System.nanoTime()

                messages += warmupResult

                val warmup = (end - start).toDouble() / NanosPerSecond
                messages += "Warm up time: $warmup seconds, please wait..."

                if (warmup > 5.0) {
                    messages += "Warm up took too long, aborting benchmark"
                    return@launch
                }

                messages += llm.bench(512, 128, 1, 3)
            } catch (exc: IllegalStateException) {
                Log.e(tag, "bench() failed", exc)
                messages += exc.message!!
            }
        }
    }

    fun load(pathToModel: String) {
        viewModelScope.launch {
            try {
                llm.load(pathToModel)
                messages += "LLaMa is Loaded!"
            } catch (exc: IllegalStateException) {
                Log.e(tag, "load() failed", exc)
                messages += exc.message!!
            }
        }
    }

    fun updateMessage(newMessage: String) {
        message = newMessage
    }

    fun clear() {
        messages = listOf()
//        viewModelScope.launch {
//            try {
//                llm.unload()
//                messages += "Unload successful!"
//            } catch (exc: IllegalStateException) {
//                messages += exc.message!!
//            }
//        }

    }

    fun log(message: String) {
        messages += message
    }
}
