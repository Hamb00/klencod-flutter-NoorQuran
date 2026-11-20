package com.alquran.noor.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromStream
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

@Serializable
data class QuranEnvelope(val data: QuranData)

@Serializable
data class QuranData(val surahs: List<SurahDto>)

@Serializable
data class SurahEnvelope(val data: SurahDto)

@Serializable
data class SurahDto(
    val number: Int,
    val name: String = "",
    val englishName: String = "",
    val englishNameTranslation: String = "",
    val revelationType: String = "",
    val ayahs: List<AyahDto> = emptyList(),
)

@Serializable
data class AyahDto(
    val number: Int,
    val text: String,
    val numberInSurah: Int,
    val juz: Int = 0,
    val page: Int = 0,
    val hizbQuarter: Int = 0,
    /** false أو كائن يصف السجدة */
    val sajda: JsonElement? = null,
)

/**
 * مصدر النص: https://alquran.cloud (رسم عثماني + التفسير الميسر).
 */
class QuranApi(private val client: OkHttpClient) {

    private val json = Json { ignoreUnknownKeys = true }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun fullQuran(): List<SurahDto> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$BASE/quran/quran-uthmani").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body ?: throw IOException("Empty body")
            json.decodeFromStream<QuranEnvelope>(body.byteStream()).data.surahs
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun tafsir(surah: Int): List<AyahDto> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$BASE/surah/$surah/$TAFSIR_EDITION").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body ?: throw IOException("Empty body")
            json.decodeFromStream<SurahEnvelope>(body.byteStream()).data.ayahs
        }
    }

    private companion object {
        const val BASE = "https://api.alquran.cloud/v1"
        const val TAFSIR_EDITION = "ar.muyassar"
    }
}
