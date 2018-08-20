package org.talend.camel.designer;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.talend.camel.core.model.camelProperties.BeanItem;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.core.model.general.ModuleNeeded;
import org.talend.core.model.properties.Item;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.designer.core.model.utils.emf.component.IMPORTType;
import org.talend.librariesmanager.model.ModulesNeededProvider;
import org.talend.repository.documentation.ERepositoryActionName;

/**
 * The activator class controls the plug-in life cycle
 */
public class CamelDesignerPlugin extends AbstractUIPlugin {

    public static final String BEAN_WIZ_ICON = "icons/bean_wiz.png"; //$NON-NLS-1$

    public static final String DEPEN_ICON = "icons/dependencies/dependencies.gif"; //$NON-NLS-1$
    public static final String IMPORT_PKG_ICON = "icons/dependencies/importPackage.gif"; //$NON-NLS-1$
    public static final String REQUIRE_BD_ICON = "icons/dependencies/requireBundle.gif"; //$NON-NLS-1$
    public static final String BUNDLE_CP_ICON = "icons/dependencies/bundleClass.gif"; //$NON-NLS-1$
    public static final String REFRESH_ICON = "icons/dependencies/refresh.gif"; //$NON-NLS-1$
    public static final String GRAY_REM_ICON = "icons/dependencies/gray_rem.gif"; //$NON-NLS-1$
    public static final String HIGHLIGHT_REM_ICON = "icons/dependencies/highlight_rem.gif"; //$NON-NLS-1$
    public static final String OPTIONAL_OVERLAY_ICON = "icons/dependencies/optional.gif"; //$NON-NLS-1$
    public static final String IMPORT_PACKAGE_OVERLAY_ICON = "IMPORT_PACKAGE_OVERLAY_ICON"; //$NON-NLS-1$
    public static final String REQUIRE_BUNDLE_OVERLAY_ICON = "REQUIRE_BUNDLE_OVERLAY_ICON"; //$NON-NLS-1$

    // The shared instance
    private static CamelDesignerPlugin plugin;

    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;

        ProxyRepositoryFactory.getInstance().addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent event) {
                String propertyName = event.getPropertyName();
                Object newValue = event.getNewValue();
                if (propertyName.equals(ERepositoryActionName.IMPORT.getName())) {
                    caseImport(propertyName, newValue);
                }
            }

        });
    }

    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     * 
     * @return the shared instance
     */
    public static CamelDesignerPlugin getDefault() {
        return plugin;
    }

    @Override
    protected void initializeImageRegistry(ImageRegistry reg) {
        super.initializeImageRegistry(reg);
        reg.put(BEAN_WIZ_ICON, createImageDescriptor(BEAN_WIZ_ICON).createImage());

        reg.put(DEPEN_ICON, createImageDescriptor(DEPEN_ICON).createImage());
        reg.put(BUNDLE_CP_ICON, createImageDescriptor(BUNDLE_CP_ICON).createImage());
        reg.put(REQUIRE_BD_ICON, createImageDescriptor(REQUIRE_BD_ICON).createImage());
        reg.put(IMPORT_PKG_ICON, createImageDescriptor(IMPORT_PKG_ICON).createImage());
        reg.put(REFRESH_ICON, createImageDescriptor(REFRESH_ICON).createImage());
        reg.put(GRAY_REM_ICON, createImageDescriptor(GRAY_REM_ICON).createImage());
        reg.put(HIGHLIGHT_REM_ICON, createImageDescriptor(HIGHLIGHT_REM_ICON).createImage());
        reg.put(OPTIONAL_OVERLAY_ICON, createImageDescriptor(OPTIONAL_OVERLAY_ICON).createImage());
        reg.put(IMPORT_PACKAGE_OVERLAY_ICON, getOptionalOverlayIcon(getImage(IMPORT_PKG_ICON)));
        reg.put(REQUIRE_BUNDLE_OVERLAY_ICON, getOptionalOverlayIcon(getImage(REQUIRE_BD_ICON)));
    }

    /**
     * Returns an image descriptor for the image file at the given plug-in relative path
     * 
     * @param path the path
     * @return the image descriptor
     */
    private static ImageDescriptor createImageDescriptor(String path) {
        return ImageDescriptor.createFromURL(FileLocator.find(getDefault().getBundle(), new Path(path), null));
    }

    private static Image getOptionalOverlayIcon(Image base) {
        DecorationOverlayIcon decorationOverlayIcon = new DecorationOverlayIcon(base,
            createImageDescriptor(OPTIONAL_OVERLAY_ICON), IDecoration.TOP_LEFT);
        return decorationOverlayIcon.createImage();
    }

    public static Image getImage(String path) {
        return getDefault().getImageRegistry().get(path);
    }

    public static ImageDescriptor getImageDescriptor(String path) {
        return getDefault().getImageRegistry().getDescriptor(path);
    }

    /**
     * DOC Update import bean libraries version with the studio inside versions.(TESB-23162)
     * 
     * @param propertyName
     * @param newValue
     */
    private void caseImport(String propertyName, Object newValue) {
        ProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
        if (newValue instanceof Set) {
            Set<Item> importItems = (Set<Item>) newValue;
            for (Item item : importItems) {
                if (item instanceof BeanItem) {
                    BeanItem beanItem = (BeanItem) item;
                    EList imports = beanItem.getImports();
                    List<ModuleNeeded> beansDefaultNeeds = ModulesNeededProvider.getModulesNeededForBeans();
                    for (ModuleNeeded defaultNeed : beansDefaultNeeds) {
                        String moduleName = defaultNeed.getModuleName().substring(0,
                                defaultNeed.getModuleName().lastIndexOf('-'));
                        for (Object imp : imports) {
                            if (imp instanceof IMPORTType) {
                                IMPORTType importType = (IMPORTType) imp;
                                String impName = importType.getMODULE().substring(0, importType.getMODULE().lastIndexOf('-'));
                                if (moduleName.equals(impName) && !importType.getMODULE().equals(defaultNeed.getModuleName())) {
                                    importType.setMODULE(defaultNeed.getModuleName());
                                    importType.setMESSAGE(defaultNeed.getInformationMsg());
                                    importType.setREQUIRED(defaultNeed.isRequired());
                                    importType.setMVN(defaultNeed.getMavenUri());
                                    try {
                                        factory.save(beanItem, true);
                                    } catch (PersistenceException e) {
                                        ExceptionHandler.process(e);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
