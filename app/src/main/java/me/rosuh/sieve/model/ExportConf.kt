package me.rosuh.sieve.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 导出配置
 * @param id 主键
 * @param name 配置名称
 * @param listSize 配置长度
 * @param createTimeMill 创建时间
 */
@Entity
data class ExportConf(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "list_size") val listSize: Int,
    @ColumnInfo(name = "create_time") val createTimeMill: Long,
)