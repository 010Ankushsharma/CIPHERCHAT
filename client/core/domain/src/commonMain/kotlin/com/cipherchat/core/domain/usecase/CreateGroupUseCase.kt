package com.cipherchat.core.domain.usecase

import com.cipherchat.core.domain.model.Chat
import com.cipherchat.core.domain.model.UserId
import com.cipherchat.core.domain.repository.ChatRepository

/**
 * Creates a new group chat. Encapsulates validation that's about
 * *product rules*, not persistence — [ChatRepository.createGroup]
 * itself stays a thin, rule-free contract so it can be tested/mocked
 * without re-encoding these constraints, and so the rules live in
 * exactly one place if they ever change (e.g. raising the member cap
 * for a future "Communities" tier).
 *
 * Rules enforced here:
 *  - Title must be non-blank after trimming.
 *  - At least 1 other member is required (a "group" of just yourself
 *    doesn't make sense as a [com.cipherchat.core.domain.model.ChatKind.Group] —
 *    it should be created as nothing, or the caller should use
 *    [com.cipherchat.core.domain.model.ChatKind.OneToOne] semantics instead).
 *  - Member list is de-duplicated before creation, since the UI layer
 *    (multi-select contact picker) could otherwise pass the same user
 *    twice if selection state has a bug — defending here means a UI
 *    bug becomes a no-op instead of corrupt member-role data.
 *  - Caps membership at [MAX_GROUP_MEMBERS] to match the documented
 *    product limit; exceeding it is reported back, not silently truncated.
 */
class CreateGroupUseCase(
    private val chatRepository: ChatRepository,
) {
    suspend operator fun invoke(title: String, memberIds: List<UserId>): CreateGroupResult {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            return CreateGroupResult.Rejected(CreateGroupRejection.BlankTitle)
        }

        val distinctMembers = memberIds.distinct()
        if (distinctMembers.isEmpty()) {
            return CreateGroupResult.Rejected(CreateGroupRejection.NoMembers)
        }
        if (distinctMembers.size > MAX_GROUP_MEMBERS) {
            return CreateGroupResult.Rejected(CreateGroupRejection.TooManyMembers(MAX_GROUP_MEMBERS))
        }

        val chat = chatRepository.createGroup(trimmedTitle, distinctMembers)
        return CreateGroupResult.Created(chat)
    }

    companion object {
        const val MAX_GROUP_MEMBERS = 1000
    }
}

sealed class CreateGroupResult {
    data class Created(val chat: Chat) : CreateGroupResult()
    data class Rejected(val reason: CreateGroupRejection) : CreateGroupResult()
}

sealed class CreateGroupRejection {
    data object BlankTitle : CreateGroupRejection()
    data object NoMembers : CreateGroupRejection()
    data class TooManyMembers(val max: Int) : CreateGroupRejection()
}
