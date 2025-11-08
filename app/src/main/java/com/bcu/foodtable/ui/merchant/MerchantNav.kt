// navigation/MerchantNav.kt
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
                onQrPay      = { storeId -> nav.navigate("qrpay/$storeId") },
                onSales      = { storeId -> nav.navigate("sales/$storeId") },
                onStoreInfo  = { storeId -> nav.navigate("storeInfo/$storeId") },
                onOrders     = { storeId -> nav.navigate("orders/$storeId") },
                onProducts   = { storeId -> nav.navigate("products/$storeId") },
                onStaff      = { storeId -> nav.navigate("staff/$storeId") },
                onCoupons    = { storeId -> nav.navigate("coupons/$storeId") },
                onSettlements= { storeId -> nav.navigate("settlements/$storeId") },
                onReports    = { storeId -> nav.navigate("reports/$storeId") },
                onSettings   = { nav.navigate("settings") },
            )
        }

        // QR 결제 생성(가맹점용)
        composable(
            route = "qrpay/{storeId}",
            arguments = listOf(navArgument("storeId") { type = NavType.StringType })
        ) { b ->
            val storeId = b.arguments?.getString("storeId").orEmpty()
            QrOrderScreen(storeId = storeId, onBack = { nav.popBackStack() })
        }

        // (옵션) QR 스캐너(가맹점이 손님 QR을 스캔할 때 사용)
        composable(
            route = "qrscan/{storeId}",
            arguments = listOf(navArgument("storeId") { type = NavType.StringType })
        ) { b ->
            // 필요 시 홈 타일/버튼에서 nav.navigate("qrscan/$storeId")로 진입
            // storeId가 필요하면 QrPayScannerScreen에 파라미터 추가 후 넘겨주세요.
            QrPayScannerScreen(onBack = { nav.popBackStack() })
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
            StoreInfoScreen(storeId = storeId, onBack = { nav.popBackStack() })
        }

        // 상품관리
        composable(
            "products/{storeId}",
            arguments = listOf(navArgument("storeId") { type = NavType.StringType })
        ) { b ->
            val storeId = b.arguments?.getString("storeId").orEmpty()
            ProductManagementScreen(storeId = storeId, onBack = { nav.popBackStack() })
        }

        // 쿠폰/프로모션
        composable(
            "coupons/{storeId}",
            arguments = listOf(navArgument("storeId") { type = NavType.StringType })
        ) { b ->
            val storeId = b.arguments?.getString("storeId").orEmpty()
            CouponPromotionScreen(storeId = storeId, onBack = { nav.popBackStack() })
        }

        // 직원관리
        composable(
            "staff/{storeId}",
            arguments = listOf(navArgument("storeId") { type = NavType.StringType })
        ) { b ->
            val storeId = b.arguments?.getString("storeId").orEmpty()
            StaffManagementScreen(storeId = storeId, onBack = { nav.popBackStack() })
        }

        // 매출 대시보드 (→ 주문 상세로도 진입 가능하도록 훅 추가)
        composable(
            "sales/{storeId}",
            arguments = listOf(navArgument("storeId") { type = NavType.StringType })
        ) { b ->
            val storeId = b.arguments?.getString("storeId").orEmpty()
            SalesDashboardScreen(
                storeId = storeId,
                onBack = { nav.popBackStack() },
                onOpenOrderDetail = { orderId -> nav.navigate("orderDetail/$storeId/$orderId") } // ★ 추가 연결
            )
        }

        // 주문 목록 → 상세 진입
        composable(
            "orders/{storeId}",
            arguments = listOf(navArgument("storeId") { type = NavType.StringType })
        ) { b ->
            val storeId = b.arguments?.getString("storeId").orEmpty()
            OrdersScreen(
                storeId = storeId,
                onBack = { nav.popBackStack() },
                onOpenDetail = { orderId -> nav.navigate("orderDetail/$storeId/$orderId") } // ★ 연결
            )
        }

        // 주문 상세
        composable(
            route = "orderDetail/{storeId}/{orderId}",
            arguments = listOf(
                navArgument("storeId") { type = NavType.StringType },
                navArgument("orderId") { type = NavType.StringType },
            )
        ) { b ->
            val storeId = b.arguments?.getString("storeId").orEmpty()
            val orderId = b.arguments?.getString("orderId").orEmpty()
            OrderDetailScreen(
                storeId = storeId,
                orderId = orderId,
                onBack = { nav.popBackStack() }
            )
        }

        // 정산
        composable(
            "settlements/{storeId}",
            arguments = listOf(navArgument("storeId") { type = NavType.StringType })
        ) { b ->
            val storeId = b.arguments?.getString("storeId").orEmpty()
            SettlementsScreen(storeId = storeId, onBack = { nav.popBackStack() })
        }

        // 리포트
        composable(
            "reports/{storeId}",
            arguments = listOf(navArgument("storeId") { type = NavType.StringType })
        ) { b ->
            val storeId = b.arguments?.getString("storeId").orEmpty()
            ReportsScreen(storeId = storeId, onBack = { nav.popBackStack() })
        }

        // 설정
        composable("settings") {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}
