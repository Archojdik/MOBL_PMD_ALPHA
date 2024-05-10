package com.example.mobl_pmd;

public class OPNSynthesizer {
    private static int masterClock;
    //private static int sampleRate;
    private static boolean isInited;

    public static void init(int sampleRate) {
        if (!isInited) {
            //OPNSynthesizer.sampleRate = sampleRate;
            int samplingTimes = (8000000 / sampleRate);
            masterClock = sampleRate * samplingTimes;

            native_initSynths(masterClock, sampleRate);

            isInited = true;
        }
    }
    // - Может, здесь сделать метод для очистки памяти?

    public static void attackNote(int channel, int midiNoteNumber, int velocity) throws NotInitializedException {
        if (!isInited)
            throw new NotInitializedException();

        double freq = calcMidiFreq(midiNoteNumber);
        int octave = calcMidiOctave(midiNoteNumber);

        int mmlVelocity = (127 - velocity) / 4; //4 //6

        int fnum = calcOPNFNum(freq, octave);
        native_attackKey(channel, midiNoteNumber, fnum, octave, mmlVelocity);
    }
    public static void releaseNote(int channel, int midiNoteNumber) throws NotInitializedException {
        if (!isInited)
            throw new NotInitializedException("init() method has to be invoked first.");

        native_releaseKey(channel, midiNoteNumber);
    }
    public static void releaseAll() throws NotInitializedException {
        if (!isInited)
            throw new NotInitializedException("init() method has to be invoked first.");

        native_releaseAllkeys();
    }

    public static void setInstrument(int ch, int instrumentNum) throws NotInitializedException {
        if (!isInited)
            throw new NotInitializedException("init() method has to be invoked first.");

        if (InstrumentBank.getIsLoaded(instrumentNum)) {
            byte[] program = InstrumentBank.instruments[instrumentNum];
            native_setProgram2(ch, program);
        }
    }

    public static void readSound(short[] soundBuffer, int nsamples) throws NotInitializedException {
        if (!isInited)
            throw new NotInitializedException("init() method has to be invoked first.");

        native_mixAll(soundBuffer, nsamples);
    }

    private static double calcMidiFreq(int midiNoteNum) {
        return (440.0 / 32) * Math.pow(2, (midiNoteNum - 9) / 12.0);
    }
    private static int calcMidiOctave(int midiNoteNum) {
        return (midiNoteNum - 12) / 12;
    }
    private static int calcOPNFNum(double noteFrequency, int block) {
        // Формула взята из 24 страницы документации по YM2608
        double fnumDouble = (144 * noteFrequency * Math.pow(2, 20) / masterClock) / Math.pow(2, block - 1);
        int fnum = (int)Math.floor(fnumDouble);

        //fnum &= 0b0000011111111111; // Отрезаем всё, что не входит в FNUM

        return fnum;
    }

    // Методы для обращения к нативному коду
    private static native void native_initSynths(int clock, int rate);
    private static native void native_destructSynths();
    private static native void native_setProgram2(int ch, byte[] program);
    private static native void native_attackKey(int channel, int midiNoteNum, int fnum, int block, int pmdVelocity);
    private static native void native_releaseKey(int channel, int midiNoteNum);
    private static native void native_releaseAllkeys();
    private static native void native_mixAll(short[] buffer, int nsamples);
}
