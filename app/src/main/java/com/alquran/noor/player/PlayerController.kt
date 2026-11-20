package com.alquran.noor.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.alquran.noor.data.Reciter
import com.alquran.noor.data.Reciters
import com.alquran.noor.data.displayName
import com.alquran.noor.util.ArabicText
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaybackUiState(
    val hasMedia: Boolean = false,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val surah: Int = 0,
    val ayah: Int = 0,
    val surahName: String = "",
    val reciterName: String = "",
    val speed: Float = 1f,
    val repeatAyah: Boolean = false,
    val sleepMinutesLeft: Int? = null,
)

/**
 * واجهة الواجهات الرسومية للتحكم بخدمة التشغيل.
 * كل سورة تُحمَّل كقائمة تشغيل (آية = عنصر) فيمكن تتبّع الآية الجارية وتظليلها.
 */
class PlayerController(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var future: ListenableFuture<MediaController>? = null
    private val queued = mutableListOf<(MediaController) -> Unit>()
    private var sleepJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            sync(player)
        }
    }

    fun connect() {
        if (future != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val f = MediaController.Builder(context, token).buildAsync()
        future = f
        f.addListener(
            {
                try {
                    val c = f.get()
                    controller = c
                    c.addListener(listener)
                    sync(c)
                    queued.forEach { it(c) }
                } catch (e: Exception) {
                    future = null
                } finally {
                    queued.clear()
                }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) {
            block(c)
        } else {
            queued += block
            connect()
        }
    }

    // ---------- أوامر التشغيل ----------

    fun playSurah(surah: Int, ayahCount: Int, startAyah: Int, surahName: String, reciter: Reciter) {
        val items = (1..ayahCount).map { a ->
            MediaItem.Builder()
                .setMediaId("$surah:$a")
                .setUri(Reciters.audioUrl(reciter, surah, a))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("$surahName • ${ArabicText.number(a)}")
                        .setArtist(reciter.displayName())
                        .setAlbumTitle(surahName)
                        .build(),
                )
                .build()
        }
        withController { c ->
            c.setMediaItems(items, (startAyah - 1).coerceIn(0, items.lastIndex), 0L)
            c.prepare()
            c.play()
        }
    }

    fun togglePlay() = withController { c ->
        if (c.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_ENDED) c.seekToDefaultPosition(0)
            c.play()
        }
    }

    fun next() = withController { it.seekToNextMediaItem() }

    fun previous() = withController { it.seekToPreviousMediaItem() }

    fun seekToAyah(ayah: Int) = withController { it.seekToDefaultPosition((ayah - 1).coerceAtLeast(0)) }

    fun setSpeed(speed: Float) = withController { it.setPlaybackSpeed(speed) }

    fun toggleRepeatAyah() = withController {
        it.repeatMode =
            if (it.repeatMode == Player.REPEAT_MODE_ONE) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE
    }

    fun stop() {
        setSleepTimer(null)
        withController {
            it.stop()
            it.clearMediaItems()
        }
    }

    /** مؤقت النوم: إيقاف التلاوة بعد عدد من الدقائق. null لإلغائه. */
    fun setSleepTimer(minutes: Int?) {
        sleepJob?.cancel()
        sleepJob = null
        _state.update { it.copy(sleepMinutesLeft = minutes) }
        if (minutes == null) return
        sleepJob = scope.launch {
            var left = minutes
            while (left > 0) {
                _state.update { it.copy(sleepMinutesLeft = left) }
                delay(60_000)
                left--
            }
            withController { it.pause() }
            _state.update { it.copy(sleepMinutesLeft = null) }
        }
    }

    // ---------- مزامنة الحالة ----------

    private fun sync(p: Player) {
        val item = p.currentMediaItem
        val ids = item?.mediaId?.split(":")
        _state.update {
            it.copy(
                hasMedia = item != null,
                isPlaying = p.isPlaying,
                isLoading = p.playWhenReady && p.playbackState == Player.STATE_BUFFERING,
                surah = ids?.getOrNull(0)?.toIntOrNull() ?: 0,
                ayah = ids?.getOrNull(1)?.toIntOrNull() ?: 0,
                surahName = item?.mediaMetadata?.albumTitle?.toString().orEmpty(),
                reciterName = item?.mediaMetadata?.artist?.toString().orEmpty(),
                speed = p.playbackParameters.speed,
                repeatAyah = p.repeatMode == Player.REPEAT_MODE_ONE,
            )
        }
    }
}
