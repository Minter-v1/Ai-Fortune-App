package com.minter.ai_fortune_app.data.model

import com.minter.ai_fortune_app.utils.DateUtils
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String, // 매개변수로 받아야 함
    val message: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val conversationCount: Int = 0,
    val maxConversations: Int = 3,
    val createdDate: String = DateUtils.getCurrentDate(), // 오타 수정
    val isCompleted: Boolean = false,
    val completedAt: Long? = null
) {
    // 남은 대화 횟수
    val remainingChat: Int
        get() = maxOf(0, maxConversations - conversationCount)

    // 대화 가능 여부 3번 제한
    val canChat: Boolean
        get() = !isCompleted && isToday && conversationCount < maxConversations

    // 오늘 세션인지
    val isToday: Boolean
        get() = DateUtils.isToday(createdDate)

    // 다음 대화 진행 (카운트 증가)
    fun nextConversation(): ChatSession {
        val newCount = conversationCount + 1
        return copy(
            conversationCount = newCount,
            isCompleted = newCount >= maxConversations,
            completedAt = if (newCount >= maxConversations) System.currentTimeMillis() else completedAt
        )
    }

    companion object {
        fun createToday(): ChatSession {
            return ChatSession(createdDate = DateUtils.getCurrentDate())
        }
    }
}