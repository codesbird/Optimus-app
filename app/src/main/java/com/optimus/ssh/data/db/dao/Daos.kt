package com.optimus.ssh.data.db.dao

import androidx.room.*
import com.optimus.ssh.data.db.entity.*
import com.optimus.ssh.model.HostStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface HostDao {
    @Query("SELECT * FROM hosts ORDER BY label ASC")
    fun getAllHosts(): Flow<List<HostEntity>>

    @Query("SELECT * FROM hosts WHERE id = :id")
    suspend fun getHostById(id: String): HostEntity?

    @Query("SELECT * FROM hosts WHERE groupId = :groupId ORDER BY label ASC")
    fun getHostsByGroup(groupId: String): Flow<List<HostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHost(host: HostEntity)

    @Update
    suspend fun updateHost(host: HostEntity)

    @Query("UPDATE hosts SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: HostStatus)

    @Query("UPDATE hosts SET lastConnectedAt = :timestamp WHERE id = :id")
    suspend fun updateLastConnected(id: String, timestamp: Long)

    @Delete
    suspend fun deleteHost(host: HostEntity)

    @Query("DELETE FROM hosts WHERE id = :id")
    suspend fun deleteHostById(id: String)
}

@Dao
interface IdentityDao {
    @Query("SELECT * FROM identities ORDER BY label ASC")
    fun getAllIdentities(): Flow<List<IdentityEntity>>

    @Query("SELECT * FROM identities WHERE id = :id")
    suspend fun getIdentityById(id: String): IdentityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdentity(identity: IdentityEntity)

    @Delete
    suspend fun deleteIdentity(identity: IdentityEntity)
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY name ASC")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity)

    @Delete
    suspend fun deleteGroup(group: GroupEntity)
}

@Dao
interface KeyDao {
    @Query("SELECT * FROM keys ORDER BY label ASC")
    fun getAllKeys(): Flow<List<KeyEntity>>

    @Query("SELECT * FROM keys WHERE id = :id")
    suspend fun getKeyById(id: String): KeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKey(key: KeyEntity)

    @Delete
    suspend fun deleteKey(key: KeyEntity)
}

@Dao
interface PortForwardDao {
    @Query("SELECT * FROM port_forwards WHERE hostId = :hostId")
    fun getRulesForHost(hostId: String): Flow<List<PortForwardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: PortForwardEntity)

    @Delete
    suspend fun deleteRule(rule: PortForwardEntity)
}

@Dao
interface SnippetDao {
    @Query("SELECT * FROM snippets ORDER BY title ASC")
    fun getAllSnippets(): Flow<List<SnippetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnippet(snippet: SnippetEntity)

    @Delete
    suspend fun deleteSnippet(snippet: SnippetEntity)
}
