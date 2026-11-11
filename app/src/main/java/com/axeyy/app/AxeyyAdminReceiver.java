package com.axeyy.app;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AxeyyAdminReceiver extends android.app.admin.DeviceAdminReceiver {
    private static final String TAG = "AXEYY-Profile";

    @Override
    public void onProfileProvisioningComplete(Context context, Intent intent) {
        super.onProfileProvisioningComplete(context, intent);

        DevicePolicyManager policyManager = (DevicePolicyManager)
            context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (policyManager == null) {
            Log.e(TAG, "DevicePolicyManager unavailable after profile provisioning.");
            return;
        }

        ComponentName admin = new ComponentName(context, AxeyyAdminReceiver.class);
        try {
            policyManager.setProfileName(admin, "AXEYY Workspace");
        } catch (RuntimeException e) {
            Log.e(TAG, "Could not set the Work Profile name.", e);
        }

        try {
            policyManager.setProfileEnabled(admin);
        } catch (RuntimeException e) {
            Log.e(TAG, "Could not enable the Work Profile.", e);
        }
    }
}
