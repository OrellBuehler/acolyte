package ch.orellbuehler.acolyte.ui.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.api.model.AccountFollow
import ch.orellbuehler.acolyte.api.model.AccountSession
import ch.orellbuehler.acolyte.api.model.AccountSsoLink
import ch.orellbuehler.acolyte.feature.account.AccountDevicesPresenter
import ch.orellbuehler.acolyte.feature.account.AccountProfilePresenter
import ch.orellbuehler.acolyte.feature.account.AccountSecurityPresenter
import ch.orellbuehler.acolyte.feature.account.AiConnectPresenter
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AccountProfileViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    val presenter: AccountProfilePresenter = sdk.accountProfile()
    val state = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() = viewModelScope.launch { presenter.refresh() }

    fun saveName(name: String) = viewModelScope.launch { presenter.saveName(name) }

    fun saveLanguage(code: String) = viewModelScope.launch { presenter.saveLanguage(code) }

    fun upload(bytes: ByteArray) = viewModelScope.launch { presenter.uploadPicture(bytes, "image/jpeg") }

    fun removePicture() = viewModelScope.launch { presenter.removePicture() }

    fun saveAlerts(
        email: String,
        discord: String,
        newEpisodes: Boolean,
    ) = viewModelScope.launch { presenter.saveAlerts(email, discord, newEpisodes) }

    fun unfollow(follow: AccountFollow) = viewModelScope.launch { presenter.unfollow(follow) }

    fun consume() = presenter.consume()
}

class AccountSecurityViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    val presenter: AccountSecurityPresenter = sdk.accountSecurity()
    val state = presenter.state
    private var plexJob: Job? = null

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() = viewModelScope.launch { presenter.refresh() }

    fun changePassword(
        current: String,
        new: String,
        confirm: String,
    ) = viewModelScope.launch { presenter.changePassword(current, new, confirm) }

    fun startTwoFactor() = viewModelScope.launch { presenter.startTwoFactor() }

    fun cancelTwoFactor() = presenter.cancelTwoFactor()

    fun enableTwoFactor(code: String) = viewModelScope.launch { presenter.enableTwoFactor(code) }

    fun disableTwoFactor(code: String) = viewModelScope.launch { presenter.disableTwoFactor(code) }

    fun renewCodes(code: String) = viewModelScope.launch { presenter.renewBackupCodes(code) }

    fun dismissCodes() = presenter.dismissBackupCodes()

    fun linkPlex() {
        if (plexJob?.isActive == true) return
        plexJob = viewModelScope.launch { presenter.linkPlex() }
    }

    fun cancelPlex() {
        plexJob?.cancel()
        plexJob = null
    }

    fun unlinkPlex() = viewModelScope.launch { presenter.unlinkPlex() }

    fun linkMedia(
        serverId: Long,
        name: String,
        password: String,
    ) = viewModelScope.launch { presenter.linkMedia(serverId, name, password) }

    fun unlinkMedia(kind: String) = viewModelScope.launch { presenter.unlinkMedia(kind) }

    fun unlinkSso(link: AccountSsoLink) = viewModelScope.launch { presenter.unlinkSso(link) }

    fun consume() = presenter.consume()

    override fun onCleared() {
        plexJob?.cancel()
    }
}

class AccountDevicesViewModel(
    sdk: AcolyteSdk,
    userId: Long?,
) : ViewModel() {
    private val presenter: AccountDevicesPresenter = sdk.accountDevices(userId)
    val state = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() = viewModelScope.launch { presenter.refresh() }

    fun end(session: AccountSession) = viewModelScope.launch { presenter.end(session) }

    fun endAll() = viewModelScope.launch { presenter.endAll() }

    fun consume() = presenter.consume()
}

class AiConnectViewModel(
    private val sdk: AcolyteSdk,
) : ViewModel() {
    var link: String = ""
        private set

    var presenter: AiConnectPresenter? by mutableStateOf(null)
        private set

    fun start(text: String): AiConnectPresenter {
        link = text
        return sdk.aiConnect(text).also {
            presenter = it
            viewModelScope.launch { it.load() }
        }
    }

    fun allow() {
        val current = presenter ?: return
        viewModelScope.launch { current.allow() }
    }
}
