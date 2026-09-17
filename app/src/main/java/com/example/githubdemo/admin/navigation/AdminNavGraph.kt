package com.example.githubdemo.admin.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.githubdemo.admin.components.AdminBottomBar
import com.example.githubdemo.admin.components.AdminTopBar
import com.example.githubdemo.admin.screen.AdminAnnouncementScreen
import com.example.githubdemo.admin.screen.AdminDashboardScreen
import com.example.githubdemo.admin.screen.AnnouncementDetailScreen
import com.example.githubdemo.admin.screen.FarmerVerificationScreen
import com.example.githubdemo.admin.screen.FoodBoxDetailsScreen
import com.example.githubdemo.admin.screen.FoodBoxManagementScreen
import com.example.githubdemo.admin.screen.FoodItemEditScreen
import com.example.githubdemo.admin.screen.NotificationScreen
import com.example.githubdemo.admin.screen.UserManagementScreen
import com.example.githubdemo.admin.screen.UserProfileScreen
import com.example.githubdemo.admin.viewmodel.AdminDashboardViewModel
import com.example.githubdemo.admin.viewmodel.AnnouncementViewModel
import com.example.githubdemo.admin.viewmodel.FarmerVerificationViewModel
import com.example.githubdemo.admin.viewmodel.FoodBoxViewModel
import com.example.githubdemo.admin.viewmodel.NotificationViewModel
import com.example.githubdemo.admin.viewmodel.UserManagementViewModel
import com.example.githubdemo.admin.viewmodel.UserProfileViewModel
import com.example.githubdemo.nav.popBackStackSafely

private const val HOME_ROUTE = "home"
private const val USER_ROUTE = "user"
private const val FARMER_ROUTE = "farmer"
private const val ANNOUNCEMENT_ROUTE = "announcement"
private const val NOTIFICATION_ROUTE = "notification"
private const val FOOD_BOX_ROUTE = "foodbox"
private const val FOOD_ITEMS_ROUTE = "foodItems"
private const val PROFILE_ROUTE = "profile/{id}"
private const val ANNOUNCEMENT_DETAIL_ROUTE = "announcementDetail/{id}"
private const val FOOD_BOX_DETAIL_ROUTE = "foodboxDetail/{id}"
private const val FOOD_ITEM_EDIT_ROUTE = "foodItemDetailEdit/{id}"
private const val ID_ARGUMENT = "id"

@Composable
fun AdminNavGraph(
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val navController = rememberNavController()

    val dashboardViewModel: AdminDashboardViewModel = viewModel()
    val userViewModel: UserManagementViewModel = viewModel()
    val userProfileViewModel: UserProfileViewModel = viewModel()
    val farmerViewModel: FarmerVerificationViewModel = viewModel()
    val announcementViewModel: AnnouncementViewModel = viewModel()
    val foodBoxViewModel: FoodBoxViewModel = viewModel()

    val notificationViewModel: NotificationViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>
            ): T {
                return NotificationViewModel(
                    context.applicationContext
                ) as T
            }
        }
    )

    val currentRoute = navController
        .currentBackStackEntryAsState()
        .value
        ?.destination
        ?.route
        ?: HOME_ROUTE

    Scaffold(
        topBar = {
            AdminTopBar(
                title = getAdminTitle(currentRoute),
                onLogout = onLogout,
                onNotificationClick = {
                    navController.navigate(NOTIFICATION_ROUTE) {
                        launchSingleTop = true
                    }
                },
                unreadCount = notificationViewModel.unreadCount()
            )
        },
        bottomBar = {
            AdminBottomBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                        restoreState = true

                        popUpTo(
                            navController.graph.startDestinationId
                        ) {
                            saveState = true
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HOME_ROUTE,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(HOME_ROUTE) {
                AdminDashboardScreen(
                    viewModel = dashboardViewModel
                )
            }

            composable(USER_ROUTE) {
                UserManagementScreen(
                    navController = navController,
                    onSelectUser = { user ->
                        navController.navigate("profile/${user.id}") {
                            launchSingleTop = true
                        }
                    },
                    viewModel = userViewModel
                )
            }

            composable(PROFILE_ROUTE) { entry ->
                val userId =
                    entry.arguments?.getString(ID_ARGUMENT).orEmpty()

                UserProfileScreen(
                    navController = navController,
                    userId = userId,
                    viewModel = userProfileViewModel
                )
            }

            composable(FARMER_ROUTE) {
                FarmerVerificationScreen(
                    navController = navController,
                    viewModel = farmerViewModel
                )
            }

            composable(ANNOUNCEMENT_ROUTE) {
                AdminAnnouncementScreen(
                    navController = navController,
                    viewModel = announcementViewModel
                )
            }

            composable(NOTIFICATION_ROUTE) { entry ->
                NotificationScreen(
                    onBack = {
                        navController.popBackStackSafely(entry)
                    },
                    viewModel = notificationViewModel
                )
            }

            composable(FOOD_BOX_ROUTE) {
                FoodBoxManagementScreen(
                    viewModel = foodBoxViewModel,
                    onFoodBoxClick = { foodBoxId ->
                        navController.navigate(
                            "foodboxDetail/$foodBoxId"
                        ) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(FOOD_BOX_DETAIL_ROUTE) { entry ->
                val foodBoxId =
                    entry.arguments?.getString(ID_ARGUMENT).orEmpty()

                FoodBoxDetailsScreen(
                    foodBoxId = foodBoxId,
                    navController = navController,
                    viewModel = foodBoxViewModel,
                    onBack = {
                        navController.popBackStackSafely(entry)
                    }
                )
            }

            composable(FOOD_ITEMS_ROUTE) { entry ->
                FoodItemEditScreen(
                    viewModel = foodBoxViewModel,
                    onBack = {
                        navController.popBackStackSafely(entry)
                    }
                )
            }

            composable(FOOD_ITEM_EDIT_ROUTE) { entry ->
                val foodBoxId =
                    entry.arguments?.getString(ID_ARGUMENT).orEmpty()

                FoodItemEditScreen(
                    viewModel = foodBoxViewModel,
                    foodBoxId = foodBoxId,
                    onBack = {
                        navController.popBackStackSafely(entry)
                    }
                )
            }

            composable(ANNOUNCEMENT_DETAIL_ROUTE) { entry ->
                val announcementId =
                    entry.arguments?.getString(ID_ARGUMENT).orEmpty()

                AnnouncementDetailScreen(
                    announcementId = announcementId,
                    onBack = {
                        navController.popBackStackSafely(entry)
                    },
                    viewModel = announcementViewModel
                )
            }
        }
    }
}

private fun getAdminTitle(route: String): String {
    return when {
        route == HOME_ROUTE -> "Dashboard"
        route == USER_ROUTE -> "User Management"
        route.startsWith("profile") -> "User Profile"
        route == FARMER_ROUTE -> "Farmer Verification"
        route == ANNOUNCEMENT_ROUTE -> "Announcement"
        route == NOTIFICATION_ROUTE -> "Notification"
        route == FOOD_BOX_ROUTE -> "Food Box"
        route.startsWith("foodboxDetail") -> "Food Box Details"
        route.startsWith("foodItemDetailEdit") -> "Edit Item"
        route == FOOD_ITEMS_ROUTE -> "Edit Items"
        else -> "Dashboard"
    }
}