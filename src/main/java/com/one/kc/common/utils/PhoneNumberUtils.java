package com.one.kc.common.utils;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;

public final class PhoneNumberUtils {

    private static final PhoneNumberUtil phoneUtil =
            PhoneNumberUtil.getInstance();

    private PhoneNumberUtils() {}

    public static String toE164(String countryCode, String phoneNumber) {
        try {
            // countryCode example: "+91"
            String region = phoneUtil.getRegionCodeForCountryCode(
                    Integer.parseInt(countryCode.replace("+", ""))
            );

            Phonenumber.PhoneNumber number =
                    phoneUtil.parse(phoneNumber, region);

            if (!phoneUtil.isValidNumber(number)) {
                throw new IllegalArgumentException("Invalid phone number");
            }

            return phoneUtil.format(number, PhoneNumberFormat.E164);

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid phone number", e);
        }
    }
}

