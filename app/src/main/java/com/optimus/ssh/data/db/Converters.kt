package com.optimus.ssh.data.db

import androidx.room.TypeConverter
import com.optimus.ssh.model.AuthType
import com.optimus.ssh.model.HostStatus
import com.optimus.ssh.model.KeyAlgorithm
import com.optimus.ssh.model.TunnelType

class Converters {
    @TypeConverter
    fun fromAuthType(value: AuthType): String = value.name

    @TypeConverter
    fun toAuthType(value: String): AuthType = AuthType.valueOf(value)

    @TypeConverter
    fun fromHostStatus(value: HostStatus): String = value.name

    @TypeConverter
    fun toHostStatus(value: String): HostStatus = HostStatus.valueOf(value)

    @TypeConverter
    fun fromTunnelType(value: TunnelType): String = value.name

    @TypeConverter
    fun toTunnelType(value: String): TunnelType = TunnelType.valueOf(value)

    @TypeConverter
    fun fromKeyAlgorithm(value: KeyAlgorithm): String = value.name

    @TypeConverter
    fun toKeyAlgorithm(value: String): KeyAlgorithm = KeyAlgorithm.valueOf(value)
}
