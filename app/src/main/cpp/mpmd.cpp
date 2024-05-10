//#include "fmgen/fmgen.h"
//#include "fmgen/opna.h"
//#include "MpmdChannel.h"
#include <jni.h>
#include <malloc.h>
#include "PolyphonedSynth.h"


//MpmdChannel *mainChannel = NULL;
//MpmdChannel **channelsArray; // = new MpmdChannel*[16];

PolyphonedSynth *polySynth;



// Для вывода звука с синтезаторов
//--------------------------------
int tempBufferSize = 480;
FM::Sample* _audioBuffer = new FM::Sample[tempBufferSize];
//================================

// Новая версия кода связи с JAVA частью
//--------------------------------------
extern "C"
JNIEXPORT void JNICALL
Java_com_example_mobl_1pmd_OPNSynthesizer_native_1initSynths(JNIEnv *env, jclass clazz,
                                                             jint clock, jint rate) {
    delete polySynth;
    polySynth = new PolyphonedSynth(clock, rate);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_mobl_1pmd_OPNSynthesizer_native_1attackKey(JNIEnv *env, jclass clazz,
                                                            jint ch, jint midi_note_num, jint fnum,
                                                            jint block, jint pmdVelocity) {
    polySynth->attackKey(ch, midi_note_num, fnum, block, pmdVelocity);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_mobl_1pmd_OPNSynthesizer_native_1releaseKey(JNIEnv *env, jclass clazz,
                                                             jint ch, jint midi_note_num) {
    polySynth->releaseKey(ch, midi_note_num);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_mobl_1pmd_OPNSynthesizer_native_1releaseAllkeys(JNIEnv *env, jclass clazz) {
    polySynth->releaseAll();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_mobl_1pmd_OPNSynthesizer_native_1mixAll(JNIEnv *env, jclass clazz,
                                                         jshortArray buffer, jint nsamples) {
    // Создаём временные буферы (Только если размер предыдущих буферов меньше требуемого)
    if (tempBufferSize < nsamples) {
        tempBufferSize = nsamples;
        delete[] _audioBuffer;
        _audioBuffer = new FM::Sample[tempBufferSize];
    }
    // Очистка буферов
    for (int i = 0; i < nsamples; i++)
        _audioBuffer[i] = 0;

    polySynth->mix(_audioBuffer, nsamples);
    env->SetShortArrayRegion(buffer, 0, nsamples, _audioBuffer);
}
//======================================
extern "C"
JNIEXPORT void JNICALL
Java_com_example_mobl_1pmd_OPNSynthesizer_native_1destructSynths(JNIEnv *env, jclass clazz) {
    delete polySynth;
    delete[] _audioBuffer;
    tempBufferSize = 0;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_mobl_1pmd_OPNSynthesizer_native_1setProgram2(JNIEnv *env, jclass clazz, jint ch,
                                                              jbyteArray program) {
    jboolean successfull;
    jbyte* signedProgram = env->GetByteArrayElements(program, &successfull);

    // Переводим signed char в unsigned char......
    unsigned char unsignedProgram[42];
    for (int i = 0; i < 42; i++)
        unsignedProgram[i] = (unsigned char)signedProgram[i];

    if (successfull == true)
        polySynth->setChannelProgram(ch, unsignedProgram);
}