package com.carlos.controlmedicamentos.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.carlos.controlmedicamentos.data.local.MedicalAppointment

class MedicalAppointmentScheduler(private val context: Context) {

    companion object {
        const val EXTRA_APPOINTMENT_ID = "APPOINTMENT_ID"
        const val EXTRA_APPOINTMENT_TITLE = "APPOINTMENT_TITLE"
        const val EXTRA_APPOINTMENT_DOCTOR = "APPOINTMENT_DOCTOR"
        const val EXTRA_APPOINTMENT_LOCATION = "APPOINTMENT_LOCATION"
        const val EXTRA_APPOINTMENT_TIME = "APPOINTMENT_TIME"
        const val EXTRA_APPOINTMENT_RETRY_ATTEMPT = "APPOINTMENT_RETRY_ATTEMPT"
        const val EXTRA_NOTIFICATION_ID = "NOTIFICATION_ID"
        private const val REQUEST_CODE_BASE = 700_000
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun programar(appointment: MedicalAppointment) {
        cancelar(appointment.id)
        if (!appointment.alarmEnabled || appointment.isCompleted || appointment.scheduledAt <= System.currentTimeMillis()) {
            return
        }

        val reminderAt = appointment.scheduledAt - appointment.reminderMinutes * 60_000L
        val triggerAt = reminderAt.coerceAtLeast(System.currentTimeMillis() + 1_000L)
        val intent = buildAppointmentIntent(appointment, retryAttempt = 0)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            buildRequestCode(appointment.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarm(triggerAt, pendingIntent)
    }

    fun programarReintento(
        appointment: MedicalAppointment,
        retryAttempt: Int,
        delayMinutes: Int,
        notificationId: Int = 0
    ) {
        if (!appointment.alarmEnabled || appointment.isCompleted || appointment.scheduledAt <= System.currentTimeMillis()) {
            return
        }

        val nextTriggerAt = System.currentTimeMillis() + delayMinutes.coerceAtLeast(1) * 60_000L
        if (nextTriggerAt > appointment.scheduledAt) {
            return
        }

        val intent = buildAppointmentIntent(appointment, retryAttempt, notificationId)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            buildRequestCode(appointment.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarm(nextTriggerAt, pendingIntent)
    }

    private fun buildAppointmentIntent(
        appointment: MedicalAppointment,
        retryAttempt: Int,
        notificationId: Int = 0
    ): Intent {
        return Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_APPOINTMENT_ID, appointment.id)
            putExtra(EXTRA_APPOINTMENT_TITLE, appointment.title)
            putExtra(EXTRA_APPOINTMENT_DOCTOR, appointment.doctorName)
            putExtra(EXTRA_APPOINTMENT_LOCATION, appointment.location)
            putExtra(EXTRA_APPOINTMENT_TIME, appointment.scheduledAt)
            putExtra(EXTRA_APPOINTMENT_RETRY_ATTEMPT, retryAttempt.coerceAtLeast(0))
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
    }

    private fun scheduleAlarm(triggerAt: Long, pendingIntent: PendingIntent) {

        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms() -> {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                }

                else -> {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                }
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun cancelar(appointmentId: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            buildRequestCode(appointmentId),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun buildRequestCode(appointmentId: Int): Int = REQUEST_CODE_BASE + appointmentId
}