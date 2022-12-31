import Flutter
import UIKit
import ReplayKit
import Photos

public class SwiftEdScreenRecorderPlugin: NSObject, FlutterPlugin {
    
    let recorder = RPScreenRecorder.shared()
    
    var videoOutputURL : URL?
    var videoWriter : AVAssetWriter?
    
    var audioInput:AVAssetWriterInput!
    var videoWriterInput : AVAssetWriterInput?
    
    var fileName: String = ""
    var dirPathToSave:NSString = ""
    var isAudioEnabled: Bool! = false;
    var addTimeCode: Bool! = false;
    var filePath: NSString = "";
    var videoFrame: Int?;
    var videoBitrate: Int?;
    var fileOutputFormat: String? = "";
    var fileExtension: String? = "";
    var videoHash: String! = "";
    var startDate: Int?;
    var endDate: Int?;
    var isProgress: Bool! = false;
    var eventName: String! = "";
    
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "ed_screen_recorder", binaryMessenger: registrar.messenger())
        let instance = SwiftEdScreenRecorderPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        if(call.method == "isAvailable") {
            result(isAvailable());
            return;
        } else if(call.method == "requestPermission"){
            requestPermission(result);
            return;
        } else if(call.method == "pauseRecordScreen" || call.method == "resumeRecordScreen") {
            // Pause/Resume isn't supported by ReplayKit...
            result(true);
            return;
        } else if(call.method == "startRecordScreen"){
            let args = call.arguments as? Dictionary<String, Any>
            self.isAudioEnabled=((args?["audioenable"] as? Bool?) ?? false)
            self.fileName=(args?["filename"] as? String)!+".mp4"
            self.dirPathToSave = ((args?["dirpathtosave"] as? NSString) ?? "")
            self.addTimeCode=((args?["addtimecode"] as? Bool?) ?? false)
            self.videoFrame=(args?["videoframe"] as? Int)
            self.videoBitrate=(args?["videobitrate"] as? Int)
            self.fileOutputFormat=(args?["fileoutputformat"] as? String)
            self.fileExtension=(args?["fileextension"] as? String)
            self.videoHash=(args?["videohash"] as? String)
            self.isProgress=Bool(true)
            self.eventName=String("startRecordScreen")
            var width = args?["width"]; // in pixels
            var height = args?["height"] // in pixels
            if UIDevice.current.orientation.isLandscape {
                if(width == nil || width is NSNull) {
                    width = Int32(UIScreen.main.nativeBounds.height); // pixels
                } else {
                    width = Int32(height as! Int32);
                }
                if(height == nil || height is NSNull) {
                    height = Int32(UIScreen.main.nativeBounds.width); // pixels
                } else {
                    height = Int32(width as! Int32);
                }
            }else{
                if(width == nil || width is NSNull) {
                    width = Int32(UIScreen.main.nativeBounds.width); // pixels
                } else {
                    width = Int32(width as! Int32);
                }
                if(height == nil || height is NSNull) {
                    height = Int32(UIScreen.main.nativeBounds.height); // pixels
                } else {
                    height = Int32(height as! Int32);
                }
            }
            startRecording(width: width as! Int32 ,height: height as! Int32,dirPathToSave:(self.dirPathToSave as NSString) as String) { _ in
                self.writeDataToResult(result);
            };
            self.startDate=Int(NSDate().timeIntervalSince1970 * 1_000)
            
        }else if(call.method == "stopRecordScreen"){
            
            if(videoWriter != nil){
                stopRecording() { _ in
                    self.writeDataToResult(result)
                }
                self.filePath=NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true)[0] as NSString
                self.isProgress=Bool(false)
                self.eventName=String("stopRecordScreen")
                self.endDate=Int(NSDate().timeIntervalSince1970 * 1_000)
            }
        }
    }
    
    func randomString(length: Int) -> String {
        let letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return String((0..<length).map{ _ in letters.randomElement()! })
    }
    
    func writeDataToResult(_ result: @escaping FlutterResult) {
        struct JsonObj : Codable {
            var success: Bool!
            var file: String
            var isProgress: Bool!
            var eventname: String!
            var message: String?
            var videohash: String!
            var startdate: Int?
            var enddate: Int?
        }
        
        let jsonObject: JsonObj = JsonObj(
            file: String("\(self.filePath)/\(self.fileName)"),
            isProgress: Bool(self.isProgress),
            eventname: String(self.eventName ?? "eventName"),
            videohash: String(self.videoHash),
            startdate: Int(self.startDate ?? Int(NSDate().timeIntervalSince1970 * 1_000)),
            enddate: Int(self.endDate ?? 0)
        )
        let encoder = JSONEncoder()
        let json = try! encoder.encode(jsonObject)
        let jsonStr = String(data:json,encoding: .utf8)
        result(jsonStr)
    }
    
    func isAvailable() -> Bool {
        return recorder.isAvailable;
    }
    
    func requestPermission(_ result: @escaping FlutterResult) {
        self.isAudioEnabled = false;
        self.fileName = "tmp.mp4";
        
        startRecording(width: 100, height: 100, dirPathToSave: "") {error in
            if(error == nil) {
                self.stopRecording() { _ in
                    result(error == nil);
                }
            }
        }
    }
    
    @objc func startRecording(width: Int32, height: Int32,dirPathToSave:String, startedHandler: ((Error?) -> Void)? = nil) {
        if(recorder.isAvailable){
            NSLog("startRecording: w x h = \(width) x \(height) pixels");
            if dirPathToSave != "" {
                self.filePath = dirPathToSave as NSString;
                self.videoOutputURL = URL(fileURLWithPath: String(self.filePath.appendingPathComponent(self.fileName)))
            } else {
                self.filePath = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true)[0] as NSString
                self.videoOutputURL = URL(fileURLWithPath: String(self.filePath.appendingPathComponent(self.fileName)))
            }
            do {
                if(FileManager.default.fileExists(atPath: videoOutputURL!.path)) {
                    try FileManager.default.removeItem(at: videoOutputURL!);
                }
            } catch let error as NSError{
                print("Error", error);
            }
            
            do {
                try videoWriter = AVAssetWriter(outputURL: videoOutputURL!, fileType: AVFileType.mp4)
            } catch let writerError as NSError {
                print("Error opening video file", writerError);
                videoWriter = nil;
                startedHandler?(writerError);
                return
            }
            
            if #available(iOS 11.0, *) {
                recorder.isMicrophoneEnabled = false;
                let videoSettings: [String : Any] = [
                    AVVideoCodecKey  : AVVideoCodecType.h264,
                    AVVideoWidthKey  : NSNumber.init(value: width),
                    AVVideoHeightKey : NSNumber.init(value: height),
                    AVVideoCompressionPropertiesKey: [
                        AVVideoProfileLevelKey: AVVideoProfileLevelH264HighAutoLevel,
                        //AVVideoAverageBitRateKey: self.videoBitrate!
                    ],
                ]
                self.videoWriterInput = AVAssetWriterInput(mediaType: AVMediaType.video, outputSettings: videoSettings);
                self.videoWriterInput?.expectsMediaDataInRealTime = true;
                self.videoWriter?.add(videoWriterInput!);
                if(isAudioEnabled){
                    let audioOutputSettings: [String : Any] = [
                        AVNumberOfChannelsKey : 2,
                        AVFormatIDKey : kAudioFormatMPEG4AAC,
                        AVSampleRateKey: 44100,
                        AVEncoderAudioQualityKey: AVAudioQuality.high.rawValue
                    ]
                    self.audioInput = AVAssetWriterInput(mediaType: AVMediaType.audio, outputSettings: audioOutputSettings)
                    self.audioInput?.expectsMediaDataInRealTime = true;
                    self.videoWriter?.add(audioInput!);
                }
                
                recorder.startCapture(handler: {
                    (cmSampleBuffer, rpSampleType, error) in guard error == nil else {
                        startedHandler?(error);
                        return;
                    }
                    switch rpSampleType {
                    case RPSampleBufferType.video:
                        if self.videoWriter?.status == AVAssetWriter.Status.unknown {
                            self.videoWriter?.startWriting()
                            self.videoWriter?.startSession(atSourceTime:  CMSampleBufferGetPresentationTimeStamp(cmSampleBuffer));
                            startedHandler?(nil);
                        }else if self.videoWriter?.status == AVAssetWriter.Status.writing {
                            if (self.videoWriterInput?.isReadyForMoreMediaData == true) {
                                if  self.videoWriterInput?.append(cmSampleBuffer) == false {
                                    print("Problems writing video")
                                    startedHandler?(NSError(domain: "", code: 500));
                                }
                            }
                        }
                    case RPSampleBufferType.audioApp:
                        if(self.isAudioEnabled){
                            if self.audioInput?.isReadyForMoreMediaData == true {
                                print("starting audio....");
                                if self.audioInput?.append(cmSampleBuffer) == false {
                                    print("Problems writing audio")
                                }
                            }
                        }
                    default:
                        break;
                    }
                }){(error) in guard error == nil else {
                    print("Screen record not allowed");
                    startedHandler?(error);
                    return
                }
                }
            }
        }
    }
    
    @objc func stopRecording(stoppedHandler: @escaping ((Error?) -> Void)) {
        if(recorder.isRecording){
            if #available(iOS 11.0, *) {
                recorder.stopCapture( handler: { (error) in
                    stoppedHandler(error);
                })
            } else {
                stoppedHandler(nil);
            }
            
            self.videoWriterInput?.markAsFinished();
            if(self.isAudioEnabled) {
                self.audioInput?.markAsFinished();
            }
            
            self.videoWriter?.finishWriting {
                stoppedHandler(nil);
            }
        }else{
            stoppedHandler(nil);
        }
        
    }
}
