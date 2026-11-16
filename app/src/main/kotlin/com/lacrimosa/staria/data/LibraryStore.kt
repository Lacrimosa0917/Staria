package com.lacrimosa.staria.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * 内容库（日记 / 纸条 / 随笔 / 星尘）的本地持久化。
 *
 * 昵称与设置各自已有 SharedPreferences 存储；这里补上「内容数据」这一块，
 * 否则日记、摘录、随笔与回收站在进程被杀后都会回到内存初始值。
 *
 * 用 SharedPreferences + 内置 org.json 序列化，避免为这一处引入 Gson/Moshi/Room。
 * 数据量级（个人笔记）很小，整库读写足够。
 */
class LibraryStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 从磁盘读取整库。首次运行（无存档）返回 null，由调用方用示例数据初始化。 */
    fun load(): LibrarySnapshot? {
        val diaries = prefs.getString(KEY_DIARIES, null) ?: return null
        val papers = prefs.getString(KEY_PAPERS, null) ?: return null
        val essays = prefs.getString(KEY_ESSAYS, null) ?: return null
        val trash = prefs.getString(KEY_TRASH, null) ?: return null

        return LibrarySnapshot(
            diaries = parseDiaries(diaries),
            papers = parsePapers(papers),
            essays = parseEssays(essays),
            trash = parseTrash(trash),
        )
    }

    fun save(snapshot: LibrarySnapshot) {
        // 用 commit 而不是 apply：apply 是异步写盘，进程在写入前被杀死
        // 会丢数据（「偶尔删除不进星尘」的来源）。commit 同步落盘，
        // 数据量小（个人笔记），主线程开销可忽略。
        prefs.edit()
            .putString(KEY_DIARIES, serializeDiaries(snapshot.diaries))
            .putString(KEY_PAPERS, serializePapers(snapshot.papers))
            .putString(KEY_ESSAYS, serializeEssays(snapshot.essays))
            .putString(KEY_TRASH, serializeTrash(snapshot.trash))
            .commit()
    }

    // ---------- 序列化 ----------

    private fun serializeDiaries(list: List<DiaryEntry>): String {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id)
                put("date", it.date.toString())
                put("title", it.title)
                put("body", it.body)
                put("deleted", it.deleted)
            })
        }
        return arr.toString()
    }

    private fun parseDiaries(json: String): List<DiaryEntry> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            DiaryEntry(
                id = o.getLong("id"),
                date = LocalDate.parse(o.getString("date")),
                title = o.getString("title"),
                body = o.optString("body", ""),
                deleted = o.optBoolean("deleted", false),
            )
        }
    }

    private fun serializePapers(list: List<PaperNote>): String {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id)
                put("text", it.text)
                put("source", it.source ?: JSONObject.NULL)
                put("editedOn", it.editedOn.toString())
            })
        }
        return arr.toString()
    }

    private fun parsePapers(json: String): List<PaperNote> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            PaperNote(
                id = o.getLong("id"),
                text = o.getString("text"),
                source = if (o.isNull("source")) null else o.getString("source"),
                editedOn = LocalDate.parse(o.getString("editedOn")),
            )
        }
    }

    private fun serializeEssays(list: List<Essay>): String {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id)
                put("date", it.date.toString())
                put("title", it.title)
                put("body", it.body)
            })
        }
        return arr.toString()
    }

    private fun parseEssays(json: String): List<Essay> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Essay(
                id = o.getLong("id"),
                date = LocalDate.parse(o.getString("date")),
                title = o.getString("title"),
                body = o.getString("body"),
            )
        }
    }

    private fun serializeTrash(list: List<TrashItem>): String {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("trashId", it.trashId)
                put("kind", it.kind.name)
                put("originalId", it.originalId)
                put("date", it.date.toString())
                put("title", it.title)
                put("body", it.body)
                put("source", it.source ?: JSONObject.NULL)
                put("deletedAt", it.deletedAt)
            })
        }
        return arr.toString()
    }

    private fun parseTrash(json: String): List<TrashItem> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            TrashItem(
                trashId = o.getLong("trashId"),
                kind = TrashKind.valueOf(o.getString("kind")),
                originalId = o.getLong("originalId"),
                date = LocalDate.parse(o.getString("date")),
                title = o.optString("title", ""),
                body = o.optString("body", ""),
                source = if (o.isNull("source")) null else o.getString("source"),
                deletedAt = o.getLong("deletedAt"),
            )
        }
    }

    private companion object {
        const val PREFS_NAME = "staria_library"
        const val KEY_DIARIES = "diaries"
        const val KEY_PAPERS = "papers"
        const val KEY_ESSAYS = "essays"
        const val KEY_TRASH = "trash"
    }
}

/** 整库快照，供 [LibraryStore] 读写。 */
data class LibrarySnapshot(
    val diaries: List<DiaryEntry>,
    val papers: List<PaperNote>,
    val essays: List<Essay>,
    val trash: List<TrashItem>,
)
