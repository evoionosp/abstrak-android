package org.evoionosp.abstrak.presentation.settings
import android.security.KeyChain
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import org.evoionosp.abstrak.presentation.R

@Composable
internal fun rememberClientCertificatePicker(onPicked: (String) -> Unit): (currentAlias: String?) -> Unit {
    val activity = LocalActivity.current
    val currentOnPicked by rememberUpdatedState(onPicked)
    return remember(activity) {
        pick@{ currentAlias ->
            val host = activity ?: return@pick
            KeyChain.choosePrivateKeyAlias(
                host,
                { alias ->
                    if (alias != null) {
                        currentOnPicked(alias)
                    } else {
                        host.runOnUiThread {
                            Toast
                                .makeText(
                                    host,
                                    host.getString(R.string.server_connection_client_cert_not_chosen),
                                    Toast.LENGTH_LONG,
                                ).show()
                        }
                    }
                },
                null,
                null,
                null,
                -1,
                currentAlias,
            )
        }
    }
}
