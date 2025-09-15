package com.bcu.foodtable.JetpackCompose.Social.Appointment

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Reminders
import androidx.core.content.ContextCompat
import android.Manifest
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.TimeZone

private const val TAG = "DeviceCalendarSync"
private val tzSeoul: TimeZone = TimeZone.getTimeZone("Asia/Seoul")

object DeviceCalendarSync {

    private fun findWritableCalendarId(context: Context): Long? {
        val cr = context.contentResolver
        val projection = arrayOf(
            Calendars._ID,
            Calendars.CALENDAR_DISPLAY_NAME,
            Calendars.CALENDAR_ACCESS_LEVEL,
            Calendars.IS_PRIMARY,
            Calendars.VISIBLE,
            Calendars.SYNC_EVENTS,
            Calendars.ACCOUNT_NAME,
            Calendars.ACCOUNT_TYPE
        )
        val selection = "${Calendars.VISIBLE}=1" //  SYNC_EVENTS 조건 제거
        return try {
            cr.query(Calendars.CONTENT_URI, projection, selection, null, null)?.use { c ->
                var primary: Long? = null
                var firstWritable: Long? = null
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    val name = c.getString(1)
                    val access = c.getInt(2)
                    val isPrimary = runCatching { c.getInt(3) == 1 }.getOrDefault(false)
                    val visible = c.getInt(4)
                    val sync = c.getInt(5)
                    val accName = c.getString(6)
                    val accType = c.getString(7)

                    val writable = access >= Calendars.CAL_ACCESS_CONTRIBUTOR
                    Log.d(TAG, "Cal[id=$id, name=$name, access=$access, primary=$isPrimary, visible=$visible, sync=$sync, acc=$accName/$accType, writable=$writable]")

                    if (writable) {
                        if (firstWritable == null) firstWritable = id
                        if (isPrimary) primary = id
                    }
                }
                primary ?: firstWritable
            }
        } catch (t: Throwable) {
            Log.e(TAG, "findWritableCalendarId failed", t)
            null
        }
    }

    private fun hasCalendarPermissions(context: Context): Boolean {
        val w = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
        val r = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        return w && r
    }

    fun insertEvent(
        context: Context,
        title: String,
        description: String?,
        startMillis: Long,
        endMillis: Long,
        location: String?,
        reminderMinutes: Int = 10,
        timeZoneId: String = tzSeoul.id
    ): Pair<Long, Long>? {
        if (!hasCalendarPermissions(context)) {
            Log.w(TAG, "insertEvent: no calendar permissions")
            return null
        }
        val calendarId = findWritableCalendarId(context) ?: run {
            Log.w(TAG, "insertEvent: no writable calendar found")
            return null
        }
        return try {
            val v = ContentValues().apply {
                put(Events.CALENDAR_ID, calendarId)
                put(Events.TITLE, title)
                put(Events.DESCRIPTION, description ?: "")
                put(Events.DTSTART, startMillis)
                put(Events.DTEND, endMillis)
                put(Events.EVENT_TIMEZONE, timeZoneId)
                put(Events.EVENT_LOCATION, location ?: "")
                put(Events.AVAILABILITY, Events.AVAILABILITY_BUSY)
            }
            val eventUri: Uri = context.contentResolver.insert(Events.CONTENT_URI, v) ?: return null
            val eventId = ContentUris.parseId(eventUri)

            // 기본 알림 10분 전
            runCatching {
                val r = ContentValues().apply {
                    put(Reminders.EVENT_ID, eventId)
                    put(Reminders.METHOD, Reminders.METHOD_ALERT)
                    put(Reminders.MINUTES, reminderMinutes)
                }
                context.contentResolver.insert(Reminders.CONTENT_URI, r)
            }

            calendarId to eventId
        } catch (t: Throwable) {
            Log.e(TAG, "insertEvent failed", t)
            null
        }
    }

    fun deleteEvent(context: Context, eventId: Long): Boolean {
        if (!hasCalendarPermissions(context)) {
            Log.w(TAG, "deleteEvent: no calendar permissions")
            return false
        }
        return try {
            val uri = ContentUris.withAppendedId(Events.CONTENT_URI, eventId)
            context.contentResolver.delete(uri, null, null) > 0
        } catch (t: Throwable) {
            Log.e(TAG, "deleteEvent failed", t)
            false
        }
    }
}

/** 이벤트 추가 + Firestore 매핑 저장(데이터클래스 대신 Map 사용) */
suspend fun addToDeviceCalendarAndMap(ctx: Context, apptId: String, ap: Appointment): Boolean {
    val pair = DeviceCalendarSync.insertEvent(
        context = ctx,
        title = ap.title,
        description = ap.note,
        startMillis = ap.startAt,
        endMillis = ap.endAt,
        location = ap.placeName
    ) ?: return false

    return try {
        val me = FirebaseAuth.getInstance().currentUser!!.uid
        FirebaseFirestore.getInstance()
            .collection("appointments").document(apptId)
            .collection("deviceEvents").document(me)
            .set(mapOf("calendarId" to pair.first, "eventId" to pair.second))
            .await()
        true
    } catch (t: Throwable) {
        Log.e(TAG, "Firestore mapping save failed", t)
        false
    }
}

/** Firestore 매핑 읽어서 디바이스 이벤트 삭제 + 매핑 제거 */
suspend fun removeFromDeviceCalendarByMap(ctx: Context, apptId: String): Boolean {
    val me = FirebaseAuth.getInstance().currentUser!!.uid
    val db = FirebaseFirestore.getInstance()
    return try {
        val doc = db.collection("appointments").document(apptId)
            .collection("deviceEvents").document(me)
            .get().await()
        if (!doc.exists()) return true
        val eventId = doc.getLong("eventId") ?: return false
        val ok = DeviceCalendarSync.deleteEvent(ctx, eventId)
        if (ok) {
            db.collection("appointments").document(apptId)
                .collection("deviceEvents").document(me)
                .delete().await()
        }
        ok
    } catch (t: Throwable) {
        Log.e(TAG, "removeFromDeviceCalendarByMap failed", t)
        false
    }
}
