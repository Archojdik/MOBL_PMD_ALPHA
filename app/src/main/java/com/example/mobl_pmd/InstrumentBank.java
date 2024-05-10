package com.example.mobl_pmd;

import android.util.Log;

import java.io.InputStream;
import java.util.Scanner;

public class InstrumentBank {
    public static byte[][] instruments = new byte[255][42];

    private static boolean[] instrumentLoaded = new boolean[255];
    private static boolean isLoaded = false;

    public static void load(InputStream instrumentListFile) {
        // Читаем все инструменты из файла с инструментами
        try {
            Scanner list = new Scanner(instrumentListFile);

            while (list.hasNext()) {
                if (list.next().equals("@"))
                    loadInstrument(list);
            }

            instrumentListFile.close();
        } catch (Exception ignored) {
            Log.e("Synth init", ignored.toString());
        }

        isLoaded = true;
    }

    public static boolean getIsLoaded() {
        return isLoaded;
    }
    public static boolean getIsLoaded(int instrumentNum) { return instrumentLoaded[instrumentNum]; }

    private static void loadInstrument(Scanner fileScanner) {
        int instrumentNumber = -1;

        for (int paramIdx = 0; paramIdx < 43; ) {
            try {
                byte value = Byte.parseByte(fileScanner.next());

                if (paramIdx == 0)
                    instrumentNumber = value;
                else
                    instruments[instrumentNumber][paramIdx - 1] = value;

                paramIdx++;
            } catch (NumberFormatException ignored) { }
        }

        instrumentLoaded[instrumentNumber] = true;
    }
}
