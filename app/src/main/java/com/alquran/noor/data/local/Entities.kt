package com.alquran.noor.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "surahs")
data class SurahEntity(
    @PrimaryKey val number: Int,
    val name: String,
    val englishName: String,
    val englishNameTranslation: String,
    val revelationType: String,
    val ayahCount: Int,
) {
    val isMeccan: Boolean get() = revelationType.equals("Meccan", ignoreCase = true)
}

@Entity(
    tableName = "ayahs",
    indices = [Index("surah"), Index("juz"), Index("page")],
)
data class AyahEntity(
    /** الرقم العام للآية من 1 إلى 6236 */
    @PrimaryKey val id: Int,
    val surah: Int,
    val numberInSurah: Int,
    val juz: Int,
    val page: Int,
    val hizbQuarter: Int,
    val sajda: Boolean,
    val text: String,
    /** نص مُطبَّع بلا تشكيل لأغراض البحث */
    val textClean: String,
    /** التفسير الميسر، يُحمَّل عند الطلب ثم يُخزَّن */
    val tafsir: String? = null,
)

@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["surah", "ayah"], unique = true)],
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val surah: Int,
    val ayah: Int,
    val createdAt: Long,
)

/** نتائج الاستعلامات المركبة */
data class BookmarkItem(
    val id: Long,
    val surah: Int,
    val ayah: Int,
    val createdAt: Long,
    val surahName: String,
    val text: String,
)

data class SearchHit(
    val surah: Int,
    val ayah: Int,
    val text: String,
    val surahName: String,
)

data class JuzStart(
    val juz: Int,
    val surah: Int,
    val ayah: Int,
    val surahName: String,
)
