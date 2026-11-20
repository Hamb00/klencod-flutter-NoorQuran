package com.alquran.noor.data

import androidx.room.withTransaction
import com.alquran.noor.data.local.AyahEntity
import com.alquran.noor.data.local.BookmarkEntity
import com.alquran.noor.data.local.BookmarkItem
import com.alquran.noor.data.local.JuzStart
import com.alquran.noor.data.local.QuranDatabase
import com.alquran.noor.data.local.SearchHit
import com.alquran.noor.data.local.SurahEntity
import com.alquran.noor.data.remote.QuranApi
import com.alquran.noor.data.settings.SettingsStore
import com.alquran.noor.util.ArabicText
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject
import java.io.IOException

class QuranRepository(
    private val db: QuranDatabase,
    private val api: QuranApi,
    val settings: SettingsStore,
) {
    private val dao = db.dao()

    val surahs: Flow<List<SurahEntity>> = dao.surahs()
    val juzStarts: Flow<List<JuzStart>> = dao.juzStarts()
    val bookmarks: Flow<List<BookmarkItem>> = dao.bookmarkItems()

    fun ayahs(surah: Int): Flow<List<AyahEntity>> = dao.ayahs(surah)
    fun bookmarkedAyahs(surah: Int): Flow<List<Int>> = dao.bookmarkedAyahs(surah)

    suspend fun surah(number: Int): SurahEntity? = dao.surah(number)
    suspend fun surahsOnce(): List<SurahEntity> = dao.surahsOnce()
    suspend fun search(normalizedQuery: String): List<SearchHit> = dao.search(normalizedQuery)

    // ---------- التنزيل الأول للمصحف ----------

    suspend fun isDownloaded(): Boolean = dao.ayahCount() >= TOTAL_AYAHS

    /** يجلب المصحف كاملًا مرة واحدة ويخزنه محليًا ليعمل التطبيق دون اتصال. */
    suspend fun download() {
        val remote = api.fullQuran()

        val surahEntities = remote.map {
            SurahEntity(
                number = it.number,
                name = it.name,
                englishName = it.englishName,
                englishNameTranslation = it.englishNameTranslation,
                revelationType = it.revelationType,
                ayahCount = it.ayahs.size,
            )
        }

        val ayahEntities = ArrayList<AyahEntity>(TOTAL_AYAHS)
        for (s in remote) {
            for (a in s.ayahs) {
                val text = ArabicText.stripBismillah(a.text, s.number, a.numberInSurah)
                ayahEntities += AyahEntity(
                    id = a.number,
                    surah = s.number,
                    numberInSurah = a.numberInSurah,
                    juz = a.juz,
                    page = a.page,
                    hizbQuarter = a.hizbQuarter,
                    sajda = a.sajda is JsonObject,
                    text = text,
                    textClean = ArabicText.normalize(text),
                )
            }
        }

        // حماية من تنزيل ناقص
        if (surahEntities.size != 114 || ayahEntities.size != TOTAL_AYAHS) {
            throw IOException("Incomplete Quran data: ${surahEntities.size} surahs, ${ayahEntities.size} ayahs")
        }

        db.withTransaction {
            dao.insertSurahs(surahEntities)
            ayahEntities.chunked(1000).forEach { dao.insertAyahs(it) }
        }
    }

    // ---------- التفسير ----------

    suspend fun tafsir(surah: Int, ayah: Int): Result<String> = runCatching {
        dao.ayah(surah, ayah)?.tafsir?.let { return@runCatching it }
        val remote = api.tafsir(surah)
        db.withTransaction {
            remote.forEach { dao.setTafsir(surah, it.numberInSurah, it.text) }
        }
        dao.ayah(surah, ayah)?.tafsir ?: throw IOException("Tafsir not found")
    }

    // ---------- العلامات ----------

    suspend fun toggleBookmark(surah: Int, ayah: Int) {
        if (dao.bookmarkExists(surah, ayah) > 0) {
            dao.deleteBookmark(surah, ayah)
        } else {
            dao.insertBookmark(BookmarkEntity(surah = surah, ayah = ayah, createdAt = System.currentTimeMillis()))
        }
    }

    suspend fun removeBookmark(surah: Int, ayah: Int) = dao.deleteBookmark(surah, ayah)

    private companion object {
        const val TOTAL_AYAHS = 6236
    }
}
