package com.example.union;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class OpeningActivity extends AppCompatActivity {
    boolean isLooping;
    private RelativeLayout lin;
    private ImageView start;
    private ImageButton clickBtn;
    private int btn_click;
    TextToSpeech textToSpeech;
    String origin, dest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opening);
        lin = findViewById(R.id.lin);
        start = findViewById(R.id.start);
        clickBtn = findViewById(R.id.click_btn);
        Handler handler = new Handler();
        int delayMillis = 2000;
        btn_click = 0;
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Locale russian = new Locale("ru", "RU");
                int result = textToSpeech.setLanguage(russian);
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(OpeningActivity.this, "language not supported",Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(OpeningActivity.this, "failed tts",Toast.LENGTH_SHORT).show();
            }
        });


        lin.setAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.move));

        Animation scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_animation);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                clickBtn.startAnimation(scaleAnimation);
                start.setVisibility(View.GONE);
                textToSpeech.speak(" Нажмите на экран и скажите пожалуйста адрес где вас забрать", TextToSpeech.QUEUE_FLUSH, null, null);


            }
        }, delayMillis);





    }



    public void voice_input(){
        Intent mic_intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mic_intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mic_intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU"); // Set the language to Russian
        mic_intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Speak Now");
        startActivityForResult(mic_intent,88);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 88 && resultCode == RESULT_OK){
            if(btn_click==0) origin = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0);
            else if(btn_click==1) dest = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0);
            read_output(btn_click);
        }
    }

    public void read_output(int status){
        switch (status){
            case 0:
                btn_click += 1;
                textToSpeech.speak("Нажмите на экран и скажите пожалуйста ваш адрес прибытия", TextToSpeech.QUEUE_FLUSH, null, null);
                break;
            case 1:
                btn_click += 1;
                textToSpeech.speak("Ваш адрес отправления " + origin+ ". Aдрес прибытия " + dest + ". Нажмите еще раз чтобы найти такси", TextToSpeech.QUEUE_FLUSH, null, null);
                break;

        }
    }

    public void onClicked(View view) {
        String text;
        switch (btn_click){
            case 0:
                voice_input();
                break;
            case 1:
                voice_input();
                break;
            case 2:
                text = "Такси найдено";
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
                Intent intent = new Intent(OpeningActivity.this, FaceRecognitionActivity.class);
                startActivity(intent);

                }


        }
    }

