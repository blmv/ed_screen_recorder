package com.ed_screen_recorder.ed_screen_recorder;

import com.hbisoft.hbrecorder.HBRecorderListener;

public class RecorderListener implements HBRecorderListener {
    final EdScreenRecorderPlugin plugin;

    public RecorderListener(EdScreenRecorderPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void HBRecorderOnStart() {
        plugin.HBRecorderOnStart();
    }

    @Override
    public void HBRecorderOnComplete() {
        plugin.HBRecorderOnComplete();
    }

    @Override
    public void HBRecorderOnError(int i, String s) {
        plugin.HBRecorderOnError(s);
    }

    @Override
    public void HBRecorderOnPause() {
        // I don't think this is needed, it's just a callback for when HBRecorder decides to pause
        // if we were to recall onPause, we'll be in a loop.
    }

    @Override
    public void HBRecorderOnResume() {
        // I don't think this is needed, it's just a callback for when HBRecorder decides to pause
        // if we were to recall onResume, we'll be in a loop.
    }
}
