#!/bin/bash
sed -i '/val isNepali = MutableStateFlow(false)/a \
    val appUpdateInfo = MutableStateFlow<AppUpdateInfo?>(null)' app/src/main/java/com/example/ui/BoteCommunityViewModel.kt
