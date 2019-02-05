package com.company.demo.web;

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Property;

public interface GoogleApiConfig extends Config {

    @Property("google.apiKey")
    String getApiKey();
}