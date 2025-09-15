package com.bcu.foodtable.JetpackCompose.Social.Appointment

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import java.util.TimeZone
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object DeviceCalendarSync {
    fun insertEvent(
        context: Context,
        title: String,
        description: String?,
        startMillis: Long,
        endMillis: Long,
        location: String?
    ): Pair<Long, Long>? {
        val calId = 1L // TODO: 사용자 캘린더 선택 UI로 대체 권장
        val v = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, description ?: "")
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.EVENT_LOCATION, location ?: "")
        }
        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, v) ?: return null
        val eventId = uri.lastPathSegment?.toLongOrNull() ?: return null
        return calId to eventId
    }

    fun deleteEvent(context: Context, eventId: Long): Boolean {
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        return context.contentResolver.delete(uri, null, null) > 0
    }
}

suspend fun addToDeviceCalendarAndMap(ctx: Context, apptId: String, ap: Appointment): Boolean {
    val pair = DeviceCalendarSync.insertEvent(
        ctx, title = ap.title, description = ap.note,
        startMillis = ap.startAt, endMillis = ap.endAt, location = ap.placeName
    ) ?: return false

    val me = FirebaseAuth.getInstance().currentUser!!.uid
    val db = FirebaseFirestore.getInstance()
    db.collection("appointments").document(apptId)
        .collection("deviceEvents").document(me)
        .set(DeviceEventMapping(pair.first, pair.second)).await()
    return true
}

suspend fun removeFromDeviceCalendarByMap(ctx: Context, apptId: String): Boolean {
    val me = FirebaseAuth.getInstance().currentUser!!.uid
    val db = FirebaseFirestore.getInstance()
    val doc = db.collection("appointments").document(apptId)
        .collection("deviceEvents").document(me).get().await()
    if (!doc.exists()) return true
    val eventId = doc.getLong("eventId") ?: return false
    val ok = DeviceCalendarSync.deleteEvent(ctx, eventId)
    if (ok) {
        db.collection("appointments").document(apptId)
            .collection("deviceEvents").document(me).delete().await()
    }
    return ok
}