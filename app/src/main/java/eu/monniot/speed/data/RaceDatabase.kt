package eu.monniot.speed.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DataPoint::class, Session::class], version = 3, exportSchema = false)
abstract class RaceDatabase : RoomDatabase() {
    abstract fun dataPointDao(): DataPointDao

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
