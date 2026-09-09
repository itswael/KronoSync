package com.kronosync.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DaoTests {
    private lateinit var db: KronoDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, KronoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun template_crud_and_unique_name() = runTest {
        val dao = db.templateDao()
        val id = dao.insert(Template(name = "Daily"))
        val id2 = dao.insert(Template(name = "Workday"))
        assertTrue(id > 0 && id2 > 0)
        val list = dao.observeAll().first()
        assertEquals(2, list.size)
    }

    @Test
    fun schedule_block_day_filtering() = runTest {
        val dao = db.scheduleBlockDao()
        val day = 1725849600000L // 2024-09-09 UTC midnight example
        val otherDay = day + 86_400_000
        dao.insert(ScheduleBlock(dayEpoch = day, startMinute = 480, durationMinutes = 60, title = "Work"))
        dao.insert(ScheduleBlock(dayEpoch = otherDay, startMinute = 480, durationMinutes = 60, title = "Work2"))
        val list = dao.observeForDay(day).first()
        assertEquals(1, list.size)
        assertEquals("Work", list.first().title)
    }
}
