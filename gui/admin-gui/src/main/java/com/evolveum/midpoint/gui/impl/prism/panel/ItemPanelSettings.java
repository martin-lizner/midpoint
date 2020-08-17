/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import java.io.Serializable;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemEditabilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemMandatoryHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;

public class ItemPanelSettings implements Serializable {

    private ItemVisibilityHandler visibilityHandler;
    private ItemEditabilityHandler editabilityHandler = wrapper -> true;
    private boolean headerVisible = true;
    private ItemMandatoryHandler mandatoryHandler;

    ItemPanelSettings() {

    }

    public ItemVisibilityHandler getVisibilityHandler() {
        return visibilityHandler;
    }

    void setVisibilityHandler(ItemVisibilityHandler visibilityHandler) {
        this.visibilityHandler = visibilityHandler;
    }

    public ItemEditabilityHandler getEditabilityHandler() {
        return editabilityHandler;
    }

    public void setEditabilityHandler(ItemEditabilityHandler editabilityHandler) {
        this.editabilityHandler = editabilityHandler;
    }

    public boolean isHeaderVisible() {
        return headerVisible;
    }

    void setHeaderVisible(boolean headerVisible) {
        this.headerVisible = headerVisible;
    }

    public ItemMandatoryHandler getMandatoryHandler() {
        return mandatoryHandler;
    }

    public void setMandatoryHandler(ItemMandatoryHandler mandatoryHandler) {
        this.mandatoryHandler = mandatoryHandler;
    }

    public ItemPanelSettings copy() {
        return new ItemPanelSettingsBuilder()
                .editabilityHandler(editabilityHandler)
                .visibilityHandler(visibilityHandler)
                .mandatoryHandler(mandatoryHandler)
                .build();
    }
}
