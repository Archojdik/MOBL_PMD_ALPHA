package com.example.mobl_pmd;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ChannelVisualFragment extends Fragment {
    private ChannelVisualHandler handler;
    private int chNum;
    private boolean initialized;

    private TextView channelName;
    private TextView knField;
    private TextView tnView;
    private PianoVisualView pianoVisual;

    private int currentKey = -1;
    private int currentTN = 101;

    public ChannelVisualFragment() {
        // Required empty public constructor
        super(R.layout.fragment_channel_visual);

        // Получаем номер канала и запоминаем его.
        chNum = PlayerActivity.getStaticChannelFragmentChNum();
        handler = new ChannelVisualHandler();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        channelName = view.findViewById(R.id.fragment_channel_name);
        pianoVisual = view.findViewById(R.id.piano_view);
        knField = view.findViewById(R.id.kn_field);
        tnView = view.findViewById(R.id.tn_field);

        channelName.setText("FM" + chNum);
        channelName.setTextColor(Color.rgb(0x66, 0x88, 0xFF));

        initialized = true;

        // Передаём созданный обработчик в визуализатор
        MidiPlayerFast.VisualizingHandler.addFragmentHandler(chNum-1, handler);
    }

    public class ChannelVisualHandler extends Handler {
        /// В arg2 передаётся номер клавиши в midi формате
        public static final int ARG1_ATTACK = 1;
        /// В arg2 передаётся номер клавиши в midi формате
        public static final int ARG1_RELEASE = 2;
        public static final int ARG1_REDRAW = 3;
        public static final int ARG1_SET_TN = 4;

        @Override
        public void handleMessage(@NonNull Message msg) {
            if (!initialized)
                return;

            final String[] keyNames = {"C", "C+", "D", "D+", "E", "F", "F+", "G", "G+", "A", "A+", "B"};

            switch (msg.arg1)
            {
                // @TODO: По ощущениям, после включения визуализаторы очень медленные.
                // После паузы такое ощёщение почему-то пропадает
                // Есть вероятность, что это так выглядит только на эмуляторе
                case ARG1_ATTACK:
                    int akey = msg.arg2 - 12;
                    pianoVisual.setKeyPressed(akey, true);
                    currentKey = akey;
                    break;
                case ARG1_RELEASE:
                    int rkey = msg.arg2 - 12;
                    pianoVisual.setKeyPressed(rkey, false);
                    currentKey = -1;
                    break;
                case ARG1_REDRAW:
                    pianoVisual.requestRedraw();

                    if (currentKey == -1)
                        knField.setText(" R ");
                    else {
                        int oct = currentKey / 12;
                        int knm = currentKey % 12;
                        knField.setText("o" + oct + keyNames[knm]);
                    }

                    StringBuilder tnStr = new StringBuilder();
                    tnStr.append(currentTN);
                    while (tnStr.length() < 3)
                        tnStr.insert(0, '0');
                    tnView.setText(tnStr.toString());
                    break;
                case ARG1_SET_TN:
                    currentTN = msg.arg2;
                    break;
            }
            super.handleMessage(msg);
        }
    }
}