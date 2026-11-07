package com.axeyy.app;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.net.*;
import android.os.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class ProxyVpnService extends VpnService {
 public static final String START="com.axeyy.app.proxy.START", STOP="com.axeyy.app.proxy.STOP";
 private static final String CHANNEL="axeyy_proxy";
 private ParcelFileDescriptor tun;
 private static native boolean TProxyStartService(String path,int fd);
 private static native boolean TProxyStopService();
 private static native boolean TProxyIsRunning();
 static { System.loadLibrary("hev-socks5-tunnel"); }
 @Override public int onStartCommand(Intent intent,int flags,int id){
  if(intent!=null&&STOP.equals(intent.getAction())){stopTunnel();return START_NOT_STICKY;}
  notification(); if(tun==null)startTunnel(); return START_NOT_STICKY;
 }
 private void startTunnel(){
  android.content.SharedPreferences p=getSharedPreferences("axeyy_proxy",MODE_PRIVATE);
  String host=p.getString("host","").trim(),port=p.getString("port","1080");
  int n;try{n=Integer.parseInt(port);}catch(Exception e){stopSelf();return;}
  if(host.isEmpty()||n<1||n>65535){stopSelf();return;}
  try{
   Builder b=new Builder().setSession("AXEYY SOCKS5").setMtu(1500);
   b.addAddress("10.77.0.2",32); b.addRoute("0.0.0.0",0); b.addDnsServer("1.1.1.1");
   b.addDisallowedApplication(getPackageName());
   if(Build.VERSION.SDK_INT>=29){b.addAddress("fd77:77::2",128);b.addRoute("::",0);}
   tun=b.establish(); if(tun==null){stopSelf();return;}
   String user=p.getString("username",""),pass=p.getString("password","");
   String cfg="tunnel:\n  mtu: 1500\n  ipv4: 10.77.0.2\n  icmp: 'reply'\n"+
    "socks5:\n  address: '"+yaml(host)+"'\n  port: "+n+"\n  udp: 'udp'\n";
   if(!user.isEmpty()&&!pass.isEmpty())cfg+="  username: '"+yaml(user)+"'\n  password: '"+yaml(pass)+"'\n";
   cfg+="misc:\n  log-level: warn\n";
   File f=new File(getFilesDir(),"axeyy-socks5.yml");
   try(FileOutputStream o=new FileOutputStream(f)){o.write(cfg.getBytes(StandardCharsets.UTF_8));}
   if(!TProxyStartService(f.getAbsolutePath(),tun.getFd())){stopTunnel();}
  }catch(Exception e){stopTunnel();}
 }
 private String yaml(String s){return s.replace("\\","\\\\").replace("'","''").replace("\n","").replace("\r","");}
 private void stopTunnel(){
  try{if(TProxyIsRunning())TProxyStopService();}catch(Throwable ignored){}
  try{if(tun!=null)tun.close();}catch(Exception ignored){} tun=null;
  stopForeground(true);stopSelf();
 }
 @Override public void onRevoke(){stopTunnel();super.onRevoke();}
 @Override public void onDestroy(){try{if(tun!=null)stopTunnel();}finally{super.onDestroy();}}
 private void notification(){
  NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
  if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel(CHANNEL,"AXEYY Proxy",NotificationManager.IMPORTANCE_LOW));
  Intent stop=new Intent(this,ProxyVpnService.class);stop.setAction(STOP);
  PendingIntent pi=PendingIntent.getService(this,1,stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder nb=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
  Notification no=nb.setContentTitle("AXEYY Proxy").setContentText("Permintaan tunnel SOCKS5 berjalan").setSmallIcon(android.R.drawable.stat_sys_warning).addAction(new Notification.Action.Builder(null,"PUTUS",pi).build()).setOngoing(true).build();
  if(Build.VERSION.SDK_INT>=34)startForeground(22,no,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);else startForeground(22,no);
 }
}
