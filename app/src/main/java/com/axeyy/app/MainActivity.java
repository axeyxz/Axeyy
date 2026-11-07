package com.axeyy.app;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.*;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.*;

public class MainActivity extends Activity {
 private final int bg=0xff080d17, panel=0xff121d2d, blue=0xff2878e8;
 private LinearLayout root; private DevicePolicyManager dpm; private ComponentName admin;
 public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);dpm=(DevicePolicyManager)getSystemService(DEVICE_POLICY_SERVICE);admin=new ComponentName(this,AxeyyAdminReceiver.class);render();}
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 private GradientDrawable shape(int c){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(20));return d;}
 private TextView txt(String s,int z,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
 private void gap(int n){View v=new View(this);root.addView(v,new LinearLayout.LayoutParams(1,dp(n)));}
 private void button(String s,Runnable r){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(-1);b.setBackground(shape(blue));b.setOnClickListener(v->r.run());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52));p.topMargin=dp(9);root.addView(b,p);}
 private void card(String title,String desc,Runnable r){LinearLayout c=new LinearLayout(this);c.setOrientation(1);c.setPadding(dp(16),dp(14),dp(16),dp(14));c.setBackground(shape(panel));c.addView(txt(title,17,-1,true));c.addView(txt(desc,12,0xffa9bad0,false));Button b=new Button(this);b.setText("BUKA");b.setAllCaps(false);b.setTextColor(0xff75b7ff);b.setBackgroundColor(Color.TRANSPARENT);b.setOnClickListener(v->r.run());c.addView(b);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(10);root.addView(c,p);}
 private void render(){boolean owner=dpm.isProfileOwnerApp(getPackageName());ScrollView sc=new ScrollView(this);sc.setBackgroundColor(bg);root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(22),dp(30),dp(22),dp(30));sc.addView(root);root.addView(txt("AXEYY",30,-1,true));root.addView(txt("YOUR WORKSPACE. YOUR CONTROL.",11,0xff70adff,true));gap(24);
 LinearLayout s=new LinearLayout(this);s.setOrientation(1);s.setPadding(dp(16),dp(17),dp(16),dp(17));s.setBackground(shape(panel));s.addView(txt(owner?"● WORK PROFILE AKTIF":"● WORK PROFILE BELUM AKTIF",15,owner?0xff50dfa8:0xffffc76a,true));s.addView(txt(owner?"AXEYY berjalan di ruang kerja Android.":"Buat ruang kerja menggunakan proses resmi Android.",13,0xffa9bad0,false));root.addView(s);
 button(owner?"WORK PROFILE AKTIF":"BUAT WORK PROFILE",()->provision());gap(25);root.addView(txt("WORKSPACE TOOLS",12,0xff70adff,true));
 card("App Manager","Lihat dan buka aplikasi pada profil tempat AXEYY berjalan.",()->apps());
 card("Proxy Manager","Konfigurasi SOCKS5 untuk Work Profile saja.",()->proxy());
 card("Profile Settings","Informasi dan status ruang kerja.",()->info("Work Profile dikelola oleh Android. AXEYY tidak membuat virtual machine."));
 setContentView(sc);}
 private void provision(){if(!dpm.isProvisioningAllowed(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)){info("Android tidak mengizinkan pembuatan Work Profile saat ini. Pastikan belum ada Work Profile lain.");return;}Intent i=new Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE);i.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,admin);try{startActivity(i);}catch(Exception e){info("Gagal membuka proses Work Profile: "+e.getMessage());}}
 private void apps(){Intent q=new Intent(Intent.ACTION_MAIN);q.addCategory(Intent.CATEGORY_LAUNCHER);java.util.List<android.content.pm.ResolveInfo> a=getPackageManager().queryIntentActivities(q,0);java.util.Collections.sort(a,(x,y)->String.valueOf(x.loadLabel(getPackageManager())).compareToIgnoreCase(String.valueOf(y.loadLabel(getPackageManager()))));String[] names=new String[a.size()];for(int n=0;n<a.size();n++)names[n]=String.valueOf(a.get(n).loadLabel(getPackageManager()));if(a.isEmpty()){info("Belum ada aplikasi launcher di profil ini.");return;}new android.app.AlertDialog.Builder(this).setTitle("Aplikasi di profil ini ("+a.size()+")").setItems(names,(d,w)->{Intent launch=getPackageManager().getLaunchIntentForPackage(a.get(w).activityInfo.packageName);if(launch!=null)startActivity(launch);}).setNegativeButton("TUTUP",null).show();}
 private static final int VPN_REQ=7821;
 private void proxy(){
  if(!dpm.isProfileOwnerApp(getPackageName())){info("Buka AXEYY dari dalam Work Profile. VPN ini hanya akan melindungi profil tempat AXEYY berjalan.");return;}
  android.content.SharedPreferences prefs=getSharedPreferences("axeyy_proxy",0);
  EditText h=new EditText(this),p=new EditText(this),u=new EditText(this),pw=new EditText(this);
  h.setHint("Server / IP");h.setText(prefs.getString("host",""));
  p.setHint("Port SOCKS5");p.setText(prefs.getString("port","1080"));
  u.setHint("Username (opsional)");u.setText(prefs.getString("username",""));
  pw.setHint("Password (opsional)");pw.setInputType(129);
  LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(15),0,dp(15),0);box.addView(h);box.addView(p);box.addView(u);box.addView(pw);
  new android.app.AlertDialog.Builder(this).setTitle("SOCKS5 Proxy").setMessage("VPN hanya berjalan pada user Android tempat AXEYY dipasang. Pasang AXEYY di Work Profile untuk membatasi proxy ke ruang kerja.").setView(box)
   .setPositiveButton("SIMPAN & HUBUNGKAN",(d,w)->{
    String host=h.getText().toString().trim(),port=p.getText().toString().trim();int n;
    try{n=Integer.parseInt(port);}catch(Exception e){info("Port harus angka.");return;}
    if(host.isEmpty()||n<1||n>65535){info("Alamat server atau port tidak valid.");return;}
    prefs.edit().putString("host",host).putString("port",port).putString("username",u.getText().toString()).putString("password",pw.getText().toString()).apply();
    Intent prep=android.net.VpnService.prepare(this);
    if(prep!=null)startActivityForResult(prep,VPN_REQ);else startProxy();
   }).setNeutralButton("PUTUSKAN",(d,w)->{Intent i=new Intent(this,ProxyVpnService.class);i.setAction(ProxyVpnService.STOP);startService(i);info("Permintaan memutuskan VPN dikirim.");})
   .setNegativeButton("BATAL",null).show();
 }
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==VPN_REQ){if(result==RESULT_OK)startProxy();else info("Izin VPN ditolak; proxy tidak tersambung.");}}
 private void startProxy(){try{Intent i=new Intent(this,ProxyVpnService.class);i.setAction(ProxyVpnService.START);if(android.os.Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);info("Permintaan tunnel dikirim. Uji IP dari aplikasi di Work Profile.");}catch(Exception e){info("Tidak dapat memulai VPN: "+e.getMessage());}}
 private void info(String s){new android.app.AlertDialog.Builder(this).setMessage(s).setPositiveButton("OK",null).show();}
}
