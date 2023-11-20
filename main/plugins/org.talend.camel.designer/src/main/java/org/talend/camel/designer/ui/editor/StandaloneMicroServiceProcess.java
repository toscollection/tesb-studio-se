package org.talend.camel.designer.ui.editor;

import java.util.Map;

import org.apache.commons.lang.ObjectUtils;
import org.talend.core.model.properties.Property;
import org.talend.core.runtime.process.LastGenerationInfo;

public class StandaloneMicroServiceProcess  extends RouteProcess {

    public StandaloneMicroServiceProcess(Property property) {
        super(property);
    }

    public boolean isEnableMetrics() {
        Map<String, Object> argumentsMap = LastGenerationInfo.getInstance().getLastMainJob().getProcessor().getArguments();

        return ObjectUtils.equals(argumentsMap.get("ESB_METRICS"), Boolean.TRUE);

    }

}
