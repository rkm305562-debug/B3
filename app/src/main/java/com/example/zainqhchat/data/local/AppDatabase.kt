package com.example.zainqhchat.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.zainqhchat.data.local.dao.ChatMessageDao
import com.example.zainqhchat.data.local.dao.SocialDao
import com.example.zainqhchat.data.local.dao.UserDao
import com.example.zainqhchat.data.local.entities.BlockedUserEntity
import com.example.zainqhchat.data.local.entities.ChatMessageEntity
import com.example.zainqhchat.data.local.entities.FollowEntity
import com.example.zainqhchat.data.local.entities.UserEntity

@Database(
    entities = [
        UserEntity::class,
        ChatMessageEntity::class,
        FollowEntity::class,
        BlockedUserEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun socialDao(): SocialDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zainqh_chat_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
