package id.teratai.dompet.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeFmt {
    private val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("in", "ID"))

    fun formatCreatedAt(ms: Long): String = sdf.format(Date(ms))
}
