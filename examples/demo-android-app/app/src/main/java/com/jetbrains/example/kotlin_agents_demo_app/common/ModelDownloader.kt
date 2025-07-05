package com.jetbrains.example.kotlin_agents_demo_app.common

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

sealed class DownloadProgress {
    data class InProgress(val progress: Float) : DownloadProgress() // progress 0.0 to 1.0
    data class Completed(val file: File) : DownloadProgress()
    data class Failed(val error: Throwable) : DownloadProgress()
}

class ModelDownloader(private val httpClient: HttpClient) {

    suspend fun downloadFile(url: String, outputFile: File): Flow<DownloadProgress> = flow {
        try {
            httpClient.prepareGet(url).execute { httpResponse ->
                if (!httpResponse.status.isSuccess()) {
                    emit(DownloadProgress.Failed(IllegalStateException("HTTP error: ${httpResponse.status.value} ${httpResponse.status.description}")))
                    return@execute
                }

                val totalBytes = httpResponse.contentLength() ?: -1L
                var bytesCopied = 0L
                val channel: ByteReadChannel = httpResponse.bodyAsChannel()

                withContext(Dispatchers.IO) {
                    FileOutputStream(outputFile).use { outputStream ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var bytesRead: Int
                        while (channel.readAvailable(buffer).also { bytesRead = it } != -1 && bytesRead > 0) {
                            outputStream.write(buffer, 0, bytesRead)
                            bytesCopied += bytesRead
                            if (totalBytes > 0) {
                                val progress = (bytesCopied.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                                emit(DownloadProgress.InProgress(progress))
                            } else {
                                // Emit InProgress without percentage if total size is unknown
                                emit(DownloadProgress.InProgress(-1f)) // Or some other indicator
                            }
                        }
                    }
                }

                if (bytesCopied > 0 || totalBytes == 0L) { // Consider download successful if any bytes were copied or if content length was 0
                    emit(DownloadProgress.Completed(outputFile))
                } else if (totalBytes > 0 && bytesCopied < totalBytes) { // Check if not all bytes were copied
                     outputFile.delete() // Clean up partial file
                    emit(DownloadProgress.Failed(IllegalStateException("Download incomplete: Copied $bytesCopied of $totalBytes bytes.")))
                }
                // If totalBytes was -1 and bytesCopied is 0, it might be an empty file or error not caught by status.
                // The existing check for bytesCopied > 0 handles successful empty file downloads if totalBytes is also 0.
            }
        } catch (e: Exception) {
            outputFile.delete() // Clean up partial file on any exception
            emit(DownloadProgress.Failed(e))
        }
    }
}
