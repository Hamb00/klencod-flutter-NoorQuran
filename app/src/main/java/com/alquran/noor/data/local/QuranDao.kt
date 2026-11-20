package com.alquran.noor.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranDao {

    // ---- السور ----
    @Query("SELECT * FROM surahs ORDER BY number")
    fun surahs(): Flow<List<SurahEntity>>

    @Query("SELECT * FROM surahs ORDER BY number")
    suspend fun surahsOnce(): List<SurahEntity>

    @Query("SELECT * FROM surahs WHERE number = :number")
    suspend fun surah(number: Int): SurahEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurahs(items: List<SurahEntity>)

    // ---- الآيات ----
    @Query("SELECT COUNT(*) FROM ayahs")
    suspend fun ayahCount(): Int

    @Query("SELECT * FROM ayahs WHERE surah = :surah ORDER BY numberInSurah")
    fun ayahs(surah: Int): Flow<List<AyahEntity>>

    @Query("SELECT * FROM ayahs WHERE surah = :surah AND numberInSurah = :ayah")
    suspend fun ayah(surah: Int, ayah: Int): AyahEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyahs(items: List<AyahEntity>)

    @Query("UPDATE ayahs SET tafsir = :tafsir WHERE surah = :surah AND numberInSurah = :ayah")
    suspend fun setTafsir(surah: Int, ayah: Int, tafsir: String)

    // ---- البحث والفهارس ----
    @Query(
        """
        SELECT a.surah AS surah, a.numberInSurah AS ayah, a.text AS text, s.name AS surahName
        FROM ayahs a JOIN surahs s ON s.number = a.surah
        WHERE a.textClean LIKE '%' || :query || '%'
        ORDER BY a.id LIMIT 100
        """
    )
    suspend fun search(query: String): List<SearchHit>

    @Query(
        """
        SELECT a.juz AS juz, a.surah AS surah, a.numberInSurah AS ayah, s.name AS surahName
        FROM ayahs a JOIN surahs s ON s.number = a.surah
        WHERE a.id IN (SELECT MIN(id) FROM ayahs GROUP BY juz)
        ORDER BY a.juz
        """
    )
    fun juzStarts(): Flow<List<JuzStart>>

    // ---- العلامات ----
    @Query(
        """
        SELECT b.id AS id, b.surah AS surah, b.ayah AS ayah, b.createdAt AS createdAt,
               s.name AS surahName, a.text AS text
        FROM bookmarks b
        JOIN surahs s ON s.number = b.surah
        JOIN ayahs a ON a.surah = b.surah AND a.numberInSurah = b.ayah
        ORDER BY b.createdAt DESC
        """
    )
    fun bookmarkItems(): Flow<List<BookmarkItem>>

    @Query("SELECT ayah FROM bookmarks WHERE surah = :surah")
    fun bookmarkedAyahs(surah: Int): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBookmark(item: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE surah = :surah AND ayah = :ayah")
    suspend fun deleteBookmark(surah: Int, ayah: Int)

    @Query("SELECT COUNT(*) FROM bookmarks WHERE surah = :surah AND ayah = :ayah")
    suspend fun bookmarkExists(surah: Int, ayah: Int): Int
}
