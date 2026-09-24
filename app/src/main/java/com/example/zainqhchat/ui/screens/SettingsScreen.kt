package com.example.zainqhchat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
import com.example.zainqhchat.ui.theme.LuxuryBorderGold
import com.example.zainqhchat.ui.theme.LuxurySurfaceCard
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
            Surface(color = LuxuryBlackBg) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(LuxurySurfaceCard)
                            .border(1.dp, LuxuryBorderGold, RoundedCornerShape(14.dp))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = TextPrimaryWhite)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text("الإعدادات ⚙️", color = TextPrimaryWhite, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
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
