package com.robustcode.delivery.delivery.dto;

/**
 * Lenient phone validation: Belgian national ("0470 12 34 56", "02/123.45.67")
 * or international ("+32 470 12 34 56", "0032470123456") formats.
 * 8 to 15 digits, optional leading "+", separators: space . / - ( ).
 */
final class PhoneNumbers {

    static final String PATTERN = "^\\+?(?:[\\s./()-]*\\d){8,15}[\\s./()-]*$";

    static final String MESSAGE =
            "Customer phone must be a valid phone number (8-15 digits, e.g. +32 470 12 34 56 or 0470 12 34 56)";

    private PhoneNumbers() {
    }
}
