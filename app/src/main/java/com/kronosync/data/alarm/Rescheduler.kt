package com.kronosync.data.alarm

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Rescheduler @Inject constructor() {
    fun onBootCompleted() {
        // TODO: Load next upcoming block from DB and schedule exact alarm.
    }
}
