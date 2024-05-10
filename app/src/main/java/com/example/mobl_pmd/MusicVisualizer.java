package com.example.mobl_pmd;

import android.os.Message;

import java.io.IOException;
import java.util.Arrays;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

import kotlin.NotImplementedError;

public class MusicVisualizer {
    private VisualThread visualThread;

    private Sequence sequence;
    private Track[] tracks;
    private int[] trackIndexes;
    private Thread playingThread;


    // Поля для визуализации и таймингов
    private long lastCircleTickTick;

    private long currentTick = 0;
    private int ticksPerMeasure = 192;
    private int BPM = 120; // Количество четвертных нот в минуту?

    private boolean isLoaded = false;
    private boolean isPlaying = false;
    private boolean isFinished = true;

    //private boolean isInited;

    public MusicVisualizer() {
        playingThread = new VisualThread();
        playingThread.start();
    }

    public void load(Sequence sequence) throws InvalidMidiDataException, IOException {
        this.sequence = sequence;
        tracks = sequence.getTracks();
        trackIndexes = new int[tracks.length];

        ticksPerMeasure = 4 * sequence.getResolution();

        isLoaded = true;
    }
    public boolean getIsLoaded() {
        return isLoaded;
    }
    public void play() throws InvalidMidiDataException {
        if (!isLoaded)
            throw new InvalidMidiDataException("Midi data is not loaded.");

        isPlaying = true;

        if (isFinished) {
            Arrays.fill(trackIndexes, 0);
            currentTick = 0;
            lastCircleTickTick = 0;
            isFinished = false;
        }
    }
    public void pause() throws InvalidMidiDataException {
        if (!isLoaded)
            throw new InvalidMidiDataException("Midi data is not loaded.");

        isPlaying = false;
    }
    public void stop() throws InvalidMidiDataException {
        if (!isLoaded)
            throw new InvalidMidiDataException("Midi data is not loaded.");

        isPlaying = false;
        isFinished = true;
    }
    public void panic() {
        throw new NotImplementedError();
    }

    private MidiEvent nextEvent() {
        // Находим ближайшее следующее событие
        MidiEvent closestEvent = null;
        int closestEventTrackIndx = -1;
        for (int i = 0; i < tracks.length; i++) {
            Track track = tracks[i];
            // Если трек не закончился
            if (trackIndexes[i] < track.size()) {
                MidiEvent event = track.get(trackIndexes[i]);

                if (closestEvent == null) {
                    // Запоминаем первое событие
                    closestEvent = event;
                    closestEventTrackIndx = i;
                } else {
                    // Сравниваем событие с предыдущим
                    if (event.getTick() < closestEvent.getTick()) {
                        closestEvent = event;
                        closestEventTrackIndx = i;
                    }
                }
            }
        }

        if (closestEventTrackIndx != -1)
            trackIndexes[closestEventTrackIndx]++;

        return closestEvent;
    }

    private class VisualThread extends Thread {
        private boolean isRunning = true;

        private long lastTickTime;
        private long lastCircleTickTime;
        private VisualCommand lastHandledCommand = VisualCommand.none;
        private boolean isCircleActive = false;

        private MidiEvent nextEvent = null;

        @Override
        public void run() {
            super.run();

            isRunning = true;
            while (isRunning) {
                // Останова
                if (isFinished) {
                    if (lastHandledCommand != VisualCommand.stop) {
                        Message msg = new Message();
                        msg.arg1 = PlayButtonFragment.CIRCLE_OFF_ARG1;
                        PlayButtonFragment.BroadcastMessage(msg);

                        lastHandledCommand = VisualCommand.stop;
                    }
                }
                // Пауз
                else if (!isPlaying) {
                    if (lastHandledCommand != VisualCommand.pause) {
                        lastCircleTickTime = System.currentTimeMillis();
                        isCircleActive = true;
                        lastHandledCommand = VisualCommand.pause;
                    }

                    // Моргаем каждые 250 мс
                    if (lastCircleTickTime + 250 < System.currentTimeMillis()) {
                        isCircleActive = !isCircleActive;
                        Message msg = new Message();
                        if (isCircleActive)
                            msg.arg1 = PlayButtonFragment.CIRCLE_ON_ARG1;
                        else
                            msg.arg1 = PlayButtonFragment.CIRCLE_OFF_ARG1;
                        PlayButtonFragment.BroadcastMessage(msg);

                        lastCircleTickTime = System.currentTimeMillis();
                    }
                }
                // Игр
                else {
                    nextPlayingTick();
                }

                try {
                    sleep(0);
                } catch (InterruptedException ignored) { }
            }
        }

        public void safeStop() {
            isRunning = false;
        }

        private void nextPlayingTick() {
            // @TODO: доделать
            long tickMs = 60000 / (BPM * ticksPerMeasure / 4);

            if (System.currentTimeMillis() >= lastTickTime + tickMs) {
                lastTickTime = System.currentTimeMillis();
                //lastTickTime += tickMs; //<- вот это не работает
                currentTick++;

                // Код обработки такта
                //...

                // Код обработки колеса воспроизведения
                if (currentTick >= lastCircleTickTick + ticksPerMeasure / 4) {
                    lastCircleTickTick = currentTick;

                    Message msg = new Message();
                    msg.arg1 = PlayButtonFragment.UPDATE_CIRCLE_ARG1;
                    PlayButtonFragment.BroadcastMessage(msg);
                }
            }
        }
    }
    private enum VisualCommand {
        none,
        stop,
        pause
    }
}
