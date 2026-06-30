package de.nicerdicer.util

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object KtorUtils
{
    suspend fun downloadImage(url: String, outputFile: File) {
        // 1. Create the HTTP client (Best practice: reuse this instance in production)
        val client = HttpClient(CIO)

        // 2. Switch to the IO dispatcher for non-blocking I/O operations
        withContext(Dispatchers.IO) {
            try {
                // 3. Make a streaming request so you don't load the whole image into memory at once
                client.prepareGet(url).execute { response ->
                    if (response.status.value in 200..299) {
                        val channel: ByteReadChannel = response.bodyAsChannel()
                        // 4. Efficiently copy the network byte channel directly to the file
                        channel.copyTo(outputFile.writeChannel())
                    } else {
                        println("Failed to download image: ${response.status}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                client.close() // Close if you aren't reusing the client instance
            }
        }
    }
}