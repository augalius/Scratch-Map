package com.auggie.scratch_map_app.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * Whether an administrator has suspended the signed-in account (set from the backend).
 * While suspended, progress is not synced and the app shows a suspension screen.
 */
interface AccountStatusRepository {
    /** False while signed out or when the status can't be read. */
    val isDisabled: StateFlow<Boolean>
}
