/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.exceptions.base;

import org.mockito.configuration.IMockitoConfiguration;
import org.mockito.internal.configuration.GlobalConfiguration;

import java.io.Serializable;

public class ConditionalStackTraceFilter implements Serializable {
    static final long serialVersionUID = -8085849703510292641L;
    
    private IMockitoConfiguration config = new GlobalConfiguration();
    private StackTraceFilter filter = new StackTraceFilter();
    
    public void filter(Throwable throwable) {
        if (!config.cleansStackTrace()) {
            return;
        }
        StackTraceElement[] filtered = filter.filter(throwable.getStackTrace(), true);
        throwable.setStackTrace(filtered);
    }
}