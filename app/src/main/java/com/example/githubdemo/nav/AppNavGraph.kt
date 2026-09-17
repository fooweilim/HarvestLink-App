package com.example.githubdemo.nav

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.githubdemo.data.AppData
import com.example.githubdemo.data.UserRole
import com.example.githubdemo.data.local.LocalAccountStorage
import com.example.githubdemo.data.meals.MealData
import com.example.githubdemo.screen.AdminDashboardScreen
import com.example.githubdemo.screen.AppBottomNavigationBar
import com.example.githubdemo.screen.FarmerDashboardScreen
import com.example.githubdemo.screen.HomeScreen
import com.example.githubdemo.screen.MealDetailScreen
import com.example.githubdemo.screen.RoleSelectionScreen
import com.example.githubdemo.screen.authentication.AuthRateLimitDialog
import com.example.githubdemo.screen.authentication.FarmerSignUpScreen
import com.example.githubdemo.screen.authentication.LoginScreen
import com.example.githubdemo.screen.authentication.SignUpScreen
import com.example.githubdemo.screen.foodbox.CustomizeFoodBoxScreen
import com.example.githubdemo.screen.foodbox.DeliveryScheduleScreen
import com.example.githubdemo.screen.foodbox.FoodBoxCheckoutScreen
import com.example.githubdemo.screen.foodbox.FoodBoxDetailScreen
import com.example.githubdemo.screen.foodbox.FoodBoxPlansScreen
import com.example.githubdemo.screen.foodbox.ManageSubscriptionScreen
import com.example.githubdemo.screen.foodbox.SubscriptionSuccessScreen
import com.example.githubdemo.screen.market.CartScreen
import com.example.githubdemo.screen.market.MarketPaymentScreen
import com.example.githubdemo.screen.market.MarketScreen
import com.example.githubdemo.screen.meals.FavouriteScreen
import com.example.githubdemo.screen.meals.MealsScreen
import com.example.githubdemo.screen.userprofile.ProfileScreen
import com.example.githubdemo.viewmodel.authentication.AuthViewModel
import com.example.githubdemo.viewmodel.foodbox.FoodBoxViewModel
import com.example.githubdemo.viewmodel.market.CartViewModel

private const val ROLE_ARGUMENT = "role"
private const val PLAN_ID_ARGUMENT = "planId"
private const val CART_ROUTE = "cart"
private const val PAYMENT_ROUTE = "payment"
private const val MEAL_FAVOURITES_ROUTE = "meal_favourites"
private const val MEAL_ID_ARGUMENT = "mealId"
private const val MEAL_DETAIL_ROUTE = "meal_detail/{mealId}"

private fun getMealDetailRoute(mealId: Int): String {
    return "meal_detail/$mealId"
}

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current

    val authViewModel: AuthViewModel = viewModel()
    val cartViewModel: CartViewModel = viewModel()
    val foodBoxViewModel: FoodBoxViewModel = viewModel()

    val savedRole = remember {
        LocalAccountStorage.getSelectedRole(context)
    }

    val startDestination = if (savedRole == null) {
        AppData.ROLE_SELECTION_ROUTE
    } else {
        AppData.getRoleDestination(savedRole)
    }

    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    val isAuthPage =
        currentRoute == AppData.LOGIN_ROUTE ||
                currentRoute == AppData.SIGN_UP_ROUTE

    val isMealSubPage =
        currentRoute == MEAL_FAVOURITES_ROUTE ||
                currentRoute == MEAL_DETAIL_ROUTE

    val isFoodBoxPage =
        currentRoute != null &&
                currentRoute in AppData.foodBoxRoutes

    val selectedBottomRoute = when {
        isMealSubPage -> AppData.MEALS_ROUTE
        isFoodBoxPage -> AppData.FOOD_BOX_ROUTE
        else -> currentRoute
    }

    val showBottomNavigation =
        currentRoute in AppData.buyerRoutes || isMealSubPage

    val onPageNavigate: (String) -> Unit = { route ->
        if (route == AppData.ROLE_SELECTION_ROUTE) {
            authViewModel.signOut {
                navController.navigate(AppData.ROLE_SELECTION_ROUTE) {
                    popUpTo(navController.graph.id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        } else {
            navController.navigate(route) {
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        modifier = if (isAuthPage) {
            Modifier.fillMaxSize().imePadding()
        } else {
            Modifier.fillMaxSize()
        },
        bottomBar = {
            if (showBottomNavigation) {
                AppBottomNavigationBar(
                    currentRoute = selectedBottomRoute,
                    onItemClick = { route ->
                        navController.navigate(route) {
                            popUpTo(AppData.HOME_ROUTE) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            composable(AppData.ROLE_SELECTION_ROUTE) {
                RoleSelectionScreen(
                    onRoleSelected = { selectedRole ->
                        navController.navigate(
                            AppData.getLoginRoute(selectedRole)
                        ) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = AppData.LOGIN_ROUTE,
                arguments = listOf(
                    navArgument(ROLE_ARGUMENT) {
                        type = NavType.StringType
                    }
                )
            ) { entry ->
                val userRole =
                    entry.arguments?.getString(ROLE_ARGUMENT)

                if (
                    userRole != null &&
                    UserRole.isValidRole(userRole)
                ) {
                    LoginScreen(
                        userRole = userRole,
                        onLoginSuccess = { loggedInRole ->
                            navController.navigate(
                                AppData.getRoleDestination(loggedInRole)
                            ) {
                                popUpTo(navController.graph.id) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        },
                        onSignUpClick = { signUpRole ->
                            if (signUpRole != UserRole.ADMIN) {
                                navController.navigate(
                                    AppData.getSignUpRoute(signUpRole)
                                ) {
                                    launchSingleTop = true
                                }
                            } else {
                                authViewModel.showErrorMessage(
                                    "Admin accounts cannot sign up."
                                )
                            }
                        },
                        onBackClick = {
                            authViewModel.clearMessage()
                            navController.popBackStackSafely(entry)
                        },
                        authViewModel = authViewModel
                    )
                } else {
                    TextButton(
                        onClick = {
                            navController.popBackStackSafely(entry)
                        }
                    ) {
                        Text("Invalid role. Go back")
                    }
                }
            }

            composable(
                route = AppData.SIGN_UP_ROUTE,
                arguments = listOf(
                    navArgument(ROLE_ARGUMENT) {
                        type = NavType.StringType
                    }
                )
            ) { entry ->
                val userRole =
                    entry.arguments?.getString(ROLE_ARGUMENT)

                val returnToLogin: () -> Unit = {
                    authViewModel.clearMessage()
                    navController.popBackStackSafely(entry)
                }

                when (userRole) {
                    UserRole.BUYER -> {
                        SignUpScreen(
                            userRole = UserRole.BUYER,
                            onSignUpSuccess = { returnToLogin() },
                            onLoginClick = { returnToLogin() },
                            onBackClick = returnToLogin,
                            authViewModel = authViewModel
                        )
                    }

                    UserRole.FARMER -> {
                        FarmerSignUpScreen(
                            onSignUpSuccess = { returnToLogin() },
                            onLoginClick = { returnToLogin() },
                            onBackClick = returnToLogin,
                            authViewModel = authViewModel
                        )
                    }

                    else -> {
                        TextButton(onClick = returnToLogin) {
                            Text("Admin accounts cannot sign up. Go back")
                        }
                    }
                }
            }

            composable(AppData.HOME_ROUTE) {
                HomeScreen(onNavigate = onPageNavigate)
            }

            composable(AppData.MARKET_ROUTE) {
                MarketScreen(
                    onNavigate = { route ->
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    },
                    cartViewModel = cartViewModel
                )
            }

            composable(CART_ROUTE) { entry ->
                CartScreen(
                    onBack = {
                        navController.popBackStackSafely(entry)
                    },
                    onCheckout = {
                        navController.navigate(PAYMENT_ROUTE) {
                            launchSingleTop = true
                        }
                    },
                    cartViewModel = cartViewModel
                )
            }

            composable(PAYMENT_ROUTE) { entry ->
                MarketPaymentScreen(
                    onBack = {
                        navController.popBackStackSafely(entry)
                    },
                    onPaymentSuccess = {
                        navController.navigate(AppData.MARKET_ROUTE) {
                            popUpTo(AppData.MARKET_ROUTE) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                    cartViewModel = cartViewModel
                )
            }

            composable(AppData.FOOD_BOX_ROUTE) {
                val userId =
                    LocalAccountStorage.getProfile(context)?.id

                LaunchedEffect(userId) {
                    foodBoxViewModel.refreshActiveSubscription(userId)
                }

                FoodBoxPlansScreen(
                    foodBoxViewModel = foodBoxViewModel,
                    onViewPlanClick = { planId ->
                        navController.navigate(
                            AppData.getFoodBoxDetailRoute(planId)
                        ) {
                            launchSingleTop = true
                        }
                    },
                    onManageClick = {
                        navController.navigate(
                            AppData.FOOD_BOX_MANAGE_ROUTE
                        ) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = AppData.FOOD_BOX_DETAIL_ROUTE,
                arguments = listOf(
                    navArgument(PLAN_ID_ARGUMENT) {
                        type = NavType.StringType
                    }
                )
            ) { entry ->
                val planId = entry.arguments
                    ?.getString(PLAN_ID_ARGUMENT)
                    .orEmpty()

                FoodBoxDetailScreen(
                    planId = planId,
                    foodBoxViewModel = foodBoxViewModel,
                    onBackClick = {
                        navController.popBackStackSafely(entry)
                    },
                    onCustomizeClick = {
                        foodBoxViewModel.selectPlan(planId)
                        navController.navigate(
                            AppData.FOOD_BOX_CUSTOMIZE_ROUTE
                        ) {
                            launchSingleTop = true
                        }
                    },
                    onManageClick = {
                        navController.navigate(
                            AppData.FOOD_BOX_MANAGE_ROUTE
                        ) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(AppData.FOOD_BOX_CUSTOMIZE_ROUTE) { entry ->
                CustomizeFoodBoxScreen(
                    foodBoxViewModel = foodBoxViewModel,
                    onBackClick = {
                        navController.popBackStackSafely(entry)
                    },
                    onContinueClick = {
                        navController.navigate(
                            AppData.FOOD_BOX_SCHEDULE_ROUTE
                        ) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(AppData.FOOD_BOX_SCHEDULE_ROUTE) { entry ->
                DeliveryScheduleScreen(
                    foodBoxViewModel = foodBoxViewModel,
                    onBackClick = {
                        navController.popBackStackSafely(entry)
                    },
                    onContinueClick = {
                        navController.navigate(
                            AppData.FOOD_BOX_CHECKOUT_ROUTE
                        ) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(AppData.FOOD_BOX_CHECKOUT_ROUTE) { entry ->
                FoodBoxCheckoutScreen(
                    foodBoxViewModel = foodBoxViewModel,
                    onBackClick = {
                        navController.popBackStackSafely(entry)
                    },
                    onSubscribeClick = {
                        val userId =
                            LocalAccountStorage.getProfile(context)?.id

                        foodBoxViewModel.confirmSubscription(
                            userId = userId,
                            onFinished = {
                                navController.navigate(
                                    AppData.FOOD_BOX_SUCCESS_ROUTE
                                ) {
                                    popUpTo(AppData.FOOD_BOX_ROUTE)
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                )
            }

            composable(AppData.FOOD_BOX_SUCCESS_ROUTE) {
                SubscriptionSuccessScreen(
                    foodBoxViewModel = foodBoxViewModel,
                    onManageClick = {
                        navController.navigate(
                            AppData.FOOD_BOX_MANAGE_ROUTE
                        ) {
                            launchSingleTop = true
                        }
                    },
                    onBrowseMoreClick = {
                        navController.navigate(AppData.FOOD_BOX_ROUTE) {
                            popUpTo(AppData.FOOD_BOX_ROUTE)
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(AppData.FOOD_BOX_MANAGE_ROUTE) { entry ->
                ManageSubscriptionScreen(
                    foodBoxViewModel = foodBoxViewModel,
                    onBackClick = {
                        navController.popBackStackSafely(entry)
                    },
                    onBrowsePlansClick = {
                        navController.navigate(AppData.FOOD_BOX_ROUTE) {
                            popUpTo(AppData.FOOD_BOX_ROUTE)
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(AppData.MEALS_ROUTE) {
                MealsScreen(
                    onViewDetails = { mealId ->
                        navController.navigate(
                            getMealDetailRoute(mealId)
                        ) {
                            launchSingleTop = true
                        }
                    },
                    onFavouriteClick = {
                        navController.navigate(MEAL_FAVOURITES_ROUTE) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(MEAL_FAVOURITES_ROUTE) { entry ->
                FavouriteScreen(
                    onBack = {
                        navController.popBackStackSafely(entry)
                    },
                    onViewDetails = { mealId ->
                        navController.navigate(
                            getMealDetailRoute(mealId)
                        ) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = MEAL_DETAIL_ROUTE,
                arguments = listOf(
                    navArgument(MEAL_ID_ARGUMENT) {
                        type = NavType.IntType
                    }
                )
            ) { entry ->
                val mealId =
                    entry.arguments?.getInt(MEAL_ID_ARGUMENT)

                val meal = mealId?.let {
                    MealData.getMealById(it)
                }

                if (meal != null) {
                    MealDetailScreen(
                        meal = meal,
                        onBack = {
                            navController.popBackStackSafely(entry)
                        }
                    )
                } else {
                    TextButton(
                        onClick = {
                            navController.popBackStackSafely(entry)
                        }
                    ) {
                        Text("Meal not found. Go back")
                    }
                }
            }

            composable(AppData.PROFILE_ROUTE) {
                ProfileScreen(onNavigate = onPageNavigate)
            }

            composable(AppData.FARMER_ROUTE) {
                FarmerDashboardScreen(onNavigate = onPageNavigate)
            }

            composable(AppData.ADMIN_ROUTE) {
                AdminDashboardScreen(onNavigate = onPageNavigate)
            }
        }
    }

    AuthRateLimitDialog(authViewModel = authViewModel)
}