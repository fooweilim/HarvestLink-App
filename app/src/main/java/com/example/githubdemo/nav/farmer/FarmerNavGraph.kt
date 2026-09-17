package com.example.githubdemo.nav.farmer

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.githubdemo.nav.popBackStackSafely
import com.example.githubdemo.screen.farmer.AddProductScreen
import com.example.githubdemo.screen.farmer.EditProductScreen
import com.example.githubdemo.screen.farmer.FarmerDashboardScreen
import com.example.githubdemo.screen.farmer.FarmerOrdersScreen
import com.example.githubdemo.screen.farmer.FarmerProductScreen
import com.example.githubdemo.screen.farmer.FarmerProfileScreen

object FarmerRoute {
    const val DASHBOARD = "dashboard"
    const val PRODUCTS = "products"
    const val ADD_PRODUCT = "addProduct"
    const val EDIT_PRODUCT = "editProduct/{productId}"
    const val ORDERS = "orders"
    const val PROFILE = "profile"

    private const val EDIT_PRODUCT_PREFIX = "editProduct"

    fun getEditProductRoute(productId: String): String {
        return "$EDIT_PRODUCT_PREFIX/$productId"
    }
}

@Composable
fun FarmerNavGraph(
    navController: NavHostController,
    onSignOut: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = FarmerRoute.DASHBOARD
    ) {
        composable(FarmerRoute.DASHBOARD) {
            FarmerDashboardScreen(
                onNavigate = { route ->
                    navigateToFarmerBottomRoute(navController, route)
                }
            )
        }

        composable(FarmerRoute.PRODUCTS) {
            FarmerProductScreen(
                onAddProduct = {
                    navController.navigate(FarmerRoute.ADD_PRODUCT) {
                        launchSingleTop = true
                    }
                },
                onEditProduct = { product ->
                    val productId = product.id

                    if (!productId.isNullOrBlank()) {
                        navController.navigate(
                            FarmerRoute.getEditProductRoute(productId)
                        ) {
                            launchSingleTop = true
                        }
                    }
                },
                onNavigate = { route ->
                    navigateToFarmerBottomRoute(navController, route)
                }
            )
        }

        composable(FarmerRoute.ADD_PRODUCT) { entry ->
            AddProductScreen(
                onBack = {
                    navController.popBackStackSafely(entry)
                },
                onProductAdded = {
                    navController.popBackStackSafely(entry)
                }
            )
        }

        composable(
            route = FarmerRoute.EDIT_PRODUCT,
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            val productId =
                entry.arguments?.getString("productId").orEmpty()

            EditProductScreen(
                productId = productId,
                onBack = {
                    navController.popBackStackSafely(entry)
                },
                onUpdated = {
                    navController.popBackStackSafely(entry)
                }
            )
        }

        composable(FarmerRoute.ORDERS) {
            FarmerOrdersScreen(
                onNavigate = { route ->
                    navigateToFarmerBottomRoute(navController, route)
                }
            )
        }

        composable(FarmerRoute.PROFILE) {
            FarmerProfileScreen(
                onNavigate = { route ->
                    navigateToFarmerBottomRoute(navController, route)
                },
                onSignOut = onSignOut
            )
        }
    }
}

private fun navigateToFarmerBottomRoute(
    navController: NavHostController,
    route: String
) {
    val destination = when (route) {
        FarmerRoute.DASHBOARD -> FarmerRoute.DASHBOARD
        FarmerRoute.PRODUCTS -> FarmerRoute.PRODUCTS
        FarmerRoute.ADD_PRODUCT -> FarmerRoute.ADD_PRODUCT
        FarmerRoute.ORDERS -> FarmerRoute.ORDERS
        FarmerRoute.PROFILE -> FarmerRoute.PROFILE
        else -> FarmerRoute.DASHBOARD
    }

    navController.navigate(destination) {
        popUpTo(FarmerRoute.DASHBOARD) {
            saveState = true
        }

        launchSingleTop = true
        restoreState = true
    }
}