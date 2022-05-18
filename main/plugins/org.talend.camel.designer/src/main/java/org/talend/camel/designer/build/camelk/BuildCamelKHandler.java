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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.talend.camel.core.model.camelProperties.CamelProcessItem;
import org.talend.camel.core.model.camelProperties.impl.CamelProcessItemImpl;
import org.talend.commons.CommonsPlugin;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.utils.io.FilesUtils;
import org.talend.core.model.properties.ProcessItem;
import org.talend.core.model.properties.Property;
import org.talend.core.model.utils.JavaResourcesHelper;
import org.talend.designer.core.model.utils.emf.talendfile.ElementParameterType;
import org.talend.designer.core.model.utils.emf.talendfile.NodeType;
import org.talend.designer.core.model.utils.emf.talendfile.ProcessType;
import org.talend.designer.maven.model.MavenSystemFolders;
import org.talend.designer.maven.model.TalendMavenConstants;
import org.talend.designer.maven.utils.PomIdsHelper;
import org.talend.designer.runprocess.ProcessorUtilities;
import org.talend.repository.ProjectManager;
import org.talend.repository.ui.wizards.exportjob.handler.BuildJobHandler;
import org.talend.repository.ui.wizards.exportjob.scriptsmanager.JobScriptsManager.ExportChoice;
import org.talend.repository.utils.ZipFileUtils;

/**
 * created by xldai Detailled comment
 *
 */
public class BuildCamelKHandler extends BuildJobHandler {

    private CamelProcessItemImpl camelProcessItem;

    /**
     * (non-Javadoc)
     *
     * @param processItem
     * @param version
     * @param contextName
     * @param exportChoiceMap
     */
    public BuildCamelKHandler(ProcessItem processItem, String version, String contextName,
            Map<ExportChoice, Object> exportChoiceMap) {

        super(processItem, version, contextName, exportChoiceMap);

        if (processItem instanceof CamelProcessItemImpl) {
            this.camelProcessItem = (CamelProcessItemImpl) processItem;
        }

        IFolder resFolder = talendProcessJavaProject.getProject().getFolder(MavenSystemFolders.RESOURCES.getPath());

        IFolder configFolder = resFolder.getFolder("config");

        try {
            if (!configFolder.isSynchronized(IResource.DEPTH_ONE)) {
                configFolder.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
            }

            if (configFolder.exists()) {
                configFolder.delete(false, new NullProgressMonitor());
                configFolder.create(false, true, new NullProgressMonitor());
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see org.talend.repository.ui.wizards.exportjob.handler.AbstractBuildJobHandler#prepare(org.eclipse.core.runtime.
     * IProgressMonitor, java.util.Map)
     */
    @Override
    public void prepare(IProgressMonitor monitor, Map<String, Object> parameters) throws Exception {
        try {
            IFolder javaFolder = talendProcessJavaProject.getProject().getFolder(MavenSystemFolders.JAVA.getPath());

            if (javaFolder.exists()) {
                javaFolder.delete(false, new NullProgressMonitor());
                javaFolder.create(false, true, new NullProgressMonitor());
            }

        } catch (Exception e) {
            // throw new InvocationTargetException(e);
        }
        super.prepare(monitor, parameters);
    }

    @Override
    public IFile getJobTargetFile() {
        try {
            Property jobProperty = processItem.getProperty();
	    	String projectName = ProjectManager.getInstance().getCurrentProject().getTechnicalLabel().toLowerCase();
	    	String jobName = jobProperty.getLabel();
	    	String jobVersion = jobProperty.getVersion();

            IFolder srcFolder = talendProcessJavaProject.getSrcFolder();
            IFile jobFile = srcFolder.getFile(projectName + "/" + jobName.toLowerCase() + "_0_1" + "/" + jobName + ".java");

            File groovyFile = new File(talendProcessJavaProject.getTempFolder().getLocation().toString() + "/" + jobName + ".groovy");
            FilesUtils.copyFile(jobFile.getLocation().toFile(), groovyFile);

            File scriptFile = new File(talendProcessJavaProject.getTempFolder().getLocation().toString() + "/" + jobName + ".sh");
            FileOutputStream fos = new FileOutputStream(scriptFile);
            String scriptContent = "./kamel run " + addComponentsDepends(camelProcessItem) + jobName + ".groovy" + " --dev";
            fos.write(scriptContent.getBytes());
            fos.close();

            File[] outputFiles = new File[2];
            outputFiles[0] = groovyFile;
            outputFiles[1] = scriptFile;

            IFile zipFile = talendProcessJavaProject.getTempFolder().getFile(jobName + ".zip");
            ZipFileUtils.zip(outputFiles, zipFile.getLocation().toString());

            return talendProcessJavaProject.getTempFolder().getFile(jobName + ".zip");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    private String addComponentsDepends(CamelProcessItemImpl camelProcessItem){
        StringBuffer dependsBuffer = new StringBuffer();

        ProcessType processType = camelProcessItem.getProcess();

        for (Object o : processType.getNode()) {
            if (o instanceof NodeType) {
                NodeType currentNode = (NodeType) o;
                String componentName = currentNode.getComponentName();
                if (componentName.equals("cSOAP") || componentName.equals("cREST")) {
                    dependsBuffer.append("-d camel-cxf");
                }
                if (componentName.equals("cMessagingEndpoint")) {
                    for (Object e : currentNode.getElementParameter()) {
                        ElementParameterType p = (ElementParameterType) e;
                        if ("URI".equals(p.getName())) {
                            if (p.getValue() != null && p.getValue().startsWith("\"aws2-s3")) {
                                dependsBuffer.append("-d camel-aws2-s3");
                            }
                            if (p.getValue() != null && p.getValue().startsWith("\"aws2-sqs")) {
                                dependsBuffer.append("-d camel-aws2-sqs");
                            }
                            if (p.getValue() != null && p.getValue().startsWith("\"aws2-sns")) {
                                dependsBuffer.append("-d camel-aws2-sns");
                            }
                            if (p.getValue() != null && p.getValue().startsWith("\"aws2-ses")) {
                                dependsBuffer.append("-d camel-aws2-ses");
                            }
                        }
                    }
                }

            }
        }

        if (dependsBuffer.length() <= 0) {
            return "";
        } else {
            return dependsBuffer.toString() + " ";
        }
    }

}
