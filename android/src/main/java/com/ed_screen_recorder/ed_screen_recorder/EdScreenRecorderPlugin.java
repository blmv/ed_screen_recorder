package com.ed_screen_recorder.ed_screen_recorder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.hbisoft.hbrecorder.HBRecorder;
import com.hbisoft.hbrecorder.HBRecorderListener;

import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;


/**
 * EdScreenRecorderPlugin
 */
public class EdScreenRecorderPlugin implements FlutterPlugin, ActivityAware, MethodCallHandler,
        PluginRegistry.RequestPermissionsResultListener,
        PluginRegistry.ActivityResultListener {

    private static final int SCREEN_RECORD_REQUEST_CODE = 777;
    final private HBRecorderListener listener = new RecorderListener(this);
    Result flutterResult;
    Activity activity;
    boolean isAudioEnabled;
    String fileName;
    String dirPathToSave;
    boolean addTimeCode;
    String filePath;
    Integer videoFrame;
    Integer videoBitrate;
    String fileOutputFormat;
    String fileExtension;
    boolean success;
    String videoHash;
    Long startDate;
    Long endDate;
    private MediaProjectionManager mediaProjectionManager;
    private FlutterPluginBinding flutterPluginBinding;
    private ActivityPluginBinding activityPluginBinding;
    private HBRecorder hbRecorder;
    private Intent permissionResultData = null;
    private Boolean startRecordingOnPermissionResult;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        this.flutterPluginBinding = binding;
        hbRecorder = new HBRecorder(flutterPluginBinding.getApplicationContext(), listener);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        this.flutterPluginBinding = null;
        if (hbRecorder != null) {
            hbRecorder.stopScreenRecording();
        }
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        this.activityPluginBinding = binding;
        setupChannels(flutterPluginBinding.getBinaryMessenger(), binding.getActivity());
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        onAttachedToActivity(binding);
    }

    @Override
    public void onDetachedFromActivity() {
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        this.flutterResult = result;
        switch (call.method) {
            case "isAvailable":
                result.success(isAvailable());
                break;
            case "requestPermission":
                requestPermission();
                break;
            case "startRecordScreen":
                try {
                    isAudioEnabled = Boolean.TRUE.equals(call.argument("audioenable"));
                    fileName = call.argument("filename");
                    dirPathToSave = call.argument("dirpathtosave");
                    addTimeCode = Boolean.TRUE.equals(call.argument("addtimecode"));
                    videoFrame = call.argument("videoframe");
                    videoBitrate = call.argument("videobitrate");
                    fileOutputFormat = call.argument("fileoutputformat");
                    fileExtension = call.argument("fileextension");
                    videoHash = call.argument("videohash");
                    startDate = call.argument("startdate");
                    customSettings(videoFrame, videoBitrate, fileOutputFormat, addTimeCode, fileName);
                    if (dirPathToSave != null) {
                        System.out.println(">>>>>>>>>>> 1");
                        setOutputPath(addTimeCode, fileName, dirPathToSave);
                    }
                    success = startRecordingScreen();
                } catch (Exception e) {
                    Map<Object, Object> dataMap = new HashMap<>();
                    dataMap.put("success", false);
                    dataMap.put("isProgress", false);
                    dataMap.put("file", "");
                    dataMap.put("eventname", "startRecordScreen Error");
                    dataMap.put("message", e.getMessage());
                    dataMap.put("videohash", videoHash);
                    dataMap.put("startdate", startDate);
                    dataMap.put("enddate", endDate);
                    JSONObject jsonObj = new JSONObject(dataMap);
                    result.success(jsonObj.toString());
                    System.out.println("Error: " + e.getMessage());
                }
                break;
            case "pauseRecordScreen":
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Log.d("pauseRecordScreen:", "Pausing screen recording");
                    hbRecorder.pauseScreenRecording();
                }
                break;
            case "resumeRecordScreen":
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Log.d("resumeRecordScreen:", "Resuming screen recording");
                    hbRecorder.resumeScreenRecording();
                }
                break;
            case "stopRecordScreen":
                endDate = call.argument("enddate");
                hbRecorder.stopScreenRecording();
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
            boolean hasPermission = resultCode == Activity.RESULT_OK;
            if (hasPermission) {
                permissionResultData = data;
                if (startRecordingOnPermissionResult) {
                    hbRecorder.startScreenRecording(permissionResultData, Activity.RESULT_OK);
                    return true;
                }
            }

            flutterResult.success(hasPermission);
        }
        return true;
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        return false;
    }

    private void setupChannels(BinaryMessenger messenger, Activity activity) {
        if (activityPluginBinding != null) {
            activityPluginBinding.addActivityResultListener(this);
            activityPluginBinding.addRequestPermissionsResultListener(this);
        }
        this.activity = activity;
        MethodChannel channel = new MethodChannel(messenger, "ed_screen_recorder");
        channel.setMethodCallHandler(this);
    }

    public void HBRecorderOnStart() {

        Log.e("Video Start:", "Start called");
        Map<Object, Object> dataMap = new HashMap<>();
        dataMap.put("success", success);
        dataMap.put("isProgress", true);
        if (dirPathToSave != null) {
            dataMap.put("file", filePath + "." + fileExtension);
        } else {
            dataMap.put("file", generateFileName(fileName, addTimeCode) + "." + fileExtension);
        }
        dataMap.put("eventname", "startRecordScreen");
        dataMap.put("message", "Started Video");
        dataMap.put("videohash", videoHash);
        dataMap.put("startdate", startDate);
        dataMap.put("enddate", null);
        JSONObject jsonObj = new JSONObject(dataMap);
        flutterResult.success(jsonObj.toString());
    }

    public void HBRecorderOnComplete() {
        Log.e("Video Complete:", "Complete called");
        Map<Object, Object> dataMap = new HashMap<>();
        dataMap.put("success", success);
        dataMap.put("isProgress", false);
        if (dirPathToSave != null) {
            dataMap.put("file", filePath + "." + fileExtension);
        } else {
            dataMap.put("file", generateFileName(fileName, addTimeCode) + "." + fileExtension);
        }
        dataMap.put("eventname", "stopRecordScreen");
        dataMap.put("message", "Paused Video");
        dataMap.put("videohash", videoHash);
        dataMap.put("startdate", startDate);
        dataMap.put("enddate", endDate);
        JSONObject jsonObj = new JSONObject(dataMap);
        try {
            flutterResult.success(jsonObj.toString());
        } catch (Exception e) {
            System.out.println("Error:" + e.getMessage());
        }
    }

    public void HBRecorderOnError(String reason) {
        Log.e("Video Error:", reason);
    }

    private Boolean isAvailable() {
        if (mediaProjectionManager != null) {
            return true;
        }

        try {
            mediaProjectionManager = (MediaProjectionManager) flutterPluginBinding
                    .getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);

            return mediaProjectionManager != null;
        } catch (Exception e) {
            System.out.println("isAvailable Error:" + e.getMessage());
            return false;
        }
    }

    private void requestPermission() {
        if(!isAvailable()) {
            flutterResult.error("Not Available", "Recording not available", null);
            return;
        }

        // Android 14 forces to call createScreenCaptureIntent before every capture session
//        if(permissionResultData != null) {
//            flutterResult.success(true);
//            return;
//        }

        startRecordingOnPermissionResult = false;

        hbRecorder.enableCustomSettings();

        Intent permissionIntent = mediaProjectionManager.createScreenCaptureIntent();
        activity.startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE);
    }


    private Boolean startRecordingScreen() {
        if (!isAvailable()) {
            flutterResult.error("Not Available", "Recording not available", null);
            return false;
        }

        startRecordingOnPermissionResult = true;

        try {
            if (permissionResultData == null) {
                hbRecorder.enableCustomSettings();

                Intent permissionIntent = mediaProjectionManager.createScreenCaptureIntent();
                activity.startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE);
            } else {
                hbRecorder.startScreenRecording(permissionResultData, Activity.RESULT_OK);
            }

            return true;
        } catch (Exception e) {
            System.out.println("startRecordingScreen Error:" + e.getMessage());
            return false;
        }
    }

    private void customSettings(Integer videoFrame, Integer videoBitrate, String fileOutputFormat, boolean addTimeCode,
                                String fileName) {
        hbRecorder.isAudioEnabled(isAudioEnabled);
        hbRecorder.setAudioSource("DEFAULT");
        hbRecorder.setVideoEncoder("DEFAULT");
        if (fileOutputFormat != null) {
            hbRecorder.setOutputFormat(fileOutputFormat);
        }
        if (videoFrame != null) {
            hbRecorder.setVideoFrameRate(videoFrame);
        }
        if (videoBitrate != null) {
            hbRecorder.setVideoBitrate(videoBitrate);
        }
        if (dirPathToSave == null) {
            System.out.println(">>>>>>>>>>> 2" + fileName);
            hbRecorder.setFileName(generateFileName(fileName, addTimeCode));
        }
    }

    private void setOutputPath(boolean addTimeCode, String fileName, String dirPathToSave) {
        hbRecorder.setFileName(generateFileName(fileName, addTimeCode));
        if (dirPathToSave != null && !dirPathToSave.equals("")) {
            File dirFile = new File(dirPathToSave);
            hbRecorder.setOutputPath(dirFile.getAbsolutePath());
            filePath = dirFile.getAbsolutePath() + "/" + generateFileName(fileName, addTimeCode);
        } else {
            hbRecorder.setOutputPath(
                    flutterPluginBinding.getApplicationContext().getExternalCacheDir().getAbsolutePath());
            filePath = flutterPluginBinding.getApplicationContext().getExternalCacheDir().getAbsolutePath() + "/"
                    + generateFileName(fileName, addTimeCode);
        }

    }

    private String generateFileName(String fileName, boolean addTimeCode) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault());
        Date curDate = new Date(System.currentTimeMillis());
        if (addTimeCode) {
            return fileName + "-" + formatter.format(curDate).replace(" ", "");
        } else {
            return fileName;
        }
    }
}
