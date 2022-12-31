import 'dart:async';
import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:uuid/uuid.dart';

import '../models/record_output_model.dart';

class EdScreenRecorder {
  static const MethodChannel _channel = MethodChannel('ed_screen_recorder');

  Future<bool> isAvailable() async {
    final isAvailable = await _channel.invokeMethod<bool>(
      'isAvailable',
    );
    return isAvailable != null && isAvailable;
  }

  Future<bool> requestPermission() async {
    final hasPermission = await _channel.invokeMethod<bool>(
      'requestPermission',
    );
    return hasPermission != null && hasPermission;
  }

  /// [startRecordScreen] function takes the necessary parameters. The user can change all of these according to himself.
  /// Thanks to the [uuid] and [videoHash] variables, we can detect that each recorded video is unique from each other.
  /// After the process, we get a model result called [RecordOutput].
  /// On the front end we can see this result as [Map] .
  Future<Map<String, dynamic>> startRecordScreen({
    required bool audioEnable,
    required String fileName,
    bool addTimeCode = true,
    String fileExtension = "mp4",
    String? dirPathToSave,
    String? fileOutputFormat,
    int? videoBitrate,
    int? videoFrame,
  }) async {
    var uuid = const Uuid();
    String videoHash = uuid.v1().replaceAll('-', '');
    var dateNow = DateTime.now().microsecondsSinceEpoch;

    var response = await _channel.invokeMethod(
      'startRecordScreen',
      {
        "videohash": videoHash,
        "startdate": dateNow,
        "audioenable": audioEnable,
        "filename": fileName,
        "addtimecode": addTimeCode,
        "fileextension": fileExtension,
        if (dirPathToSave != null && dirPathToSave.isNotEmpty)
          "dirpathtosave": dirPathToSave,
        if (fileOutputFormat != null && fileOutputFormat.isNotEmpty)
          "fileoutputformat": fileOutputFormat,
        if (videoFrame != null) "videoframe": videoFrame,
        if (videoBitrate != null) "videobitrate": videoBitrate,
      },
    );

    var formatResponse = RecordOutput.fromJson(json.decode(response));
    if (kDebugMode) {
      debugPrint("""
      >>> Start Record Response Output:
      File: ${formatResponse.file}
      Event Name: ${formatResponse.eventName}
      Progressing: ${formatResponse.isProgress}
      Message: ${formatResponse.message}
      Success: ${formatResponse.success}
      Video Hash: ${formatResponse.videoHash}
      Start Date: ${formatResponse.startDate}
      End Date: ${formatResponse.endDate}
      """);
    }

    return formatResponse.toJson();
  }

  Future<Map<String, dynamic>> stopRecord() async {
    var dateNow = DateTime.now().microsecondsSinceEpoch;
    var response = await _channel.invokeMethod(
      'stopRecordScreen',
      {
        "enddate": dateNow,
      },
    );

    var formatResponse = RecordOutput.fromJson(json.decode(response));
    if (kDebugMode) {
      debugPrint("""
      >>> Stop Record Response Output:  
      File: ${formatResponse.file} 
      Event Name: ${formatResponse.eventName}  
      Progressing: ${formatResponse.isProgress} 
      Message: ${formatResponse.message} 
      Success: ${formatResponse.success} 
      Video Hash: ${formatResponse.videoHash} 
      Start Date: ${formatResponse.startDate} 
      End Date: ${formatResponse.endDate}
      """);
    }
    return formatResponse.toJson();
  }

  Future<void> pauseRecord() {
    return _channel.invokeMethod('pauseRecordScreen');
  }

  Future<void> resumeRecord() {
    return _channel.invokeMethod('resumeRecordScreen');
  }
}
