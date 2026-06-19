package com.abhay.firebasekeys

import android.app.Application
import io.branch.referral.Branch

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Branch.getAutoInstance(this, NativeKeys.getBranchKey())
    }
}
