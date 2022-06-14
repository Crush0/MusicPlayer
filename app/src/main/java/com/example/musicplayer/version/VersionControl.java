package com.example.musicplayer.version;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import com.ejlchina.data.Mapper;
import com.ejlchina.okhttps.Download;
import com.ejlchina.okhttps.HTTP;
import com.ejlchina.okhttps.OkHttps;
import com.ejlchina.okhttps.Process;
import com.ejlchina.okhttps.fastjson.FastjsonMsgConvertor;
import com.example.musicplayer.Database.TencentCOS;
import com.example.musicplayer.MainActivity;
import com.tencent.cos.xml.exception.CosXmlClientException;
import com.tencent.cos.xml.exception.CosXmlServiceException;
import com.tencent.cos.xml.listener.CosXmlResultListener;
import com.tencent.cos.xml.model.CosXmlRequest;
import com.tencent.cos.xml.model.CosXmlResult;

import java.io.File;


public class VersionControl {

    public static class Version{
        public String name;
        public String body;
        public String publishedAt;
        public String downloadUrl;
        public Long size;
    }

    public static final String APP_VERSION = "2.3";
    public TencentCOS tencentCOS;

    private Context context;

    public VersionControl(Context context) {
        this.context = context;
        tencentCOS = new TencentCOS(this.context);
    }

    @Nullable
    public  Version hasLeastVersion(){
        try {
            HTTP http = HTTP.builder()
                    .baseUrl("https://api.github.com/repos/crush0/MusicPlayer/releases")
                    .bodyType(OkHttps.JSON)
                    .addMsgConvertor(new FastjsonMsgConvertor())
                    .build();
            Mapper versionMapper = http.sync("/latest")
                    .get()
                    .getBody()
                    .toMapper();
            Version version = new Version();
            version.name = versionMapper.getString("name");
            version.body = versionMapper.getString("body");
            version.publishedAt = versionMapper.getString("published_at");
            version.downloadUrl = versionMapper.getArray("assets").getMapper(0).getString("browser_download_url");
            version.size = versionMapper.getArray("assets").getMapper(0).getLong("size");
            if (compareVersion(version.name)) {
                return version;
            } else {
                return null;
            }
        }
        catch (Exception e){
            Looper.prepare();
            Toast.makeText(context, "获取版本信息失败", Toast.LENGTH_SHORT).show();
            Looper.loop();
            return null;
        }
    }
    final int NEW_MESSAGE_ID = 0;
    @RequiresApi(api = Build.VERSION_CODES.O)
    private static boolean isHasInstallPermissionWithO(Context context) {
        return context != null && context.getPackageManager().canRequestPackageInstalls();
    }

    @SuppressLint("DefaultLocale")
    public void useOKHTTP(Activity activity, String url, File file){
        NotificationCompat.Builder builder = ((MainActivity)activity).builder;
        NotificationManager notificationManager = ((MainActivity)activity).notificationManager;
        OkHttps.sync(url)
                .get()
                .getBody()
                .stepBytes(512)
                .setOnProcess((Process process) -> {
                    long doneBytes = process.getDoneBytes();   // 已下载字节数
                    long totalBytes = process.getTotalBytes();
                    ; // 当前下载速度
                    builder.setProgress((int)totalBytes, (int)doneBytes, false);
                    System.out.println("doneBytes:" + doneBytes + " totalBytes:" + totalBytes);
                    builder.setContentText("下载" + String.format("%.2f", process.getRate() * 100.0) + "%");
                    notificationManager.notify(NEW_MESSAGE_ID, builder.build());

                })
                .toFile(file)
                .setOnComplete((v) -> {
                    if (v == Download.Status.DONE) {
                        notificationManager.cancel(NEW_MESSAGE_ID);
                        Uri uri;
                        if (android.os.Build.VERSION.SDK_INT >= 24) {
                            uri = FileProvider.getUriForFile(activity.getApplicationContext(), "com.example.musicplayer.fileprovider", file);
                        } else {
                            uri = Uri.fromFile(file);
                        }
                        if (Build.VERSION.SDK_INT >= 26) {
                            boolean hasInstallPermission = isHasInstallPermissionWithO(activity);
                            if (!hasInstallPermission) {
                                startInstallPermissionSettingActivity(activity);
                                Toast.makeText(activity, "请允许歌吧安装未知来源应用权限", Toast.LENGTH_LONG).show();
                                return;
                            }
                        }
                        Intent intent = new Intent("android.intent.action.VIEW");
                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        if (Build.VERSION.SDK_INT >= 24) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        }
                        intent.setDataAndType(uri, "application/vnd.android.package-archive");
                        activity.startActivity(intent);
                    }
                }).start();
    }

    @SuppressLint("DefaultLocale")
    public void downloadAPK(Activity activity, String cosPath, String savePath,String saveName,File file){
        try {
            Looper.prepare();
            NotificationCompat.Builder builder = ((MainActivity)activity).builder;
            NotificationManager notificationManager = ((MainActivity)activity).notificationManager;
            tencentCOS.downloadFile(cosPath, savePath,saveName, (progress, max) -> {
                ; // 当前下载速度
                builder.setProgress((int)max, (int)progress, false);
                System.out.println("doneBytes:" + progress + " totalBytes:" + max);
                builder.setContentText("下载" + String.format("%.2f", progress/max * 100.0) + "%");
                notificationManager.notify(NEW_MESSAGE_ID, builder.build());
            }, new CosXmlResultListener() {
                @Override
                public void onSuccess(CosXmlRequest cosXmlRequest, CosXmlResult cosXmlResult) {
                    notificationManager.cancel(NEW_MESSAGE_ID);
                    Uri uri;
                    if (android.os.Build.VERSION.SDK_INT >= 24) {
                        uri = FileProvider.getUriForFile(activity.getApplicationContext(), "com.example.musicplayer.fileprovider", file);
                    } else {
                        uri = Uri.fromFile(file);
                    }
                    if (Build.VERSION.SDK_INT >= 26) {
                        boolean hasInstallPermission = isHasInstallPermissionWithO(activity);
                        if (!hasInstallPermission) {
                            startInstallPermissionSettingActivity(activity);
                            Toast.makeText(activity, "请允许歌吧安装未知来源应用权限", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    Intent intent = new Intent("android.intent.action.VIEW");
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    if (Build.VERSION.SDK_INT >= 24) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    }
                    intent.setDataAndType(uri, "application/vnd.android.package-archive");
                    activity.startActivity(intent);
                }

                @Override
                public void onFail(CosXmlRequest cosXmlRequest, @Nullable CosXmlClientException e, @Nullable CosXmlServiceException e1) {
                    Toast.makeText(activity, "下载失败", Toast.LENGTH_LONG).show();
                }
            });
            Looper.loop();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(activity, "下载失败", Toast.LENGTH_LONG).show();
        }
    }

    private static void startInstallPermissionSettingActivity(Context context) {
        if (context != null) {
            Uri uri = Uri.parse("package:" + context.getPackageName());
            Intent intent = new Intent("android.settings.MANAGE_UNKNOWN_APP_SOURCES", uri);
            ((Activity) context).startActivity(intent);
        }
    }

    public boolean compareVersion(String version1){
        int first1 = Integer.parseInt(version1.split("\\.")[0]);
        int first2 = Integer.parseInt(VersionControl.APP_VERSION.split("\\.")[0]);
        int second1 = Integer.parseInt(version1.split("\\.")[1]);
        int second2 = Integer.parseInt(VersionControl.APP_VERSION.split("\\.")[1]);
        if(first1 > first2){
            return true;
        } else if(first1 == first2){
            if(second1 > second2){
                return true;
            } else return second1 != second2;
        } else {
            return false;
        }
    }
}
