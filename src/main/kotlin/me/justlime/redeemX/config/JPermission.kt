package me.justlime.redeemX.config

sealed interface JPermission {
    data object Admin : JPermission {
        const val USE = "redeemx.admin.use"
        const val GEN = "$USE.gen"
        const val MODIFY = "$USE.modify"
        const val DELETE = "$USE.delete"
        const val INFO = "$USE.info"
        const val RENEW = "$USE.renew"
        const val RELOAD = "$USE.reload"
    }

    data object Player : JPermission {
        const val USE = "redeemx.use"
    }

}