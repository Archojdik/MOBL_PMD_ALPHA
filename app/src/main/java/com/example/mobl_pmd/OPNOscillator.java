package com.example.mobl_pmd;

public class OPNOscillator {
    private static final double MAX_PHASE = 6.28;

    public boolean isActive = false;

    private int sampleRate = 48000;
    private double frequency = 440;
    private double volume = 0.08; // Никогда не допускать >1 (иначе - перегруз)
    private double phase = 0;

    public void readNextWave(short[] destArray, int offset, int sampleCounts) {
        // Сколько семплов будет длиться фаза
        double phaseDuration = sampleRate / frequency;

        // Шаг фазы: полная фаза / кол-во шагов
        double phaseStep = MAX_PHASE / phaseDuration;

        for (int i = 0; i < sampleCounts; i++) {
            double pulseCode = Math.sin(phase) * volume * Short.MAX_VALUE;
            destArray[offset + i] = (short)Math.floor(pulseCode);

            phase += phaseStep;
            if (phase >= MAX_PHASE)
                phase -= MAX_PHASE;
        }
    }
    public void setSampleRate(int newSampleRate) {
        this.sampleRate = newSampleRate;
    }
    public void setFrequency(double frequency) {
        this.frequency = frequency;
    }
    public void setVolume(double volume) {
        // @TODO: Ограничение на максимальную громкость
        this.volume = volume;
    }
    public void reset() {
        isActive = false;
        phase = 0;
    }
}
