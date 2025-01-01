package com.nocomake.serialremote;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import DiyRemote.R;

public class HelpActivity extends FullscreenActivityBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help_activity);

        ImageButton openUrlButton = findViewById(R.id.buttonToGithub);
        openUrlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(getResources().getString(R.string.receiverCodeURL)));
                startActivity(intent);
            }
        });
    }
}
