/*
 * Copyright (c) 2012 Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.scenicview.update;

import org.fxconnector.helper.WindowChecker;
import org.fxconnector.AppController;
import org.fxconnector.StageControllerImpl;
import org.fxconnector.AppControllerImpl;
import org.fxconnector.StageController;
import org.fxconnector.ConnectorUtils;
import java.util.*;

import javafx.stage.*;

import org.fxconnector.helper.WindowChecker.WindowFilter;

public class LocalVMUpdateStrategy extends CommonUpdateStrategy implements WindowFilter {

    public LocalVMUpdateStrategy() {
        super(LocalVMUpdateStrategy.class.getName());
    }

    @Override List<AppController> getActiveApps() {
        final AppController local = new AppControllerImpl();
        final List<Window> stages = WindowChecker.getValidWindows(this);
        for (int i = 0; i < stages.size(); i++) {
            final StageController sc = new StageControllerImpl((Stage) stages.get(i), local);
            local.getStages().add(sc);
        }
        final List<AppController> controllers = new ArrayList<AppController>(1);
        if (!local.getStages().isEmpty())
            controllers.add(local);
        return controllers;
    }

    @Override public boolean accept(final Window window) {
        if (window instanceof Stage) {
            return ConnectorUtils.acceptWindow(window);
        } else {
            return false;
        }

    }

}
