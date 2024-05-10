//
// Created by True Administrator on 26.04.2024.
//

#ifndef MOBL_PMD_POLYPHONEDSYNTH_H
#define MOBL_PMD_POLYPHONEDSYNTH_H


#include "fmgen/opna.h"

class PolyphonedSynth {
private:
    unsigned char _channelPrograms[16][42]; // Хранит установленные программы для каждого Midi-канала

    static const int POLYPHONY = 12; // Надо добавить возможность изменять её

    bool _isVoicePlaying[POLYPHONY]; // Играет ли данный голос?
    unsigned char _channelsOnVoices[POLYPHONY]; // Хранит, за какие midi-каналы отвечает каждый голос (в данный момент)
    unsigned char _notesOnVoices[POLYPHONY]; // Хранит ноты, играемые голосами сейчас

    // Хранит, сколько "Атак" назад была отпущена клавиша на конкретном голосе.
    // Нужна для поиска самого старого голоса, который можно задействовать ещё раз.
    unsigned char _voiceLastReleaseCounter[POLYPHONY];

    // Для создания OPN синтезаторов, которые генерируют сам звук.
    // В каждом синтезаторе по 3 голоса.
    // В будущем надо переделать массив синтезаторов в массив голосов.
    int _synthsTotal;
    FM::OPN* _OPNSynths;

public:
    PolyphonedSynth(int clock, int rate);
    ~PolyphonedSynth();

    void setDefProgram();

    void setChannelProgram(uint8 channel, unsigned char program[42]);
    void attackKey(uint8 channel, uint8 midiNoteNum, uint16 fnum, uint8 block, uint8 pmdVelocity);
    void releaseKey(uint8 channel, uint8 midiNoteNum);
    void releaseAll();

    void mix(FM::Sample* audioBuffer, int nsamples);

private:
    void setAlg(FM::OPN &synth, uint8 voice, uint8 connection, uint8 feedback);
    void attackVoice(int voice, uint16 fnum, uint8 block, uint8 pmdVelocity);
    void releaseVoice(int voice);
    void setVoiceProgram(int voice, unsigned char program[42]);
};


#endif //MOBL_PMD_POLYPHONEDSYNTH_H
