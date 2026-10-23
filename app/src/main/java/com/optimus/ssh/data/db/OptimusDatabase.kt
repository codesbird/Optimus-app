package com.optimus.ssh.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.optimus.ssh.data.db.dao.*
import com.optimus.ssh.data.db.entity.*
import com.optimus.ssh.security.KeystoreManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        HostEntity::class,
        IdentityEntity::class,
        GroupEntity::class,
        KeyEntity::class,
        PortForwardEntity::class,
        SnippetEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class OptimusDatabase : RoomDatabase() {

    abstract fun hostDao(): HostDao
    abstract fun identityDao(): IdentityDao
    abstract fun groupDao(): GroupDao
    abstract fun keyDao(): KeyDao
    abstract fun portForwardDao(): PortForwardDao
    abstract fun snippetDao(): SnippetDao

    companion object {
        @Volatile
        private var INSTANCE: OptimusDatabase? = null

        fun getInstance(context: Context, keystoreManager: KeystoreManager): OptimusDatabase {
            return INSTANCE ?: synchronized(this) {
                val passphrase = keystoreManager.getDatabasePassphrase()
                val factory = SupportOpenHelperFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OptimusDatabase::class.java,
                    "optimus_encrypted.db"
                )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
