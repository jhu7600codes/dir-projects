package com.vanbank.app.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val HOME = "home"
    const val TRANSFER = "transfer"
    const val TRANSACTIONS = "transactions"
    const val STATEMENTS = "statements"
    const val LOANS = "loans"
    const val LOAN_DETAIL = "loans/{loanId}"
    const val VAULTS = "vaults"
    const val BUDGET = "budget"
    const val BILL_PAY = "bill_pay"
    const val ADMIN = "admin"
    const val AI_REQUESTS = "ai_requests"

    fun loanDetail(loanId: Long) = "loans/$loanId"
}
