#include "cmath"
#include "PolyphonedSynth.h"

PolyphonedSynth::PolyphonedSynth(int clock, int rate) {
    _synthsTotal = ceil(POLYPHONY / 3.0);
    _OPNSynths = new FM::OPN[_synthsTotal];

    for (int i = 0; i < _synthsTotal; i++) {
        if (!_OPNSynths[i].Init(clock / 2, rate, false)) //4 - оптимально для всех инструментов (но долгое затухание)
            throw;
        _OPNSynths[i].Reset();
        _OPNSynths[i].SetVolumeFM(-36); // Пока хватает. Потом увеличим.
    }

    setDefProgram();
}
PolyphonedSynth::~PolyphonedSynth() {
    delete[] _OPNSynths;
}

void PolyphonedSynth::setDefProgram() {
/*
 @0 4 6
 27  4  0  6 15  35 1  3 3 0
 31  7  0  7 15   0 2  1 4 0
 31  7  0  4 14  41 1 14 7 0
 31  8  0  7 15   0 0  4 7 0
 */
    unsigned char defaultProgram[42] = {4, 6,
                                        27, 4, 0, 6, 15, 35, 1,  3, 3, 0,
                                        31, 7, 0, 7, 15,  0, 2,  1, 4, 0,
                                        31, 7, 0, 4, 14, 41, 1, 14, 7, 0,
                                        31, 8, 0, 7, 15,  0, 0,  4, 7, 0};

    // Ставим программу на все каналы
    for (int i = 0; i < 16; i++) {
        for (int j = 0; j < 42; j++)
            _channelPrograms[i][j] = defaultProgram[j];
    }

    // Ставим программу на все голоса
    for (int i = 0; i < POLYPHONY; i++)
        setVoiceProgram(i, defaultProgram);
}
void PolyphonedSynth::setChannelProgram(uint8 channel, unsigned char *program) {
     // Обновляем программу на канале
     for (int i = 0; i < 42; i++)
         _channelPrograms[channel][i] = program[i];

     // Ищем голоса, играющие на определённом канале
     for (int i = 0; i < POLYPHONY; i++) {
         if (_channelsOnVoices[i] == channel)
             setVoiceProgram(i, program);
     }
}
void PolyphonedSynth::setVoiceProgram(int voice, unsigned char *program) {
    int synthNum = voice / 3;
    int voiceNum = voice % 3;

    FM::OPN &synth = _OPNSynths[synthNum];
    setAlg(synth, voiceNum, program[0], program[1]);

    synth.SetVoiceOpEnvel(voiceNum, 0,
                          program[2], program[3], program[4], program[5], program[6], program[7]);
    synth.SetVoiceOpOther(voiceNum, 0, program[8], program[9], program[10], program[11]);

    synth.SetVoiceOpEnvel(voiceNum, 1,
                          program[12], program[13], program[14], program[15], program[16], program[17]);
    synth.SetVoiceOpOther(voiceNum, 1, program[18], program[19], program[20], program[21]);

    synth.SetVoiceOpEnvel(voiceNum, 2,
                          program[22], program[23], program[24], program[25], program[26], program[27]);
    synth.SetVoiceOpOther(voiceNum, 2, program[28], program[29], program[30], program[31]);

    synth.SetVoiceOpEnvel(voiceNum, 3,
                          program[32], program[33], program[34], program[35], program[36], program[37]);
    synth.SetVoiceOpOther(voiceNum, 3, program[38], program[39], program[40], program[41]);
}
void PolyphonedSynth::setAlg(FM::OPN &synth, uint8 voice, uint8 connection, uint8 feedback) {
    uint8 addr = 0xB0 + voice;
    uint8 data = (feedback << 3) | connection;

    synth.SetReg(addr, data);
}

// Для управления синтезаторами после инициализации
//-------------------------------------------------
void PolyphonedSynth::attackKey(uint8 channel, uint8 midiNoteNum, uint16 fnum, uint8 block, uint8 pmdVelocity) {
    // Ищем свободный голос:
    // 1. В том же канале с той же нотой
    // 2. Самый старый отпущенный
    // Если ничего не нашли, то пропустить

    int sameNoteVoiceIndx = -1;
    int oldestReleasedVoiceIndx = -1;
    int oldestVoiceAge = -1;
    for (int i = 0; i < POLYPHONY; i++) {
        if (_channelsOnVoices[i] == channel && _notesOnVoices[i] == midiNoteNum) {
            sameNoteVoiceIndx = i;
            break;
        }
        if (!_isVoicePlaying[i] &&
            _voiceLastReleaseCounter[i] > oldestVoiceAge) {
            oldestReleasedVoiceIndx = i;
            oldestVoiceAge = _voiceLastReleaseCounter[i];
        }
    }

    int targetVoiceIndx = -1;
    if (sameNoteVoiceIndx != -1)
        targetVoiceIndx = sameNoteVoiceIndx;
    else if (oldestReleasedVoiceIndx != -1)
        targetVoiceIndx = oldestReleasedVoiceIndx;
    else
        return;

    // Если на голосе должен будет измениться канал,
    // То стоит поменять на нём текущий инструмент
    if (channel != _channelsOnVoices[targetVoiceIndx]) {
        setVoiceProgram(targetVoiceIndx, _channelPrograms[channel]);
    }

    // Играем ноту на найденном голосе
    attackVoice(targetVoiceIndx, fnum, block, pmdVelocity);

    _channelsOnVoices[targetVoiceIndx] = channel;
    _notesOnVoices[targetVoiceIndx] = midiNoteNum;
    _isVoicePlaying[targetVoiceIndx] = true;
    _voiceLastReleaseCounter[targetVoiceIndx] = 0;

    // Обновляем счётчики для определения
    // самого старого отпущенного голоса
    for (int i = 0; i < POLYPHONY; i++) {
        if (!_isVoicePlaying[i])
            _voiceLastReleaseCounter[i]++;
    }
}
void PolyphonedSynth::attackVoice(int voice, uint16 fnum, uint8 block, uint8 pmdVelocity) {
    int synthIdx = voice / 3;
    int channelIdx = voice % 3;

    FM::OPN &synth = _OPNSynths[synthIdx];

    // @TODO: нет обработки слишком высоких нот (они режутся из-за block)
    uint8 fnumL = fnum & 0xFF;
    uint8 fnumH = (fnum >> 8) & 0b111;
    uint8 dataH = (block << 3) | fnumH;
    uint8 kON = 0xF0 | channelIdx;

    synth.SetVelocity(channelIdx, pmdVelocity);
    synth.SetReg(0xA4 + channelIdx, dataH);
    synth.SetReg(0xA0 + channelIdx, fnumL);
    synth.SetReg(0x28, kON);
}
void PolyphonedSynth::releaseKey(uint8 channel, uint8 midiNoteNum) {
    // Ищем голос, который играет данную ноту
    int targetVoiceIndx = -1;
    for (int i = 0; i < POLYPHONY; i++) {
        if (_channelsOnVoices[i] == channel && _notesOnVoices[i] == midiNoteNum) {
            targetVoiceIndx = i;
            break;
        }
    }

    if (targetVoiceIndx != -1) {
        // Отпускаем голос
        releaseVoice(targetVoiceIndx);
        _isVoicePlaying[targetVoiceIndx] = false;
        _voiceLastReleaseCounter[targetVoiceIndx] = 0;
    }
}
void PolyphonedSynth::releaseVoice(int voice) {
    _OPNSynths[voice / 3].SetReg(0x28, voice % 3);
}
void PolyphonedSynth::releaseAll() {
    for (int i = 0; i < POLYPHONY; i++) {
        releaseVoice(i);
        _isVoicePlaying[i] = false;
        _voiceLastReleaseCounter[i] = 0;
    }
}
//=================================================

// Для вывода звука с синтезаторов
//--------------------------------
void PolyphonedSynth::mix(FM::Sample *audioBuffer, int nsamples) {
    for (int i = 0; i < _synthsTotal; i++)
        _OPNSynths[i].Mix(audioBuffer, nsamples);
}
//================================