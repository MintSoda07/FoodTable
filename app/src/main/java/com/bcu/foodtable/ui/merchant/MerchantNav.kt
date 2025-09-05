// MerchantNav.kt
package com.bcu.foodtable.ui.merchant

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun MerchantRoot() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = "home") {

        // 홈
        composable("home") {
            MerchantHomeGrid(
                onStoreManage = { storeId -> nav.navigate("storeMgmt/$storeId") },
                onQrPay = { storeId -> nav.navigate("qrpay/$storeId") },
                onSales = { /* TODO */ },
                onStoreInfo = { storeId -> nav.navigate("storeInfo/$storeId") },
                onOrders = { /* TODO */ },
                onProducts = { storeId -> nav.navigate("products/$storeId") },
                onStaff = { storeId -> nav.navigate("staff/$storeId") },
                onCoupons = { storeId -> nav.navigate("coupons/$storeId") },
                onSettlements = { /* TODO */ },
                onReports = { /* TODO */ },
                onSettings = { /* TODO */ },
            )
        }
        // MerchantNav.kt (또는 MerchantRoot가 있는 파일)
        composable(
            route = "qrpay/{storeId}",
            arguments = listOf(navArgument("storeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val storeId = backStackEntry.arguments?.getString("storeId").orEmpty()
            QrOrderScreen(
                storeId = storeId,
                onBack = { nav.popBackStack() }
            )
        }

        // 가게관리
        composable(
            "storeMgmt/{storeId}",
            arguments = listOf(navArgument("storeId") { type = NavType.StringType })
        ) { b ->
            val storeId = b.arguments?.getString("storeId").orEmpty()
            StoreManageScreen(
                storeId = storeId,
                onBack = { nav.popBackStack() },
                onEditInfo = { nav.navigate("storeInfo/$storeId") },
            )
        }

        // 가게정보관리
        composable(
            "storeInfo/{storeId}",
            arguments = listOf(navArgument("storeId") { type = NavType.StringType })
        ) { b ->
            val storeId = b.arguments?.getString("storeId").orEmpty()
            StoreInfoScreen(
                storeId = storeId,
                onBack = { nav.popBackStack() }
            )
        }

        // 상품관리
        composable(
            "products/{storeId}",
            arguments = listOf(navArgument("storeId") { type = NavType.StringType })
        ) { b ->
            val storeId = b.arguments?.getString("storeId").orEmpty()
            ProductManagementScreen(
                storeId = storeId,
                onBack = { nav.popBackStack() }
            )
        }

        // 쿠폰/프로모션
        composable(
            "coupons/{storeId}",
            arguments = listOf(navArgument("storeId") { type = NavType.StringType })
        ) { b ->
            val storeId = b.arguments?.getString("storeId").orEmpty()
            CouponPromotionScreen(
                storeId = storeId,
                onBack = { nav.popBackStack() }
            )
        }

        // 직원관리
        composable(
            "staff/{storeId}",
            arguments = listOf(navArgument("storeId") { type = NavType.StringType })
        ) { b ->
            val storeId = b.arguments?.getString("storeId").orEmpty()
            StaffManagementScreen(
                storeId = storeId,
                onBack = { nav.popBackStack() }
            )
        }
    }
}
