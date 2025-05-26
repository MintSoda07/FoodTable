//package com.bcu.foodtable.JetpackCompose//package com.bcu.foodtable.JetpackCompose//    package com.bcu.foodtable.JetpackCompose
//
//    import androidx.compose.runtime.Composable
//    import androidx.compose.runtime.remember
//    import androidx.navigation.NavHostController
//    import androidx.navigation.compose.NavHost
//    import androidx.navigation.compose.composable
//    import androidx.navigation.navArgument
//    import com.bcu.foodtable.JetpackCompose.Channel.ChannelScreen
//    import com.bcu.foodtable.ui.subscribeNavMenu.ChannelViewModel
//    import com.bcu.foodtable.JetpackCompose.Channel.SubscribeScreen
//    import com.bcu.foodtable.ui.subscribeNavMenu.SubscribeViewModel
//    import com.bcu.foodtable.di.AppContainer
//    import com.bcu.foodtable.ui.home.HomeScreen
//    import androidx.lifecycle.viewmodel.compose.viewModel
//
//
//
//
//    package com.bcu.foodtable.JetpackCompose
//
//    import androidx.compose.runtime.Composable
//    import androidx.compose.runtime.remember
//    import androidx.navigation.NavHostController
//    import androidx.navigation.compose.NavHost
//    import androidx.navigation.compose.composable
//    import androidx.navigation.navArgument
//    import com.bcu.foodtable.JetpackCompose.Channel.ChannelScreen
//    import com.bcu.foodtable.JetpackCompose.Channel.SubscribeScreen
//    import com.bcu.foodtable.di.AppContainer
//    import com.bcu.foodtable.ui.home.HomeScreen
//    import com.bcu.foodtable.ui.subscribeNavMenu.ChannelViewModel
//    import com.bcu.foodtable.ui.subscribeNavMenu.SubscribeViewModel
//    import com.bcu.foodtable.JetpackCompose.HomeViewModel
//
//    @Composable
//    fun AppNavGraph(
//        navController: NavHostController,
//        appContainer: AppContainer
//    ) {
//        NavHost(
//            navController = navController,
//            startDestination = "home"
//        ) {
//            composable("home") {
//                val homeViewModel = remember {
//                    HomeViewModel(appContainer.channelRepository)
//                }
//                HomeScreen(
//                    viewModel = homeViewModel,
//                    navController = navController,
//                    repository = appContainer.channelRepository
//                )
//            }
//
//            composable("subscribe") {
//                val subscribeViewModel = remember {
//                    SubscribeViewModel(appContainer.channelRepository)
//                }
//                SubscribeScreen(
//                    viewModel = subscribeViewModel,
//                    navController = navController
//                )
//            }
//
//            composable(
//                "channel/{channelName}",
//                arguments = listOf(navArgument("channelName") { defaultValue = "" })
//            ) { backStackEntry ->
//                val channelName = backStackEntry.arguments?.getString("channelName") ?: ""
//                val channelViewModel = remember {
//                    ChannelViewModel(appContainer.channelRepository)
//                }
//
//                ChannelScreen(
//                    channelName = channelName,
//                    viewModel = channelViewModel,
//                    onRecipeClick = { recipeId ->
//                        navController.navigate("recipe/$recipeId")
//                    },
//                    onWriteClick = { chName ->
//                        navController.navigate("write/$chName")
//                    }
//                )
//            }
//
//            // recipe, write 경로는 필요시 아래 주석 해제 후 구현
////        composable(
////            "recipe/{recipeId}",
////            arguments = listOf(navArgument("recipeId") {})
////        ) { backStackEntry ->
////            val recipeId = backStackEntry.arguments?.getString("recipeId") ?: ""
////            RecipeViewScreen(recipeId = recipeId)
////        }
////
////        composable(
////            "write/{channelName}",
////            arguments = listOf(navArgument("channelName") {})
////        ) { backStackEntry ->
////            val channelName = backStackEntry.arguments?.getString("channelName") ?: ""
////            WriteScreen(channelName = channelName)
////        }
//        }
//    }
//
