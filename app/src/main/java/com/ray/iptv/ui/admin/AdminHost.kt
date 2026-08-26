package com.ray.iptv.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ray.iptv.data.admin.AdminUser
import com.ray.iptv.ui.RayViewModel

private enum class AdminPage {
    HOME, ORDERS, USERS, USER, CRASHES, ONLINE, NOTIFICATIONS
}

@Composable
fun AdminHost(
    vm: RayViewModel,
    tr: Boolean,
    onExit: () -> Unit
) {
    var page by remember { mutableStateOf(AdminPage.HOME) }
    var user by remember { mutableStateOf<AdminUser?>(null) }
    BackHandler {
        if (page == AdminPage.HOME) onExit() else {
            page = if (page == AdminPage.USER) AdminPage.USERS else AdminPage.HOME
            if (page != AdminPage.USER) user = null
        }
    }
    androidx.tv.material3.MaterialTheme {
        when (page) {
            AdminPage.HOME -> AdminPanelScreen(
                vm = vm,
                tr = tr,
                onBack = onExit,
                onOrders = { page = AdminPage.ORDERS },
                onUsers = { page = AdminPage.USERS },
                onCrashes = { page = AdminPage.CRASHES },
                onOnline = { page = AdminPage.ONLINE },
                onNotifications = { page = AdminPage.NOTIFICATIONS }
            )
            AdminPage.ORDERS -> AdminOrdersScreen(vm, tr) { page = AdminPage.HOME }
            AdminPage.USERS -> AdminUsersScreen(
                vm = vm,
                tr = tr,
                onBack = { page = AdminPage.HOME },
                onOpen = {
                    user = it
                    page = AdminPage.USER
                }
            )
            AdminPage.USER -> {
                val current = user
                if (current != null) {
                    AdminUserDetailScreen(vm, tr, current) {
                        page = AdminPage.USERS
                        user = null
                    }
                } else {
                    AdminUsersScreen(
                        vm = vm,
                        tr = tr,
                        onBack = { page = AdminPage.HOME },
                        onOpen = {
                            user = it
                            page = AdminPage.USER
                        }
                    )
                }
            }
            AdminPage.CRASHES -> AdminCrashesScreen(vm, tr) { page = AdminPage.HOME }
            AdminPage.ONLINE -> AdminOnlineScreen(vm, tr) { page = AdminPage.HOME }
            AdminPage.NOTIFICATIONS -> AdminNotificationsScreen(vm, tr) { page = AdminPage.HOME }
        }
    }
}
