package com.example.campsitelottery

/**
 * Where the current device's profile is in its lifecycle. Held at the app level by
 * [CampsiteLotteryApp] and passed to the screens that need the current user.
 */
sealed interface UserState {

    /** The profile is still being fetched or created. */
    data object Loading : UserState

    /** The profile is loaded. */
    data class Ready(val user: User) : UserState

    /** The profile could not be loaded; [message] says why. */
    data class Failed(val message: String) : UserState
}
