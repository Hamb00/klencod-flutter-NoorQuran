package com.alquran.noor.data

import java.util.Locale

data class Reciter(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    /** اسم المجلد على everyayah.com */
    val folder: String,
)

fun Reciter.displayName(): String =
    if (Locale.getDefault().language == "ar") nameAr else nameEn

object Reciters {
    val all = listOf(
        Reciter("alafasy", "مشاري راشد العفاسي", "Mishary Rashid Alafasy", "Alafasy_128kbps"),
        Reciter("sudais", "عبد الرحمن السديس", "Abdurrahman As-Sudais", "Abdurrahmaan_As-Sudais_192kbps"),
        Reciter("shuraim", "سعود الشريم", "Saud Ash-Shuraim", "Saood_ash-Shuraym_128kbps"),
        Reciter("maher", "ماهر المعيقلي", "Maher Al-Muaiqly", "Maher_AlMuafy_128kbps"),
        Reciter("husary", "محمود خليل الحصري", "Mahmoud Khalil Al-Husary", "Husary_128kbps"),
        Reciter("minshawi", "محمد صديق المنشاوي (مرتل)", "Al-Minshawi (Murattal)", "Minshawy_Murattal_128kbps"),
        Reciter("basit", "عبد الباسط عبد الصمد (مرتل)", "Abdul Basit (Murattal)", "Abdul_Basit_Murattal_192kbps"),
        Reciter("ghamdi", "سعد الغامدي", "Saad Al-Ghamdi", "Ghamadi_40kbps"),
        Reciter("hudhaify", "علي الحذيفي", "Ali Al-Hudhaify", "Hudhaify_128kbps"),
        Reciter("shatri", "أبو بكر الشاطري", "Abu Bakr Ash-Shatri", "Abu_Bakr_Ash-Shaatree_128kbps"),
        Reciter("rifai", "هاني الرفاعي", "Hani Ar-Rifai", "Hani_Rifai_192kbps"),
        Reciter("dussary", "ياسر الدوسري", "Yasser Ad-Dussary", "Yasser_Ad-Dussary_128kbps"),
        Reciter("qatami", "ناصر القطامي", "Nasser Al-Qatami", "Nasser_Alqatami_128kbps"),
    )

    val default: Reciter = all.first()

    fun byId(id: String): Reciter = all.firstOrNull { it.id == id } ?: default

    fun audioUrl(reciter: Reciter, surah: Int, ayah: Int): String =
        String.format(Locale.US, "https://everyayah.com/data/%s/%03d%03d.mp3", reciter.folder, surah, ayah)
}
