package ch.orellbuehler.acolyte.ui.account

import ch.orellbuehler.acolyte.session.AccountRules

fun passwordTooShort(
    password: String,
    minimum: Int,
): Boolean = password.isNotEmpty() && !AccountRules.validPassword(password, minimum)

fun passwordMismatch(
    password: String,
    confirm: String,
): Boolean = confirm.isNotEmpty() && confirm != password

fun passwordReady(
    password: String,
    confirm: String,
    minimum: Int,
): Boolean = AccountRules.validPassword(password, minimum) && password == confirm

fun followDisplayName(
    name: String,
    displayName: String,
    edited: Boolean,
): String = if (edited) displayName else name
