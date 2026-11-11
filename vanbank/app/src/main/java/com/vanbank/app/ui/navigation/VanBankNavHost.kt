package com.vanbank.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vanbank.app.di.AppContainer
import com.vanbank.app.di.VanBankViewModelFactory
import com.vanbank.app.ui.admin.AdminScreen
import com.vanbank.app.ui.admin.AdminViewModel
import com.vanbank.app.ui.ai.AiRequestViewModel
import com.vanbank.app.ui.ai.AiRequestsScreen
import com.vanbank.app.ui.auth.AuthViewModel
import com.vanbank.app.ui.auth.LoginScreen
import com.vanbank.app.ui.auth.SignupScreen
import com.vanbank.app.ui.billpay.BillPayScreen
import com.vanbank.app.ui.billpay.BillPayViewModel
import com.vanbank.app.ui.budgeting.BudgetScreen
import com.vanbank.app.ui.budgeting.BudgetViewModel
import com.vanbank.app.ui.home.HomeScreen
import com.vanbank.app.ui.home.HomeViewModel
import com.vanbank.app.ui.loans.LoanDetailScreen
import com.vanbank.app.ui.loans.LoansScreen
import com.vanbank.app.ui.loans.LoansViewModel
import com.vanbank.app.ui.statements.StatementsScreen
import com.vanbank.app.ui.statements.StatementsViewModel
import com.vanbank.app.ui.transactions.TransactionsScreen
import com.vanbank.app.ui.transactions.TransactionsViewModel
import com.vanbank.app.ui.transfer.TransferScreen
import com.vanbank.app.ui.transfer.TransferViewModel
import com.vanbank.app.ui.vaults.VaultsScreen
import com.vanbank.app.ui.vaults.VaultsViewModel
import kotlinx.coroutines.flow.first

@Composable
fun VanBankNavHost(container: AppContainer) {
    val factory = remember { VanBankViewModelFactory(container) }
    val navController = rememberNavController()

    // Resume straight into Home if a session already exists (DataStore-backed sign-in).
    LaunchedEffect(Unit) {
        val existingUserId = container.authRepository.currentUserId.first()
        if (existingUserId != null) {
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.LOGIN) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            val vm: AuthViewModel = viewModel(factory = factory)
            LoginScreen(
                viewModel = vm,
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
                onNavigateToSignup = { navController.navigate(Routes.SIGNUP) },
            )
        }
        composable(Routes.SIGNUP) {
            val vm: AuthViewModel = viewModel(factory = factory)
            SignupScreen(
                viewModel = vm,
                onSignupSuccess = { navController.popBackStack() },
                onNavigateToLogin = { navController.popBackStack() },
            )
        }
        composable(Routes.HOME) {
            val vm: HomeViewModel = viewModel(factory = factory)
            HomeScreen(
                viewModel = vm,
                onNavigateToTransfer = { navController.navigate(Routes.TRANSFER) },
                onNavigateToTransactions = { navController.navigate(Routes.TRANSACTIONS) },
                onNavigateToStatements = { navController.navigate(Routes.STATEMENTS) },
                onNavigateToLoans = { navController.navigate(Routes.LOANS) },
                onNavigateToVaults = { navController.navigate(Routes.VAULTS) },
                onNavigateToBudget = { navController.navigate(Routes.BUDGET) },
                onNavigateToBillPay = { navController.navigate(Routes.BILL_PAY) },
                onNavigateToAdmin = { navController.navigate(Routes.ADMIN) },
                onNavigateToAiRequests = { navController.navigate(Routes.AI_REQUESTS) },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) { popUpTo(0) }
                },
            )
        }
        composable(Routes.TRANSFER) {
            val vm: TransferViewModel = viewModel(factory = factory)
            TransferScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.TRANSACTIONS) {
            val vm: TransactionsViewModel = viewModel(factory = factory)
            TransactionsScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.STATEMENTS) {
            val vm: StatementsViewModel = viewModel(factory = factory)
            StatementsScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.LOANS) {
            val vm: LoansViewModel = viewModel(factory = factory)
            LoansScreen(
                viewModel = vm,
                onOpenLoan = { loanId -> navController.navigate(Routes.loanDetail(loanId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            Routes.LOAN_DETAIL,
            arguments = listOf(navArgument("loanId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val vm: LoansViewModel = viewModel(factory = factory)
            val loanId = backStackEntry.arguments?.getLong("loanId") ?: 0L
            LoanDetailScreen(viewModel = vm, loanId = loanId, onBack = { navController.popBackStack() })
        }
        composable(Routes.VAULTS) {
            val vm: VaultsViewModel = viewModel(factory = factory)
            VaultsScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.BUDGET) {
            val vm: BudgetViewModel = viewModel(factory = factory)
            BudgetScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.BILL_PAY) {
            val vm: BillPayViewModel = viewModel(factory = factory)
            BillPayScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.ADMIN) {
            val vm: AdminViewModel = viewModel(factory = factory)
            AdminScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.AI_REQUESTS) {
            val vm: AiRequestViewModel = viewModel(factory = factory)
            AiRequestsScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
    }
}
