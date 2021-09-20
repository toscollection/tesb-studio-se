package org.talend.camel.designer.migration;

import java.util.Date;
import java.util.GregorianCalendar;

import org.eclipse.emf.common.util.EList;
import org.talend.commons.exception.PersistenceException;
import org.talend.designer.core.model.utils.emf.talendfile.ElementParameterType;
import org.talend.designer.core.model.utils.emf.talendfile.ElementValueType;
import org.talend.designer.core.model.utils.emf.talendfile.NodeType;
import org.talend.migration.MigrationReportRecorder;

public class RemoveUnsupportedCompMigrationTask extends AbstractRouteItemComponentMigrationTask {

    @Override
    public String getComponentNameRegex() {
        return "cMessagingEndpoint";
    }

    public Date getOrder() {
        GregorianCalendar gc = new GregorianCalendar(2021, 9, 8, 13, 0, 0);
        return gc.getTime();
    }

	@Override
	protected boolean execute(NodeType node) throws Exception {
		return removeUnsupportedComp(node);
	}

	private boolean removeUnsupportedComp(NodeType currentNode) throws PersistenceException {
        boolean needSave = false;

        for (Object e : currentNode.getElementParameter()) {
            ElementParameterType p = (ElementParameterType) e;
            if ("HOTLIBS".equals(p.getName())) {
                EList<?> elementValue = p.getElementValue();
                for (Object pv : elementValue) {
                    ElementValueType evt = (ElementValueType) pv;
                    String evtValue = evt.getValue();
                    if(null != evtValue && evtValue.contains("AZURE")) {
                        evt.setValue("");
                        elementValue.remove(evt);

                        generateReportRecord(new MigrationReportRecorder(this,
                                MigrationReportRecorder.MigrationOperationType.MODIFY, getRouteItem(), currentNode, "MODULE_LIST of cMessagingEndpoint",
                                evtValue, ""));
                        needSave = true;
                    }
                }
                break;
            }
        }

        return needSave;
    }

}
