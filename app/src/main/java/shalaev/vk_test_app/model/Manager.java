package shalaev.vk_test_app.model;

import android.content.Context;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Observable;

public abstract class Manager extends Observable {
    private final Context context;
    private ArrayList<JSONObject> data;

    protected Manager(final Context context) {this.context = context;}

    public void request(final boolean force) {
        if (force) {
            loadData();
        } else {
            if (null != data) {
                deliverData(data);
            } else {
                loadData();
            }
        }
    }

    protected abstract void loadData();

    protected void deliverData(final ArrayList<JSONObject> data) {
        this.data = data;
        setChanged();
        notifyObservers(data);
    }

    protected void deliverError(final String error) {
        setChanged();
        notifyObservers(error);
    }
}
