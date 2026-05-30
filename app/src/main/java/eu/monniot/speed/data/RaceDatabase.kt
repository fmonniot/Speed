package eu.monniot.speed.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// v4: E1 adds DataPoint.leanAngleDeg / lateralGz; E4 adds Segment + SegmentAttempt tables.
// Destructive fallback is enabled, so the bump simply recreates the schema.
@Database(
    entities = [DataPoint::class, Session::class, Segment::class, SegmentAttempt::class],
    version = 4,
    exportSchema = false,
)
abstract class RaceDatabase : RoomDatabase() {
    abstract fun dataPointDao(): DataPointDao
    abstract fun segmentDao(): SegmentDao

    companion object {
        @Volatile
        private var INSTANCE: RaceDatabase? = null

        fun getDatabase(context: Context): RaceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RaceDatabase::class.java,
                    "race_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
