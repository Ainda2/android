package mega.privacy.android.app.main.listeners;


import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;

import android.app.Activity;
import android.content.Context;

import androidx.recyclerview.widget.RecyclerView;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.di.DbHandlerModuleKt;
import mega.privacy.android.data.database.DatabaseHandler;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaError;
import timber.log.Timber;

public class ChatNonContactNameListener implements MegaChatRequestListenerInterface {

    Context context;
    RecyclerView.ViewHolder holder;
    RecyclerView.Adapter adapter;
    boolean isUserHandle;
    DatabaseHandler dbH;
    String firstName;
    String lastName;
    String mail;
    long userHandle;
    boolean receivedFirstName = false;
    boolean receivedLastName = false;
    boolean receivedEmail = false;
    MegaApiAndroid megaApi;
    boolean isPreview = false;
    int pos;

    public ChatNonContactNameListener(Context context, RecyclerView.ViewHolder holder, RecyclerView.Adapter adapter, long userHandle, boolean isPreview) {
        this.context = context;
        this.holder = holder;
        this.adapter = adapter;
        this.isUserHandle = true;
        this.userHandle = userHandle;
        this.isPreview = isPreview;

        dbH = DbHandlerModuleKt.getDbHandler();

        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
        }
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        Timber.d("onRequestFinish()");

        if (e.getErrorCode() == MegaError.API_OK) {
            if (adapter == null) {
                return;
            }

            if (request.getType() == MegaChatRequest.TYPE_GET_FIRSTNAME) {
                Timber.d("First name received");
                firstName = request.getText();
                receivedFirstName = true;
                if (!isTextEmpty(firstName)) {
                    dbH.setNonContactFirstName(firstName, request.getUserHandle() + "");
                }
            } else if (request.getType() == MegaChatRequest.TYPE_GET_LASTNAME) {
                Timber.d("Last name received");
                lastName = request.getText();
                receivedLastName = true;
                if (!isTextEmpty(lastName)) {
                    dbH.setNonContactLastName(lastName, request.getUserHandle() + "");
                }
            } else if (request.getType() == MegaChatRequest.TYPE_GET_EMAIL) {
                Timber.d("Email received");
                mail = request.getText();
                receivedEmail = true;
                if (!isTextEmpty(mail)) {
                    dbH.setNonContactEmail(mail, request.getUserHandle() + "");
                }
            }
        } else {
            Timber.e("ERROR: requesting: %s", request.getRequestString());
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }
}