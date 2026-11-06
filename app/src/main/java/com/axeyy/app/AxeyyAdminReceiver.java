package com.axeyy.app;
import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
public class AxeyyAdminReceiver extends DeviceAdminReceiver {
 @Override public void onProfileProvisioningComplete(Context c, Intent i) { super.onProfileProvisioningComplete(c,i); android.app.admin.DevicePolicyManager d=(android.app.admin.DevicePolicyManager)c.getSystemService(Context.DEVICE_POLICY_SERVICE); d.setProfileName(new android.content.ComponentName(c,AxeyyAdminReceiver.class),"AXEYY Workspace"); d.setProfileEnabled(new android.content.ComponentName(c,AxeyyAdminReceiver.class)); }
}
