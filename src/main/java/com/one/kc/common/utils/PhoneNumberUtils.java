package com.one.kc.common.utils;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.one.kc.common.exceptions.UserFacingException;

public final class PhoneNumberUtils {

    private static final PhoneNumberUtil phoneUtil =
            PhoneNumberUtil.getInstance();

    public record PhoneParts(String countryCode, String phoneNumber) {}

    public static PhoneParts fromE164(String e164Number) {
        try {
            Phonenumber.PhoneNumber number =
                    phoneUtil.parse(e164Number, null);

            if (!phoneUtil.isValidNumber(number)) {
                throw new IllegalArgumentException("Invalid E.164 phone number");
            }

            String countryCode = "+" + number.getCountryCode();
            String phoneNumber = String.valueOf(number.getNationalNumber());

            return new PhoneParts(countryCode, phoneNumber);

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid E.164 phone number", e);
        }
    }



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
                throw new UserFacingException("Invalid phone number"+number);
            }

            return phoneUtil.format(number, PhoneNumberFormat.E164);

        } catch (Exception e) {
            throw new UserFacingException("Invalid phone number");
        }
    }
}

