package com.example.mobl_pmd;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Message;

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
    private static final int BUFFER_LENGTH = 9600 * 2; // Размер звукового буфера в байтах

    private static Sequence sequence;
    private static Track[] tracks;
    private static int[] trackIndexes;
    private static PlayingThread playingThread;

    private static int ticksPerMeasure = 192;
    private static int BPM = 120; // Количество четвертных нот в минуту?

    private static boolean isLoaded = false;
    private static boolean isPlaying = false;
    private static boolean isFinished = true;

    private static boolean isInited;

    public static void init() throws MidiUnavailableException {
        if (!isInited) {
            OPNSynthesizer.init(SAMPLE_RATE);
            SoundPlayingHandler.init();
            VisualizingHandler.init();
            playingThread = new PlayingThread();
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

        isLoaded = true;
        stop();
    }

    public static boolean getIsLoaded() { return isLoaded; }
    public static boolean getIsPlaying() { return isPlaying; }
    public static boolean getIsFinished() { return isFinished; }

    public static void play() throws InvalidMidiDataException, NotInitializedException {
        if (!isInited)
            throw new NotInitializedException();
        if (!isLoaded)
            throw new InvalidMidiDataException("Midi data is not loaded.");
        if (isPlaying)
            return;

        if (isFinished) {
            OPNSynthesizer.releaseAll();

            Arrays.fill(trackIndexes, 0);
            playingThread.startPlayback(true);
            isFinished = false;
        }
        else {
            playingThread.startPlayback(false);
        }

        VisualizingHandler.showPlay();
        isPlaying = true;
    }
    public static void pause() throws InvalidMidiDataException, NotInitializedException {
        if (!isInited)
            throw new NotInitializedException();
        if (!isLoaded)
            throw new InvalidMidiDataException("Midi data is not loaded.");

        VisualizingHandler.showPause();
        isPlaying = false;
    }
    public static void stop() throws InvalidMidiDataException, NotInitializedException {
        if (!isInited)
            throw new NotInitializedException();
        if (!isLoaded)
            throw new InvalidMidiDataException("Midi data is not loaded.");

        OPNSynthesizer.releaseAll();
        VisualizingHandler.showPause();
        isPlaying = false;
        isFinished = true;
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
    private static void handleEvent(MidiEvent event, long currentTick) {
        MidiMessage msg = event.getMessage();
        if (msg instanceof MetaMessage) {
            MetaMessage metaMsg = (MetaMessage) msg;
            if (metaMsg.getType() == MetaMessage.TYPE_TEMPO) {
                byte[] data = metaMsg.getData();
                int tempo = (data[0] & 0xff) << 16 | (data[1] & 0xff) << 8 | (data[2] & 0xff);
                BPM = 60000000 / tempo;
            }
            if (metaMsg.getType() == MetaMessage.TYPE_END_OF_TRACK && currentTick >= sequence.getTickLength()) {
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


    private static class SoundPlayingHandler {
        private static AudioTrack audioTrack;
        private static short[] audioBuffer;

        protected static void init() {
            audioBuffer = new short[SAMPLE_RATE];

            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, BUFFER_LENGTH,
                    AudioTrack.MODE_STREAM);
        }
        protected static void play() {
            audioTrack.play();
        }
        protected static void stop() {
            audioTrack.stop();
        }
        protected static void update(long deltaTicks) {
            int ticksInMinute = ticksPerMeasure * BPM / 4;
            int nsamples = (int)(SAMPLE_RATE * 60 * deltaTicks / ticksInMinute);
            readNext(nsamples);
        }
        protected static void readNext(int nsamples) {
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
    public static class VisualizingHandler {
        private static ChannelVisualFragment.ChannelVisualHandler[] channelVisualHandlers = new ChannelVisualFragment.ChannelVisualHandler[16];
        private static PlayButtonFragment.PlayButtonHandler playButtonHandler;

        protected static void init() {

        }
        protected static void update(long deltaTicks) {

        }
        protected static void showEvent(MidiEvent event) {
            MidiMessage msg = event.getMessage();
            if (msg instanceof MetaMessage) {
                MetaMessage metaMsg = (MetaMessage) msg;
                if (metaMsg.getType() == MetaMessage.TYPE_TEMPO) {
                    byte[] data = metaMsg.getData();
                    int tempo = (data[0] & 0xff) << 16 | (data[1] & 0xff) << 8 | (data[2] & 0xff);
                    BPM = 60000000 / tempo;
                }
                // Конец одной из дорожек (не всей песни)
                /*if (metaMsg.getType() == MetaMessage.TYPE_END_OF_TRACK && currentTick > sequence.getTickLength()) {
                    isPlaying = false;
                    isFinished = true;
                }*/
                return;
            }

            byte[] message = event.getMessage().getMessage();
            byte status = message[0];

            int command = status & 0xf0;
            int channel = status & 0x0f;

            Message handlerMsg = new Message();
            switch (command) {
                case ShortMessage.NOTE_OFF:
                    handlerMsg.arg1 = ChannelVisualFragment.ChannelVisualHandler.ARG1_RELEASE;
                    handlerMsg.arg2 = message[1];
                    if (channelVisualHandlers[channel] != null)
                        channelVisualHandlers[channel].sendMessage(handlerMsg);
                    break;
                case ShortMessage.NOTE_ON:
                    handlerMsg.arg1 = ChannelVisualFragment.ChannelVisualHandler.ARG1_ATTACK;
                    handlerMsg.arg2 = message[1];
                    Message setVolMsg = new Message();
                    setVolMsg.arg1 = ChannelVisualFragment.ChannelVisualHandler.ARG1_SET_VL;
                    setVolMsg.arg2 = message[2];
                    if (channelVisualHandlers[channel] != null) {
                        channelVisualHandlers[channel].sendMessage(handlerMsg);
                        channelVisualHandlers[channel].sendMessage(setVolMsg);
                    }
                    break;
                case ShortMessage.PROGRAM_CHANGE:
                    handlerMsg.arg1 = ChannelVisualFragment.ChannelVisualHandler.ARG1_SET_TN;
                    handlerMsg.arg2 = message[1];
                    if (channelVisualHandlers[channel] != null)
                        channelVisualHandlers[channel].sendMessage(handlerMsg);
                    break;
            }
        }
        public static void showPlay() {
            if (playButtonHandler != null) {
                Message msg = new Message();
                msg.arg1 = PlayButtonFragment.PlayButtonHandler.SET_PLAYING_ARG1;
                playButtonHandler.sendMessage(msg);
            }
        }
        protected static void showPause() {
            if (playButtonHandler != null) {
                Message msg = new Message();
                msg.arg1 = PlayButtonFragment.PlayButtonHandler.SET_PAUSED_ARG1;
                playButtonHandler.sendMessage(msg);
            }
        }
        protected static void requestRedraw() {
            for (ChannelVisualFragment.ChannelVisualHandler channelVisualHandler : channelVisualHandlers) {
                if (channelVisualHandler == null)
                    continue;
                Message msg = new Message();
                msg.arg1 = ChannelVisualFragment.ChannelVisualHandler.ARG1_REDRAW;
                channelVisualHandler.sendMessage(msg);
            }
        }

        public static void addFragmentHandler(int channelIndex, ChannelVisualFragment.ChannelVisualHandler handler) {
            channelVisualHandlers[channelIndex] = handler;
        }
        public static void setPlayButtonHandler(PlayButtonFragment.PlayButtonHandler handler) {
            playButtonHandler = handler;
        }
    }


    private static class PlayingThread extends Thread {
        private static final long NANOS_IN_MINUTE = 60000000000L;

        private boolean isRunning = true;
        private MidiEvent nextEvent;
        private long onpauseTickValue;
        public long currentTick;
        public long playingStartTimeNanos;
        public long lastAudioUpdateTick;

        private final Object timerSemaphore = "Bluh-bluh-bluh";

        public void startPlayback(boolean restart) {
            synchronized (timerSemaphore) {
                if (restart)
                {
                    onpauseTickValue = 0;
                    nextEvent = null;
                }
                else
                    onpauseTickValue = getPlaybackTicks();
                currentTick = 0;

                playingStartTimeNanos = System.nanoTime();
                lastAudioUpdateTick = getPlaybackTicks();
            }
        }
        public long getPlaybackTicks() {
            return currentTick + onpauseTickValue;
        }

        @Override
        public void run() {
            SoundPlayingHandler.play();
            setPriority(Thread.MAX_PRIORITY);

            while (isRunning) {
                if (isPlaying) {
                    // # Новый алгоритм воспроизведения:
                    // Берём событие
                    // Сверяем с текущими тактами
                    // Если событие должно произойти сейчас,
                        // Выполняем событие
                        // Обновляем визуализаторы
                        // Берём следующее событие
                        // Если оно уже происходит или происходило, повторить
                    // Заполняем звуковой буфер на пройденное количество тактов


                    if (nextEvent == null) {
                        nextEvent = nextEvent();
                        if (nextEvent == null) {
                            // @TODO: Thread.sleep(...);
                            continue;
                        }
                    }

                    synchronized (timerSemaphore) {
                        // Подсчитываем такты
                        long nanosFromStart = System.nanoTime() - playingStartTimeNanos;
                        long currentTick = (ticksPerMeasure * BPM / 4) * nanosFromStart / NANOS_IN_MINUTE;
                        long deltaTick = currentTick - this.currentTick;
                        this.currentTick = currentTick;

                        while (getPlaybackTicks() >= nextEvent.getTick()) {
                            // Обновляем аудиобуфер до нужного количества тактов
                            // @TODO: (Пока не проиграют все предыдущие такты, пауза не настанет.) Дописать обработку паузы и остановки
                            // (Именно поэтому после паузы повторная пауза требует задержки)
                            // (Зато имеется большая точность во времени проигрывания ноты)
                            if (nextEvent.getTick() > lastAudioUpdateTick)
                                SoundPlayingHandler.update(nextEvent.getTick() - lastAudioUpdateTick);
                            lastAudioUpdateTick = nextEvent.getTick();

                            handleEvent(nextEvent, getPlaybackTicks());
                            VisualizingHandler.showEvent(nextEvent);

                            nextEvent = nextEvent();
                            if (nextEvent == null)
                                break;
                        }

                        // Обновляем аудиобуфер до текущего количества тактов
                        // Также перерисовываем все пианино
                        if (getPlaybackTicks() > lastAudioUpdateTick)
                            SoundPlayingHandler.update(getPlaybackTicks() - lastAudioUpdateTick);
                        lastAudioUpdateTick = getPlaybackTicks();
                        VisualizingHandler.requestRedraw();
                    }
                } else {
                    // Работаем по инерции
                    // @TODO: как-то переделать.
                    SoundPlayingHandler.readNext(BUFFER_LENGTH);
                }
            }

            SoundPlayingHandler.stop();
        }
        public void safeStop() {
            isRunning = false;
        }
    }
}
