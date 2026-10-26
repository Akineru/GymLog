package com.example.gymlog

import com.example.gymlog.data.NotificationPostPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 「划掉后不被每秒刷新弹回来」的唯一闸门。
 *
 * 这是 Android 14+ 才允许的路径，API 33 模拟器上无法真正划掉，
 * 所以必须用纯逻辑测试把语义钉住，而不是只靠真机点一下。
 */
class WorkoutNotificationPostPolicyTest {

    @Test
    fun firstPostAlwaysGoesOut() {
        assertTrue(
            NotificationPostPolicy.shouldPost(
                dismissedByUser = false,
                force = false,
                contentKey = "k1",
                lastPostedKey = null,
            ),
        )
    }

    @Test
    fun identicalContentIsSkipped() {
        assertFalse(
            NotificationPostPolicy.shouldPost(
                dismissedByUser = false,
                force = false,
                contentKey = "k1",
                lastPostedKey = "k1",
            ),
        )
    }

    @Test
    fun changedContentIsPosted() {
        // 独立计时器每秒变化的文本就属于这一类
        assertTrue(
            NotificationPostPolicy.shouldPost(
                dismissedByUser = false,
                force = false,
                contentKey = "组歇 · 01:52",
                lastPostedKey = "组歇 · 01:53",
            ),
        )
    }

    @Test
    fun structuralChangeIsForced() {
        assertTrue(
            NotificationPostPolicy.shouldPost(
                dismissedByUser = false,
                force = true,
                contentKey = "k1",
                lastPostedKey = "k1",
            ),
        )
    }

    @Test
    fun dismissedBlocksTextRefresh() {
        // 关键用例：用户划掉后，计时器每秒的新文本不得把通知弹回来
        assertFalse(
            NotificationPostPolicy.shouldPost(
                dismissedByUser = true,
                force = false,
                contentKey = "组歇 · 01:51",
                lastPostedKey = "组歇 · 01:52",
            ),
        )
    }

    @Test
    fun dismissedBlocksEvenForcedPost() {
        // 恢复显示只能靠 publish() 先清标记，闸门本身不放行
        assertFalse(
            NotificationPostPolicy.shouldPost(
                dismissedByUser = true,
                force = true,
                contentKey = "k2",
                lastPostedKey = "k1",
            ),
        )
    }
}
