package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.auth.AuthState
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.DebtType
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.ui.components.AdMobBannerView
import com.example.ui.components.SpeedDialFab
import com.example.ui.screens.AddEditDebtSheet
import com.example.ui.screens.AddEditTransactionSheet
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DebtKhataScreen
import com.example.ui.screens.DebtPersonDetailScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.PinLockScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodel.MainViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Splash : Screen("splash", "স্প্ল্যাশ", Icons.Default.Dashboard)
    object Login : Screen("login", "লগইন", Icons.Default.Dashboard)
    object Dashboard : Screen("dashboard", "হোম", Icons.Default.Dashboard)
    object Transactions : Screen("transactions", "লেনদেন", Icons.Default.ReceiptLong)
    object DebtKhata : Screen("debt_khata", "দেনা-পাওনা", Icons.Default.Handshake)
    object Reports : Screen("reports", "রিপোর্ট", Icons.Default.Assessment)
    object Budget : Screen("budget", "বাজেট", Icons.Default.PieChart)
    object Profile : Screen("profile", "প্রোফাইল", Icons.Default.Dashboard)
    object DebtDetail : Screen("debt_detail/{debtId}", "বিবরণ", Icons.Default.Handshake) {
        fun createRoute(debtId: String) = "debt_detail/$debtId"
    }
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Transactions,
    Screen.DebtKhata,
    Screen.Reports,
    Screen.Budget
)

@Composable
fun AppNavigation(
    viewModel: MainViewModel,
    navController: NavHostController = rememberNavController()
) {
    val isPinLocked by viewModel.isPinLocked.collectAsStateWithLifecycle()
    val isPinVerified by viewModel.isPinVerified.collectAsStateWithLifecycle()

    if (isPinLocked && !isPinVerified) {
        PinLockScreen(
            onPinEntered = { pin -> viewModel.verifyPin(pin) }
        )
        return
    }

    var showTransactionSheet by remember { mutableStateOf(false) }
    var transactionSheetType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }

    var showDebtSheet by remember { mutableStateOf(false) }
    var debtSheetType by remember { mutableStateOf(DebtType.RECEIVABLE) }
    var debtToEdit by remember { mutableStateOf<DebtEntity?>(null) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            val shouldShowBottomBar = bottomNavItems.any { it.route == currentRoute }
            if (shouldShowBottomBar) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        bottomNavItems.forEach { screen ->
                            val selected = currentRoute == screen.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrimaryGreen,
                                    selectedTextColor = PrimaryGreen,
                                    indicatorColor = PrimaryGreen.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("nav_item_${screen.route}")
                            )
                        }
                    }
                    AdMobBannerView()
                }
            }
        },
        floatingActionButton = {
            val shouldShowFab = bottomNavItems.any { it.route == currentRoute }
            if (shouldShowFab) {
                SpeedDialFab(
                    onAddIncome = {
                        transactionToEdit = null
                        transactionSheetType = TransactionType.INCOME
                        showTransactionSheet = true
                    },
                    onAddExpense = {
                        transactionToEdit = null
                        transactionSheetType = TransactionType.EXPENSE
                        showTransactionSheet = true
                    },
                    onAddDebt = {
                        debtToEdit = null
                        debtSheetType = DebtType.RECEIVABLE
                        showDebtSheet = true
                    },
                    onAddPayment = {}
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onTimeout = {
                        val currentAuth = viewModel.authState.value
                        if (currentAuth is AuthState.Authenticated) {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onTransactionClick = { tx ->
                        transactionToEdit = tx
                        showTransactionSheet = true
                    }
                )
            }

            composable(Screen.Transactions.route) {
                TransactionsScreen(
                    viewModel = viewModel,
                    onEditTransaction = { tx ->
                        transactionToEdit = tx
                        showTransactionSheet = true
                    }
                )
            }

            composable(Screen.DebtKhata.route) {
                DebtKhataScreen(
                    viewModel = viewModel,
                    onDebtClick = { item ->
                        navController.navigate(Screen.DebtDetail.createRoute(item.debt.id))
                    }
                )
            }

            composable(Screen.Reports.route) {
                ReportsScreen(viewModel = viewModel)
            }

            composable(Screen.Budget.route) {
                BudgetScreen(viewModel = viewModel)
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    viewModel = viewModel,
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.DebtDetail.route,
                arguments = listOf(navArgument("debtId") { type = NavType.StringType })
            ) { backStackEntry ->
                val debtId = backStackEntry.arguments?.getString("debtId") ?: ""
                DebtPersonDetailScreen(
                    debtId = debtId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onEditDebt = {
                        val d = viewModel.allDebts.value.find { it.id == debtId }
                        debtToEdit = d
                        showDebtSheet = true
                    }
                )
            }
        }

        // Add / Edit Transaction Bottom Sheet
        if (showTransactionSheet) {
            AddEditTransactionSheet(
                viewModel = viewModel,
                transactionToEdit = transactionToEdit,
                initialType = transactionSheetType,
                onDismiss = {
                    showTransactionSheet = false
                    transactionToEdit = null
                }
            )
        }

        // Add / Edit Debt Bottom Sheet
        if (showDebtSheet) {
            AddEditDebtSheet(
                viewModel = viewModel,
                debtToEdit = debtToEdit,
                initialType = debtSheetType,
                onDismiss = {
                    showDebtSheet = false
                    debtToEdit = null
                }
            )
        }
    }
}
