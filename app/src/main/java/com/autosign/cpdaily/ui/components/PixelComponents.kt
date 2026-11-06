package com.autosign.cpdaily.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.autosign.cpdaily.ui.icons.PixelIcons
import com.autosign.cpdaily.ui.theme.pixelCardBackgroundColor

/**
 * 分组小标题 (SectionHeader)
 * 位于分组上方，labelLarge 字号，colorScheme.primary 颜色，与卡片内文字左对齐 (16.dp)
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp)
    )
}

/**
 * Pixel 分段卡片列表作用域
 */
class SegmentedGroupScope {
    internal val items = mutableListOf<@Composable (index: Int, count: Int) -> Unit>()

    fun item(content: @Composable (index: Int, count: Int) -> Unit) {
        items.add(content)
    }
}

/**
 * Pixel 分段卡片列表 (SegmentedGroup)
 * - 同一分组内的每个条目都是独立的 Surface，条目之间垂直间距固定为 2.dp
 * - 严禁使用 HorizontalDivider 分割线
 */
@Composable
fun SegmentedGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: SegmentedGroupScope.() -> Unit
) {
    val scope = SegmentedGroupScope().apply(content)
    if (scope.items.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        if (!title.isNullOrBlank()) {
            SectionHeader(title = title)
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val count = scope.items.size
            scope.items.forEachIndexed { index, itemContent ->
                itemContent(index, count)
            }
        }
    }
}

/**
 * 位置感知圆角 + Expressive 按压弹性形变独立卡片容器
 * - 组内仅 1 项：四角均为 20.dp
 * - 组内首项 (index == 0)：顶部两角 20.dp，底部两角 4.dp
 * - 组内中间项：四角均为 4.dp
 * - 组内末项 (index == count - 1)：顶部两角 4.dp，底部两角 20.dp
 * - 按压形变交互：按住任意条目时，四角通过 MotionScheme.expressive().fastSpatialSpec() 实时展开为 20.dp 大圆角
 * - 按下态底色：colorScheme.surfaceContainerHigh；常态底色：surfaceContainerLowest (浅色) / surfaceContainerLow (深色)
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SegmentedItemSurface(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val baseTop = if (count <= 1 || index == 0) 20.dp else 4.dp
    val baseBottom = if (count <= 1 || index == count - 1) 20.dp else 4.dp

    val targetTop = if (isPressed && enabled) 20.dp else baseTop
    val targetBottom = if (isPressed && enabled) 20.dp else baseBottom

    val spatialSpec = MotionScheme.expressive().fastSpatialSpec< androidx.compose.ui.unit.Dp>()
    val topCorner by animateDpAsState(
        targetValue = targetTop,
        animationSpec = spatialSpec,
        label = "segmentedTopCorner"
    )
    val bottomCorner by animateDpAsState(
        targetValue = targetBottom,
        animationSpec = spatialSpec,
        label = "segmentedBottomCorner"
    )

    val normalBg = pixelCardBackgroundColor()
    val pressedBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed && enabled) pressedBg else normalBg,
        label = "segmentedBgColor"
    )

    val itemShape = RoundedCornerShape(
        topStart = topCorner,
        topEnd = topCorner,
        bottomStart = bottomCorner,
        bottomEnd = bottomCorner
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(itemShape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        enabled = enabled,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        shape = itemShape,
        color = backgroundColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(modifier = Modifier.alpha(if (enabled) 1f else 0.48f)) {
            content()
        }
    }
}

/**
 * 左侧图标底托 (IconBadge)
 * 统一使用 40.dp 圆形底托 (CircleShape)，背景色为 colorScheme.secondaryContainer.copy(alpha = 0.65f)，
 * 内部图标 20.dp，颜色为 colorScheme.onSecondaryContainer
 */
@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * 基础列表项 (ListItem)
 * 双行最小高度 72.dp，单行最小高度 56.dp，水平内边距 16.dp，垂直内边距 12.dp
 */
@Composable
fun ListItem(
    index: Int,
    count: Int,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val minHeight = if (!subtitle.isNullOrBlank()) 72.dp else 56.dp
    SegmentedItemSurface(
        index = index,
        count = count,
        modifier = modifier,
        enabled = enabled,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                IconBadge(icon = icon)
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(12.dp))
                trailingContent()
            }
        }
    }
}

/**
 * Switch 开关项 (SwitchItem)
 * 整行可点击并触发触觉反馈 (HapticFeedbackType.ToggleOn/Off)，
 * 开启状态下 Switch 滑块内部必须显示 ✓ 图标 (thumbContent)
 */
@Composable
fun SwitchItem(
    index: Int,
    count: Int,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    ListItem(
        index = index,
        count = count,
        title = title,
        subtitle = subtitle,
        icon = icon,
        enabled = enabled,
        modifier = modifier,
        onClick = {
            val nextState = !checked
            haptic.performHapticFeedback(
                if (nextState) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
            )
            onCheckedChange(nextState)
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                thumbContent = if (checked) {
                    {
                        Icon(
                            imageVector = PixelIcons.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                } else {
                    null
                }
            )
        }
    )
}

/**
 * 导航跳转/动作行 (NavItem)
 */
@Composable
fun NavItem(
    index: Int,
    count: Int,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    valueText: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    showChevron: Boolean = true,
    enabled: Boolean = true
) {
    ListItem(
        index = index,
        count = count,
        title = title,
        subtitle = subtitle,
        icon = icon,
        enabled = enabled,
        modifier = modifier,
        onClick = onClick,
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!valueText.isNullOrBlank()) {
                    Text(
                        text = valueText,
                        style = MaterialTheme.typography.labelLarge,
                        color = valueColor
                    )
                    if (showChevron) {
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
                if (showChevron) {
                    Icon(
                        imageVector = PixelIcons.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    )
}

/**
 * 单选弹窗选项数据
 */
data class ChoiceOption<T>(
    val value: T,
    val label: String,
    val description: String? = null
)

/**
 * 多选/单选弹窗项 (ChoiceItem)
 * 点击后弹出 28.dp 超大圆角 (shapes.extraLarge) 的 Material 3 AlertDialog，
 * 内部为带 RadioButton 的圆角可点击行，严禁依赖第三方嵌套 Overlay 宿主。
 */
@Composable
fun <T> ChoiceItem(
    index: Int,
    count: Int,
    title: String,
    options: List<ChoiceOption<T>>,
    selectedValue: T,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    dialogTitle: String = title,
    cancelText: String = "取消",
    enabled: Boolean = true
) {
    var showDialog by remember { mutableStateOf(false) }
    val selectedOption = options.find { it.value == selectedValue }

    NavItem(
        index = index,
        count = count,
        title = title,
        subtitle = subtitle ?: selectedOption?.label,
        icon = icon,
        valueText = if (subtitle != null) selectedOption?.label else null,
        valueColor = MaterialTheme.colorScheme.primary,
        enabled = enabled,
        modifier = modifier,
        onClick = { showDialog = true }
    )

    if (showDialog) {
        PixelSingleChoiceDialog(
            title = dialogTitle,
            options = options,
            selectedValue = selectedValue,
            cancelText = cancelText,
            onOptionSelected = { value ->
                onOptionSelected(value)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

/**
 * 原生 Material 3 28.dp 超大圆角单选弹窗，内部为带 RadioButton 的圆角可点击行
 */
@Composable
fun <T> PixelSingleChoiceDialog(
    title: String,
    options: List<ChoiceOption<T>>,
    selectedValue: T,
    onOptionSelected: (T) -> Unit,
    onDismiss: () -> Unit,
    cancelable: Boolean = true,
    cancelText: String? = "取消"
) {
    val haptic = LocalHapticFeedback.current
    AlertDialog(
        onDismissRequest = { if (cancelable) onDismiss() },
        shape = MaterialTheme.shapes.extraLarge, // 28.dp
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                options.forEach { option ->
                    val isSelected = option.value == selectedValue
                    val rowBg = if (isSelected) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                    } else {
                        Color.Transparent
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                                onOptionSelected(option.value)
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = rowBg,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (!option.description.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = option.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (cancelable && !cancelText.isNullOrBlank()) {
                TextButton(onClick = onDismiss) {
                    Text(text = cancelText)
                }
            }
        }
    )
}

/**
 * Hero 状态卡枚举
 */
enum class HeroCardState {
    ACTIVE,     // 激活态：primaryContainer
    INACTIVE,   // 未激活态：secondaryContainer
    ERROR       // 异常/到期/安全模式：errorContainer
}

/**
 * 主页顶部 Hero 状态卡 (HeroStatusCard)
 * - 采用 28.dp 超大圆角 (shapes.extraLarge)
 * - 激活态背景色为 colorScheme.primaryContainer，未激活态为 secondaryContainer，异常/安全模式为 errorContainer
 * - 卡片左侧使用 60.dp 的 MaterialShapes.Cookie9Sided.toShape() 九边形徽章承载状态大图标（底色 primary，图标色 onPrimary）
 * - 卡片底部状态标签使用紧凑型胶囊 Surface (RoundedCornerShape(50)，底色 surfaceBright.copy(alpha = 0.55f)，
 *   字号 labelSmall，内边距 horizontal = 10.dp, vertical = 5.dp)，确保核心状态信息在一行内完整并排显示
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HeroStatusCard(
    state: HeroCardState,
    title: String,
    subtitle: String,
    icon: ImageVector,
    badges: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        targetValue = when (state) {
            HeroCardState.ACTIVE -> scheme.primaryContainer
            HeroCardState.INACTIVE -> scheme.secondaryContainer
            HeroCardState.ERROR -> scheme.errorContainer
        },
        label = "heroContainerColor"
    )
    val contentColor = when (state) {
        HeroCardState.ACTIVE -> scheme.onPrimaryContainer
        HeroCardState.INACTIVE -> scheme.onSecondaryContainer
        HeroCardState.ERROR -> scheme.onErrorContainer
    }
    val cookieBgColor = if (state == HeroCardState.ERROR) scheme.error else scheme.primary
    val cookieIconColor = if (state == HeroCardState.ERROR) scheme.onError else scheme.onPrimary

    val cookieShape = MaterialShapes.Cookie9Sided.toShape()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge, // 28.dp
        color = containerColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 60.dp MaterialShapes.Cookie9Sided 九边形徽章
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(cookieShape)
                        .background(cookieBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = cookieIconColor,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (badges.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                // 底部紧凑型胶囊状态标签在一行内完整并排显示
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    badges.forEach { badgeText ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = scheme.surfaceBright.copy(alpha = 0.55f),
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
