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
}
