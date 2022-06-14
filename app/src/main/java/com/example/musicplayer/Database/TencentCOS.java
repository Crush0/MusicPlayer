package com.example.musicplayer.Database;

import android.content.Context;

import com.example.musicplayer.version.VersionControl;
import com.tencent.cos.xml.CosXmlService;
import com.tencent.cos.xml.CosXmlServiceConfig;
import com.tencent.cos.xml.listener.CosXmlProgressListener;
import com.tencent.cos.xml.listener.CosXmlResultListener;
import com.tencent.cos.xml.model.object.GetObjectRequest;

public class TencentCOS {
    private final Context context;
    private CosXmlService cosXmlService;

    public TencentCOS(Context context){
        this.context = context;
//        copyAssetAndWrite("key.json");
//        File file = new File(context.getCacheDir(),"key.json");
//        try {
//            FileInputStream fis = new FileInputStream(file);
//            int length = fis.available();
//            byte [] buffer = new byte[length];
//            fis.read(buffer);
//            String res = new String(buffer, StandardCharsets.UTF_8);
//            JSONObject object = (JSONObject) JSON.parse(res);
//            SecretId = object.getString("SecretId");
//            SecretKey = object.getString("SecretKey");
//            if(SecretId != null && SecretKey != null){
//                isPrepared = true;
//            }
//            fis.close();
        String region = "region";
        // 使用 HTTPS 请求, 默认为 HTTP 请求
        CosXmlServiceConfig serviceConfig = new CosXmlServiceConfig.Builder()
                .setRegion(region)
                .isHttps(true) // 使用 HTTPS 请求, 默认为 HTTP 请求
                .builder();
        cosXmlService = new CosXmlService(context, serviceConfig);
    }

//    private boolean copyAssetAndWrite(String fileName){
//        try {
//            File cacheDir=context.getCacheDir();
//            if (!cacheDir.exists()){
//                cacheDir.mkdirs();
//            }
//            File outFile =new File(cacheDir,fileName);
//            if (!outFile.exists()){
//                boolean res=outFile.createNewFile();
//                if (!res){
//                    return false;
//                }
//            }else {
//                if (outFile.length()>10){//表示已经写入一次
//                    return true;
//                }
//            }
//            InputStream is=context.getAssets().open(fileName);
//            FileOutputStream fos = new FileOutputStream(outFile);
//            byte[] buffer = new byte[1024];
//            int byteCount;
//            while ((byteCount = is.read(buffer)) != -1) {
//                fos.write(buffer, 0, byteCount);
//            }
//            fos.flush();
//            is.close();
//            fos.close();
//            return true;
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        return false;
//    }

    public void downloadFile(String cosPath, String savePath,String saveName, CosXmlProgressListener cosXmlProgressListener, CosXmlResultListener cosXmlResultListener){
        String bucket = "BucketName-APPID"; //存储桶名称，格式：BucketName-APPID
        GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, cosPath,
                savePath,saveName);
        getObjectRequest.setProgressListener(cosXmlProgressListener);
        cosXmlService.getObjectAsync(getObjectRequest, cosXmlResultListener);
    }
}
