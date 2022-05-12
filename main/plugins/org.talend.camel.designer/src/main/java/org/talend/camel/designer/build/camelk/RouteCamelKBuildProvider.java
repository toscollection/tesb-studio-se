// ============================================================================
//
// Copyright (C) 2006-2021 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.camel.designer.build.camelk;

import java.util.Collections;
import java.util.Map;

import org.talend.camel.core.model.camelProperties.CamelProcessItem;
import org.talend.core.model.process.IProcess2;
import org.talend.core.model.properties.Property;
import org.talend.core.model.properties.ProcessItem;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.runtime.process.IBuildJobHandler;
import org.talend.core.runtime.repository.build.IBuildExportHandler;
import org.talend.core.runtime.repository.build.RepositoryObjectTypeBuildProvider;
import org.talend.repository.ui.wizards.exportjob.scriptsmanager.JobScriptsManager.ExportChoice;

/**
 * DOC xldai class global comment. Detailled comment <br/>
 */
public class RouteCamelKBuildProvider extends RepositoryObjectTypeBuildProvider {

    /*
     * (non-Javadoc)
     *
     * @see org.talend.core.runtime.repository.build.AbstractBuildProvider#createBuildExportHandler(java.util.Map)
     */
    @Override
    public IBuildExportHandler createBuildExportHandler(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return null;
        }
        final Object item = parameters.get(ITEM);
        if (item == null || !(item instanceof ProcessItem)) {
            return null;
        }
        final Object version = parameters.get(VERSION);
        if (version == null) {
            return null;
        }
        final Object contextGroup = parameters.get(CONTEXT_GROUP);
        if (contextGroup == null) {
            return null;
        }
        Object choiceOption = parameters.get(CHOICE_OPTION);
        if (choiceOption == null) {
            choiceOption = Collections.emptyMap();
        }
        if (!(choiceOption instanceof Map)) {
            return null;
        }
        IBuildJobHandler buildHandler = new BuildCamelKHandler((ProcessItem) item, version.toString(),
                contextGroup.toString(), (Map<ExportChoice, Object>) choiceOption);
        return buildHandler;
    }

    /*
     * @see org.talend.core.runtime.repository.build.RepositoryObjectTypeBuildProvider#valid(java.util.Map)
     */
    @Override
    public boolean valid(Map<String, Object> parameters) {
        ERepositoryObjectType type = null;

        Object object = parameters.get(PROCESS);
        if (object != null && object instanceof IProcess2) {
            Property property = ((IProcess2) object).getProperty();
            if (property != null) {
                type = ERepositoryObjectType.getType(property);
            }
        } else {
            object = parameters.get(ITEM);
            if (object != null && object instanceof CamelProcessItem) {
                Property property = ((CamelProcessItem) object).getProperty();
                if (property != null) {
                    type = ERepositoryObjectType.getType(property);
                }
            }
        }

        if (ERepositoryObjectType.PROCESS_ROUTE.equals(type)) {
            return true;
        } else {
            return false;
        }

    }

    /*
     * @see org.talend.core.runtime.repository.build.RepositoryObjectTypeBuildProvider#getObjectType()
     */
    @Override
    protected ERepositoryObjectType getObjectType() {
        return ERepositoryObjectType.PROCESS_ROUTE_CAMELK;
    }
}
