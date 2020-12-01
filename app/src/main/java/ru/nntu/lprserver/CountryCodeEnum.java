package ru.nntu.lprserver;

import androidx.annotation.NonNull;

public enum CountryCodeEnum {
    USA(R.string.country_code_usa, "us"),
    EU(R.string.country_code_eu, "eu");

    CountryCodeEnum(int textId, String apiCode) {
        this.textId = textId;
        this.apiCode = apiCode;
    }

    private int textId;

    private String apiCode;

    public String getApiCode() {
        return apiCode;
    }

    @NonNull
    @Override
    public String toString() {
        return LprApplication.getAppContext().getString(textId);
    }
}
