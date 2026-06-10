package com.delivery.user.util;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
public class MessageCommon {

    private final MessageSource messageSource;

    public MessageCommon(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String getMessage(String code, Object... args) {
        try {
            // Automatically uses the user's Locale (e.g., from the Accept-Language header)
            return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
        } catch (Exception e) {
            return code; // If no translation exists, return the raw code key
        }
    }
}