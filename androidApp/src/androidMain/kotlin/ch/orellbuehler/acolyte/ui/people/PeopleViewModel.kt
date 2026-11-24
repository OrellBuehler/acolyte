package ch.orellbuehler.acolyte.ui.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.api.model.AccountSsoLink
import ch.orellbuehler.acolyte.api.model.PeopleInvite
import ch.orellbuehler.acolyte.api.model.TitleUser
import ch.orellbuehler.acolyte.feature.account.PeopleInviteDraft
import ch.orellbuehler.acolyte.feature.account.PeoplePresenter
import kotlinx.coroutines.launch

class PeopleViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    val presenter: PeoplePresenter = sdk.people()
    val state = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() = viewModelScope.launch { presenter.refresh() }

    fun makeInvite(draft: PeopleInviteDraft) = viewModelScope.launch { presenter.makeInvite(draft) }

    fun revoke(invite: PeopleInvite) = viewModelScope.launch { presenter.revokeInvite(invite) }

    fun save(
        user: TitleUser,
        admin: Boolean,
        perms: List<String>,
        rating: String,
    ) = viewModelScope.launch { presenter.save(user, admin, perms, rating) }

    fun turnOff(user: TitleUser) = viewModelScope.launch { presenter.turnOff(user) }

    fun turnOn(user: TitleUser) = viewModelScope.launch { presenter.turnOn(user) }

    fun setPassword(
        user: TitleUser,
        password: String,
    ) = viewModelScope.launch { presenter.setPassword(user, password) }

    fun resetTwoFactor(user: TitleUser) = viewModelScope.launch { presenter.resetTwoFactor(user) }

    fun delete(user: TitleUser) = viewModelScope.launch { presenter.delete(user) }

    fun unlinkSso(
        user: TitleUser,
        link: AccountSsoLink,
    ) = viewModelScope.launch { presenter.unlinkSso(user, link) }

    fun confirmNoWayIn() = viewModelScope.launch { presenter.confirmNoWayIn() }

    fun loadServers() = viewModelScope.launch { presenter.loadServers() }

    fun linkMedia(
        user: TitleUser,
        serverId: Long,
        accountId: String,
    ) = viewModelScope.launch { presenter.linkMedia(user, serverId, accountId) }

    fun unlinkMedia(
        user: TitleUser,
        kind: String,
    ) = viewModelScope.launch { presenter.unlinkMedia(user, kind) }

    fun consume() = presenter.consume()

    fun dismissInvite() = presenter.dismissInvite()

    fun dismissNoWayIn() = presenter.dismissNoWayIn()
}
