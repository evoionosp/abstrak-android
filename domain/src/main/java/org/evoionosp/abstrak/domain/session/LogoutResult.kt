package org.evoionosp.abstrak.domain.session

data class LogoutResult(
    val failedSteps: List<LogoutStep>,
) {
    val isFullyClean: Boolean get() = failedSteps.isEmpty()
}
