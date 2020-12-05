package ru.nntu.lprclient;

import androidx.annotation.NonNull;

/**
 * Supported country codes enum.
 */
public enum CountryCodeEnum {
    USA(R.string.country_code_usa, "us"),
    EU(R.string.country_code_eu, "eu");

    /**
     * All-args constructor.
     *
     * @param textId  string resource id
     * @param apiCode country code used in API request
     */
    CountryCodeEnum(int textId, String apiCode) {
        this.textId = textId;
        this.apiCode = apiCode;
    }

    private final int textId;

    private final String apiCode;

    /**
     * Returns API request code
     *
     * @return country code used in API request
     */
    public String getApiCode() {
        return apiCode;
    }

    /**
     * Implementation of {@link Object#toString()} which returns text from resources.
     *
     * @return text from resources
     */
    @NonNull
    @Override
    public String toString() {
        return LprApplication.getAppContext().getString(textId);
    }
}
