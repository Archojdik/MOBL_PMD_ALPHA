// ---------------------------------------------------------------------------
//	PSG-like sound generator
//	Copyright (C) cisc 1997, 1999.
// ---------------------------------------------------------------------------
//	$Id: psg.h,v 1.1 2001/04/23 22:25:35 kaoru-k Exp $

#ifndef PSG_H
#define PSG_H

#include "types.h"

#define PSG_SAMPLETYPE		int16		// int32 or int16

// ---------------------------------------------------------------------------
//	class PSG
//	PSG ���ɤ����������������벻����˥å�
//	
//	interface:
//	bool SetClock(uint clock, uint rate)
//		����������Υ��饹����Ѥ������ˤ��ʤ餺�Ƥ�Ǥ������ȡ�
//		PSG �Υ���å��� PCM �졼�Ȥ����ꤹ��
//
//		clock:	PSG ��ư���å�
//		rate:	�������� PCM �Υ졼��
//		retval	���������������� true
//
//	void Mix(Sample* dest, int nsamples)
//		PCM �� nsamples ʬ�������� dest �ǻϤޤ�����˲ä���(�û�����)
//		�����ޤǲû��ʤΤǡ��ǽ������򥼥��ꥢ����ɬ�פ�����
//	
//	void Reset()
//		�ꥻ�åȤ���
//
//	void SetReg(uint reg, uint8 data)
//		�쥸���� reg �� data ��񤭹���
//	
//	uint GetReg(uint reg)
//		�쥸���� reg �����Ƥ��ɤ߽Ф�
//	
//	void SetVolume(int db)
//		�Ʋ����β��̤�Ĵ�᤹��
//		ñ�̤��� 1/2 dB
//
class PSG
{
public:
	typedef PSG_SAMPLETYPE Sample;
	
	enum
	{
		noisetablesize = 1 << 10,	// ����������̤򸺤餷�����ʤ鸺�餷��
		toneshift = 24,
		envshift = 22,
		noiseshift = 14,
		oversampling = 2,		// �� �������®�٤�ͥ��ʤ鸺�餹�Ȥ�������
	};

public:
	PSG();
	~PSG();

	void Mix(Sample* dest, int nsamples);
	void SetClock(int clock, int rate);
	
	void SetVolume(int vol);
	void SetChannelMask(int c);
	
	void Reset();
	void SetReg(uint regnum, uint8 data);
	uint GetReg(uint regnum) { return reg[regnum & 0x0f]; }

protected:
	void MakeNoiseTable();
	void MakeEnvelopTable();
	static void StoreSample(Sample& dest, int32 data);
	
	uint8 reg[16];

	const uint* envelop;
	uint olevel[3];
	uint32 scount[3], speriod[3];
	uint32 ecount, eperiod;
	uint32 ncount, nperiod;
	uint32 tperiodbase;
	uint32 eperiodbase;
	uint32 nperiodbase;
	int volume;
	int mask;

	static uint enveloptable[16][64];
	static uint noisetable[noisetablesize];
	static int EmitTable[32];
};

#endif // PSG_H
