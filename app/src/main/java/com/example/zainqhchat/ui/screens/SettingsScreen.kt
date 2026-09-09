package com.example.zainqhchat.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zainqhchat.domain.model.User
import com.example.zainqhchat.ui.theme.LuxuryBlackBg
import com.example.zainqhchat.ui.theme.LuxurySurfaceDark
import com.example.zainqhchat.ui.theme.TextPrimaryWhite
import com.example.zainqhchat.ui.viewmodels.AuthViewModel
import com.example.zainqhchat.ui.viewmodels.SettingsViewModel
import com.example.zainqhchat.ui.viewmodels.UserViewModel

@Composable
fun SettingsScreen(
    currentUser: User?,
    settingsViewModel: SettingsViewModel,
    userViewModel: UserViewModel,
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    onDeleteAccount: (onResult: (Boolean) -> Unit) -> Unit,
    onOpenAdmin: () -> Unit,
    onContactAdmin: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            Surface(color = LuxurySurfaceDark, shadowElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = TextPrimaryWhite)
                    }
                    Text("الإعدادات ⚙️", color = TextPrimaryWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        },
        containerColor = LuxuryBlackBg
    ) { paddingValues ->
        SettingsSection(
            currentUser = currentUser,
            settingsViewModel = settingsViewModel,
            userViewModel = userViewModel,
            authViewModel = authViewModel,
            onLogout = onLogout,
            onDeleteAccount = onDeleteAccount,
            onOpenAdmin = onOpenAdmin,
            onContactAdmin = onContactAdmin,
            modifier = Modifier.padding(paddingValues)
        )
    }
}
