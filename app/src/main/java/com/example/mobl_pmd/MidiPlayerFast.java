package com.example.mobl_pmd;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import kotlin.NotImplementedError;

// Использует внутренний статический синтезатор. По этой причине класс статический
public class MidiPlayerFast {
    private static final int SAMPLE_RATE = 96000;
    private static final int BUFFER_LENGTH = 9600; // 100 мс
    public static final String GET_NOTES_KEY = "pressed_notes";
    private final String[] NOTES = new String[] {"C-", "C#", "D-", "D#", "E-", "F-", "F#", "G-", "G#", "A-", "A#", "B-"};

    private static MusicVisualizer musicVisualizer;

    private static Sequence sequence;
    private static Track[] tracks;
    private static int[] trackIndexes;
    private static PlayingThread playingThread;

    private static long currentTick = 0;
    private static int ticksPerMeasure = 192;
    private static int BPM = 120; // Количество четвертных нот в минуту?

    private static boolean isLoaded = false;
    private static boolean isPlaying = false;
    private static boolean isFinished = true;

    private static boolean isInited;

    public static void init(MusicVisualizer visualizer) throws MidiUnavailableException {
        if (!isInited) {
            musicVisualizer = visualizer;

            OPNSynthesizer.init(SAMPLE_RATE);
            playingThread = new PlayingThread(SAMPLE_RATE, BUFFER_LENGTH);
            playingThread.start();

            isInited = true;
        }
    }

    public static void load(InputStream midiFile) throws InvalidMidiDataException, IOException, NotInitializedException {
        if (!isInited)
            throw new NotInitializedException();

        sequence = MidiSystem.getSequence(midiFile);
        tracks = sequence.getTracks();
        trackIndexes = new int[tracks.length];

        BPM = 120;
        ticksPerMeasure = 4 * sequence.getResolution();

        musicVisualizer.load(sequence);

        isLoaded = true;
    }
    public static boolean getIsLoaded() {
        return isLoaded;
    }
    public static void play() throws InvalidMidiDataException, NotInitializedException {
        if (!isInited)
            throw new NotInitializedException();
        if (!isLoaded)
            throw new InvalidMidiDataException("Midi data is not loaded.");

        isPlaying = true;
        musicVisualizer.play();

        if (isFinished) {
            OPNSynthesizer.releaseAll();
            Arrays.fill(trackIndexes, 0);
            currentTick = 0;
            isFinished = false;
        }
    }
    public static void pause() throws InvalidMidiDataException, NotInitializedException {
        if (!isInited)
            throw new NotInitializedException();
        if (!isLoaded)
            throw new InvalidMidiDataException("Midi data is not loaded.");

        isPlaying = false;
        musicVisualizer.pause();
    }
    public static void stop() throws InvalidMidiDataException, NotInitializedException {
        if (!isInited)
            throw new NotInitializedException();
        if (!isLoaded)
            throw new InvalidMidiDataException("Midi data is not loaded.");

        OPNSynthesizer.releaseAll();
        isPlaying = false;
        isFinished = true;
        musicVisualizer.stop();
    }
    public static void panic() {
        throw new NotImplementedError();
    }

    private static MidiEvent nextEvent() {
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
    private static void handleEvent(MidiEvent event) {
        MidiMessage msg = event.getMessage();
        if (msg instanceof MetaMessage) {
            MetaMessage metaMsg = (MetaMessage) msg;
            if (metaMsg.getType() == MetaMessage.TYPE_TEMPO) {
                byte[] data = metaMsg.getData();
                int tempo = (data[0] & 0xff) << 16 | (data[1] & 0xff) << 8 | (data[2] & 0xff);
                BPM = 60000000 / tempo;
            }
            if (metaMsg.getType() == MetaMessage.TYPE_END_OF_TRACK && currentTick == sequence.getTickLength()) {
                isPlaying = false;
                isFinished = true;
            }
            return;
        }

        byte[] message = event.getMessage().getMessage();
        byte status = message[0];

        int command = status & 0xf0;
        int channel = status & 0x0f;

        try {
            switch (command) {
                case ShortMessage.NOTE_OFF:
                    OPNSynthesizer.releaseNote(channel, message[1]);
                    break;
                case ShortMessage.NOTE_ON:
                    OPNSynthesizer.attackNote(channel, message[1], message[2]);
                    break;
                case ShortMessage.PROGRAM_CHANGE:
                    OPNSynthesizer.setInstrument(channel, message[1]);
                    break;
            }
        }
        catch (NotInitializedException ignored) {}
    }

    private static class PlayingThread extends Thread {
        private final AudioTrack audioTrack;
        private boolean isRunning = true;
        private final short[] audioBuffer;

        public PlayingThread(int sampleRate, int bufferLength) {
            this.audioBuffer = new short[bufferLength];

            this.audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferLength * 4,
                    AudioTrack.MODE_STREAM);
        }
        @Override
        public void run() {
            audioTrack.play();

            while (isRunning) {
                if (isPlaying) {
                    // Берём следующее событие
                    MidiEvent closestEvent = nextEvent();
                    if (closestEvent == null)
                        continue;
                    if (closestEvent == null)
                        throw new NullPointerException();

                    // Скипаем и заполняем буфер до следующего события
                    // Просчитать нужное количество семплов
                    long deltaTick = closestEvent.getTick() - currentTick;
                    int ticksInMinute = ticksPerMeasure * BPM / 4; // / 4;
                    int nsamples = (int)(SAMPLE_RATE * 60 * deltaTick / ticksInMinute);
                    readNext(nsamples);
                    currentTick = closestEvent.getTick();

                    // Выполняем команду
                    handleEvent(closestEvent);
                } else {
                    // Работаем по инерции
                    readNext(audioBuffer.length);
                }
            }

            audioTrack.stop();
        }
        public void safeStop() {
            isRunning = false;
        }

        private void readNext(int nsamples) {
            try {
                int samplesLeft = nsamples;
                for (; samplesLeft > audioBuffer.length; samplesLeft -= audioBuffer.length) {
                    OPNSynthesizer.readSound(audioBuffer, audioBuffer.length);
                    audioTrack.write(audioBuffer, 0, audioBuffer.length);
                }
                if (samplesLeft > 0)
                {
                    OPNSynthesizer.readSound(audioBuffer, samplesLeft);
                    audioTrack.write(audioBuffer, 0, samplesLeft);
                }
            } catch (NotInitializedException ignored) { }
        }
    }
}
