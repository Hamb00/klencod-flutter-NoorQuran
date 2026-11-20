package com.alquran.noor.util

import java.util.Locale

/** أدوات معالجة النص العربي: التطبيع للبحث، والأرقام، وفصل البسملة. */
object ArabicText {

    const val BISMILLAH = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"

    // الحركات وعلامات الضبط القرآنية والتطويل
    private val marks = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u08D3-\\u08FF\\u0640]")

    /** يزيل التشكيل ويوحّد صور الحروف ليصبح البحث غير حساس للرسم العثماني. */
    fun normalize(input: String): String {
        val sb = StringBuilder(marks.replace(input, ""))
        for (i in sb.indices) {
            sb[i] = when (sb[i]) {
                'ٱ', 'أ', 'إ', 'آ' -> 'ا' // ٱ أ إ آ -> ا
                'ى', 'ی' -> 'ي'                     // ى ی -> ي
                'ة' -> 'ه'                               // ة -> ه
                'ؤ' -> 'و'                               // ؤ -> و
                'ئ' -> 'ي'                               // ئ -> ي
                'ک' -> 'ك'                               // ک -> ك
                else -> sb[i]
            }
        }
        return sb.toString().trim()
    }

    fun arabicIndic(n: Int): String =
        n.toString().map { '٠' + (it - '0') }.joinToString("")

    /** رقم بحسب لغة الواجهة. */
    fun number(n: Int): String =
        if (Locale.getDefault().language == "ar") arabicIndic(n) else n.toString()

    /**
     * في رواية حفص يأتي نص الآية الأولى من كل سورة (عدا الفاتحة والتوبة) مسبوقًا بالبسملة
     * في بعض المصادر؛ نفصلها لنعرضها كترويسة مستقلة.
     */
    fun stripBismillah(text: String, surah: Int, ayah: Int): String {
        if (ayah != 1 || surah == 1 || surah == 9) return text
        if (!normalize(text).startsWith("بسم الله الرحمن الرحيم")) return text
        val words = text.trim().split(' ')
        return if (words.size > 4) words.drop(4).joinToString(" ") else text
    }
}
