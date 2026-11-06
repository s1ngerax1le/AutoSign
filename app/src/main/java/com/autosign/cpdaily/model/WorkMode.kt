package com.autosign.cpdaily.model

import android.content.Context
import com.autosign.cpdaily.R

enum class WorkMode(
    val id: Int,
    val title: String,
    val desc: String,
    val titleResId: Int,
    val descResId: Int,
    val category: String,
    val subName: String
) {
    ROOT_NORMAL(0, "Root - 普通模式", "使用 Root 权限硬件级唤醒与指令解锁，走多重防错和多步校验打卡流程，稳扎稳打。", R.string.mode_root_normal_title, R.string.mode_root_normal_desc, "Root", "普通模式"),
    ACCESSIBILITY_NORMAL(1, "无障碍 - 普通模式", "无需 Root 权限，通过 Android 原生辅助服务模拟人工点击和息屏自适应唤醒。适合绝大多数普通用户。", R.string.mode_a11y_normal_title, R.string.mode_a11y_normal_desc, "无障碍", "普通模式"),
    ROOT_SPRINT(2, "Root - 极速模式", "专为争夺排行榜第 1 名打造！提前 20 秒开屏预热、GPS 保鲜、06:30 准点秒开、倒计时归零毫秒级提交。若遇阻自动降级普通模式自愈。", R.string.mode_root_sprint_title, R.string.mode_root_sprint_desc, "Root", "极速模式"),
    ACCESSIBILITY_SKZ(3, "无障碍 - SKZ 模式", "通过 Shizuku ADB Shell 权限执行辅助打卡，兼顾免 Root 体验与 ADB 级调度稳定性。", R.string.mode_a11y_skz_title, R.string.mode_a11y_skz_desc, "无障碍", "SKZ 模式");

    fun getTitle(context: Context): String = context.getString(titleResId)
    fun getDesc(context: Context): String = context.getString(descResId)

    val isRoot: Boolean get() = this == ROOT_NORMAL || this == ROOT_SPRINT
    val isSprint: Boolean get() = this == ROOT_SPRINT
    val isSkz: Boolean get() = this == ACCESSIBILITY_SKZ

    companion object {
        // 兼容旧代码别名
        val ROOT = ROOT_NORMAL
        val NO_PWD_ACCESSIBILITY = ACCESSIBILITY_NORMAL
        val SPRINT = ROOT_SPRINT

        fun fromId(id: Int): WorkMode {
            return values().find { it.id == id } ?: ACCESSIBILITY_NORMAL
        }
    }
}
